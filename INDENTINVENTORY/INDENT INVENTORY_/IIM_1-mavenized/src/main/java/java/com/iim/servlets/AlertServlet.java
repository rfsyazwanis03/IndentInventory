package com.iim.servlets;

import com.iim.dao.AlertDAO;
import com.iim.models.Alert;
import com.iim.models.User;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@WebServlet(name="AlertServlet", urlPatterns={"/alerts"})
public class AlertServlet extends HttpServlet {

    private final AlertDAO dao = new AlertDAO();
    private static final ZoneId MYT = ZoneId.of("Asia/Kuala_Lumpur");
    private static final int PAGE_SIZE = 8; // same as before

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User me = (User) req.getSession().getAttribute("user");
        if (me == null) { resp.sendRedirect("login.jsp"); return; }

        // ensure alerts & pending reminders are created from current inventory
        dao.generateLowStockAlerts();

        // Refresh due reminders into active alerts
        dao.resendDueReminders();

        Integer clickedId = parseIntObj(req.getParameter("id"));
        if (clickedId != null) dao.markSingleRead(clickedId, true);

        String sort   = opt(req, "sort",   "latest");
        String filter = opt(req, "filter", "all");

        // Total pages for display (traditional pagination)
        int total = dao.count(filter);
        int pages = Math.max(1, (int) Math.ceil(total / (double) PAGE_SIZE));

        int page = parseInt(req.getParameter("page"), 1);
        if (page < 1) page = 1;
        if (page > pages) page = pages;

        int offset = (page - 1) * PAGE_SIZE;

        // Fetch one extra row; if we get PAGE_SIZE+1, there is another page.
        List<Alert> listPlusOne = dao.list(sort, filter, PAGE_SIZE + 1, offset);
        boolean hasNext = listPlusOne.size() > PAGE_SIZE;
        List<Alert> alerts = hasNext ? listPlusOne.subList(0, PAGE_SIZE) : listPlusOne;
        boolean hasPrev = page > 1;

        Alert selected = null;
        if (clickedId != null) selected = dao.findById(clickedId);
        if (selected == null && !alerts.isEmpty()) selected = alerts.get(0);

        // For page
        req.setAttribute("alerts", alerts);
        req.setAttribute("selected", selected);
        req.setAttribute("page", page);
        req.setAttribute("pages", pages);
        req.setAttribute("sort", sort);
        req.setAttribute("filter", filter);
        req.setAttribute("hasPrev", hasPrev);
        req.setAttribute("hasNext", hasNext);

        // For header dropdown
        req.setAttribute("notifList", dao.headerFeed());
        req.setAttribute("unreadCount", dao.unreadCount());

        req.getRequestDispatcher("alert.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User me = (User) req.getSession().getAttribute("user");
        if (me == null) { resp.sendRedirect("login.jsp"); return; }

        String action = req.getParameter("action");
        String sort   = opt(req, "sort",   "latest");
        String filter = opt(req, "filter", "all");
        int page      = parseInt(req.getParameter("page"), 1);

        if ("markRead".equals(action) || "markUnread".equals(action)) {
            dao.markRead(parseIds(req), "markRead".equals(action));
        } else if ("delete".equals(action)) {
            dao.delete(parseIds(req));
        } else if ("snooze".equals(action)) {
            try {
                int id = Integer.parseInt(req.getParameter("id"));
                String d = req.getParameter("remind_at_date"); // yyyy-MM-dd (MYT)
                String t = req.getParameter("remind_at_time"); // HH:mm (MYT)
                if (d != null && !d.isBlank()) {
                    if (t == null || t.isBlank()) t = "09:00";
                    LocalDate date = LocalDate.parse(d);
                    LocalTime time = LocalTime.parse(t);
                    dao.scheduleDuplicateReminder(id, Timestamp.valueOf(date.atTime(time)));
                }
            } catch (Exception ignored) { }
        }

        String qs = String.format("?page=%d&sort=%s&filter=%s", page, url(sort), url(filter));
        resp.sendRedirect(req.getContextPath() + "/alerts" + qs);
    }

    // helpers
    private static String opt(HttpServletRequest r, String k, String def) {
        String v = r.getParameter(k);
        return (v == null || v.isBlank()) ? def : v.trim();
    }
    private static int parseInt(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }
    private static Integer parseIntObj(String s) {
        try { return (s == null)? null : Integer.parseInt(s); } catch (Exception e) { return null; }
    }
    private static List<Integer> parseIds(HttpServletRequest r) {
        String[] ids = r.getParameterValues("ids");
        if (ids == null) return Collections.emptyList();
        List<Integer> out = new ArrayList<>(ids.length);
        for (String id : ids) {
            try { out.add(Integer.parseInt(id)); } catch (Exception ignored) {}
        }
        return out;
    }
    private static String url(String s){ return s==null?"":s.replace(" ", "%20"); }
}
