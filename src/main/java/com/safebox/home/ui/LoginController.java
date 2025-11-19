package com.safebox.home.ui;

import com.safebox.home.crypto.CryptoService;
import com.safebox.home.model.Vault;
import com.safebox.home.persistence.VaultRepository;
import com.safebox.home.service.VaultContext;
import com.safebox.home.service.VaultService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LoginController {



    @FXML
    private PasswordField masterPasswordField;

    @FXML
    private Label errorLabel;

    @FXML
    private Label vaultPathLabel  = new Label();

    private final VaultRepository vaultRepository = new VaultRepository();

    private final Path appDir = Paths.get("").toAbsolutePath();
    private final Path vaultDir = appDir.resolve("vaults");
    private final Path vaultFile = vaultDir.resolve("safebox.vault");

    @FXML
    public void initialize() {
        errorLabel.setText("");
        vaultPathLabel.setText("Vault: " + vaultFile.toAbsolutePath());
    }

    @FXML
    public void handleOpenVault() {
        errorLabel.setText("");

        char[] master = getMasterPassword();
        if (master.length == 0) {
            errorLabel.setText("Inserisci la master password.");
            return;
        }

        if (!Files.exists(vaultFile)) {
            errorLabel.setText("Nessun vault esistente trovato (" + vaultFile + ").");
            return;
        }



        try {
            Vault vault = vaultRepository.load(master, vaultFile);

                // Se il vecchio vault non aveva salt → generiamola
            if (vault.getSalt() == null) {
                vault.generateSalt();
            }

            VaultContext context = new VaultContext(vault, master, vaultFile);


            openMainWindow(context);
        } catch (javax.crypto.AEADBadTagException ex) {
            ex.printStackTrace();
            errorLabel.setText("Master password errata oppure file vault non compatibile.");
        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("Errore nell'apertura del vault.");
        }
    }

    @FXML
    public void handleCreateVault() {
        errorLabel.setText("");

        char[] master = getMasterPassword();
        if (master.length == 0) {
            errorLabel.setText("Inserisci una master password per creare il vault.");
            return;
        }

        try {
            if (!Files.exists(vaultDir)) {
                Files.createDirectories(vaultDir);
            }

            Vault vault = new Vault();
            vault.generateSalt();

            vaultRepository.save(vault, master, vaultFile);

            VaultContext context = new VaultContext(vault, master, vaultFile);
            openMainWindow(context);

        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("Errore nella creazione del vault.");
        } finally {
            wipeCharArray(master);
        }
    }
    @FXML
    private void handleNewVault() {

        Alert confirm = new Alert(Alert.AlertType.WARNING);
        confirm.setTitle("Creazione nuovo vault");
        confirm.setHeaderText("ATTENZIONE: Operazione irreversibile");
        confirm.setContentText(
                "Stai per creare un nuovo vault.\n\n" +
                        "Tutti i segreti esistenti verranno eliminati.\n" +
                        "Tutti i file cifrati nel vault precedente saranno PERDUTI definitivamente.\n\n" +
                        "Procedere?"
        );

        ButtonType yes = new ButtonType("Crea nuovo vault", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Annulla", ButtonBar.ButtonData.CANCEL_CLOSE);

        confirm.getButtonTypes().setAll(yes, cancel);
        confirm.initOwner(masterPasswordField.getScene().getWindow());

        // Se l’utente NON conferma → stop
        ButtonType result = confirm.showAndWait().orElse(cancel);
        if (result != yes) {
            return;
        }

        try {


            // 2) elimina cartella dei file
            Path dir = Paths.get("vaults");
            Path filesDir = dir.resolve("files");
            if (Files.exists(filesDir)) {
                deleteDirectoryRecursively(filesDir);
            }

            // 3) crea nuovo vault
            handleCreateVault();

            Alert ok = new Alert(Alert.AlertType.INFORMATION,
                    "Nuovo vault creato con successo.", ButtonType.OK);
            ok.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            Alert error = new Alert(Alert.AlertType.ERROR,
                    "Errore durante la creazione del nuovo vault.", ButtonType.OK);
            error.showAndWait();
        }
    }

    private void deleteDirectoryRecursively(Path path) throws IOException {
        if (!Files.exists(path)) return;

        Files.walk(path)
                .sorted((a, b) -> b.compareTo(a)) // elimina prima i file poi le cartelle
                .forEach(p -> {
                    try { Files.delete(p); } catch (IOException ignored) {}
                });
    }
    private char[] getMasterPassword() {
        String pwd = masterPasswordField.getText();
        return pwd != null ? pwd.toCharArray() : new char[0];
    }

    private void wipeCharArray(char[] arr) {
        for (int i = 0; i < arr.length; i++) {
            arr[i] = 0;
        }
    }

    private void openMainWindow(VaultContext context) throws IOException {

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/main.fxml"));
        Scene scene = new Scene(loader.load());

        MainController controller = loader.getController();
        controller.init(context);

        Stage currentStage = (Stage) masterPasswordField.getScene().getWindow();
        currentStage.setTitle("Safebox Home - Vault");
        currentStage.setScene(scene);
        currentStage.centerOnScreen();
    }
}
