package com.onecritto.home.crypto;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.function.LongConsumer;

public class CryptoService {

    public static final int ITERATIONS = 300_000;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    // Limite pratico imposto da SunJCE per AES/GCM
    public static final long MAX_GCM_SIZE_BYTES = 2_000_000_000L; // ~1.86 GiB
    /* =====================================================
       SALT
       ===================================================== */

    public byte[] generateSalt(int length) {
        byte[] salt = new byte[length];
        SECURE_RANDOM.nextBytes(salt);
        return salt;
    }

    /* =====================================================
       PBKDF2 → AES-256 KEY DERIVATION
       ===================================================== */

    public SecretKey deriveKeyFromPassword(char[] password, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, 256);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
            byte[] keyBytes = skf.generateSecret(spec).getEncoded();
            return new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            throw new RuntimeException("Errore derivazione chiave PBKDF2", e);
        }
    }

    /* =====================================================
       AES/GCM ENCRYPT / DECRYPT
       ===================================================== */

    public byte[] encrypt(SecretKey key, byte[] iv, byte[] data) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
        return cipher.doFinal(data);
    }

    public byte[] decrypt(SecretKey key, byte[] iv, byte[] cipherBytes) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
        return cipher.doFinal(cipherBytes);
    }

    /* =====================================================
       FILE STREAM ENCRYPT
       ===================================================== */

    public void encryptFileStreaming(Path inFile, Path outFile,
                                     SecretKey key, byte[] iv,
                                     LongConsumer progressCallback) throws Exception {

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));

        Files.createDirectories(outFile.getParent());

        try (InputStream in = Files.newInputStream(inFile);
             CipherOutputStream cos = new CipherOutputStream(Files.newOutputStream(outFile), cipher)) {

            byte[] buffer = new byte[8192];
            int read;
            long copied = 0;

            while ((read = in.read(buffer)) > 0) {
                cos.write(buffer, 0, read);
                copied += read;
                if (progressCallback != null) progressCallback.accept(copied);
            }
        }
    }

    /* =====================================================
       FILE STREAM DECRYPT
       ===================================================== */

    public void decryptFileStreaming(Path input, Path output,
                                     SecretKey key, byte[] iv,
                                     LongConsumer onChunk) throws Exception {

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));

        try (InputStream is = Files.newInputStream(input);
             CipherInputStream cis = new CipherInputStream(is, cipher);
             OutputStream os = Files.newOutputStream(output)) {

            byte[] buffer = new byte[8192];
            int read;
            long total = 0;

            while ((read = cis.read(buffer)) != -1) {
                os.write(buffer, 0, read);
                total += read;
                if (onChunk != null) onChunk.accept(total);
            }
        }
    }
}
