package com.iim.servlets;

import com.iim.db.DBConnection;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.*;

@WebServlet(name="DbInfoServlet", urlPatterns={"/debug/db-info"})
public class DbInfoServlet extends HttpServlet {
  @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    resp.setContentType("application/json; charset=UTF-8");
    try (Connection c = DBConnection.getConnection()) {
      String url = c.getMetaData().getURL();
      String user = c.getMetaData().getUserName();
      String dbName = "", host = "", version = "";
      try (Statement st = c.createStatement();
           ResultSet rs = st.executeQuery("SELECT DATABASE() db, @@hostname host, VERSION() ver")) {
        if (rs.next()) { dbName = rs.getString("db"); host = rs.getString("host"); version = rs.getString("ver"); }
      }
      int count = 0;
      StringBuilder sample = new StringBuilder("[");
      try (PreparedStatement ps = c.prepareStatement(
              "SELECT id,code,name,status,created_at FROM departments ORDER BY id DESC LIMIT 5");
           ResultSet rs = ps.executeQuery()) {
        boolean first = true;
        while (rs.next()) {
          count++; if (!first) sample.append(','); first=false;
          sample.append("{\"id\":").append(rs.getInt("id"))
                .append(",\"code\":\"").append(s(rs.getString("code"))).append("\"")
                .append(",\"name\":\"").append(s(rs.getString("name"))).append("\"")
                .append(",\"status\":\"").append(s(rs.getString("status"))).append("\"")
                .append(",\"created_at\":\"").append(String.valueOf(rs.getTimestamp("created_at"))).append("\"}");
        }
      }
      sample.append("]");
      resp.getWriter().write("{"
        +"\"jdbc_url\":\""+s(url)+"\","
        +"\"jdbc_user\":\""+s(user)+"\","
        +"\"database\":\""+s(dbName)+"\","
        +"\"mysql_host\":\""+s(host)+"\","
        +"\"mysql_version\":\""+s(version)+"\","
        +"\"departments_count\":"+count+","
        +"\"departments_sample\":"+sample+"}");
    } catch (Exception e) {
      resp.setStatus(500); resp.getWriter().write("{\"error\":\""+s(e.getMessage())+"\"}");
    }
  }
  private String s(String x){ return x==null? "" : x.replace("\\","\\\\").replace("\"","\\\""); }
}
