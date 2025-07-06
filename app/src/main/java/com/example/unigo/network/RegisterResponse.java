package com.example.unigo.network;

public class RegisterResponse {
    private boolean success;
    private String message;
    private String email;
    private String phone;
    private int id;
    private String photoUrl;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public int getId() { return id; }
    public String getPhotoUrl() { return photoUrl; }
}