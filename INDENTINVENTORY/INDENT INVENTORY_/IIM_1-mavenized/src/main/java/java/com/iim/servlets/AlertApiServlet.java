package com.iim.servlets;

import com.iim.dao.AlertDAO;
import com.iim.models.Alert;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.List;

@WebServlet(name="AlertApiServlet", urlPatterns={"/api/alerts-feed"})
public class AlertApiServlet extends HttpServlet {
    private final AlertDAO dao = new AlertDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // ensure pending reminders exist
        dao.generateLowStockAlerts();

        // Pastikan reminder due dipublish
        dao.resendDueReminders();

        List<Alert> feed = dao.headerFeed();
        int unread = dao.unreadCount();
        Integer latest = dao.latestVisibleId(); // penting untuk trigger bunyi

        resp.setContentType("application/json; charset=UTF-8");
        resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        resp.setHeader("Pragma", "no-cache");

        StringBuilder sb = new StringBuilder();
        sb.append("{\"unread\":").append(unread)
          .append(",\"latest\":").append(latest==null? "null": latest)
          .append(",\"items\":[");
        for (int i = 0; i < feed.size(); i++) {
            Alert a = feed.get(i);
            if (i > 0) sb.append(",");
            sb.append("{\"id\":").append(a.getId())
              .append(",\"title\":").append(json(a.getTitle()))
              .append(",\"severity\":").append(json(a.getSeverity()==null? "normal" : a.getSeverity().toLowerCase()))
              .append("}");
        }
        sb.append("]}");
        resp.getWriter().write(sb.toString());
    }

    private String json(String s) {
        if (s == null) return "null";
        return "\"" + s.replace("\\", "\\\\")
                       .replace("\"", "\\\"")
                       .replace("\n","\\n")
                       .replace("\r","") + "\"";
    }
}
