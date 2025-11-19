package com.iim.dao;

import com.iim.db.DBConnection;
import com.iim.models.Item;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ItemDAO {

  private static volatile Boolean HAS_ITEMS_UPDATED_AT = null;
  private static volatile Boolean HAS_INVENTORY_UPDATED_AT = null;

  private boolean columnExists(Connection c, String table, String column) {
    try (ResultSet rs = c.getMetaData().getColumns(null, null, table, column)) {
      return rs.next();
    } catch (SQLException e) {
      String sql = "SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_NAME=? AND COLUMN_NAME=? LIMIT 1";
      try (PreparedStatement ps = c.prepareStatement(sql)) {
        ps.setString(1, table);
        ps.setString(2, column);
        try (ResultSet r2 = ps.executeQuery()) { return r2.next(); }
      } catch (SQLException ignored) {}
    }
    return false;
  }

  private void ensureColumnFlags(Connection c) {
    if (HAS_ITEMS_UPDATED_AT == null) {
      synchronized (ItemDAO.class) {
        if (HAS_ITEMS_UPDATED_AT == null) HAS_ITEMS_UPDATED_AT = columnExists(c, "items", "updated_at");
      }
    }
    if (HAS_INVENTORY_UPDATED_AT == null) {
      synchronized (ItemDAO.class) {
        if (HAS_INVENTORY_UPDATED_AT == null) HAS_INVENTORY_UPDATED_AT = columnExists(c, "inventory", "updated_at");
      }
    }
  }

  public Item findByBarcode(String code){
    if (code == null) return null;
    String norm = "REPLACE(REPLACE(REPLACE(REPLACE(TRIM(%s), CHAR(13), ''), CHAR(10), ''), CHAR(160), ''), CHAR(9), '')";
    String sql = "SELECT id, item_name, code_barcode FROM items WHERE status='active' " +
                 "AND " + String.format(norm, "code_barcode") + " = " + String.format(norm, "?") + " LIMIT 1";
    try (Connection c = DBConnection.getConnection();
         PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setString(1, code);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          Item it = new Item();
          it.setId(rs.getInt("id"));
          it.setItemName(rs.getString("item_name"));
          it.setCodeBarcode(rs.getString("code_barcode"));
          return it;
        }
      }
    } catch (Exception e) { e.printStackTrace(); }
    return null;
  }

  public Item findById(int id){
    String sql = "SELECT id, item_name, code_barcode FROM items WHERE id=? LIMIT 1";
    try (Connection c=DBConnection.getConnection();
         PreparedStatement ps=c.prepareStatement(sql)) {
      ps.setInt(1,id);
      try(ResultSet rs=ps.executeQuery()){
        if(rs.next()){
          Item it=new Item();
          it.setId(rs.getInt("id"));
          it.setItemName(rs.getString("item_name"));
          it.setCodeBarcode(rs.getString("code_barcode"));
          return it;
        }
      }
    } catch(Exception e){ e.printStackTrace(); }
    return null;
  }

  public List<Item> searchByName(String q, int limit){
    String sql="SELECT id, item_name, code_barcode FROM items WHERE status='active' AND item_name LIKE ? ORDER BY item_name LIMIT ?";
    List<Item> out=new ArrayList<>();
    try(Connection c=DBConnection.getConnection();
        PreparedStatement ps=c.prepareStatement(sql)){
      ps.setString(1, "%"+q+"%");
      ps.setInt(2, limit);
      try(ResultSet rs=ps.executeQuery()){
        while(rs.next()){
          Item it=new Item();
          it.setId(rs.getInt("id"));
          it.setItemName(rs.getString("item_name"));
          it.setCodeBarcode(rs.getString("code_barcode"));
          out.add(it);
        }
      }
    } catch(Exception e){ e.printStackTrace(); }
    return out;
  }

  public List<Item> listForInventory(String rawSearch, String sortKey){
    String search = (rawSearch == null) ? "" : rawSearch.trim();
    List<Item> list = new ArrayList<>();

    String order;
    if ("stock_asc".equalsIgnoreCase(sortKey)) {
      order = " ORDER BY quantity ASC, i.item_name ASC";
    } else if ("stock_desc".equalsIgnoreCase(sortKey)) {
      order = " ORDER BY quantity DESC, i.item_name ASC";
    } else if ("code_asc".equalsIgnoreCase(sortKey)) {
      order = " ORDER BY i.code_barcode ASC";
    } else {
      order = " ORDER BY i.item_name ASC";
    }

    String base =
        "SELECT i.id, i.item_name, i.code_barcode, i.status, i.min_quantity, i.critical_min_quantity, " +
        "       COALESCE(inv.quantity,0) AS quantity " +
        "FROM items i " +
        "LEFT JOIN inventory inv ON inv.item_id = i.id ";
    String where = "";
    boolean has = !search.isEmpty();
    if (has){
      where = "WHERE i.item_name LIKE ? OR i.code_barcode LIKE ? ";
    }

    try (Connection c = DBConnection.getConnection();
         PreparedStatement ps = c.prepareStatement(base + where + order)) {
      if (has){
        String like = "%" + search + "%";
        ps.setString(1, like);
        ps.setString(2, like);
      }
      try (ResultSet rs = ps.executeQuery()){
        while (rs.next()){
          Item it = new Item();
          it.setId(rs.getInt("id"));
          it.setItemName(rs.getString("item_name"));
          it.setCodeBarcode(rs.getString("code_barcode"));
          it.setStatus(rs.getString("status"));
          it.setQuantity(rs.getInt("quantity"));
          it.setMinQuantity(rs.getInt("min_quantity"));
          it.setCriticalMinQuantity(rs.getInt("critical_min_quantity"));
          list.add(it);
        }
      }
    } catch (Exception e){ e.printStackTrace(); }
    return list;
  }
  public List<Item> listForInventory(String rawSearch){ return listForInventory(rawSearch, "name_asc"); }

  public Item findDetailForInventory(int id){
    String sql =
        "SELECT i.id, i.item_name, i.code_barcode, i.status, i.min_quantity, i.critical_min_quantity, " +
        "       COALESCE(inv.quantity,0) AS quantity " +
        "FROM items i LEFT JOIN inventory inv ON inv.item_id = i.id WHERE i.id=? LIMIT 1";
    try (Connection c=DBConnection.getConnection();
         PreparedStatement ps=c.prepareStatement(sql)) {
      ps.setInt(1, id);
      try (ResultSet rs = ps.executeQuery()){
        if (rs.next()){
          Item it = new Item();
          it.setId(rs.getInt("id"));
          it.setItemName(rs.getString("item_name"));
          it.setCodeBarcode(rs.getString("code_barcode"));
          it.setStatus(rs.getString("status"));
          it.setQuantity(rs.getInt("quantity"));
          it.setMinQuantity(rs.getInt("min_quantity"));
          it.setCriticalMinQuantity(rs.getInt("critical_min_quantity"));
          return it;
        }
      }
    } catch(Exception e){ e.printStackTrace(); }
    return null;
  }

  public void addWithOpeningStock(String name, String code, int qty) throws SQLException {
    String sql1 = "INSERT INTO items(item_name, code_barcode, status, created_at) VALUES(?,?, 'active', NOW())";
    String sql2 = "INSERT INTO inventory(item_id, quantity, updated_at) VALUES(?, ?, NOW())";
    try (Connection c = DBConnection.getConnection()){
      c.setAutoCommit(false);
      try (PreparedStatement ps1 = c.prepareStatement(sql1, Statement.RETURN_GENERATED_KEYS)) {
        ps1.setString(1, name);
        ps1.setString(2, code);
        ps1.executeUpdate();
        int newId;
        try (ResultSet keys = ps1.getGeneratedKeys()){
          keys.next(); newId = keys.getInt(1);
        }
        try (PreparedStatement ps2 = c.prepareStatement(sql2)){
          ps2.setInt(1, newId);
          ps2.setInt(2, Math.max(0, qty));
          ps2.executeUpdate();
        }
        c.commit();
      } catch(SQLException ex){
        c.rollback();
        throw ex;
      } finally {
        c.setAutoCommit(true);
      }
    }
  }

  public void addWithOpeningStock(String name, String code, int qty, int minQty) throws SQLException {
    qty    = Math.max(0, qty);
    minQty = Math.max(0, minQty);

    String sql1 = "INSERT INTO items(item_name, code_barcode, min_quantity, status, created_at) " +
                  "VALUES(?, ?, ?, 'active', NOW())";
    String sql2 = "INSERT INTO inventory(item_id, quantity, updated_at) VALUES(?, ?, NOW())";

    try (Connection c = DBConnection.getConnection()) {
      c.setAutoCommit(false);
      try (PreparedStatement ps1 = c.prepareStatement(sql1, Statement.RETURN_GENERATED_KEYS)) {
        ps1.setString(1, name);
        ps1.setString(2, code);
        ps1.setInt(3, minQty);
        ps1.executeUpdate();

        int newId;
        try (ResultSet keys = ps1.getGeneratedKeys()) { keys.next(); newId = keys.getInt(1); }

        try (PreparedStatement ps2 = c.prepareStatement(sql2)) {
          ps2.setInt(1, newId);
          ps2.setInt(2, qty);
          ps2.executeUpdate();
        }
        c.commit();
      } catch (SQLException ex) {
        c.rollback();
        throw ex;
      } finally {
        c.setAutoCommit(true);
      }
    }
  }

  public void addWithOpeningStock(String name, String code, int qty, int minQty, int criticalMinQty) throws SQLException {
    qty    = Math.max(0, qty);
    minQty = Math.max(0, minQty);
    criticalMinQty = Math.max(0, criticalMinQty);

    String sql1 = "INSERT INTO items(item_name, code_barcode, min_quantity, critical_min_quantity, status, created_at) " +
                  "VALUES(?, ?, ?, ?, 'active', NOW())";
    String sql2 = "INSERT INTO inventory(item_id, quantity, updated_at) VALUES(?, ?, NOW())";

    try (Connection c = DBConnection.getConnection()) {
      c.setAutoCommit(false);
      try (PreparedStatement ps1 = c.prepareStatement(sql1, Statement.RETURN_GENERATED_KEYS)) {
        ps1.setString(1, name);
        ps1.setString(2, code);
        ps1.setInt(3, minQty);
        ps1.setInt(4, criticalMinQty);
        ps1.executeUpdate();

        int newId;
        try (ResultSet keys = ps1.getGeneratedKeys()) { keys.next(); newId = keys.getInt(1); }

        try (PreparedStatement ps2 = c.prepareStatement(sql2)) {
          ps2.setInt(1, newId);
          ps2.setInt(2, qty);
          ps2.executeUpdate();
        }
        c.commit();
      } catch (SQLException ex) {
        c.rollback();
        throw ex;
      } finally {
        c.setAutoCommit(true);
      }
    }
  }

  public void toggleStatus(int id){
    String sql = "UPDATE items SET status = IF(status='active','inactive','active') WHERE id=?";
    try (Connection c = DBConnection.getConnection();
         PreparedStatement ps = c.prepareStatement(sql)){
      ps.setInt(1, id);
      ps.executeUpdate();
    } catch(Exception e){ e.printStackTrace(); }
  }

  public void updateItemAndQuantity(int id, String name, String code, String status, int qty){
    if (name == null) name = "";
    if (code == null) code = "";
    if (qty < 0) qty = 0;

    try (Connection c = DBConnection.getConnection()){
      ensureColumnFlags(c);

      StringBuilder sb = new StringBuilder("UPDATE items SET item_name=?, code_barcode=?");
      ArrayList<Object> params = new ArrayList<>();
      params.add(name);
      params.add(code);

      if (status != null && !status.trim().isEmpty()){
        sb.append(", status=?");
        params.add(status.trim());
      }
      if (Boolean.TRUE.equals(HAS_ITEMS_UPDATED_AT)){
        sb.append(", updated_at=NOW()");
      }
      sb.append(" WHERE id=?");
      params.add(id);
      String sqlItems = sb.toString();

      String sqlInv;
      if (Boolean.TRUE.equals(HAS_INVENTORY_UPDATED_AT)) {
        sqlInv = "INSERT INTO inventory(item_id, quantity, updated_at) VALUES(?, ?, NOW()) " +
                 "ON DUPLICATE KEY UPDATE quantity=VALUES(quantity), updated_at=VALUES(updated_at)";
      } else {
        sqlInv = "INSERT INTO inventory(item_id, quantity) VALUES(?, ?) " +
                 "ON DUPLICATE KEY UPDATE quantity=VALUES(quantity)";
      }

      c.setAutoCommit(false);
      try {
        try (PreparedStatement ps1 = c.prepareStatement(sqlItems)) {
          for (int i=0; i<params.size(); i++) {
            Object v = params.get(i);
            if (v instanceof String) ps1.setString(i+1, (String)v);
            else if (v instanceof Integer) ps1.setInt(i+1, (Integer)v);
            else ps1.setObject(i+1, v);
          }
          ps1.executeUpdate();
        }
        try (PreparedStatement ps2 = c.prepareStatement(sqlInv)) {
          ps2.setInt(1, id);
          ps2.setInt(2, qty);
          ps2.executeUpdate();
        }
        c.commit();
      } catch (Exception ex) {
        c.rollback();
        throw ex;
      } finally {
        c.setAutoCommit(true);
      }
    } catch(Exception e){
      e.printStackTrace();
      System.err.println("[ItemDAO] updateItemAndQuantity failed for id=" + id);
    }
  }
}
