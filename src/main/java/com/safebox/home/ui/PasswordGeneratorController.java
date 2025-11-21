package com.safebox.home.ui;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

import java.security.SecureRandom;

public class PasswordGeneratorController {

    // TAB 1 (Password Forte)
    @FXML private TextField generatedPasswordField;
    @FXML private Button copyButton;
    @FXML private Slider lengthSlider;
    @FXML private Label lengthLabel;
    @FXML private CheckBox includeUpper;
    @FXML private CheckBox includeLower;
    @FXML private CheckBox includeDigits;
    @FXML private CheckBox includeSymbols;
    @FXML private CheckBox avoidAmbiguous;
    @FXML private ProgressBar strengthBar;
    @FXML private Label strengthLabel;
    @FXML private Button generateButton;

    // TAB 2 (Password Mnemonica)
    @FXML private TextArea mnemonicOutput;
    @FXML private Slider mnemonicSyllablesSlider;
    @FXML private Label mnemonicSyllablesLabel;
    @FXML private CheckBox mnemonicIncludeDigits;
    @FXML private ProgressBar mnemonicStrengthBar;
    @FXML private Label mnemonicStrengthLabel;
    @FXML private Button generateMnemonicButton;

    private final SecureRandom random = new SecureRandom();

    @FXML
    public void initialize() {

        // Slider lunghezza
        lengthLabel.setText(String.valueOf((int) lengthSlider.getValue()));
        lengthSlider.valueProperty().addListener((obs, o, n) ->
                lengthLabel.setText(String.valueOf(n.intValue()))
        );

        // Slider sillabe mnemoniche
        mnemonicSyllablesSlider.valueProperty().addListener((obs, o, n) ->
                mnemonicSyllablesLabel.setText(String.valueOf(n.intValue()))
        );

        // Generazione iniziale
        handleGenerate();
        handleGenerateMnemonic();

        generateButton.setOnAction(e -> handleGenerate());
        generateMnemonicButton.setOnAction(e -> handleGenerateMnemonic());
    }

    // ========= PASSWORD FORTE ========= //
    @FXML
    private void handleGenerate() {
        int length = (int) lengthSlider.getValue();

        String upper = includeUpper.isSelected() ? "ABCDEFGHIJKLMNOPQRSTUVWXYZ" : "";
        String lower = includeLower.isSelected() ? "abcdefghijklmnopqrstuvwxyz" : "";
        String digits = includeDigits.isSelected() ? "0123456789" : "";
        String symbols = includeSymbols.isSelected() ? "!@#$%^&*(){}[]?/+=-" : "";

        String all = upper + lower + digits + symbols;

        if (avoidAmbiguous.isSelected()) {
            all = all.replaceAll("[O0Il1]", "");
        }

        if (all.isEmpty()) {
            generatedPasswordField.setText("Seleziona almeno un set di caratteri");
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(all.charAt(random.nextInt(all.length())));
        }

        String pwd = sb.toString();
        generatedPasswordField.setText(pwd);
        updateStrengthBar(pwd, strengthBar, strengthLabel);
    }

    @FXML
    private void handleCopy() {
        String text = generatedPasswordField.getText();
        if (text.isEmpty()) return;

        ClipboardContent cc = new ClipboardContent();
        cc.putString(text);
        Clipboard.getSystemClipboard().setContent(cc);
    }

    // ========= PASSWORD MNEMONICA ========= //
    @FXML
    private void handleGenerateMnemonic() {

        int syllables = (int) mnemonicSyllablesSlider.getValue();

        // Cluster più naturali
        final String[] consonants = {
                "b","c","d","f","g","h","k","l","m","n",
                "p","r","s","t","v","z",
                "br","cr","dr","fr","gr","kr","pr","tr","vr",
                "st","sk","sp","str"
        };

        final String[] vowels = {"a","e","i","o","u"};

        // Pattern fonetici variabili: CV | CVC | VC
        final int[] patterns = {0, 1, 2};

        StringBuilder sb = new StringBuilder(syllables * 3 + 4);

        for (int i = 0; i < syllables; i++) {
            int pattern = patterns[random.nextInt(patterns.length)];

            switch (pattern) {
                case 0: // CV
                    sb.append(consonants[random.nextInt(consonants.length)]);
                    sb.append(vowels[random.nextInt(vowels.length)]);
                    break;

                case 1: // CVC
                    sb.append(consonants[random.nextInt(consonants.length)]);
                    sb.append(vowels[random.nextInt(vowels.length)]);
                    sb.append(consonants[random.nextInt(16)]); // solo consonanti singole finali
                    break;

                case 2: // VC
                    sb.append(vowels[random.nextInt(vowels.length)]);
                    sb.append(consonants[random.nextInt(consonants.length)]);
                    break;
            }
        }

        if (mnemonicIncludeDigits.isSelected()) {
            sb.append(random.nextInt(10));
        }

        String pwd = sb.toString();
        mnemonicOutput.setText(pwd);

        updateStrengthBar(pwd, mnemonicStrengthBar, mnemonicStrengthLabel);
    }


    // ========= VALUTAZIONE FORZA ========= //

    private void updateStrengthBar(String pwd, ProgressBar bar, Label label) {
        if (pwd == null || pwd.isEmpty()) {
            bar.setProgress(0);
            label.setText("Forza password: -");
            return;
        }

        int score = 0;
        if (pwd.length() >= 12) score++;
        if (pwd.matches(".*[A-Z].*")) score++;
        if (pwd.matches(".*[a-z].*")) score++;
        if (pwd.matches(".*[0-9].*")) score++;
        if (pwd.matches(".*[^A-Za-z0-9].*")) score++;

        double progress = score / 5.0;

        bar.setProgress(progress);

        String text =
                (score <= 1) ? "Debole" :
                        (score == 2) ? "Mediocre" :
                                (score == 3) ? "Buona" :
                                        (score == 4) ? "Forte" :
                                                "Molto forte";

        label.setText("Forza password: " + text);
    }
}
