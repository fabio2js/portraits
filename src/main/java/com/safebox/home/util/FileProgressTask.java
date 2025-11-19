package com.safebox.home.util;

import javafx.concurrent.Task;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.function.Consumer;

public abstract class FileProgressTask extends Task<Void> {

    protected final long totalBytes;
    protected final Consumer<Long> onProgress;

    public FileProgressTask(long totalBytes, Consumer<Long> onProgress) {
        this.totalBytes = totalBytes;
        this.onProgress = onProgress;
    }

    /**
     * Copia dati con callback di progresso + supporto annullamento.
     */
    protected void copyWithProgress(InputStream in, OutputStream out) throws IOException {

        byte[] buffer = new byte[8192];
        long copied = 0;
        int read;

        while ((read = in.read(buffer)) != -1) {

            // → controllo annullamento
            if (isCancelled()) {
                throw new IOException("Operazione annullata dall'utente");
            }

            out.write(buffer, 0, read);
            copied += read;

            // aggiorna la Task di JavaFX
            updateProgress(copied, totalBytes);

            // callback opzionale extra (velocità, ETA)
            if (onProgress != null) {
                onProgress.accept(copied);
            }
        }
    }
}
