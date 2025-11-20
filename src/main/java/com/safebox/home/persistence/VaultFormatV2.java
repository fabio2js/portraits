package com.safebox.home.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safebox.home.crypto.CryptoService;
import com.safebox.home.model.Vault;

import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;
import javax.crypto.SecretKey;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

public final class VaultFormatV2 {


    public static byte[] readSaltOnly(Path vaultPath) throws Exception {
        try (DataInputStream in = new DataInputStream(Files.newInputStream(vaultPath))) {

            // Formato V2 Header:
            //  [0-3]  magic bytes
            //  [4]    version
            //  [5-20] salt (16 bytes)
            //  [21-36] hmac (16 bytes)
            //  [....] payload cifrato

            byte[] magic = new byte[4];
            in.readFully(magic);

            byte version = in.readByte();
            if (version != 2) {
                throw new SecurityException("Formato vault V2 richiesto");
            }

            byte[] salt = new byte[16];
            in.readFully(salt);

            // ignoriamo HMAC e payload
            return salt;
        }
    }


    // Magic "SFBX"
    private static final byte[] MAGIC = new byte[] { 'S', 'F', 'B', 'X' };

    // Versione del formato
    private static final byte VERSION = 0x02;

    // Flags (per ora inutilizzati)
    private static final byte FLAGS_NONE = 0x00;

    // KDF
    private static final byte KDF_ALGO_PBKDF2_HMAC_SHA512 = 0x01;

    // Cifratura
    private static final byte ENC_ALGO_AES_256_GCM = 0x01;

    // HMAC
    private static final String HMAC_ALGO = "HmacSHA256";
    private static final int HMAC_SIZE = 32; // SHA-256 → 32 byte

    private static final int SALT_LEN = 16;
    private static final int IV_LEN   = 12;

    private final CryptoService crypto;
    private final ObjectMapper mapper;

    public VaultFormatV2(CryptoService crypto, ObjectMapper mapper) {
        this.crypto = crypto;
        this.mapper = mapper;
    }

    /**
     * Scrive il vault in formato SafeBox v2:
     *
     * MAGIC(4) | VERSION(1) | FLAGS(1)
     * KDF_ALGO(1) | KDF_SALT_LEN(1) | KDF_SALT | KDF_ITER(4 big-endian)
     * ENC_ALGO(1) | IV_LEN(1) | IV
     * CIPHERTEXT...
     * HMAC(32)
     *
     * HMAC è calcolato su: [tutto il file esclusi gli ultimi 32 byte di HMAC]
     */
    public void writeVaultV2(
            Vault vault,
            Path file,
            SecretKey keyEnc,
            SecretKey keyMac,
            byte[] salt,
            int iterations
    ) throws Exception {

        // 1) Deriva ciphertext
        byte[] iv = new byte[IV_LEN];
        new SecureRandom().nextBytes(iv);

        byte[] plain = mapper.writeValueAsBytes(vault);
        byte[] cipher = crypto.encrypt(keyEnc, iv, plain);

        // 2) Costruisci header
        ByteArrayOutputStream headerBaos = new ByteArrayOutputStream();

        headerBaos.write(MAGIC);
        headerBaos.write(VERSION);
        headerBaos.write(FLAGS_NONE);

        headerBaos.write(KDF_ALGO_PBKDF2_HMAC_SHA512);
        headerBaos.write((byte) SALT_LEN);
        headerBaos.write(salt);
        writeInt32(headerBaos, iterations);

        headerBaos.write(ENC_ALGO_AES_256_GCM);
        headerBaos.write((byte) IV_LEN);
        headerBaos.write(iv);

        byte[] header = headerBaos.toByteArray();

        // 3) HMAC header+cipher
        Mac mac = Mac.getInstance(HMAC_ALGO);
        mac.init(keyMac);
        mac.update(header);
        mac.update(cipher);

        byte[] hmac = mac.doFinal();

        // 4) Scrivi su disco: header | cipher | hmac
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(header);
        out.write(cipher);
        out.write(hmac);

        byte[] finalBytes = out.toByteArray();

        Files.createDirectories(file.getParent());
        Files.write(file, finalBytes);

        Arrays.fill(plain, (byte) 0);
    }

    public static boolean verifyVaultMac(Vault vault, SecretKey keyMac) throws Exception {
        byte[] computed = computeHmac(vault, keyMac);
        return MessageDigest.isEqual(computed, vault.getMac());
    }

    public static byte[] computeHmac(Vault vault, SecretKey keyMac) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(keyMac);

        // Serializzazione identica a quella usata in saveVaultV2
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(vault);
        oos.close();

