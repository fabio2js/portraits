package com.safebox.home.ui;

import com.safebox.home.crypto.CryptoService;
import com.safebox.home.model.Vault;
import com.safebox.home.persistence.VaultFormatV2;
import com.safebox.home.persistence.VaultRepository;
import com.safebox.home.service.VaultContext;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;

public class LoginController {

    @FXML
    private PasswordField masterPasswordField;

    @FXML
    private Label errorLabel;

    @FXML
    private Button openVaultButton;

    @FXML
    private Button createVaultButton;   // <- aggiunto per showSaveDialog

    private final VaultRepository repository = new VaultRepository();


    @FXML
    public void initialize() {
        circleProgress.setVisible(false);
        circleProgress.setProgress(-1); // spinner circolare indeterminato
    }
    /**
     * APRE un vault esistente (V2)
     */
    @FXML
    private void handleOpenVault() {
        try {
            String pwd = masterPasswordField.getText();
            if (pwd.isEmpty()) {
                showError("Inserisci la Master Password");
                return;
            }

            // file chooser...
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Seleziona il vault");
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Safebox Vault (*.vault)", "*.vault")
            );

            File file = chooser.showOpenDialog(masterPasswordField.getScene().getWindow());
            if (file == null) return;

            Path vaultPath = file.toPath();

            // CARICO IL VAULT — OK
            VaultContext ctx = repository.loadVaultV2(pwd.toCharArray(), vaultPath);
            ctx.setMasterPassword(pwd.toCharArray());

            openMainWindow(ctx);

        } catch (Exception e) {
            e.printStackTrace();
            showError("Impossibile aprire il vault. Password errata o file corrotto.");
        }
    }


    /**
     * CREA un nuovo vault V2
     */
    @FXML
    private void handleCreateVault() throws Exception {


        // Mostra subito la progress bar (spinner/torta)
        circleProgress.setVisible(true);
        circleProgress.setProgress(-1);
        openVaultButton.setDisable(true);
        createVaultButton.setDisable(true);
        masterPasswordField.setDisable(true);

         char[] pwd = masterPasswordField.getText().toCharArray();
        if (pwd.length == 0) {
            showError("Inserisci una master password");
            return;
        }

// 2) Scegli file vault
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Salva nuovo vault");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Safebox Vault (*.vault)", "*.vault"));

        File file = chooser.showSaveDialog(masterPasswordField.getScene().getWindow());
        if (file == null) return;

        Path vaultPath = file.toPath();
        CryptoService cs = new CryptoService();
// 3) Genera salt
        byte[] salt = cs.generateSalt(16);
        int iter = CryptoService.ITERATIONS;

// 4) Deriva keyEnc e keyMac
        byte[] kdf = VaultFormatV2.deriveKeyMaterial(pwd, salt, iter, 512);
        byte[] keyEncBytes = Arrays.copyOfRange(kdf, 0, 32);
        byte[] keyMacBytes = Arrays.copyOfRange(kdf, 32, 64);

        SecretKey keyEnc = new SecretKeySpec(keyEncBytes, "AES");
        SecretKey keyMac = new SecretKeySpec(keyMacBytes, "HmacSHA256");

// 5) Crea vault vuoto
        Vault vault = new Vault();
        vault.setFiles(new ArrayList<>());

// 6) Inizializza VaultContext
        VaultContext ctx = new VaultContext();
        ctx.setVault(vault);
        ctx.setVaultPath(vaultPath);
        ctx.setSalt(salt);
        ctx.setIterations(iter);
        ctx.setKeyEnc(keyEnc);
        ctx.setKeyMac(keyMac);

// 7) Salva vault con V2
        repository.saveVaultV2(ctx, vault);

// 8) Apri finestra principale
        openMainWindow(ctx);

    }
    @FXML private ProgressIndicator circleProgress;

    // ------------------------------------------------------------------

    private void showError(String msg) {
        errorLabel.setText(msg);
    }

    private void openMainWindow(VaultContext ctx) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);


        Stage stage = (Stage) masterPasswordField.getScene().getWindow();


        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();



// dopo
        MainController controller = loader.getController();
        controller.init(ctx);

// UI del login
        circleProgress.setVisible(false);

    }

}
