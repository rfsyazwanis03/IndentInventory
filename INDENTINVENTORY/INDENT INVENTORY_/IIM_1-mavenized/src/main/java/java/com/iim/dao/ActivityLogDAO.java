package com.iim.dao;

import com.iim.db.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Calendar;
import java.util.TimeZone;

public class ActivityLogDAO {

  public static class LogRow {
    public long id;
    public Timestamp eventTime;   // mapped from log_time
    public Integer userId;
    public String action;
    public String detail;         // mapped from details
    public String ip;             // mapped from ip_address
  }

  /** Simple logger (opens its own connection). */
  public void log(Integer userId, String action, String detail, String ip) {
    final String sql =
        "INSERT INTO activity_log(user_id, action, details, ip_address) VALUES (?,?,?,?)";
    try (Connection c = DBConnection.getConnection();
         PreparedStatement ps = c.prepareStatement(sql)) {
      int i = 1;
      ps.setObject(i++, userId, Types.INTEGER);
      ps.setString(i++, action);
      if (detail == null || detail.trim().isEmpty()) ps.setNull(i++, Types.VARCHAR); else ps.setString(i++, detail);
      if (ip == null || ip.trim().isEmpty()) ps.setNull(i++, Types.VARCHAR); else ps.setString(i++, ip);
      ps.executeUpdate();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /** Overload that uses an existing external connection (for transactions). */
  public void log(Connection c, Integer userId, String action, String detail, String ip) throws SQLException {
    final String sql =
        "INSERT INTO activity_log(user_id, action, details, ip_address) VALUES (?,?,?,?)";
    try (PreparedStatement ps = c.prepareStatement(sql)) {
      int i = 1;
      ps.setObject(i++, userId, Types.INTEGER);
      ps.setString(i++, action);
      if (detail == null || detail.trim().isEmpty()) ps.setNull(i++, Types.VARCHAR); else ps.setString(i++, detail);
      if (ip == null || ip.trim().isEmpty()) ps.setNull(i++, Types.VARCHAR); else ps.setString(i++, ip);
      ps.executeUpdate();
    }
  }

  public List<LogRow> list(String actionFilter, String from, String to, String q, int limit, int offset) {
    StringBuilder sb = new StringBuilder(
        "SELECT id, log_time, user_id, action, details, ip_address FROM activity_log WHERE 1=1");
    List<Object> params = new ArrayList<>();

    if (actionFilter != null && !actionFilter.trim().isEmpty() && !"ALL".equalsIgnoreCase(actionFilter)) {
      sb.append(" AND action = ?");
      params.add(actionFilter.trim());
    }
    if (from != null && !from.isEmpty()) {
      sb.append(" AND log_time >= ?");
      params.add(Timestamp.valueOf(from.trim() + " 00:00:00"));
    }
    if (to != null && !to.isEmpty()) {
      sb.append(" AND log_time <= ?");
      params.add(Timestamp.valueOf(to.trim() + " 23:59:59"));
    }
    if (q != null && !q.trim().isEmpty()) {
      sb.append(" AND (details LIKE ? OR ip_address LIKE ? OR action LIKE ?)");
      String like = "%" + q.trim() + "%";
      params.add(like); params.add(like); params.add(like);
    }

    sb.append(" ORDER BY log_time DESC, id DESC LIMIT ? OFFSET ?");
    params.add(limit <= 0 ? 50 : limit);
    params.add(Math.max(0, offset));

    // Read timestamps using Malaysia time
    Calendar MYT = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kuala_Lumpur"));

    List<LogRow> out = new ArrayList<>();
    try (Connection c = DBConnection.getConnection();
         PreparedStatement ps = c.prepareStatement(sb.toString())) {

      for (int i=0; i<params.size(); i++) {
        Object v = params.get(i);
        if (v instanceof String) ps.setString(i+1, (String) v);
        else if (v instanceof Integer) ps.setInt(i+1, (Integer) v);
        else if (v instanceof Timestamp) ps.setTimestamp(i+1, (Timestamp) v);
      }

      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          LogRow r = new LogRow();
          r.id = rs.getLong("id");
          r.eventTime = rs.getTimestamp("log_time", MYT);
          int uid = rs.getInt("user_id");
          r.userId = rs.wasNull() ? null : uid;
          r.action = rs.getString("action");
          r.detail = rs.getString("details");
          r.ip = rs.getString("ip_address");
          out.add(r);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return out;
  }

  /**
   * Return the list of distinct action codes currently stored in activity_log,
   * sorted alphabetically. Used to build the filter dropdown dynamically.
   */
  public List<String> listDistinctActions() {
    List<String> out = new ArrayList<>();
    final String sql = "SELECT DISTINCT action FROM activity_log WHERE action IS NOT NULL AND action <> '' ORDER BY action ASC";
    try (Connection c = DBConnection.getConnection();
         PreparedStatement ps = c.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        String a = rs.getString("action");
        if (a != null && !a.trim().isEmpty()) {
          out.add(a.trim());
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return out;
  }
}
