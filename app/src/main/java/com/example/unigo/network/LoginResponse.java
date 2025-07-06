package com.example.unigo.network;

import com.google.gson.annotations.SerializedName;

public class LoginResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("id")
    private int id;

    @SerializedName("nombre")
    private String nombre;

    @SerializedName("email")
    private String email;

    @SerializedName("telefono")
    private String telefono;

    @SerializedName("foto")
    private String foto;

    public boolean isSuccess()  { return success; }
    public String getMessage()  { return message; }
    public int getId()          { return id; }
    public String getNombre()   { return nombre; }
    public String getEmail()    { return email; }
    public String getTelefono() { return telefono; }
    public String getFoto()     { return foto; }
}
