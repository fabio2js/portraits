package com.onecritto.home.ui;

import com.onecritto.home.crypto.CryptoService;
import com.onecritto.home.model.Vault;
import com.onecritto.home.persistence.VaultFormatV2;
import com.onecritto.home.persistence.VaultRepository;
import com.onecritto.home.service.VaultContext;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class LoginController {
    @FXML
    public VBox rootContainer;
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


            masterPasswordField.textProperty().addListener((obs, oldValue, newValue) -> {
                if (newValue == null || newValue.isEmpty()) {
                    passwordStrengthLabel.setText("Forza password: vuota");
                    passwordStrengthBar.setProgress(0.0);
                    return;

                }
                updatePasswordStrength(newValue);
            });

           secretVisibleField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue == null || newValue.isEmpty()) {
                passwordStrengthLabel.setText("Forza password: vuota");
                passwordStrengthBar.setProgress(0.0);
                return;
            }
               updatePasswordStrength(newValue);

        });

        updatePasswordStrength("");


    }


    private boolean secretVisible = false;
    @FXML private TextField secretVisibleField ;
    @FXML private Button toggleSecretButton;
 
    @FXML private FontAwesomeIconView toggleIcon;

    @FXML
    private void handleToggleSecret() {
        if (secretVisible) {
            // VISIBILE -> NASCOSTO
            masterPasswordField.setText(secretVisibleField.getText());
            masterPasswordField.requestFocus();

            secretVisibleField.setVisible(false);
            secretVisibleField.setManaged(false);

            masterPasswordField.setVisible(true);
            masterPasswordField.setManaged(true);

            toggleIcon.setGlyphName("EYE");
            secretVisible = false;

        } else {
            // NASCOSTO -> VISIBILE
            secretVisibleField.setText(masterPasswordField.getText());
            secretVisibleField.requestFocus();

            masterPasswordField.setVisible(false);
            masterPasswordField.setManaged(false);

            secretVisibleField.setVisible(true);
            secretVisibleField.setManaged(true);

            toggleIcon.setGlyphName("EYE_SLASH");
            secretVisible = true;
        }
    }


    private String getPassword() {
        return !secretVisible ?  masterPasswordField.getText() :  this.secretVisibleField.getText();
    }



    public void handleOpenPasswordGenerator() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/password_generator.fxml"));
            DialogPane pane = loader.load();

            Dialog<Void> dialog = new Dialog<>();
            dialog.setDialogPane(pane);
            dialog.setTitle("Generatore Password");
            Stage dialogStage = (Stage) dialog.getDialogPane().getScene().getWindow();
            dialogStage.getIcons().add(new Image(
                    Objects.requireNonNull(getClass().getResourceAsStream("/icons/onecritto_white_key_32x32.png"))
            ));
            dialog.initOwner(passwordStrengthLabel.getScene().getWindow());

            // Importante: senza pulsanti il Dialog NON si chiude!
            pane.getButtonTypes().add(ButtonType.CLOSE);

            // Chiudi correttamente anche la X della finestra
            dialog.setOnCloseRequest(e -> dialog.close());

            dialog.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void updatePasswordStrength(String pwd) {

        if (pwd == null) {
            pwd = "";
        }

        int length = pwd.length();

        boolean hasLower = false;
        boolean hasUpper = false;
        boolean hasDigit = false;
        boolean hasSymbol = false;

        // --- Analisi caratteri ---
        for (int i = 0; i < length; i++) {
            char c = pwd.charAt(i);
            if (Character.isLowerCase(c)) {
                hasLower = true;
            } else if (Character.isUpperCase(c)) {
                hasUpper = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            } else {
                hasSymbol = true;
            }
        }

        // --- Calcolo score ---
        int score = 0;

        score += Math.min(10, length) * 4;

        int classes = 0;
        if (hasLower) classes++;
        if (hasUpper) classes++;
        if (hasDigit) classes++;
        if (hasSymbol) classes++;
        score += classes * 15;

        if (length >= 16 && classes >= 3) {
            score += 10;
        }
        if (length >= 20 && classes == 4) {
            score += 10;
        }

        if (score > 100) score = 100;

        // --- Band ---
        int band;
        if (length == 0) {
            band = 0;
        } else if (score < 30) {
            band = 1;
        } else if (score < 50) {
            band = 2;
        } else if (score < 70) {
            band = 3;
        } else if (score < 85) {
            band = 4;
        } else {
            band = 5;
        }

        // --- Label ---
        String labelText;
        switch (band) {
            case 0 -> labelText = "vuota";
            case 1 -> labelText = "molto debole";
            case 2 -> labelText = "debole";
            case 3 -> labelText = "discreta";
            case 4 -> labelText = "forte";
            case 5 -> labelText = "molto forte";
            default -> labelText = "vuota";
        }

        // --- Aggiornamento UI ---
        if (passwordStrengthLabel != null) {
            passwordStrengthLabel.setText("Forza password: " + labelText);
        }

        if (passwordStrengthBar != null) {

            if (length == 0) {
                passwordStrengthBar.setProgress(0.0);
            } else {
                passwordStrengthBar.setProgress(score / 100.0);
            }

            // =======================================================
            //  AGGIUNTA PUNTUALE: COLORI VIOLA ONECRITTO
            // =======================================================
            switch (band) {
                case 0, 1 -> passwordStrengthBar.setStyle("-fx-accent: #A300B8;");   // viola scuro
                case 2 ->    passwordStrengthBar.setStyle("-fx-accent: #C44CFF;");   // viola medio
                case 3 ->    passwordStrengthBar.setStyle("-fx-accent: #D48AFF;");   // viola chiaro
                case 4 ->    passwordStrengthBar.setStyle("-fx-accent: #B066FF;");   // viola brillante
                case 5 ->    passwordStrengthBar.setStyle("-fx-accent: #8C3AFF;");   // viola OneCritto pieno
            }
        }
    }








    /**
     * APRE un vault esistente (V2)
     */
    @FXML
    private void handleOpenVault() {
        try {
            String pwd = getPassword();

            if(pwd.length()<10){
                showError("Inserisci una master password di almeno 10 caratteri");
                return;
            }


            FileChooser chooser = new FileChooser();
            chooser.setTitle("Apri OneCritto vault");
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("OneCritto (*.ocv)", "*.ocv")
            );
            Stage fcStage = (Stage) rootContainer.getScene().getWindow();
            fcStage.getIcons().add(new javafx.scene.image.Image(
                    Objects.requireNonNull(getClass().getResourceAsStream("/icons/onecritto_white_key_32x32.png"))
            ));
            File file = chooser.showOpenDialog(fcStage);

            if (file == null) return;

            Path vaultPath = file.toPath();

            // CARICO IL VAULT — OK
            VaultContext ctx = repository.loadVaultV2(pwd.toCharArray(), vaultPath);
            ctx.setMasterPassword(pwd.toCharArray());

            openMainWindow(ctx);

        } catch (Exception e) {
            e.printStackTrace();
            showError("Impossibile aprire il vault.");
        }
    }


    /**
     * CREA un nuovo vault V2
     */
    @FXML
    private void handleCreateVault() throws Exception {


         char[] pwd = getPassword().toCharArray();
         if(pwd.length<10){
             showError("Inserisci una master password di almeno 10 caratteri");
            return;
         }

// 2) Scegli file vault

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Seleziona file  vault");
        Stage fcStage = (Stage) rootContainer.getScene().getWindow();
        fcStage.getIcons().add(new javafx.scene.image.Image(
                Objects.requireNonNull(getClass().getResourceAsStream("/icons/onecritto_white_key_32x32.png"))
        ));
        File file = chooser.showOpenDialog(fcStage);


        if( file==null)
        return;

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
        // Mostra subito la progress bar (spinner/torta)
        circleProgress.setVisible(true);
        circleProgress.setProgress(-1);
        openVaultButton.setDisable(true);
        createVaultButton.setDisable(true);
        masterPasswordField.setDisable(true);
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

        scene.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/css/onecritto-theme.css")).toExternalForm()
        );
        Stage stage = (Stage) masterPasswordField.getScene().getWindow();

        stage.getIcons().add(new Image(
                Objects.requireNonNull(getClass().getResourceAsStream("/icons/onecritto_white_key_256x256.png"))
        ));
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();



// dopo
        MainController controller = loader.getController();
        controller.init(ctx);

// UI del login
        circleProgress.setVisible(false);

    }

    // Nuovi componenti per la forza password
    @FXML
    private ProgressBar passwordStrengthBar;

    @FXML
    private Label passwordStrengthLabel;
}
