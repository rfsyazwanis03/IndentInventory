package com.iim.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DBConnection {

    // Tukar ke Asia/Kuala_Lumpur (bukan UTC)
    private static final String URL =
        "jdbc:mysql://localhost:3306/indent_inventory"
      + "?useUnicode=true&characterEncoding=UTF-8&useSSL=false"
      + "&serverTimezone=Asia/Kuala_Lumpur";

    private static final String USER = "root";
    private static final String PASS = "admin";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL JDBC driver not found in Libraries.", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        Connection c = DriverManager.getConnection(URL, USER, PASS);
        // Kunci: set session time_zone ke MYT untuk connection ini
        try (Statement st = c.createStatement()) {
            st.execute("SET time_zone = '+08:00'"); // Malaysia time
            st.execute("SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci");
        } catch (SQLException e) {
            System.err.println("[DBConnection] Warning: failed to set session options: " + e.getMessage());
        }
        return c;
    }
}
