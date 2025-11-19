package com.safebox.home.model;

import lombok.Data;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
@Data
public class Vault {

    private List<SecretEntry> entries = new ArrayList<>();



    public void add(SecretEntry entry) {
        entries.add(entry);
    }

    public void remove(SecretEntry entry) {
        entries.remove(entry);
    }

    public void generateSalt() {
        this.salt = new byte[16];
        new java.security.SecureRandom().nextBytes(this.salt);
    }
    /**
     * Salt usato per derivare la chiave simmetrica dei FILE nel vault.
     * È indipendente dal salt usato da VaultRepository per cifrare il JSON del vault.
     */
    private byte[] salt;


        private List<SecretFile> files = new ArrayList<>();

}
