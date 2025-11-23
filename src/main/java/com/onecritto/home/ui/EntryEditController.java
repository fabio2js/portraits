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
    @FXML private ComboBox<String> categoryBox;

    private boolean secretVisible = false;

    private SecretEntry entry;
    private Runnable onSaveCallback;

    @Setter
    @Getter
    private MainController mainController;

    private void updatePasswordStrength(String pwd) {
        int score = calculatePasswordStrength(pwd);

        double progress = score / 5.0;
        passwordStrengthBar.setProgress(progress);

        if (score <= 1) {
            passwordStrengthBar.setStyle("-fx-accent: #A300B8;"); // viola molto scuro (debole)
            passwordStrengthLabel.setText("Debole");
        } else if (score == 2) {
            passwordStrengthBar.setStyle("-fx-accent: #C44CFF;"); // viola medio
            passwordStrengthLabel.setText("Discreta");
        } else if (score == 3) {
            passwordStrengthBar.setStyle("-fx-accent: #D48AFF;"); // viola chiaro
            passwordStrengthLabel.setText("Media");
        } else if (score == 4) {
            passwordStrengthBar.setStyle("-fx-accent: #B066FF;"); // viola più brillante
            passwordStrengthLabel.setText("Forte");
        } else {
            passwordStrengthBar.setStyle("-fx-accent: #8C3AFF;"); // viola OneCritto pieno
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




    public void handleOpenPasswordGenerator() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/password_generator.fxml"));
            DialogPane pane = loader.load();

            Dialog<Void> dialog = new Dialog<>();
            dialog.setDialogPane(pane);
            dialog.setTitle("Generatore Password");
            dialog.initOwner(passwordStrengthLabel.getScene().getWindow());
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

    public void init(SecretEntry entry, Runnable onSaveCallback) {
        this.entry = entry;
        this.onSaveCallback = onSaveCallback;
        categoryBox.getItems().addAll("Password", "Note", "Altro");
        categoryBox.getSelectionModel().select(entry.getCategory());
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

            secretVisible = false;

        } else {
            // PASSA A VISIBILE
            secretVisibleField.setText(secretField.getText());
            secretField.setVisible(false);
            secretField.setManaged(false);

            secretVisibleField.setVisible(true);
            secretVisibleField.setManaged(true);
            toggleIcon.setGlyphName("EYE_SLASH");
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

        entry.setCategory(categoryBox.getSelectionModel().getSelectedItem());
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
