package com.example.unigo.network;

import com.google.gson.annotations.SerializedName;

public class GenericResponse {
    private boolean success;
    private String message;

    /** La URL pública de la imagen subida */
    @SerializedName("url")
    private String url;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getUrl() {
        return url;
    }
}