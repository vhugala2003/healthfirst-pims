package com.healthfirst.pims;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Database {
    private static final String DEFAULT_URL =
            "jdbc:mysql://127.0.0.1:3306/healthfirst_pims?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    private Database() {
    }

    public static Connection connect() throws SQLException {
        String url = setting("DB_URL", DEFAULT_URL);
        String username = setting("DB_USER", "root");
        String password = System.getenv("DB_PASSWORD");
        if (password == null || password.isBlank()) {
            throw new SQLException("Set DB_PASSWORD before starting HealthFirst PIMS.");
        }
        return DriverManager.getConnection(url, username, password);
    }

    private static String setting(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}