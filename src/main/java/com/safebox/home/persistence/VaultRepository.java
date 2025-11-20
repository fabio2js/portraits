package com.safebox.home.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safebox.home.service.VaultContext;
import com.safebox.home.crypto.CryptoService;
import com.safebox.home.model.Vault;
import lombok.Data;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.file.Path;
import java.util.Arrays;

public class VaultRepository {

    private final CryptoService crypto = new CryptoService();
    private final ObjectMapper mapper = new ObjectMapper();
    private final VaultFormatV2 formatV2 = new VaultFormatV2(crypto, mapper);


    public void verifyPassword(char[] pwd, byte[] salt, int iter, Vault vault) throws Exception {

        byte[] kdf = VaultFormatV2.deriveKeyMaterial(pwd, salt, iter, 512);
        SecretKey keyMac = new SecretKeySpec(Arrays.copyOfRange(kdf, 32, 64), "HmacSHA256");

        // se l'HMAC finale non corrisponde → password errata
        if (!VaultFormatV2.verifyVaultMac(vault, keyMac)) {
            throw new SecurityException("Password errata");
        }
    }

   @Data
    public static class VaultLoadResult {
        private final Vault vault;
        private final javax.crypto.SecretKey keyEnc;
        private final javax.crypto.SecretKey keyMac;
        private final byte[] salt;
        private final int iterations;

        public VaultLoadResult(Vault vault,
                               javax.crypto.SecretKey keyEnc,
                               javax.crypto.SecretKey keyMac,
                               byte[] salt,
                               int iterations) {
            this.vault = vault;
            this.keyEnc = keyEnc;
            this.keyMac = keyMac;
            this.salt = salt;
            this.iterations = iterations;
        }


    }
    /**
     * CARICA un Vault V2 da disco e ritorna il VaultContext completo
     */
    public VaultContext loadVaultV2(char[] masterPwd, Path vaultPath) throws Exception {

        // --- 1) Leggi il vault con formato V2 ---
         VaultLoadResult result =
                formatV2.readVaultV2(masterPwd, vaultPath);

        // --- 2) Costruisci il VaultContext ---
        VaultContext ctx = new VaultContext();
        ctx.setVaultPath(vaultPath);
        ctx.setVault(result.getVault());
        ctx.setKeyEnc(result.getKeyEnc());
        ctx.setKeyMac(result.getKeyMac());
        ctx.setSalt(result.getSalt());
        ctx.setIterations(result.getIterations());

        return ctx;
    }

    /**
     * SALVA un Vault usando V2 (header+KDF+AES-GCM+HMAC)
     */
    public void saveVaultV2(VaultContext ctx, Vault vault) throws Exception {
        formatV2.writeVaultV2(
                vault,
                ctx.getVaultPath(),
                ctx.getKeyEnc(),
                ctx.getKeyMac(),
                ctx.getSalt(),
                ctx.getIterations()
        );
    }
}
