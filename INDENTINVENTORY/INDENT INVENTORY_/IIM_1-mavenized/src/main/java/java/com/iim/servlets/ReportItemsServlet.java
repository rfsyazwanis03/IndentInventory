package com.iim.servlets;

import com.iim.dao.ReportDAO;
import com.iim.models.Item;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.List;

@WebServlet(name="ReportItemsServlet", urlPatterns={"/report-items"})
public class ReportItemsServlet extends HttpServlet {
    private final ReportDAO dao = new ReportDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json; charset=UTF-8");
        String q = req.getParameter("q");
        int limit = 20;
        try {
            List<Item> items = dao.searchActiveItemsByName(q, limit); // name-only search
            StringBuilder sb = new StringBuilder("[");
            for (int i=0; i<items.size(); i++) {
                Item it = items.get(i);
                if (i>0) sb.append(',');
                sb.append("{\"id\":").append(it.getId())
                  .append(",\"name\":\"").append(esc(it.getItemName())).append("\"}")
                  ;
            }
            sb.append("]");
            resp.getWriter().write(sb.toString());
        } catch (Exception e) {
            resp.setStatus(500);
            resp.getWriter().write("{\"error\":\""+esc(e.getMessage())+"\"}");
        }
    }

    private String esc(String s){
        return (s==null) ? "" : s.replace("\\","\\\\").replace("\"","\\\"");
    }
}
