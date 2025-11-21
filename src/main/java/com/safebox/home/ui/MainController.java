package com.safebox.home.ui;

import com.safebox.home.crypto.CryptoService;
import com.safebox.home.model.SecretEntry;
import com.safebox.home.model.SecretFile;
import com.safebox.home.persistence.VaultFormatV2;
import com.safebox.home.service.VaultContext;
import com.safebox.home.util.UIUtils;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import com.safebox.home.persistence.VaultRepository;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import lombok.Setter;

import javax.crypto.SecretKey;
import java.awt.*;
import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static com.safebox.home.util.UIUtils.*;

public class MainController {

    public Button homeButton;
    public TabPane mainTabs;
    @Setter
    private VaultContext context;

    private final VaultRepository repository = new VaultRepository();

    @FXML private TableView<SecretEntry> tableEntries;
    @FXML private TableColumn<SecretEntry, String> titleColumn;
    @FXML private TableColumn<SecretEntry, String> usernameColumn;
    @FXML private TableColumn<SecretEntry, String> categoryColumn;
    @FXML private TableColumn<SecretEntry, String> noteColumn;

    @FXML private TableView<SecretFile> fileTable;
    @FXML private TableColumn<SecretFile, String> fileNameColumn;
    @FXML private TableColumn<SecretFile, Number> fileSizeColumn;
    @FXML private TableColumn<SecretFile, String> fileTypeColumn;

    @FXML private TableColumn<SecretFile, Number> fileAddTimeColumn;
    @FXML private TableColumn<SecretFile, Number> fileLastEditColumn;


    @FXML private Label infoLabel;


    @FXML private ProgressBar fileProgressBar;
    @FXML private Label fileProgressPercentLabel;
    @FXML private Label fileProgressDetailsLabel;

    @FXML private Label    speedLabel;
    @FXML private Label    etaLabel;
    @FXML private Button  cancelButton;

    @FXML private TextField searchTextField;

