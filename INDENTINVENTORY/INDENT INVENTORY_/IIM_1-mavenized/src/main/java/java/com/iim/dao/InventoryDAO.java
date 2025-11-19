package com.iim.dao;

import com.iim.db.DBConnection;
import java.sql.*;

public class InventoryDAO {
  public int getQuantity(int itemId) {
    String sql="SELECT quantity FROM inventory WHERE item_id=?";
    try(Connection c=DBConnection.getConnection(); PreparedStatement ps=c.prepareStatement(sql)){
      ps.setInt(1, itemId);
      try(ResultSet rs=ps.executeQuery()){
        if(rs.next()) return rs.getInt(1);
      }
    }catch(Exception e){ e.printStackTrace(); }
    return 0;
  }

  public void ensureRow(int itemId){
    String sql="INSERT IGNORE INTO inventory(item_id, quantity) VALUES(?,0)";
    try(Connection c=DBConnection.getConnection(); PreparedStatement ps=c.prepareStatement(sql)){
      ps.setInt(1,itemId); ps.executeUpdate();
    }catch(Exception e){ e.printStackTrace(); }
  }

  public void decrease(int itemId, int qty) throws SQLException {
    ensureRow(itemId);
    String sql="UPDATE inventory SET quantity = quantity - ?, updated_at=CURRENT_TIMESTAMP WHERE item_id=? AND quantity >= ?";
    try(Connection c=DBConnection.getConnection(); PreparedStatement ps=c.prepareStatement(sql)){
      ps.setInt(1, qty); ps.setInt(2, itemId); ps.setInt(3, qty);
      int n = ps.executeUpdate();
      if(n==0) throw new SQLException("Insufficient stock for item "+itemId);
    }
  }
  
 public void decrease(Connection c, int itemId, int qty) throws SQLException {
  ensureRow(itemId); // keep your helper
  String sql = "UPDATE inventory SET quantity = quantity - ?, updated_at=CURRENT_TIMESTAMP " +
               "WHERE item_id=? AND quantity >= ?";
  try (PreparedStatement ps = c.prepareStatement(sql)) {
    ps.setInt(1, qty);
    ps.setInt(2, itemId);
    ps.setInt(3, qty);
    int n = ps.executeUpdate();
    if (n == 0) throw new SQLException("Insufficient stock for item " + itemId);
  }
}
 public void insertIssue(Connection c, int departmentId, int itemId, int qty,
                        String scanCode, int userId, String notes) throws SQLException {
  String sql = "INSERT INTO indent_transactions(trans_time, trans_type, department_id, item_id, quantity, scan_code, created_by, notes) " +
               "VALUES (CURRENT_TIMESTAMP, 'ISSUE', ?, ?, ?, ?, ?, ?)";
  try (PreparedStatement ps = c.prepareStatement(sql)) {
    ps.setInt(1, departmentId);
    ps.setInt(2, itemId);
    ps.setInt(3, qty);
    if (scanCode == null || scanCode.isEmpty()) ps.setNull(4, java.sql.Types.VARCHAR); else ps.setString(4, scanCode);
    ps.setInt(5, userId);
    if (notes == null || notes.isEmpty()) ps.setNull(6, java.sql.Types.VARCHAR); else ps.setString(6, notes);
    ps.executeUpdate();
  }
}
 // --- ADD: restock helpers (mirror of decrease) ---
public void increase(Connection c, int itemId, int qty) throws SQLException {
    if (qty <= 0) throw new IllegalArgumentException("increase qty must be > 0");
    // Assumes `inventory` has UNIQUE KEY on item_id
    final String sql =
        "INSERT INTO inventory (item_id, quantity, updated_at) " +
        "VALUES (?, ?, CURRENT_TIMESTAMP) " +
        "ON DUPLICATE KEY UPDATE " +
        "  quantity = GREATEST(0, quantity + VALUES(quantity)), " +
        "  updated_at = CURRENT_TIMESTAMP";
    try (PreparedStatement ps = c.prepareStatement(sql)) {
        ps.setInt(1, itemId);
        ps.setInt(2, qty);
        ps.executeUpdate();
    }
}

/** Convenience overload if you don’t already manage a Connection. */
public void increase(int itemId, int qty) throws SQLException {
    try (Connection c = com.iim.db.DBConnection.getConnection()) {
        increase(c, itemId, qty);
    }
}

}
