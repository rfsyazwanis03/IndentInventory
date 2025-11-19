package com.iim.servlets;

import com.iim.dao.DepartmentDAO;
import com.iim.models.Department;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

@WebServlet(name="DepartmentDetailServlet", urlPatterns={"/department"})
public class DepartmentDetailServlet extends HttpServlet {
    private final DepartmentDAO dao = new DepartmentDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json; charset=UTF-8");
        String idStr = req.getParameter("id");
        try {
            int id = Integer.parseInt(idStr);
            Department d = dao.findById(id);
            if (d == null) { resp.setStatus(404); resp.getWriter().write("{\"error\":\"Not found\"}"); return; }
            String json = new StringBuilder()
                .append("{\"id\":").append(d.getId())
                .append(",\"code\":\"").append(esc(d.getCode())).append("\"")
                .append(",\"name\":\"").append(esc(d.getName())).append("\"")
                .append(",\"status\":\"").append(esc(d.getStatus()==null?"active":d.getStatus().toLowerCase())).append("\"}")
                .toString();
            resp.getWriter().write(json);
        } catch (Exception e) {
            resp.setStatus(400);
            resp.getWriter().write("{\"error\":\"Invalid id\"}");
        }
    }

    private String esc(String s){ return s==null? "" : s.replace("\\","\\\\").replace("\"","\\\""); }
}