    private ObservableList<SecretEntry> fullEntries = FXCollections.observableArrayList();
    private ObservableList<SecretFile> fullFiles    = FXCollections.observableArrayList();
    /**
     * Esporta un backup cifrato del vault:
     *  - safebox.vault (già cifrato)
     *  - directory files/ con i .enc (già cifrati)
     */
    private void exportEncryptedBackup(File outputFile) throws Exception {

        // Path del file vault, es: .../vaults/safebox.vault
        Path vaultPath = context.getVaultPath();

        // Directory dei file cifrati, es: .../vaults/files
        Path filesDir = vaultPath.getParent().resolve("files");

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outputFile))) {

            // 1) Aggiungi il file vault principale
            addToZip(zos, vaultPath.toFile(), "safebox.vault");

            // 2) Aggiungi i file cifrati, se la directory esiste
            if (Files.isDirectory(filesDir)) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(filesDir)) {
                    for (Path p : stream) {
                        if (Files.isRegularFile(p)) {
                            addToZip(zos, p.toFile(), "files/" + p.getFileName().toString());
                        }
                    }
                }
            }
        }
    }

    /**
     * Utility per aggiungere un file al file ZIP.
     */
    private void addToZip(ZipOutputStream zos, File file, String entryName) throws IOException {
        ZipEntry entry = new ZipEntry(entryName);
        zos.putNextEntry(entry);

        try (InputStream in = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) != -1) {
                zos.write(buffer, 0, len);
            }
        }

        zos.closeEntry();
    }






    @FXML
    private void handleRestoreBackup() throws Exception {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Ripristina backup SafeBox");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Backup Safebox (*.sbx)", "*.sbx")
        );

        File backup = chooser.showOpenDialog(fileTable.getScene().getWindow());
        if (backup == null) return;

        restoreBackup(backup);

    }


    private void clearProgressUI() {
        fileProgressBar.setVisible(false);
        fileProgressPercentLabel.setVisible(false);
        fileProgressDetailsLabel.setVisible(false);
        speedLabel.setVisible(false);
        etaLabel.setVisible(false);
        cancelButton.setVisible(false);

        fileProgressBar.progressProperty().unbind();
        fileProgressBar.setProgress(0);
    }



    private char[] masterPasswordCopy;

    private void restoreBackup(File backupFile) throws Exception {



        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setHeaderText("Ripristino backup");
        alert.setContentText("L’operazione sovrascriverà TUTTO il vault corrente, inclusi file e password. Procedere?");
        alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

        if (alert.showAndWait().orElse(ButtonType.NO) != ButtonType.YES)
            return;

        Path vaultPath = context.getVaultPath();          // es: .../vault.json
        Path vaultDir  = vaultPath.getParent();           // es: cartella vault/
        Path filesDir  = vaultDir.resolve("files");       // es: cartella vault/files/
        // === DIALOG PASSWORD MIGLIORATO ===
        Dialog<char[]> pwdDialog = new Dialog<>();
        pwdDialog.setTitle("Password vault");
        pwdDialog.setHeaderText("Inserisci la password del vault di backup");

// Bottoni
        ButtonType okType = new ButtonType("Ripristina", ButtonBar.ButtonData.OK_DONE);
        pwdDialog.getDialogPane().getButtonTypes().setAll(okType, ButtonType.CANCEL);

// Layout migliorato
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(15);
        grid.setPadding(new Insets(20, 20, 10, 20)); // padding superiore/laterale

        PasswordField pwdField = new PasswordField();
        pwdField.setPromptText("Password");
        pwdField.setPrefWidth(260); // campo più largo

        grid.add(new Label("Password:"), 0, 0);
        grid.add(pwdField, 1, 0);

// Aggiunge layout alla dialog
        pwdDialog.getDialogPane().setContent(grid);

// Focus automatico sul campo
        Platform.runLater(pwdField::requestFocus);

// ENTER → conferma
        pwdDialog.getDialogPane().lookupButton(okType).addEventFilter(
                ActionEvent.ACTION, event -> {
                    if (pwdField.getText() == null || pwdField.getText().isEmpty()) {
                        event.consume(); // blocca conferma
                    }
                }
        );

// Conversione risultato
        pwdDialog.setResultConverter(button -> {
            if (button == okType) {
                String txt = pwdField.getText();
                return (txt != null ? txt.toCharArray() : null);
            }
            return null;
        });

        pwdDialog.setResultConverter(button -> {
            if (button == okType) {
                String txt = pwdField.getText();
                return (txt != null ? txt.toCharArray() : null);
            }
            return null;
        });

        java.util.Optional<char[]> pwdOpt = pwdDialog.showAndWait();
        if (pwdOpt.isEmpty() || pwdOpt.get() == null || pwdOpt.get().length == 0) {
            // utente ha annullato o non ha inserito password
            return;
        }
        final char[] restorePassword = pwdOpt.get();

        // === VALIDAZIONE PASSWORD PRIMA DEL RIPRISTINO ===
        // Estraggo solo "safebox.vault" in una directory temporanea.
        Path tempVault = Files.createTempFile("vault_check_", ".vault");
        tempVault.toFile().deleteOnExit();

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(backupFile))) {
            ZipEntry en;
            while ((en = zis.getNextEntry()) != null) {
                if (en.getName().equals("safebox.vault")) {
                    try (OutputStream os = Files.newOutputStream(tempVault)) {
                        zis.transferTo(os);  // estrazione unica del file vault
                    }
                    break;
                }
                zis.closeEntry();
            }
        }

        // Leggo il SALT del vault dentro il backup
        byte[] saltCheck = VaultFormatV2.readSaltOnly(tempVault);

        // Provo a caricare il vault per validare la password (HMAC check)
        try {
            char[] pwdClone = restorePassword.clone();
            VaultContext tmp = repository.loadVaultV2(pwdClone, tempVault);

        } catch (Exception ex) {
            UIUtils.showError("Password errata. Impossibile ripristinare il backup.");
            return; // BLOCCA tutto
        }




        long totalBytes = backupFile.length();

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {

                long startNanos = System.nanoTime();

                // --------------------------------------------------------------------
                // 1) CANCELLAZIONE COMPLETA DEL VAULT PRECEDENTE
                // --------------------------------------------------------------------

                // Cancella vault.json
                if (Files.exists(vaultPath)) {
                    Files.deleteIfExists(vaultPath);
                }

                // Cancella tutto il contenuto di /files/
                if (Files.exists(filesDir)) {
                    try (DirectoryStream<Path> ds = Files.newDirectoryStream(filesDir)) {
                        for (Path p : ds) {
                            Files.deleteIfExists(p);
                        }
                    }
                } else {
                    Files.createDirectories(filesDir);
                }

                // --------------------------------------------------------------------
                // 2) ESTRAZIONE ZIP + AGGIORNAMENTO PROGRESSBAR
                // --------------------------------------------------------------------
                try (InputStream is = new FileInputStream(backupFile);
                     ZipInputStream zis = new ZipInputStream(is)) {

                    ZipEntry entry;
                    byte[] buffer = new byte[8192];
                    long copied = 0;

                    while ((entry = zis.getNextEntry()) != null) {

                        Path outPath = vaultDir.resolve(entry.getName());

                        if (entry.isDirectory()) {
                            Files.createDirectories(outPath);
                        } else {
                            Files.createDirectories(outPath.getParent());

                            try (OutputStream os = Files.newOutputStream(outPath)) {
                                long lastUpdate = 0;
                                int len;

                                while ((len = zis.read(buffer)) != -1) {
                                    if (isCancelled()) return null;

                                    os.write(buffer, 0, len);
                                    copied += len;

                                    // micro pausa ogni 512 KB
                                    if (copied % (1024 * 512) == 0) {
                                        Thread.sleep(1);
                                    }

                                    long now = System.currentTimeMillis();
                                    if (now - lastUpdate > 50) {
                                        lastUpdate = now;

                                        long finalCopied = copied;
                                        updateProgress(finalCopied, totalBytes);

                                        long elapsed = System.nanoTime() - startNanos;
                                        double sec = elapsed / 1_000_000_000.0;
                                        double speed = sec > 0 ? finalCopied / sec : 0;
                                        long remaining = totalBytes - finalCopied;
                                        double eta = speed > 0 ? remaining / speed : -1;

                                        Platform.runLater(() -> {
                                            fileProgressPercentLabel.setText(
                                                    String.format("%.0f%%", (finalCopied * 100.0) / totalBytes)
                                            );
                                            fileProgressDetailsLabel.setText(
                                                    String.format("%.2f MB/s", speed / (1024 * 1024))
                                            );
                                            etaLabel.setText(
                                                    eta > 0 ? "ETA: " + formatEta(eta) : "ETA..."
                                            );
                                        });
                                    }
                                }

                                // flush + sync per evitare letture su file incompleti
                                os.flush();
                                if (os instanceof FileOutputStream fos) {
                                    fos.getFD().sync();
                                }
                            }
                        }

                        zis.closeEntry();
                    }
                }

                return null;
            }
        };

        // ------------------------------------------------------------------------
        // UI BINDINGS
        // ------------------------------------------------------------------------
        fileProgressBar.progressProperty().bind(task.progressProperty());
        fileProgressBar.setVisible(true);

        fileProgressPercentLabel.setVisible(true);
        fileProgressDetailsLabel.setVisible(true);
        speedLabel.setVisible(true);
        etaLabel.setVisible(true);

        cancelButton.setVisible(true);
        cancelButton.setOnAction(e -> task.cancel());

        task.setOnSucceeded(e -> {
            clearProgressUI();
            try {
                // per sicurezza uso una copia della password
                char[] pwdForLoad = (restorePassword != null ? restorePassword.clone() : null);

                // ricarico il vault dal file ripristinato
                this.context = repository.loadVaultV2(
                        pwdForLoad,
                        vaultPath
                );

                // Propago la master password nel nuovo context
                this.context.setMasterPassword(
                        restorePassword != null ? restorePassword.clone() : null
                );

            } catch (Throwable ex) {
                UIUtils.showError("Errore durante il ripristino del backup");
                ex.printStackTrace();
                return;
            }

            showRestoreExportDialog();
            loadTable();
            loadFiles();
            updateVaultStats();
        });

        task.setOnFailed(e -> {
            clearProgressUI();
            UIUtils.showError("Errore durante il ripristino del backup");
            e.getSource().getException().printStackTrace();
        });

        task.setOnCancelled(e -> {
            clearProgressUI();
            UIUtils.showToast(fileTable, "Operazione annullata");
            currentFileTask = null;
        });

        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
    }






    @FXML
    private void handleExportBackup() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Esporta backup SafeBox");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Backup Safebox (*.sbx)", "*.sbx")
        );
        chooser.setInitialFileName("safebox_backup.sbx");

        File out = chooser.showSaveDialog(fileTable.getScene().getWindow());
        if (out == null) return;

        runBackupTask(out);
    }

    private long calcBackupTotalBytes(Path vaultPath, Path filesDir) {
        long total = 0;

        try {
            total += Files.size(vaultPath);
            if (Files.isDirectory(filesDir)) {
                try (DirectoryStream<Path> ds = Files.newDirectoryStream(filesDir)) {
                    for (Path p : ds) {
                        if (Files.isRegularFile(p)) {
                            total += Files.size(p);
                        }
                    }
                }
            }
        } catch (IOException ignored) {}

        return total == 0 ? 1 : total;
    }


    private void runBackupTask(File outputFile) {

        Path vaultPath = context.getVaultPath();
        Path filesDir = vaultPath.getParent().resolve("files");

        long totalBytes = calcBackupTotalBytes(vaultPath, filesDir);

        Task<Void> task = new Task<>() {

            @Override
            protected Void call() throws Exception {

                long start = System.nanoTime();
                long copied = 0;

                try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outputFile))) {

                    // ---------- 1) File vault ----------
                    copied = writeWithProgressInsideTask(zos, vaultPath.toFile(),
                            "safebox.vault", copied, totalBytes, start);

                    // ---------- 2) File cifrati ----------
                    if (Files.isDirectory(filesDir)) {
                        try (DirectoryStream<Path> ds = Files.newDirectoryStream(filesDir)) {
                            for (Path p : ds) {
                                if (!Files.isRegularFile(p)) continue;

                                copied = writeWithProgressInsideTask(
                                        zos, p.toFile(),
                                        "files/" + p.getFileName(),
                                        copied, totalBytes, start
                                );
                            }
                        }
                    }
                }

                return null;
            }


            // ---- QUESTO METODO È INTERNO AL TASK ----
            // ---- COSÌ PUÒ USARE updateProgress() LEGALMENTE ----
            private long writeWithProgressInsideTask(
                    ZipOutputStream zos,
                    File file,
                    String entryName,
                    long copiedSoFar,
                    long totalBytes,
                    long startNanos
            ) throws Exception {

                ZipEntry entry = new ZipEntry(entryName);
                zos.putNextEntry(entry);

                byte[] buf = new byte[8192];
                long copied = 0;

                try (InputStream in = new FileInputStream(file)) {

                    int len;
                    while ((len = in.read(buf)) != -1) {

                        if (isCancelled()) break;
                        zos.write(buf, 0, len);

                        copied += len;
                        long globalCopied = copiedSoFar + copied;

                        updateProgress(globalCopied, totalBytes);

                        long elapsedNs = System.nanoTime() - startNanos;
                        double sec = elapsedNs / 1_000_000_000.0;
                        double speed = sec > 0 ? globalCopied / sec : 0;
                        double speedMb = speed / (1024 * 1024);
                        long remaining = totalBytes - globalCopied;
                        double eta = speed > 0 ? remaining / speed : -1;

                        Platform.runLater(() -> {
                            fileProgressPercentLabel.setText(
                                    String.format("%.0f%%", (globalCopied * 100.0) / totalBytes)
                            );
                            fileProgressDetailsLabel.setText(String.format("%.2f MB/s", speedMb));
                            etaLabel.setText(
                                    eta > 0 ? "ETA: " + formatEta(eta) : "ETA..."
                            );
                        });
                    }
                }

                zos.closeEntry();
                return copiedSoFar + copied;
            }
        };

        // ---- UI bindings ----
        currentFileTask = task;
        prepareProgressUi();
        fileProgressBar.progressProperty().bind(task.progressProperty());

        speedLabel.setVisible(true);
        etaLabel.setVisible(true);
        cancelButton.setVisible(true);
        cancelButton.setOnAction(e -> task.cancel());

        task.setOnSucceeded(e -> {
            clearProgressUI();
            showBackupExportDialog(outputFile);
            currentFileTask = null;
        });

        task.setOnFailed(e -> {
            clearProgressUI();
            UIUtils.showError("Errore durante l'esportazione del backup");
            e.getSource().getException().printStackTrace();
            currentFileTask = null;
        });

        task.setOnCancelled(e -> {
            clearProgressUI();
            UIUtils.showToast(fileTable, "Operazione annullata");
            currentFileTask = null;
        });

        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
    }

    private static final long INACTIVITY_TIMEOUT_MS = 3 * 60000L; // 3 minuti
    private javafx.animation.Timeline inactivityTimer;
    private boolean locked = false;
    @FXML
    private void initialize() {
// ----- SEARCH -----
        searchTextField.textProperty().addListener((obs, oldV, newV) -> {
            applySearchFilter(newV);
        });

// ----- HOME BUTTON -----
        homeButton.setOnAction(e -> {
            searchTextField.clear();
            applySearchFilter("");
        });
        unlockButton.setOnAction(e -> unlockScreen(unlockPasswordField.getText().toCharArray()));

        unlockPasswordField.setOnAction(e ->
                unlockScreen(unlockPasswordField.getText().toCharArray()));


    }




    @FXML private VBox lockPane;
    @FXML private PasswordField unlockPasswordField;
    @FXML private Button unlockButton;
    @FXML private Label unlockErrorLabel;
    @FXML private BorderPane appRoot;


    private void unlockScreen(char[] pwd) {
        try {
            // 1) Ricostruzione KEY da password
            byte[] keyMaterial = VaultFormatV2.deriveKeyMaterial(
                    pwd,
                    context.getSalt(),
                    context.getIterations(),
                    512
            );

            byte[] expected = context.getKeyEnc().getEncoded();
            byte[] candidate = Arrays.copyOfRange(keyMaterial, 0, 32);

            // 2) Confronto costante contro keyEnc
            if (!Arrays.equals(expected, candidate)) {
                unlockErrorLabel.setText("Password errata");
                return;
            }

            // 3) Sblocco riuscito
            locked = false;

            lockPane.setVisible(false);     // nasconde overlay
            mainRoot.setDisable(false);     // riabilita UI
            unlockPasswordField.clear();
            unlockErrorLabel.setText("");
            lockPane.setDisable(true);
            appRoot.setEffect(null);

            appRoot.setDisable(false);
            // reset timer inattività
            resetInactivityTimer();

        } catch (Exception ex) {
            unlockErrorLabel.setText("Errore durante lo sblocco");
        }
    }

    private void lockScreen() {
        locked = true;

        List<Window> windows = new ArrayList<>(Window.getWindows());
        for (Window w : windows) {
            if (w instanceof Stage stage) {
                if (!stage.equals(mainRoot.getScene().getWindow())) {
                    stage.close();
                }
            }
        }

        // Effetto blur su tutto il contenuto
        GaussianBlur blur = new GaussianBlur(25);
        appRoot.setEffect(blur);

        // Overlay "tenda" scura
        lockPane.setStyle("-fx-background-color: rgba(0,0,0,0.60);");

        lockPane.setVisible(true);
        lockPane.setDisable(false);

        unlockPasswordField.clear();
        unlockErrorLabel.setText("");

        appRoot.setDisable(true);
    }
    @FXML private StackPane mainRoot;



    private void showRestoreExportDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Ripristino effettuato");
        alert.setHeaderText("Ripristino dati OK");

        alert.setContentText("Ripristino dati avvenuto correttamente");
        // opzionale: finestra sopra tutte
        Stage stage = (Stage) fileTable.getScene().getWindow();
        alert.initOwner(stage);

        alert.showAndWait();
    }
    private void showBackupExportDialog(File output) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Backup esportato");
        alert.setHeaderText("Backup esportato correttamente");
        alert.setContentText("Percorso del file:\n" + output.getAbsolutePath());

        // opzionale: finestra sopra tutte
        Stage stage = (Stage) fileTable.getScene().getWindow();
        alert.initOwner(stage);

        alert.showAndWait();
    }

    private void registerSceneActivity(Scene scene) {
        scene.addEventFilter(MouseEvent.ANY, e -> resetInactivityTimer());
        scene.addEventFilter(KeyEvent.ANY, e -> resetInactivityTimer());
    }
    private javafx.animation.Timeline countdownTimer;
    @FXML private Label locklockCountdownLabel;

    private final CryptoService crypto = new CryptoService();
    private Task currentFileTask;

    public void init(VaultContext context) {
        tableEntries.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        fileTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        Scene scene = mainRoot.getScene();
        if (scene != null) {
            registerSceneActivity(scene);
        } else {
            // scena non pronta, la registriamo dopo il render
            Platform.runLater(() -> registerSceneActivity(mainRoot.getScene()));
        }
        this.context = context;
        char[] mp = context.getMasterPassword();
        this.masterPasswordCopy = (mp != null ? mp.clone() : null);
        // password table
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        noteColumn.setCellValueFactory(new PropertyValueFactory<>("notes"));
        tableEntries.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);


        noteColumn.setCellFactory(col -> new TableCell<SecretEntry, String>() {
            private final Label lbl = new Label();

            {
                lbl.setWrapText(false);
                lbl.setMaxHeight(Double.MAX_VALUE);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    lbl.setText(item);
                    setGraphic(lbl);

                    setPrefHeight(24);
                    setMinHeight(24);
                    setMaxHeight(24);
                }
            }
        });






        loadTable();

        // file table
        fileNameColumn.setCellValueFactory(new PropertyValueFactory<>("filename"));
        fileTypeColumn.setCellValueFactory(new PropertyValueFactory<>("mimeType"));
        fileSizeColumn.setCellValueFactory(new PropertyValueFactory<>("size"));

        // === FORMATTER DATE addTime / lastEdit ===
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        fileAddTimeColumn.setCellValueFactory(new PropertyValueFactory<>("addTime"));
        fileLastEditColumn.setCellValueFactory(new PropertyValueFactory<>("lastEdit"));

        fileAddTimeColumn.setCellFactory(col -> new TableCell<SecretFile, Number>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || ((long)item) == 0) {
                    setText(null);
                } else {
                    setText(Instant.ofEpochMilli(item.longValue())
                            .atZone(ZoneId.systemDefault())
                            .format(dtf));
                }
            }
        });

        fileLastEditColumn.setCellFactory(col -> new TableCell<SecretFile, Number>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || ((long)item) == 0) {
                    setText(null);
                } else {
                    setText(Instant.ofEpochMilli(item.longValue())
                            .atZone(ZoneId.systemDefault())
                            .format(dtf));
                }
            }
        });







        // formatter per dimensioni leggibili
        fileSizeColumn.setCellFactory(col -> new TableCell<SecretFile, Number>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(humanReadableSize(item.longValue()));
                }
            }
        });

        //fileTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        fileNameColumn.setPrefWidth(250);
        fileSizeColumn.setPrefWidth(120);
        fileTypeColumn.setPrefWidth(150);
        fileLastEditColumn.setPrefWidth(160);
        fileAddTimeColumn.setPrefWidth(160);

