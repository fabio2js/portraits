package com.safebox.home.crypto;


import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.function.LongConsumer;

public class CryptoService {



    // Limite pratico imposto da SunJCE per AES/GCM
    public static final long MAX_GCM_SIZE_BYTES = 2_000_000_000L; // ~1.86 GiB

    public byte[] generateIv() {
        byte[] iv = new byte[12];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        return iv;
    }

    public byte[] encrypt(SecretKey key, byte[] ivOut, byte[] data) throws Exception {
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        System.arraycopy(iv, 0, ivOut, 0, 12);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
        return cipher.doFinal(data);
    }

    public byte[] decrypt(SecretKey key, byte[] iv, byte[] cipherData) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
        return cipher.doFinal(cipherData);
    }

    public void encryptFileStreaming(
            Path inFile,
            Path outFile,
            SecretKey key,
            byte[] iv,
            LongConsumer progressCallback
    ) throws Exception {

        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);

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


    public void decryptFileStreaming(
            Path input,
            Path output,
            SecretKey key,
            byte[] iv,
            LongConsumer onChunk) throws Exception {

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);

        try (InputStream is = Files.newInputStream(input);
             CipherInputStream cis = new CipherInputStream(is, cipher);
             OutputStream os = Files.newOutputStream(output)) {

            byte[] buffer = new byte[8192];
            int read;
            long total = 0;

            while ((read = cis.read(buffer)) != -1) {
                os.write(buffer, 0, read);
                total += read;
                onChunk.accept(total);   // <-- callback puro (no JavaFX)
            }
        }
    }



}
