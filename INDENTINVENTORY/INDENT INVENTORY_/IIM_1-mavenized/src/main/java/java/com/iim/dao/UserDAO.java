package com.iim.dao;

import com.iim.db.DBConnection;
import com.iim.models.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    /** ------------------ LOGIN ------------------ */
    public User authenticate(String username, String password){
        String sql = "SELECT id, username, category FROM users " +
                     "WHERE username=? AND password=? AND status='active' LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try(ResultSet rs = ps.executeQuery()){
                if(rs.next()){
                    User u = new User();
                    u.setId(rs.getInt("id"));
                    u.setUsername(rs.getString("username"));
                    u.setCategory(rs.getString("category"));
                    return u;
                }
            }
        } catch (SQLException e){
            e.printStackTrace();
        }
        return null;
    }

    /** ------------------ ADMIN FUNCTIONS ------------------ */

    private User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setCategory(rs.getString("category"));
        u.setUsername(rs.getString("username"));
        u.setPassword(rs.getString("password"));
        u.setStatus(rs.getString("status"));
        u.setDescription(rs.getString("description"));
        u.setCreatedAt(rs.getTimestamp("created_at"));
        return u;
    }

    public List<User> listAll() {
        String sql = "SELECT * FROM users ORDER BY id DESC";
        List<User> out = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(map(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return out;
    }

    public User findById(int id){
        String sql = "SELECT * FROM users WHERE id=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try(ResultSet rs = ps.executeQuery()){
                if(rs.next()) return map(rs);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean insert(User u){
        String sql = "INSERT INTO users(category, username, password, status, description) VALUES(?,?,?,?,?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, u.getCategory());
            ps.setString(2, u.getUsername());
            ps.setString(3, u.getPassword());
            ps.setString(4, u.getStatus());
            ps.setString(5, u.getDescription());
            return ps.executeUpdate() == 1;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean update(User u, boolean updatePassword){
        String sql = updatePassword
                ? "UPDATE users SET category=?, username=?, password=?, status=?, description=? WHERE id=?"
                : "UPDATE users SET category=?, username=?, status=?, description=? WHERE id=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, u.getCategory());
            ps.setString(i++, u.getUsername());
            if (updatePassword) ps.setString(i++, u.getPassword());
            ps.setString(i++, u.getStatus());
            ps.setString(i++, u.getDescription());
            ps.setInt(i, u.getId());
            return ps.executeUpdate() == 1;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public int countAdmins() {
        String sql = "SELECT COUNT(*) FROM users WHERE LOWER(category) = 'admin'";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public Integer findAdminId() {
        String sql = "SELECT id FROM users WHERE LOWER(category) = 'admin' LIMIT 1";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt("id");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }


    public boolean hasAnotherAdmin(int excludeUserId) {
        String sql = "SELECT id FROM users WHERE LOWER(category)='admin' AND id<>? LIMIT 1";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, excludeUserId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
