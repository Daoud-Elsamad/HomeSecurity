package com.example.homesecurity.viewmodels;

public class da {
    private String name;
    private String email;
    private String phone;
    private String address;
    private String password;
    private String role;
    private String id;

    public da() {
    }
//
    public da(String name, String email, String phone, String address, String password, String role, String id) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.password = password;
        this.role = role;
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddress() {
        return address;
    }

    public String getPassword() {
        return password;
    }

    public String getRole() {
        return role;
    }

    public String getId() {
        return id;
    }
}
