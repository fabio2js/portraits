package com.safebox.home.model;

import com.safebox.home.crypto.CryptoService;
import lombok.Data;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
@Data
public class Vault {

    private List<SecretEntry> entries = new ArrayList<>();

    private List<SecretFile> files = new ArrayList<>();

    private byte[] mac;

    public void add(SecretEntry entry) {
        entries.add(entry);
    }

    public void remove(SecretEntry entry) {
        entries.remove(entry);
    }


    /**
     * Salt usato per derivare la chiave simmetrica dei FILE nel vault.
     * È indipendente dal salt usato da VaultRepository per cifrare il JSON del vault.
     */
    private byte[] salt;



}
