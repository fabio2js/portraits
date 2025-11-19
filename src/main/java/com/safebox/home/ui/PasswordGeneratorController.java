package com.safebox.home.ui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;



public class PasswordGeneratorController {

    @FXML
    private TextField generatedField;
    @FXML private Slider lengthSlider;
    @FXML private Label lengthValue;
    @FXML private CheckBox lowerCB, upperCB, digitsCB, symbolsCB;
    @FXML private CheckBox reqUpper, reqDigit, reqSymbol;
    @FXML private ProgressBar strengthBar;
    @FXML private Label strengthLabel;
    @FXML private Button generateButton, copyButton;

    private static final SecureRandom RAND = new SecureRandom();

    @FXML
    private void initialize() {

        lengthSlider.valueProperty().addListener((obs, oldV, newV) -> {
            int len = newV.intValue();
            lengthValue.setText(String.valueOf(len));
        });

        generateButton.setOnAction(e -> generate());
        copyButton.setOnAction(e -> copyToClipboard());

        generate(); // prima generazione
    }

    private void generate() {

        int length = (int) lengthSlider.getValue();
        String lower = "abcdefghijklmnopqrstuvwxyz";
        String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String digits = "0123456789";
        String symbols = "!@#$%^&*()_-+=[]{}|;:,.<>?";

        StringBuilder pool = new StringBuilder();

        if (lowerCB.isSelected()) pool.append(lower);
        if (upperCB.isSelected()) pool.append(upper);
        if (digitsCB.isSelected()) pool.append(digits);
        if (symbolsCB.isSelected()) pool.append(symbols);

        if (pool.length() == 0) {
            generatedField.setText("");
            return;
        }

        StringBuilder result = new StringBuilder();

        // Applica regole obbligatorie
        if (reqUpper.isSelected()) result.append(randomChar(upper));
        if (reqDigit.isSelected()) result.append(randomChar(digits));
        if (reqSymbol.isSelected()) result.append(randomChar(symbols));

        // Aggiunge caratteri casuali fino a lunghezza
        while (result.length() < length) {
            result.append(pool.charAt(RAND.nextInt(pool.length())));
        }

        // Shuffle finale
        List<Character> chars = new ArrayList<>();
        for (char c : result.toString().toCharArray()) chars.add(c);
        Collections.shuffle(chars);
        String finalPass = chars.stream()
                .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                .toString();

        generatedField.setText(finalPass);
        updateStrength(finalPass);
    }

    private char randomChar(String s) {
        return s.charAt(RAND.nextInt(s.length()));
    }

    private void updateStrength(String pass) {
        int score = 0;

        if (pass.length() >= 12) score++;
        if (pass.length() >= 20) score++;
        if (pass.matches(".*[A-Z].*")) score++;
        if (pass.matches(".*[a-z].*")) score++;
        if (pass.matches(".*\\d.*")) score++;
        if (pass.matches(".*[!@#$%^&*()_\\-+=\\[\\]{}|;:,.<>?].*")) score++;

        strengthBar.setProgress(score / 6.0);

        if (score <= 2) strengthLabel.setText("Qualità: Debole");
        else if (score <= 4) strengthLabel.setText("Qualità: Media");
        else strengthLabel.setText("Qualità: Forte");
    }

    private void copyToClipboard() {
        String text = generatedField.getText();
        if (text.isEmpty()) return;

        final Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        clipboard.setContent(content);

        copyButton.setText("Copiato");
        Platform.runLater(() -> {
            new Thread(() -> {
                try { Thread.sleep(1500); } catch (Exception ignored) {}
                Platform.runLater(() -> copyButton.setText(""));
            }).start();
        });
    }
}
