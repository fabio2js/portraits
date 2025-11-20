package com.safebox.home.service;

import com.safebox.home.model.Vault;
import lombok.Data;

import javax.crypto.SecretKey;
import java.nio.file.Path;

@Data
public class VaultContext {

    private Path vaultPath;

    private byte[] salt;
    private int iterations;

    // Chiavi derivate da PBKDF2-HMAC-SHA512
    private SecretKey keyEnc;   // AES-256-GCM
    private SecretKey keyMac;   // HMAC-SHA256

    // Vault deserializzato
    private Vault vault;

    private char[] masterPassword;

}
