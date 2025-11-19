package com.safebox.home.ui;

import com.safebox.home.model.SecretEntry;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.Stage;

import static com.safebox.home.util.UIUtils.showToast;


public class EntryEditController {

    @FXML private TextField titleField;
    @FXML private TextField usernameField;
    @FXML private TextArea notesField;

    @FXML private PasswordField secretField;
    @FXML private TextField secretVisibleField;
    @FXML private Button toggleSecretButton;
    @FXML private FontAwesomeIconView toggleIcon;


    @FXML private ProgressBar passwordStrengthBar;
    @FXML private Label passwordStrengthLabel;

    private boolean secretVisible = false;

    private SecretEntry entry;
    private Runnable onSaveCallback;

    private void updatePasswordStrength(String pwd) {
        int score = calculatePasswordStrength(pwd);

        double progress = score / 5.0;
        passwordStrengthBar.setProgress(progress);

        if (score <= 1) {
            passwordStrengthBar.setStyle("-fx-accent: #ff3b30;"); // rosso
            passwordStrengthLabel.setText("Debole");
        } else if (score == 2) {
            passwordStrengthBar.setStyle("-fx-accent: #ff9500;"); // arancione
            passwordStrengthLabel.setText("Discreta");
        } else if (score == 3) {
            passwordStrengthBar.setStyle("-fx-accent: #ffcc00;"); // giallo
            passwordStrengthLabel.setText("Media");
        } else if (score == 4) {
            passwordStrengthBar.setStyle("-fx-accent: #4cd964;"); // verde chiaro
            passwordStrengthLabel.setText("Forte");
        } else {
            passwordStrengthBar.setStyle("-fx-accent: #34c759;"); // verde pieno
            passwordStrengthLabel.setText("Molto forte");
        }
    }


    private int calculatePasswordStrength(String pwd) {
        if (pwd == null || pwd.isEmpty()) return 0;

        int score = 0;

        if (pwd.length() >= 8) score += 1;
        if (pwd.length() >= 12) score += 1;

        if (pwd.matches(".*[a-z].*")) score += 1;
        if (pwd.matches(".*[A-Z].*")) score += 1;
        if (pwd.matches(".*[0-9].*")) score += 1;
        if (pwd.matches(".*[^a-zA-Z0-9].*")) score += 1;

        return Math.min(score, 5);  // max 5
    }

    public void init(SecretEntry entry, Runnable onSaveCallback) {
        this.entry = entry;
        this.onSaveCallback = onSaveCallback;

        titleField.setText(entry.getTitle());
        usernameField.setText(entry.getUsername());
        secretVisibleField.setText(entry.getPassword());
        secretField.setText(entry.getPassword());
        notesField.setText(entry.getNotes());

        secretField.textProperty().addListener((obs, oldVal, newVal) -> updatePasswordStrength(newVal));
        secretVisibleField.textProperty().addListener((obs, oldVal, newVal) -> updatePasswordStrength(newVal));
        updatePasswordStrength(secretField.getText());




    }
    @FXML
    private void handleToggleSecret() {

        if (secretVisible) {
            // PASSA A NASCOSTO
            secretField.setText(secretVisibleField.getText());
            secretVisibleField.setVisible(false);
            secretVisibleField.setManaged(false);

            secretField.setVisible(true);
            secretField.setManaged(true);
            toggleIcon.setGlyphName("EYE");  // icona occhio

            toggleSecretButton.setText("Mostra");
            secretVisible = false;

        } else {
            // PASSA A VISIBILE
            secretVisibleField.setText(secretField.getText());
            secretField.setVisible(false);
            secretField.setManaged(false);

            secretVisibleField.setVisible(true);
            secretVisibleField.setManaged(true);
            toggleIcon.setGlyphName("EYE_SLASH");
            toggleSecretButton.setText("Nascondi");
            secretVisible = true;
        }
    }




    public void handleCopyPassword(ActionEvent event) {



        String pwd = secretVisible ? secretVisibleField.getText() : secretField.getText();;
        if (pwd == null || pwd.isBlank()) {
            return;
        }

        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(pwd);
        clipboard.setContent(content);
        showToast(secretVisibleField,"La password è stata copiata negli appunti.");

    }

    @FXML
    private void handleSave() {

        String secret = secretVisible ? secretVisibleField.getText() : secretField.getText();
        entry.setPassword(secret);


        entry.setTitle(titleField.getText());
        entry.setUsername(usernameField.getText());
        entry.setNotes(notesField.getText());

        if (onSaveCallback != null) {
            onSaveCallback.run();
        }

        close();
    }

    @FXML
    private void handleCancel() {
        close();
    }

    private void close() {
        ((Stage) titleField.getScene().getWindow()).close();
    }

}
