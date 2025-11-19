package com.safebox.home.ui;

import com.safebox.home.model.SecretEntry;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.stage.Stage;
import lombok.Getter;

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



    public void handleOpenPasswordGenerator() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/password_generator.fxml"));
            DialogPane pane = loader.load();

            Dialog<Void> dialog = new Dialog<>();
            dialog.setDialogPane(pane);
            dialog.setTitle("Generatore Password");
            dialog.initOwner(passwordTextField.getScene().getWindow());

            // Importante: senza pulsanti il Dialog NON si chiude!
            pane.getButtonTypes().add(ButtonType.CLOSE);

            // Chiudi correttamente anche la X della finestra
            dialog.setOnCloseRequest(e -> dialog.close());

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

            togglePasswordButton.setText("Nascondi");
        } else {
            // passiamo a password field
            passwordField.setText(passwordTextField.getText());
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            toggleIcon.setGlyphName("EYE_SLASH");
            passwordTextField.setVisible(false);
            passwordTextField.setManaged(false);

            togglePasswordButton.setText("Mostra");
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
                passwordStrengthBar.setStyle("-fx-accent: #ff3b30;");
                passwordStrengthLabel.setText("Debole");
            }
            case 2 -> {
                passwordStrengthBar.setStyle("-fx-accent: #ff9500;");
                passwordStrengthLabel.setText("Discreta");
            }
            case 3 -> {
                passwordStrengthBar.setStyle("-fx-accent: #ffcc00;");
                passwordStrengthLabel.setText("Media");
            }
            case 4 -> {
                passwordStrengthBar.setStyle("-fx-accent: #4cd964;");
                passwordStrengthLabel.setText("Forte");
            }
            case 5 -> {
                passwordStrengthBar.setStyle("-fx-accent: #34c759;");
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
