package com.onecritto.home.ui;

import com.onecritto.home.model.SecretEntry;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

import static com.onecritto.home.util.UIUtils.showToast;

public class AddEntryController {

    @FXML private TextField titleField;
    @FXML private TextField usernameField;

    @FXML private PasswordField passwordField;
    @FXML private TextField passwordTextField;
    @FXML private Button togglePasswordButton;

    @FXML private ProgressBar passwordStrengthBar;
    @FXML private Label passwordStrengthLabel;

    @FXML private TextArea notesField;
    @FXML private ComboBox<String> categoryBox;

    @Getter private boolean saved = false;
    @Getter private SecretEntry entry;
    @FXML private FontAwesomeIconView toggleIcon;
    @FXML private FontAwesomeIconView genPassword;
    @FXML private Button togglePasswordGenerator;

    @FXML
    public void initialize() {
        categoryBox.getItems().addAll("Password", "Note", "Altro");
        categoryBox.getSelectionModel().select("Password");

        // Listener per aggiornare la forza della password
        passwordField.textProperty().addListener((o, oldV, newV) -> updatePasswordStrength(newV));
        passwordTextField.textProperty().addListener((o, oldV, newV) -> updatePasswordStrength(newV));
    }

    public void handleCopyPassword(ActionEvent event) {

        String pwd = passwordTextField.isVisible() ? passwordTextField.getText() : passwordField.getText();;
        if (pwd == null || pwd.isBlank()) {
            return;
        }

        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(pwd);
        clipboard.setContent(content);
        showToast(passwordTextField,"La password è stata copiata negli appunti.");

    }

    @Setter
    @Getter
    private MainController mainController;

    public void handleOpenPasswordGenerator() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/password_generator.fxml"));
            DialogPane pane = loader.load();

            Dialog<Void> dialog = new Dialog<>();
            dialog.setDialogPane(pane);
            dialog.setTitle("Generatore Password");
            dialog.initOwner(passwordTextField.getScene().getWindow());
            Stage dialogStage = (Stage) dialog.getDialogPane().getScene().getWindow();
            dialogStage.getIcons().add(new Image(
                    Objects.requireNonNull(getClass().getResourceAsStream("/icons/onecritto_white_key_32x32.png"))
            ));
            // Importante: senza pulsanti il Dialog NON si chiude!
            pane.getButtonTypes().add(ButtonType.CLOSE);

            // Chiudi correttamente anche la X della finestra
            dialog.setOnCloseRequest(e -> dialog.close());
            pane.addEventFilter(MouseEvent.ANY, e -> getMainController().resetInactivityTimer());
            pane.addEventFilter(KeyEvent.ANY, e -> getMainController().resetInactivityTimer());
            dialog.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // ------------------------
    // TOGGLE SHOW / HIDE PASSWORD
    // ------------------------
    @FXML
    private void handleTogglePassword() {
        if (passwordField.isVisible()) {
            // passiamo a testo normale
            passwordTextField.setText(passwordField.getText());
            passwordTextField.setVisible(true);
            passwordTextField.setManaged(true);
            toggleIcon.setGlyphName("EYE");
            passwordField.setVisible(false);
            passwordField.setManaged(false);

        } else {
            // passiamo a password field
            passwordField.setText(passwordTextField.getText());
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            toggleIcon.setGlyphName("EYE_SLASH");
            passwordTextField.setVisible(false);
            passwordTextField.setManaged(false);

        }
    }


    // ------------------------
    // PASSWORD STRENGTH
    // ------------------------
    private void updatePasswordStrength(String pwd) {
        int score = calculatePasswordStrength(pwd);
        double progress = score / 5.0;

        passwordStrengthBar.setProgress(progress);

        switch (score) {
            case 0, 1 -> {
                // Debole
                passwordStrengthBar.setStyle("-fx-accent: #A300B8;");  // viola molto scuro
                passwordStrengthLabel.setText("Debole");
            }
            case 2 -> {
                // Discreta
                passwordStrengthBar.setStyle("-fx-accent: #C44CFF;");  // viola medio
                passwordStrengthLabel.setText("Discreta");
            }
            case 3 -> {
                // Media
                passwordStrengthBar.setStyle("-fx-accent: #D48AFF;");  // viola chiaro
                passwordStrengthLabel.setText("Media");
            }
            case 4 -> {
                // Forte
                passwordStrengthBar.setStyle("-fx-accent: #B066FF;");  // viola brillante
                passwordStrengthLabel.setText("Forte");
            }
            case 5 -> {
                // Molto forte
                passwordStrengthBar.setStyle("-fx-accent: #8C3AFF;");  // viola OneCritto pieno
                passwordStrengthLabel.setText("Molto forte");
            }
        }
    }

    private int calculatePasswordStrength(String pwd) {
        if (pwd == null || pwd.isEmpty()) return 0;

        int score = 0;

        if (pwd.length() >= 8) score++;
        if (pwd.length() >= 12) score++;

        if (pwd.matches(".*[a-z].*")) score++;
        if (pwd.matches(".*[A-Z].*")) score++;
        if (pwd.matches(".*[0-9].*")) score++;
        if (pwd.matches(".*[^a-zA-Z0-9].*")) score++;

        return Math.min(score, 5);
    }


    // ------------------------
    // SAVE / CANCEL
    // ------------------------
    @FXML
    private void handleSave() {

        if (titleField.getText().isBlank()) {
            showError("Il titolo è obbligatorio.");
            return;
        }

        String pwd = passwordField.isVisible()
                ? passwordField.getText()
                : passwordTextField.getText();

        entry = new SecretEntry();
        entry.setId(java.util.UUID.randomUUID().toString());
        entry.setTitle(titleField.getText());
        entry.setUsername(usernameField.getText());
        entry.setPassword(pwd);
        entry.setNotes(notesField.getText());
        entry.setCategory(categoryBox.getValue());

        saved = true;
        closeWindow();
    }

    @FXML
    private void handleCancel() {
        saved = false;
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) titleField.getScene().getWindow();
        stage.close();
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
