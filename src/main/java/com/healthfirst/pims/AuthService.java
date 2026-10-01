package com.healthfirst.pims;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class AuthService {
    private AuthService() {
    }

    public static PimsUser authenticate(String username, char[] password) throws SQLException {
        String sql = "SELECT user_id, username, password, role, full_name "
                + "FROM users WHERE username = ? AND active = TRUE";
        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username.trim());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next() || !PasswordUtil.verify(password, result.getString("password"))) {
                    return null;
                }
                return new PimsUser(
                        result.getInt("user_id"),
                        result.getString("username"),
                        result.getString("full_name"),
                        result.getString("role"));
            }
        }
    }
}