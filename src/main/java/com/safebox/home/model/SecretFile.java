package com.safebox.home.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class SecretFile {

    private String id;
    private String filename;
    private long size;
    private String mimeType;

    // Dove è memorizzato il file cifrato
    private String encryptedPath;

    // IV usato per AES/GCM base64
    private String iv;


}