package com.iim.servlets.quickscan;

import com.iim.dao.ItemDAO;
import com.iim.dao.ActivityLogDAO;
import com.iim.models.Item;
import com.iim.models.User;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

@WebServlet(name = "ScanItemServlet", urlPatterns = {"/scan-item"})
public class ScanItemServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json; charset=UTF-8");

        String id = req.getParameter("id");
        String code = req.getParameter("code");
        System.out.println("[ScanItemServlet] received id=" + id + ", code=" + code);

        ItemDAO dao = new ItemDAO();
        Item item = null;

        try {
            if (id != null && id.matches("\\d+")) {
                item = dao.findById(Integer.parseInt(id));
            } else if (code != null && !code.trim().isEmpty()) {
                item = dao.findByBarcode(code.trim());
            }
        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(500);
            resp.getWriter().write("{\"error\":\"server\",\"msg\":\"" + e.getMessage() + "\"}");
            return;
        }

        if (item == null) {
            resp.setStatus(404);
            resp.getWriter().write("{\"error\":\"not_found\",\"echo\":\"" + (code != null ? code : id) + "\"}");
            return;
        }

        // ✅ Log successful scan (with actor)
        User me = (User) req.getSession().getAttribute("user");
        if (me != null) {
            String codeStr = (item.getCodeBarcode() != null ? item.getCodeBarcode() : "id=" + item.getId());
            String nameStr = (item.getItemName() != null ? item.getItemName() : "");
            String actor = (me.getUsername()!=null && !me.getUsername().trim().isEmpty())
                    ? me.getUsername().trim() : ("ID#" + me.getId());
            new ActivityLogDAO().log(
                    me.getId(),
                    "SCAN_ITEM",
                    "Scanned " + codeStr + (nameStr.isEmpty() ? "" : (" " + nameStr)) + " — by " + actor,
                    req.getRemoteAddr()
            );
        }

        String json = String.format("{\"id\":%d,\"code\":%s,\"name\":%s}",
                item.getId(),
                item.getCodeBarcode() == null ? "null" : "\"" + item.getCodeBarcode().replace("\"", "\\\"") + "\"",
                "\"" + item.getItemName().replace("\"", "\\\"") + "\"");
        resp.getWriter().write(json);
        System.out.println("[ScanItemServlet] found item id=" + item.getId());
    }
}
