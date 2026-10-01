package com.healthfirst.pims;

public record PimsUser(int id, String username, String fullName, String role) {
    public boolean isAdmin() {
        return "Admin".equals(role);
    }
}