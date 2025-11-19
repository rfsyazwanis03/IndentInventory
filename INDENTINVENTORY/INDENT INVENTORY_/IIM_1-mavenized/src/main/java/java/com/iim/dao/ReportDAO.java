package com.iim.dao;

import com.iim.db.DBConnection;
import com.iim.models.Item;
import com.iim.models.ReportRow;

import java.sql.*;
import java.util.*;

public class ReportDAO {

    /** Items (active) with inventory quantity for dropdown. */
    public List<Item> listActiveItems() {
        String sql =
            "SELECT i.id, i.item_name, i.code_barcode, i.status, " +
            "       COALESCE(inv.quantity,0) AS qty " +
            "FROM items i " +
            "LEFT JOIN inventory inv ON inv.item_id = i.id " +
            "WHERE i.status='active' " +
            "ORDER BY i.item_name ASC";

        List<Item> out = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Item i = new Item();
                i.setId(rs.getInt("id"));
                i.setItemName(rs.getString("item_name"));
                i.setCodeBarcode(rs.getString("code_barcode"));
                i.setStatus(rs.getString("status"));
                i.setQuantity(rs.getInt("qty"));
                out.add(i);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return out;
    }

    /** Search active items by NAME only (limit results). */
    public List<Item> searchActiveItemsByName(String q, int limit) throws SQLException {
        String base =
            "SELECT i.id, i.item_name, i.code_barcode, i.status, COALESCE(inv.quantity,0) AS qty " +
            "FROM items i LEFT JOIN inventory inv ON inv.item_id = i.id " +
            "WHERE i.status='active' ";
        String tail = " ORDER BY i.item_name ASC LIMIT ?";

        boolean hasQ = (q != null && !q.trim().isEmpty());
        String sql = hasQ ? (base + "AND (i.item_name LIKE ?)" + tail) : (base + tail);

        List<Item> out = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            int idx = 1;
            if (hasQ) {
                String like = "%" + q.trim() + "%";
                ps.setString(idx++, like);
            }
            ps.setInt(idx, Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Item it = new Item();
                    it.setId(rs.getInt("id"));
                    it.setItemName(rs.getString("item_name"));
                    it.setCodeBarcode(rs.getString("code_barcode"));
                    it.setStatus(rs.getString("status"));
                    it.setQuantity(rs.getInt("qty"));
                    out.add(it);
                }
            }
        }
        return out;
    }

    /** Distinct years for a specific item (from indent_transactions.trans_time). */
    public List<Integer> getAvailableYearsForItem(int itemId) {
        String sql =
            "SELECT DISTINCT YEAR(t.trans_time) AS y " +
            "FROM indent_transactions t " +
            "WHERE t.item_id=? " +
            "ORDER BY y DESC";

        List<Integer> years = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) years.add(rs.getInt("y"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return years;
    }

    /** All distinct years present in indent_transactions. */
    public List<Integer> getAllAvailableYears() {
        String sql =
            "SELECT DISTINCT YEAR(trans_time) AS y " +
            "FROM indent_transactions " +
            "ORDER BY y DESC";
        List<Integer> years = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) years.add(rs.getInt("y"));
        } catch (SQLException e) { e.printStackTrace(); }
        return years;
    }

    /** Table data: monthly ISSUE usage for an item in a year, grouped by department. */
    public List<ReportRow> getUsageByDepartment(int itemId, int year) {
        String sql =
            "SELECT d.name AS dept_name, MONTH(t.trans_time) AS m, SUM(t.quantity) AS qty " +
            "FROM indent_transactions t " +
            "JOIN departments d ON d.id = t.department_id " +
            "WHERE t.item_id=? AND YEAR(t.trans_time)=? AND t.trans_type='ISSUE' " +
            "GROUP BY d.name, MONTH(t.trans_time) " +
            "HAVING SUM(t.quantity) > 0 " +
            "ORDER BY d.name ASC";

        Map<String, ReportRow> map = new LinkedHashMap<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            ps.setInt(2, year);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String dept = rs.getString("dept_name");
                    int month = rs.getInt("m");
                    int qty   = rs.getInt("qty");
                    map.computeIfAbsent(dept, ReportRow::new).addMonthQty(month, qty);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }

        List<ReportRow> rows = new ArrayList<>(map.values());
        rows.removeIf(r -> r.getTotal() == 0);
        return rows;
    }

    /** Resolve item name for headings/export. */
    public String getItemName(int itemId) {
        String sql = "SELECT item_name FROM items WHERE id=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return "Unknown Item";
    }

    /**
     * list inventory rows for inventory PDF export.
     * Returns Item objects with quantity, min_quantity and critical_min_quantity fields populated.
     *
     * q: optional filter applied to name or code (contains)
     * sort: name_asc / stock_asc / stock_desc / code_asc
     * limit: safety limit for exported rows
     */
    public List<Item> listInventoryForReport(String q, String sort, int limit) throws SQLException {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT i.id, i.item_name, i.code_barcode, i.status, ")
          .append("COALESCE(inv.quantity,0) AS quantity, ")
          .append("COALESCE(i.min_quantity,0) AS min_quantity, ")
          .append("COALESCE(i.critical_min_quantity,0) AS critical_min_quantity ")
          .append("FROM items i LEFT JOIN inventory inv ON i.id = inv.item_id ")
          .append("WHERE 1=1 ");

        if (q != null && !q.trim().isEmpty()) {
            sb.append("AND (i.item_name LIKE ? OR i.code_barcode LIKE ?) ");
        }

        String order = "ORDER BY i.item_name ASC";
        if ("stock_asc".equalsIgnoreCase(sort)) order = "ORDER BY COALESCE(inv.quantity,0) ASC";
        else if ("stock_desc".equalsIgnoreCase(sort)) order = "ORDER BY COALESCE(inv.quantity,0) DESC";
        else if ("code_asc".equalsIgnoreCase(sort)) order = "ORDER BY i.code_barcode ASC";

        sb.append(order).append(" LIMIT ?");

        List<Item> out = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sb.toString())) {
            int idx = 1;
            if (q != null && !q.trim().isEmpty()) {
                String like = "%" + q.trim() + "%";
                ps.setString(idx++, like);
                ps.setString(idx++, like);
            }
            ps.setInt(idx++, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Item it = new Item();
                    it.setId(rs.getInt("id"));
                    it.setItemName(rs.getString("item_name"));
                    it.setCodeBarcode(rs.getString("code_barcode"));
                    it.setStatus(rs.getString("status"));
                    it.setQuantity(rs.getInt("quantity"));
                    it.setMinQuantity(rs.getInt("min_quantity"));
                    it.setCriticalMinQuantity(rs.getInt("critical_min_quantity"));
                    out.add(it);
                }
            }
        }
        return out;
    }
}