        return mac.doFinal(baos.toByteArray());
    }


    /**
     * Legge un vault SafeBox v2 dal file.
     */
    public VaultRepository.VaultLoadResult readVaultV2(char[] masterPwd, Path file) throws Exception {

        byte[] all = Files.readAllBytes(file);

        if (all.length < MAGIC.length + 1 + 1 + 1 + 1 + SALT_LEN + 4 + 1 + 1 + IV_LEN + HMAC_SIZE) {
            throw new IllegalStateException("File vault troppo piccolo per essere SafeBox v2");
        }

        int offset = 0;

        // MAGIC
        for (int i = 0; i < MAGIC.length; i++) {
            if (all[offset + i] != MAGIC[i]) {
                throw new IllegalStateException("Magic header non valido. Il file non è un vault SafeBox.");
            }
        }
        offset += MAGIC.length;

        // VERSION
        byte version = all[offset++];
        if (version != VERSION) {
            throw new IllegalStateException("Versione vault non supportata: " + version);
        }

        // FLAGS (per ora ignorati)
        byte flags = all[offset++];
        // flags unused

        // KDF section
        byte kdfAlgo = all[offset++];
        if (kdfAlgo != KDF_ALGO_PBKDF2_HMAC_SHA512) {
            throw new IllegalStateException("KDF non supportata: " + kdfAlgo);
        }

        int saltLen = Byte.toUnsignedInt(all[offset++]);
        if (saltLen <= 0 || saltLen > 64 || offset + saltLen > all.length) {
            throw new IllegalStateException("Lunghezza salt KDF non valida: " + saltLen);
        }

        byte[] salt = Arrays.copyOfRange(all, offset, offset + saltLen);
        offset += saltLen;

        int iterations = readInt32(all, offset);
        offset += 4;

        // ENC section
        byte encAlgo = all[offset++];
        if (encAlgo != ENC_ALGO_AES_256_GCM) {
            throw new IllegalStateException("Algoritmo di cifratura non supportato: " + encAlgo);
        }

        int ivLen = Byte.toUnsignedInt(all[offset++]);
        if (ivLen <= 0 || ivLen > 32 || offset + ivLen > all.length) {
            throw new IllegalStateException("Lunghezza IV non valida: " + ivLen);
        }

        byte[] iv = Arrays.copyOfRange(all, offset, offset + ivLen);
        offset += ivLen;

        // A questo punto:
        // header = all[0 .. offset-1]
        // poi ciphertext e infine HMAC(32)

        int hmacOffset = all.length - HMAC_SIZE;
        if (hmacOffset <= offset) {
            throw new IllegalStateException("File vault corrotto: spazio insufficiente per ciphertext + HMAC");
        }

        byte[] cipher = Arrays.copyOfRange(all, offset, hmacOffset);
        byte[] hmacStored = Arrays.copyOfRange(all, hmacOffset, all.length);

        // Deriva keyEnc e keyMac
        byte[] kdfOutput = deriveKeyMaterial(masterPwd, salt, iterations, 512);
        byte[] keyEncBytes  = Arrays.copyOfRange(kdfOutput, 0, 32);
        byte[] keyMacBytes  = Arrays.copyOfRange(kdfOutput, 32, 64);

        SecretKey keyEnc = new SecretKeySpec(keyEncBytes, "AES");
        SecretKey keyMac = new SecretKeySpec(keyMacBytes, HMAC_ALGO);

        // Verifica HMAC
        Mac mac = Mac.getInstance(HMAC_ALGO);
        mac.init(keyMac);
        mac.update(all, 0, offset); // header
        mac.update(cipher);

        byte[] hmacComputed = mac.doFinal();

        if (!MessageDigestIsEqual(hmacStored, hmacComputed)) {
            // Da punto di vista utente: password errata o file manomesso
            throw new SecurityException("HMAC non valido. Password errata o file vault corrotto/manomesso.");
        }

        // Decrypt AES-GCM
        byte[] plain = crypto.decrypt(keyEnc, iv, cipher);

        Vault vault = mapper.readValue(plain, Vault.class);

        // Pulizia basica
        Arrays.fill(kdfOutput, (byte) 0);
        Arrays.fill(keyEncBytes, (byte) 0);
        Arrays.fill(keyMacBytes, (byte) 0);
        Arrays.fill(plain, (byte) 0);

        return new VaultRepository.VaultLoadResult(
                vault,
                keyEnc,
                keyMac,
                salt,
                iterations
        );
    }

    /**
     * Deriva key material grezzo con PBKDF2-HMAC-SHA512.
     * keyLengthBits può essere 256, 512, ecc.
     */
    public static byte[] deriveKeyMaterial(char[] password, byte[] salt, int iterations, int keyLengthBits) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLengthBits);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
            return skf.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new RuntimeException("Errore derivazione key material PBKDF2", e);
        }
    }

    /**
     * Scrive un int32 big-endian.
     */
    private static void writeInt32(ByteArrayOutputStream baos, int value) {
        baos.write((value >>> 24) & 0xFF);
        baos.write((value >>> 16) & 0xFF);
        baos.write((value >>> 8) & 0xFF);
        baos.write(value & 0xFF);
    }

    /**
     * Legge un int32 big-endian a partire da offset.
     */
    private static int readInt32(byte[] data, int offset) {
        return ((data[offset] & 0xFF) << 24)
                | ((data[offset + 1] & 0xFF) << 16)
                | ((data[offset + 2] & 0xFF) << 8)
                | (data[offset + 3] & 0xFF);
    }

    /**
     * Confronto costante per HMAC.
     */
    private static boolean MessageDigestIsEqual(byte[] a, byte[] b) {
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= (a[i] ^ b[i]);
        }
        return result == 0;
    }
}