// Evita che le colonne collassino
        fileNameColumn.setMinWidth(150);
        fileSizeColumn.setMinWidth(100);
        fileTypeColumn.setMinWidth(120);
        fileLastEditColumn.setMinWidth(140);
        fileAddTimeColumn.setMinWidth(140);

        loadFiles();

        // stato iniziale progress
        fileProgressBar.setVisible(false);
        fileProgressPercentLabel.setVisible(false);
        fileProgressDetailsLabel.setVisible(false);
        startInactivityTimer();
        registerActivityListeners();
        updateVaultStats();
    }
    private Timeline countdownTimeline;
    private int secondsLeft;
    private void startCountdown(int seconds) {
        secondsLeft = seconds;

        // Animazione fade-in se invisibile
        if (countdownLabel.getOpacity() == 0) {
            FadeTransition ft = new FadeTransition(Duration.millis(400), countdownLabel);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();
        }

        if (countdownTimeline != null) countdownTimeline.stop();

        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateCountdown())
        );
        countdownTimeline.setCycleCount(seconds);
        countdownTimeline.playFromStart();
    }

    private void updateCountdown() {
            long sec = remainingMillis.get() / 1000;

            if (sec <= 0) {
                countdownLabel.setVisible(false);
                return;
            }

            countdownLabel.setVisible(true);
            countdownLabel.setText("⏱ " + sec + "s");

            if (sec <= 5) {
                countdownLabel.setStyle("-fx-text-fill: red; -fx-font-size: 14px; "
                        + "-fx-background-color: rgba(0,0,0,0.35); -fx-padding: 6;");
            } else if (sec <= 10) {
                countdownLabel.setStyle("-fx-text-fill: orange; -fx-font-size: 14px; "
                        + "-fx-background-color: rgba(0,0,0,0.35); -fx-padding: 6;");
            } else {
                countdownLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; "
                        + "-fx-background-color: rgba(0,0,0,0.35); -fx-padding: 6;");
            }
        }




    @FXML private Label countdownLabel;



    protected void resetInactivityTimer() {
        if (locked) return;
        remainingMillis.set(INACTIVITY_TIMEOUT_MS);
        startInactivityTimer(); // <-- ricarica entrambi i timer in modo pulito
    }

    private void registerActivityListeners() {
        // Listener globale per tutto il runtime JavaFX
        EventHandler<Event> globalActivityListener = e -> resetInactivityTimer();

        // Intercetta mouse e tastiera OVUNQUE
        Toolkit.getDefaultToolkit().addAWTEventListener(
                event -> resetInactivityTimer(),
                AWTEvent.MOUSE_EVENT_MASK | AWTEvent.KEY_EVENT_MASK
        );

        // Intercetta eventi JavaFX (senza limitarsi a mainRoot)
        Scene scene = mainRoot.getScene();
        if (scene != null) {
            scene.addEventFilter(MouseEvent.ANY, globalActivityListener);
            scene.addEventFilter(KeyEvent.ANY, globalActivityListener);
        }
    }

    private final AtomicLong remainingMillis = new AtomicLong(INACTIVITY_TIMEOUT_MS);

    private void startInactivityTimer() {

        // Stoppa timer vecchi per evitare Timeline sovrapposte
        if (inactivityTimer != null) inactivityTimer.stop();
        if (countdownTimer != null) countdownTimer.stop();
       // Timer di lock
        inactivityTimer = new Timeline(
                new KeyFrame(Duration.millis(remainingMillis.get()), e -> lockScreen())
        );
        inactivityTimer.setCycleCount(1);

        // Timer countdown (1 secondo per tick)
        countdownTimer = new Timeline(
                new KeyFrame(Duration.seconds(1), e -> {
                    remainingMillis.addAndGet(-1000);
                    updateCountdown();

                    if (remainingMillis.get() <= 0) {
                        countdownTimer.stop();
                    }
                })
        );
        countdownTimer.setCycleCount(Animation.INDEFINITE);

        inactivityTimer.playFromStart();
        countdownTimer.playFromStart();
    }
    private void loadFiles() {

        ObservableList<SecretFile> data = FXCollections.observableArrayList();
        Path vaultFile = context.getVaultPath();
        Path vaultDir = (vaultFile != null) ? vaultFile.getParent() : null;

        Iterator<SecretFile> iter = context.getVault().getFiles().iterator();

        while (iter.hasNext()) {
            SecretFile e = iter.next();
            try {
                String encPath = e.getEncryptedPath();
                if (encPath != null && !encPath.isBlank() && vaultDir != null) {
                    Path filePath = vaultDir.resolve(encPath);

                    if (Files.exists(filePath)) {
                        e.setSize(Files.size(filePath));
                        if (e.getMimeType() == null || e.getMimeType().isBlank() || e.getMimeType().equals("?")) {
                            e.setMimeType(guessMimeFromName(e.getFilename()));
                        }
                        data.add(e);
                    } else {
                        iter.remove();
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        fullFiles.setAll(data);
        fileTable.setItems(FXCollections.observableArrayList(fullFiles));
    }

    private void applySearchFilter(String text) {

        if (text == null || text.isBlank()) {
            tableEntries.setItems(FXCollections.observableArrayList(fullEntries));
            fileTable.setItems(FXCollections.observableArrayList(fullFiles));
            return;
        }

        String lower = text.toLowerCase();

        // tab attivo
        int tabIndex = mainTabs.getSelectionModel().getSelectedIndex();

        if (tabIndex == 0) { // PASSWORD
            ObservableList<SecretEntry> filtered =
                    fullEntries.filtered(e ->
                            e.getTitle().toLowerCase().contains(lower)
                                    || e.getUsername().toLowerCase().contains(lower)
                                    || e.getCategory().toLowerCase().contains(lower)
                                    || e.getNotes().toLowerCase().contains(lower)
                    );

            tableEntries.setItems(filtered);
        } else { // FILE
            ObservableList<SecretFile> filtered =
                    fullFiles.filtered(f ->
                            f.getFilename().toLowerCase().contains(lower)
                                    || f.getMimeType().toLowerCase().contains(lower)
                    );

            fileTable.setItems(filtered);
        }
    }



    private String guessMimeFromName(String name) {
        try {
            return Files.probeContentType(Path.of(name));
        } catch (Exception e) {
            return "application/octet-stream";
        }
    }

    private void loadTable() {
        fullEntries.setAll(context.getVault().getEntries());
        tableEntries.setItems(FXCollections.observableArrayList(fullEntries));

        tableEntries.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        updateVaultStats();
    }



    // formatter dimensioni file
    private static String formatEta(double seconds) {
        if (seconds <= 0) {
            return "calcolo in corso";
        }
        long s = (long) seconds;
        long h = s / 3600;
        long m = (s % 3600) / 60;
        long sec = s % 60;
        if (h > 0) {
            return String.format("%dh %02dm %02ds", h, m, sec);
        } else if (m > 0) {
            return String.format("%dm %02ds", m, sec);
        } else {
            return String.format("%ds", sec);
        }
    }

    // ================== PASSWORD ENTRIES ==================

    @FXML
    private void handleEdit() {
        SecretEntry selected = tableEntries.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/entry_edit.fxml")
            );
            Parent root = loader.load();

            EntryEditController controller = loader.getController();
            controller.setMainController(this);
            // Passiamo un callback che verrà chiamato quando l'entry è salvata
            controller.init(selected, () -> {
                try {
                    // 1) Refresh tabella
                    tableEntries.refresh();

                    // 2) Salva vault aggiornato su disco
                    repository.saveVaultV2(context, context.getVault());

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            Stage stage = new Stage();
            stage.setTitle("Modifica voce");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.addEventFilter(MouseEvent.ANY, e -> resetInactivityTimer());
            stage.addEventFilter(KeyEvent.ANY, e -> resetInactivityTimer());
            stage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @FXML
    private void handleAdd() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/add_entry.fxml"));

            Stage dialog = new Stage();
            dialog.setScene(new Scene(loader.load()));
            dialog.setTitle("Nuova voce");
            dialog.initOwner(tableEntries.getScene().getWindow());
            dialog.setResizable(false);

            AddEntryController ctrl = loader.getController();
            ctrl.setMainController(this);
            dialog.addEventFilter(MouseEvent.ANY, e -> resetInactivityTimer());
            dialog.addEventFilter(KeyEvent.ANY, e -> resetInactivityTimer());
            dialog.showAndWait();

            if (ctrl.isSaved()) {
                // 1) aggiorniamo il modello in memoria
                context.getVault().add(ctrl.getEntry());

                // 2) persistiamo il vault sul file .vault (formato V2)
                repository.saveVaultV2(context, context.getVault());

                // 3) aggiorniamo la tabella a video
                loadTable();
            }

        } catch (Exception e) {
            e.printStackTrace();
            showError("Errore durante il salvataggio della voce");
        }
    }

    private void updateVaultStats() {
        if (context == null || context.getVault() == null) {

            if (infoLabel != null) {
                infoLabel.setText("Voci nel vault: 0 | Files nel valut: 0"   );
            }
            return;
        }

        int entryCount = context.getVault().getEntries().size();
        int fileCount  = fileTable.getItems().size();

        if (infoLabel != null) {
            infoLabel.setText("Voci nel vault: " + entryCount + " | Files nel vault: "+fileCount);
        }



    }



    // ================== FILE HANDLERS ==================

    private void prepareProgressUi() {
        fileProgressBar.setProgress(0);
        fileProgressBar.setVisible(true);
        fileProgressPercentLabel.setText("0%");
        fileProgressPercentLabel.setVisible(true);
        fileProgressDetailsLabel.setText("");
        fileProgressDetailsLabel.setVisible(true);
    }

    private void resetProgressUi() {
        fileProgressBar.progressProperty().unbind();
        fileProgressBar.setProgress(0);
        fileProgressBar.setVisible(false);
        fileProgressPercentLabel.setVisible(false);
        fileProgressDetailsLabel.setVisible(false);
    }

    @FXML
    private void handleAddFile() {

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Aggiungi file al vault");
        File file = chooser.showOpenDialog(fileTable.getScene().getWindow());
        if (file == null) return;

        long totalBytes = file.length();

        if (totalBytes > CryptoService.MAX_GCM_SIZE_BYTES) {
            showError(
                    "Il file è troppo grande per la cifratura attuale (limite circa 2 GB per file).\n"
                            + "Dimensione file: " + humanReadableSize(totalBytes)
            );
            return;
        }

        String id = UUID.randomUUID().toString();

        // IV AES-GCM per questo file
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);


        Task<byte[]> task = new Task<byte[]>() {

            @Override
            protected byte[] call() throws Exception {

                long startNanos = System.nanoTime();
                long copied = 0;

                ByteArrayOutputStream plainOut = new ByteArrayOutputStream(8192);

                try (InputStream in = Files.newInputStream(file.toPath())) {

                    byte[] buf = new byte[65536];
                    int read;
                    long lastUpdate =  0L;

                    while ((read = in.read(buf)) != -1) {

                        if (isCancelled()) return null;

                        long now = System.currentTimeMillis();

                        if (now - lastUpdate > 50) {
                            lastUpdate = now;
                            updateProgress(copied, totalBytes);
                            long elapsed = System.nanoTime() - startNanos;
                            double sec = elapsed / 1_000_000_000.0;
                            double speed = sec > 0 ? copied / sec : 0;

                            long remaining = totalBytes - copied;
                            double eta = speed > 0 ? remaining / speed : -1;

                            double percent = (copied * 100.0) / totalBytes;

                            String etaText = eta > 0 ? formatEta(eta) : "calcolo…";
                            double speedMB = speed / (1024 * 1024);

                            Platform.runLater(() -> {
                                fileProgressPercentLabel.setText(String.format("%.0f%%", percent));
                                fileProgressDetailsLabel.setText(String.format("%.2f MB/s, ETA: %s",
                                        speedMB, etaText));
                            });
                        }
                        plainOut.write(buf, 0, read);
                        copied += read;




                    }
                }

                // Cifra il contenuto del file
                byte[] plain = plainOut.toByteArray();
                SecretKey fileKey = context.getKeyEnc();
                if (fileKey == null) {
                    fileKey = crypto.deriveKeyFromPassword(
                            masterPasswordCopy,
                            context.getSalt(),
                            CryptoService.ITERATIONS
                    );
                    context.setKeyEnc(fileKey);
                }


                return crypto.encrypt(fileKey, iv, plain);
            }
        };

        // ---- UI ----
        currentFileTask = task;
        prepareProgressUi();
        fileProgressBar.progressProperty().bind(task.progressProperty());

        task.setOnSucceeded(e -> {
            resetProgressUi();

            byte[] cipher = task.getValue();

            SecretFile sf = new SecretFile();
            sf.setId(id);
            sf.setFilename(file.getName());
            sf.setSize(file.length());
            try {
                sf.setMimeType(Files.probeContentType(file.toPath()));
            } catch (IOException ex) {
                sf.setMimeType("?");
            }
            sf.setIv(Base64.getEncoder().encodeToString(iv));
            sf.setEncryptedPath("files/" + id + ".enc");
            sf.setIv(Base64.getEncoder().encodeToString(iv));
            sf.setSize(file.length());
            sf.setAddTime(System.currentTimeMillis());


            context.getVault().getFiles().add(sf);
            try {
                Path vaultDir = context.getVaultPath().getParent();
                Path filesDir = vaultDir.resolve("files");
                Files.createDirectories(filesDir);

                Path encPath = filesDir.resolve(id + ".enc");
                Files.write(encPath, cipher, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            } catch (IOException ex) {
                ex.printStackTrace();
                showError("Errore nel salvataggio del file cifrato su disco");
                return;
            }
            try {
                repository.saveVaultV2(context, context.getVault());
            } catch (Exception ex) {
                ex.printStackTrace();
                showError("Errore durante il salvataggio del vault");
            }

            loadFiles();
            updateVaultStats();
            UIUtils.showToast(fileTable, "File aggiunto");
            currentFileTask = null;
        });

        task.setOnFailed(e -> {
            resetProgressUi();
            showError("Errore nel caricamento/cifratura del file");
            task.getException().printStackTrace();
            currentFileTask = null;
        });

        task.setOnCancelled(e -> {

            resetProgressUi();
            UIUtils.showToast(fileTable, "Operazione annullata");
            currentFileTask = null;
        });

        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
    }


    @FXML
    private void handleOpenFile() {

        SecretFile sf = fileTable.getSelectionModel().getSelectedItem();
        if (sf == null) return;

        Path vaultDir = context.getVaultPath().getParent();
        Path encrypted = vaultDir.resolve(sf.getEncryptedPath());

        if (!Files.exists(encrypted)) {
            showError("File non trovato nel vault");
            return;
        }

        // File temporaneo
        Path temp;
        try {
            temp = Files.createTempFile("safebox_", "_" + sf.getFilename());
            temp.toFile().deleteOnExit();
        } catch (IOException e) {
            showError("Impossibile creare file temporaneo");
            return;
        }

        // IV
        byte[] iv = Base64.getDecoder().decode(sf.getIv());
        long totalBytes = sf.getSize();

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {

                long startNanos = System.nanoTime();

                crypto.decryptFileStreaming(
                        encrypted,
                        temp,
                        context.getKeyEnc(),   // chiave corretta!
                        iv,
                        copied -> {
                            if (isCancelled()) return;

                            double progress = (double) copied / totalBytes;
                            updateProgress(copied, totalBytes);

                            long elapsedNanos = System.nanoTime() - startNanos;
                            double elapsedSec = elapsedNanos / 1_000_000_000.0;

                            double speedBytes = elapsedSec > 0 ? copied / elapsedSec : 0;
                            double speedMB = speedBytes / (1024.0 * 1024.0);

                            long remaining = totalBytes - copied;
                            double etaSec = speedBytes > 0 ? remaining / speedBytes : -1;

                            String etaText = etaSec > 0 ? formatEta(etaSec) : "calcolo…";

                            Platform.runLater(() -> {
                                fileProgressPercentLabel.setText(String.format("%.0f%%", progress * 100));
                                fileProgressDetailsLabel.setText(
                                        String.format("%.2f MB/s, ETA: %s", speedMB, etaText)
                                );
                            });
                        }
                );

                return null;
            }
        };

        // UI
        currentFileTask = task;
        prepareProgressUi();
        fileProgressBar.progressProperty().bind(task.progressProperty());



        task.setOnSucceeded(e -> {
            resetProgressUi();
            try {
                // Apri il file in chiaro
                Desktop.getDesktop().open(temp.toFile());

                // === AGGIORNA LA DATA DI ULTIMO ACCESSO ===
                sf.setLastEdit(System.currentTimeMillis());

                // Salva vault aggiornato
                repository.saveVaultV2(context, context.getVault());

                // Aggiorna la tabella a video
                fileTable.refresh();

            } catch (Exception ex) {
                showError("Errore durante l'apertura del file");
            }
            currentFileTask = null;
        });

        task.setOnFailed(e -> {
            resetProgressUi();
            showError("Errore durante la decifratura del file");
            if (task.getException() != null) task.getException().printStackTrace();
            currentFileTask = null;
        });

        task.setOnCancelled(e -> {
            resetProgressUi();
            UIUtils.showToast(fileTable, "Operazione annullata");
            currentFileTask = null;
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }





    @FXML
    private void handleExportFile() {

        SecretFile selected = fileTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        try {
            Path vaultDir = context.getVaultPath().getParent();
            Path encryptedPath = vaultDir.resolve(selected.getEncryptedPath());

            if (!Files.exists(encryptedPath)) {
                showError("File cifrato non trovato nel vault");
                return;
            }

            FileChooser chooser = new FileChooser();
            chooser.setTitle("Esporta file");
            chooser.setInitialFileName(selected.getFilename());

            File dest = chooser.showSaveDialog(fileTable.getScene().getWindow());
            if (dest == null) return;

            long totalBytes = Files.size(encryptedPath);
            byte[] iv = Base64.getDecoder().decode(selected.getIv());

            Task<Void> task = new Task<>() {

                @Override
                protected Void call() throws Exception {

                    long startNanos = System.nanoTime();

                    crypto.decryptFileStreaming(
                            encryptedPath,
                            dest.toPath(),
                            context.getKeyEnc(),     // CHIAVE GIUSTA
                            iv,
                            copied -> {
                                if (isCancelled()) return;

                                double progress = (double) copied / totalBytes;
                                updateProgress(copied, totalBytes);

                                long elapsedNanos = System.nanoTime() - startNanos;
                                double elapsedSec = elapsedNanos / 1_000_000_000.0;

                                double speedBytes = elapsedSec > 0 ? copied / elapsedSec : 0;
                                double speedMB = speedBytes / (1024.0 * 1024.0);

                                long remaining = totalBytes - copied;
                                double etaSec = speedBytes > 0 ? remaining / speedBytes : -1;

                                String etaText = etaSec > 0 ? formatEta(etaSec) : "calcolo…";

                                Platform.runLater(() -> {
                                    fileProgressPercentLabel.setText(String.format("%.0f%%", progress * 100));
                                    fileProgressDetailsLabel.setText(
                                            String.format("%.2f MB/s, ETA: %s", speedMB, etaText)
                                    );
                                });
                            }
                    );

                    return null;
                }
            };

            // ---- UI ----
            currentFileTask = task;
            prepareProgressUi();
            fileProgressBar.progressProperty().bind(task.progressProperty());

            task.setOnSucceeded(e -> {
                resetProgressUi();
                selected.setLastEdit(System.currentTimeMillis());
                fileTable.refresh();
                UIUtils.showToast(fileTable, "File esportato");
                currentFileTask = null;
            });

            task.setOnFailed(e -> {
                resetProgressUi();
                showError("Errore durante l'estrazione del file");
                if (task.getException() != null) task.getException().printStackTrace();
                currentFileTask = null;
            });

            task.setOnCancelled(e -> {
                resetProgressUi();
                UIUtils.showToast(fileTable, "Operazione annullata");
                currentFileTask = null;
            });

            Thread t = new Thread(task);
            t.setDaemon(true);
            t.start();

        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Errore durante l'esportazione del file");
        }
    }
    private void deleteEntries(){
        List<SecretEntry> list = new ArrayList<>(tableEntries.getSelectionModel().getSelectedItems());

        context.getVault().getEntries().removeAll(list);

        try {
            repository.saveVaultV2(context, context.getVault());
            loadTable();
            UIUtils.showToast(tableEntries, list.size() + " voci eliminate");
        } catch (Exception e) {
            e.printStackTrace();
            showError("Errore durante il salvataggio del vault");
        }
    }

    @FXML
    private void handleDeleteEntry() {

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Conferma eliminazione");
        confirm.setHeaderText("Vuoi eliminare definitivamente le voci selezionate?");
        confirm.setContentText("Elimina Voci");

        ButtonType yes = new ButtonType("Elimina", ButtonBar.ButtonData.OK_DONE);
        ButtonType no = new ButtonType("Annulla", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(yes, no);

        confirm.initOwner(tableEntries.getScene().getWindow());

        confirm.showAndWait().ifPresent(response -> {
            if (response == yes) {
                deleteEntries();
            }
        });
    }


    private void deleteFiles() throws Exception {
        List<SecretFile> toDelete = new ArrayList<>(fileTable.getSelectionModel().getSelectedItems());
        Path vaultDir = context.getVaultPath().getParent();

        for (SecretFile sf : toDelete) {
            try {
                Path encryptedPath = vaultDir.resolve(sf.getEncryptedPath());
                if (Files.exists(encryptedPath)) {
                    Files.delete(encryptedPath);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                // continuiamo comunque con gli altri
            }
        }

// rimuovi dal vault
        context.getVault().getFiles().removeAll(toDelete);

// salva vault
        repository.saveVaultV2(context, context.getVault());

// aggiorna UI
        loadFiles();
        updateVaultStats();

        showToast(fileTable, toDelete.size() + " file eliminati");
    }


    @FXML
    private void handleDeleteFile() {


        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Conferma eliminazione file");
        confirm.setHeaderText("Vuoi eliminare questi file dal vault?");
        confirm.setContentText("eliminazione files dal vault");

        ButtonType yes = new ButtonType("Elimina", ButtonBar.ButtonData.OK_DONE);
        ButtonType no  = new ButtonType("Annulla", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(yes, no);
        confirm.initOwner(fileTable.getScene().getWindow());

        confirm.showAndWait().ifPresent(response -> {
            if (response == yes) {
                try {
                    // 1) elimina i fiels  cifrato dal filesystem (se esiste)
                    deleteFiles();

                } catch (Exception e) {
                    e.printStackTrace();
                    showToast(fileTable, "Errore eliminazione file dal vault");
                }
            }
        });
    }


    // ================== CANCEL BUTTON ==================

    @FXML
    private void handleCancelFileOperation() {
        if (currentFileTask != null && currentFileTask.isRunning() ) {
            currentFileTask.cancel();
        }
    }
}
