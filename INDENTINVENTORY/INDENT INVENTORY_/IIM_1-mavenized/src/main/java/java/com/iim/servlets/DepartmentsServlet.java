package com.iim.servlets;

import com.iim.dao.DepartmentDAO;
import com.iim.models.Department;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.List;

@WebServlet(name="DepartmentsServlet", urlPatterns={"/departments"})
public class DepartmentsServlet extends HttpServlet {
    private final DepartmentDAO dao = new DepartmentDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json; charset=UTF-8");
        String q = req.getParameter("q");
        try {
            List<Department> list = dao.list(q);
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (int i=0; i<list.size(); i++) {
                Department d = list.get(i);
                if (i>0) sb.append(',');
                sb.append("{\"id\":").append(d.getId())
                  .append(",\"code\":\"").append(esc(d.getCode())).append("\"")
                  .append(",\"name\":\"").append(esc(d.getName())).append("\"")
                  .append(",\"status\":\"").append(esc(d.getStatus()==null?"active":d.getStatus().toLowerCase())).append("\"")
                  .append("}");
            }
            sb.append("]");
            resp.getWriter().write(sb.toString());
        } catch (Exception e) {
            resp.setStatus(500);
            resp.getWriter().write("{\"error\":\""+ esc(e.getMessage()) +"\"}");
        }
    }

    private String esc(String s){ return s==null? "" : s.replace("\\","\\\\").replace("\"","\\\""); }
}
