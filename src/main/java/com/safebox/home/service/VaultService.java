package com.safebox.home.service;

import com.safebox.home.model.Vault;
import com.safebox.home.model.SecretEntry;

public class VaultService {

    private Vault currentVault = new Vault();

    public Vault getVault() {
        return currentVault;
    }

    public void setVault(Vault vault) {
        this.currentVault = vault;
    }

    public void saveCurrentVault() throws Exception {

    }

    public void addEntry(SecretEntry entry) {
        currentVault.add(entry);

    }

    public void removeEntry(SecretEntry entry) {
        currentVault.remove(entry);
    }
}
