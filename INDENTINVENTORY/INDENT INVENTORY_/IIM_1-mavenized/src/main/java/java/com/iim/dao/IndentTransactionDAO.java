package com.iim.dao;

import com.iim.db.DBConnection;
import java.sql.*;

public class IndentTransactionDAO {

  // ===== EXISTING ISSUE methods (unchanged) =====
  public void insertIssue(int departmentId, int itemId, int qty, String scanCode, int userId, String notes) throws SQLException {
    String sql = "INSERT INTO indent_transactions " +
                 "(trans_time, trans_type, department_id, item_id, quantity, scan_code, created_by, notes) " +
                 "VALUES (CURRENT_TIMESTAMP, 'ISSUE', ?, ?, ?, ?, ?, ?)";
    try (Connection c = DBConnection.getConnection();
         PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setInt(1, departmentId);
      ps.setInt(2, itemId);
      ps.setInt(3, qty);
      if (scanCode == null || scanCode.isEmpty()) ps.setNull(4, Types.VARCHAR); else ps.setString(4, scanCode);
      ps.setInt(5, userId);
      if (notes == null || notes.isEmpty()) ps.setNull(6, Types.VARCHAR); else ps.setString(6, notes);
      ps.executeUpdate();
    }
  }

  public void insertIssue(Connection c, int departmentId, int itemId, int qty, String scanCode, int userId, String notes) throws SQLException {
    String sql = "INSERT INTO indent_transactions " +
                 "(trans_time, trans_type, department_id, item_id, quantity, scan_code, created_by, notes) " +
                 "VALUES (CURRENT_TIMESTAMP, 'ISSUE', ?, ?, ?, ?, ?, ?)";
    try (PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setInt(1, departmentId);
      ps.setInt(2, itemId);
      ps.setInt(3, qty);
      if (scanCode == null || scanCode.isEmpty()) ps.setNull(4, Types.VARCHAR); else ps.setString(4, scanCode);
      ps.setInt(5, userId);
      if (notes == null || notes.isEmpty()) ps.setNull(6, Types.VARCHAR); else ps.setString(6, notes);
      ps.executeUpdate();
    }
  }

  public void insertIssueAt(Connection c,
                            int departmentId,
                            int itemId,
                            int qty,
                            String scanCode,
                            int userId,
                            String notes,
                            Timestamp transTime) throws SQLException {
    String sql = "INSERT INTO indent_transactions " +
                 "(trans_time, trans_type, department_id, item_id, quantity, scan_code, created_by, notes) " +
                 "VALUES (?, 'ISSUE', ?, ?, ?, ?, ?, ?)";
    try (PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setTimestamp(1, transTime);
      ps.setInt(2, departmentId);
      ps.setInt(3, itemId);
      ps.setInt(4, qty);
      if (scanCode == null || scanCode.isEmpty()) ps.setNull(5, Types.VARCHAR); else ps.setString(5, scanCode);
      ps.setInt(6, userId);
      if (notes == null || notes.isEmpty()) ps.setNull(7, Types.VARCHAR); else ps.setString(7, notes);
      ps.executeUpdate();
    }
  }

  public void insertIssueAt(int departmentId,
                            int itemId,
                            int qty,
                            String scanCode,
                            int userId,
                            String notes,
                            Timestamp transTime) throws SQLException {
    String sql = "INSERT INTO indent_transactions " +
                 "(trans_time, trans_type, department_id, item_id, quantity, scan_code, created_by, notes) " +
                 "VALUES (?, 'ISSUE', ?, ?, ?, ?, ?, ?)";
    try (Connection c = DBConnection.getConnection();
         PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setTimestamp(1, transTime);
      ps.setInt(2, departmentId);
      ps.setInt(3, itemId);
      ps.setInt(4, qty);
      if (scanCode == null || scanCode.isEmpty()) ps.setNull(5, Types.VARCHAR); else ps.setString(5, scanCode);
      ps.setInt(6, userId);
      if (notes == null || notes.isEmpty()) ps.setNull(7, Types.VARCHAR); else ps.setString(7, notes);
      ps.executeUpdate();
    }
  }

  // ===== NEW: RESTOCK (no department) =====
  public void insertRestock(Connection c,
                            int itemId,
                            int qty,
                            String scanCode,
                            int userId,
                            String notes,
                            Timestamp transTime) throws SQLException {
    String sql = "INSERT INTO indent_transactions " +
                 "(trans_time, trans_type, department_id, item_id, quantity, scan_code, created_by, notes) " +
                 "VALUES (?, 'RESTOCK', NULL, ?, ?, ?, ?, ?)";
    try (PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setTimestamp(1, transTime);
      ps.setInt(2, itemId);
      ps.setInt(3, qty);
      if (scanCode == null || scanCode.isEmpty()) ps.setNull(4, Types.VARCHAR); else ps.setString(4, scanCode);
      ps.setInt(5, userId);
      if (notes == null || notes.isEmpty()) ps.setNull(6, Types.VARCHAR); else ps.setString(6, notes);
      ps.executeUpdate();
    }
  }

  public void insertRestock(int itemId,
                            int qty,
                            String scanCode,
                            int userId,
                            String notes,
                            Timestamp transTime) throws SQLException {
    try (Connection c = DBConnection.getConnection()) {
      insertRestock(c, itemId, qty, scanCode, userId, notes, transTime);
    }
  }
}
