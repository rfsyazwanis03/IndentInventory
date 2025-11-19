package com.iim.servlets;

import com.iim.dao.ReportDAO;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@WebServlet(name="ReportsYearsServlet", urlPatterns={"/reports-years"})
public class ReportsYearsServlet extends HttpServlet {
    private final ReportDAO dao = new ReportDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // no-cache for dev
        resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        resp.setHeader("Pragma", "no-cache");
        resp.setDateHeader("Expires", 0);
        resp.setContentType("application/json; charset=UTF-8");

        Integer itemId = null;
        try {
            String s = req.getParameter("item");
            if (s != null && !s.isEmpty()) itemId = Integer.parseInt(s);
        } catch (Exception ignored) {}

        List<Integer> years = (itemId == null)
                ? dao.getAllAvailableYears()
                : dao.getAvailableYearsForItem(itemId);

        // (Optional fallback) If you want to always show all years when none for the item:
        // if (itemId != null && years.isEmpty()) years = dao.getAllAvailableYears();

        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < years.size(); i++) {
            if (i > 0) json.append(',');
            json.append(years.get(i));
        }
        json.append("]");

        PrintWriter out = resp.getWriter();
        out.write(json.toString());
        out.flush();
    }
}
