package com.iim.dao;

import com.iim.db.DBConnection;     
import com.iim.models.Department;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DepartmentDAO {

    private Connection getConn() throws SQLException {
        return DBConnection.getConnection();
    }

    private Department mapRow(ResultSet rs) throws SQLException {
        Department d = new Department();
        d.setId(rs.getInt("id"));
        d.setCode(rs.getString("code"));
        d.setName(rs.getString("name"));
        d.setStatus(rs.getString("status"));

        // --- created_at: tahan daripada ClassCastException ---
        Timestamp ts = null;
        try { ts = rs.getTimestamp("created_at"); } catch (SQLException ignore) { /* kolum mungkin tiada */ }

        if (ts != null) {
            // Cuba panggil setCreatedAt(Timestamp)
            try {
                Department.class.getMethod("setCreatedAt", java.sql.Timestamp.class).invoke(d, ts);
            } catch (NoSuchMethodException e1) {
                // Kalau tiada, cuba setCreatedAt(Date)
                try {
                    Department.class.getMethod("setCreatedAt", java.util.Date.class)
                            .invoke(d, new java.util.Date(ts.getTime()));
                } catch (NoSuchMethodException e2) {
                    // Tiada setter – abaikan createdAt (tak ganggu fungsi lain)
                } catch (Exception reflectErr) {
                    // Setter wujud tapi gagal -> abaikan sahaja supaya tak block Quick Scan
                }
            } catch (Exception reflectErr) {
                // Setter wujud tapi gagal -> abaikan sahaja
            }
        }
        return d;
    }

    /** Senarai department aktif; sokong status 'Active' atau 1 */
    public List<Department> listActive() {
        String sql =
            "SELECT id, code, name, status, created_at " +
            "FROM departments " +
            "WHERE (status = 'Active' OR status = 'ACTIVE' OR status = 'active' OR status = '1' OR status = 1) " +
            "ORDER BY code ASC, name ASC";

        List<Department> list = new ArrayList<>();
        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) list.add(mapRow(rs));
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load active departments", e);
        }
    }

    /** (Opsyenal) Carian generic ikut code/name */
    public List<Department> list(String q) throws SQLException {
        String base = "SELECT id, code, name, status, created_at FROM departments";
        String where = (q == null || q.trim().isEmpty())
                ? " ORDER BY code ASC"
                : " WHERE code LIKE ? OR name LIKE ? ORDER BY code ASC";
        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement(base + where)) {
            if (where.startsWith(" WHERE")) {
                String like = "%" + q.trim() + "%";
                ps.setString(1, like);
                ps.setString(2, like);
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Department> out = new ArrayList<>();
                while (rs.next()) out.add(mapRow(rs));
                return out;
            }
        }
    }

    public Department findById(int id) throws SQLException {
        String sql = "SELECT id, code, name, status, created_at FROM departments WHERE id=?";
        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    public boolean codeExists(String code, Integer excludeId) throws SQLException {
        String sql = "SELECT 1 FROM departments WHERE code=?"
                + (excludeId != null ? " AND id<>?" : "");
        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, code);
            if (excludeId != null) ps.setInt(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    public int create(Department d) throws SQLException {
        String sql = "INSERT INTO departments(code, name, status, created_at) VALUES(?,?,?,NOW())";
        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, d.getCode());
            ps.setString(2, d.getName());
            ps.setString(3, d.getStatus());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        }
    }

    public void update(Department d) throws SQLException {
        String sql = "UPDATE departments SET code=?, name=?, status=? WHERE id=?";
        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, d.getCode());
            ps.setString(2, d.getName());
            ps.setString(3, d.getStatus());
            ps.setInt(4, d.getId());
            ps.executeUpdate();
        }
    }
}
