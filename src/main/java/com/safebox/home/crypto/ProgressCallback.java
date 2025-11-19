package com.safebox.home.crypto;

public interface ProgressCallback {
    void onProgress(long written, long total);
}