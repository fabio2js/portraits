package com.safebox.home.service;

import com.safebox.home.crypto.CryptoService;
import com.safebox.home.crypto.KeyDerivationService;
import com.safebox.home.model.SecretFile;
import com.safebox.home.model.Vault;
import com.safebox.home.persistence.VaultRepository;
import lombok.Getter;

import javax.crypto.SecretKey;
import java.nio.file.Files;
import java.nio.file.Path;

public class VaultContext {
    @Getter
    private final VaultRepository repository = new VaultRepository();

    @Getter private  Vault vault;
    @Getter private final Path vaultPath;
    @Getter private final CryptoService crypto;
    @Getter private final SecretKey key;

    private final char[] masterPassword;

    public VaultContext(Vault vault, char[] masterPassword, Path vaultPath) throws Exception {
        this.vault = vault;
        this.masterPassword = masterPassword;
        this.vaultPath = vaultPath;

        // 1. Inizializza CryptoService
        this.crypto = new CryptoService();

        // 2. Deriva la chiave dal master password + salt del vault
        KeyDerivationService kdf = new KeyDerivationService();
        this.key = kdf.deriveKey(masterPassword, vault.getSalt());



        try {
            // Crea C:\git\safebox\vaults\files
            Files.createDirectories(vaultPath.getParent().resolve("files"));
        } catch (Exception e) {
            throw new RuntimeException("Impossibile creare la struttura del vault", e);
        }
    }


    public void reloadVault() throws Exception {
        vault = repository.load(masterPassword,vaultPath);
    }

    public void save() throws Exception {
        repository.save(vault, masterPassword, vaultPath);
    }

    public void addFile(SecretFile file) {
        vault.getFiles().add(file);
    }

    public void removeFile(SecretFile file) {
        vault.getFiles().remove(file);
    }
}
