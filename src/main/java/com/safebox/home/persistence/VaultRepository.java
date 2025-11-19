package com.safebox.home.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safebox.home.crypto.CryptoService;
import com.safebox.home.crypto.KeyDerivationService;
import com.safebox.home.model.Vault;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.security.SecureRandom;

public class VaultRepository {

    private final ObjectMapper mapper = new ObjectMapper();
    private final KeyDerivationService kds = new KeyDerivationService();
    private final CryptoService crypto = new CryptoService();

    public void save(Vault vault, char[] masterPwd, Path file) throws Exception {
        byte[] salt = kds.generateSalt();
        SecretKey key = kds.deriveKey(masterPwd, salt);

        byte[] iv = new byte[12];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);

        byte[] plain = mapper.writeValueAsBytes(vault);
        byte[] cipher = crypto.encrypt(key, iv, plain);

        byte[] output = new byte[16 + 12 + cipher.length];
        System.arraycopy(salt, 0, output, 0, 16);
        System.arraycopy(iv,   0, output, 16, 12);
        System.arraycopy(cipher, 0, output, 28, cipher.length);

        Files.write(file, output);


    }

    public Vault load(char[] masterPwd, Path file) throws Exception {
        byte[] all = Files.readAllBytes(file);
        byte[] salt = slice(all, 0, 16);
        byte[] iv   = slice(all, 16, 12);
        byte[] cipher = slice(all, 28, all.length - 28);

        SecretKey key = kds.deriveKey(masterPwd, salt);
        byte[] plain = crypto.decrypt(key, iv, cipher);
        return mapper.readValue(plain, Vault.class);
    }

    private byte[] slice(byte[] src, int start, int len) {
        byte[] result = new byte[len];
        System.arraycopy(src, start, result, 0, len);
        return result;
    }


}

