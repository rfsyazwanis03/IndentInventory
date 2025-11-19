package com.iim.servlets;

import com.iim.dao.DepartmentDAO;
import com.iim.dao.ActivityLogDAO;
import com.iim.models.Department;
import com.iim.models.User;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.StandardCharsets;

@WebServlet(name="DepartmentCreateServlet", urlPatterns={"/department-create"})
public class DepartmentCreateServlet extends HttpServlet {
    private final DepartmentDAO dao = new DepartmentDAO();
    private final ActivityLogDAO logDAO = new ActivityLogDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/plain; charset=UTF-8");
        String body = readBody(req);

        String code   = safeUpper(pick(body, "code"));
        String name   = safeUpper(pick(body, "name"));
        String status = safeLower(pick(body, "status"));

        if (code.isEmpty() || name.isEmpty()) { resp.setStatus(400); resp.getWriter().write("Code and Name are required."); return; }
        if (!status.equals("active") && !status.equals("inactive")) status = "active";

        try {
            if (dao.codeExists(code, null)) { resp.setStatus(409); resp.getWriter().write("Code already exists."); return; }

            Department d = new Department();
            d.setCode(code);
            d.setName(name);
            d.setStatus(status);

            int newId = dao.create(d);

            // ===== LOG: DEPT_CREATE (friendly) =====
            User me = (User) req.getSession().getAttribute("user");
            Integer userId = (me == null) ? null : me.getId();
            String username = (me == null || me.getUsername()==null || me.getUsername().trim().isEmpty())
                    ? "Unknown user" : me.getUsername().trim();
            String ip = clientIp(req);
            String display = buildDeptDisplay(code, name, status);
            String detail  = String.format("%s created department %s", username, display);
            logDAO.log(userId, "DEPT_CREATE", detail, ip);
            // =======================================

            resp.getWriter().write("ok:" + newId);
        } catch (Exception e) {
            resp.setStatus(500); resp.getWriter().write(e.getMessage());
        }
    }

    /* ===== helpers ===== */
    private String readBody(HttpServletRequest req) throws IOException {
        try(BufferedReader br = new BufferedReader(new InputStreamReader(req.getInputStream(), StandardCharsets.UTF_8))){
            StringBuilder sb = new StringBuilder(); String line;
            while((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }
    private static String pick(String json, String key){
        if(json == null) return "";
        String p = "\""+key+"\"\\s*:\\s*\"";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(p + "([^\"\\\\]*(?:\\\\.[^\"\\\\]*)*)\"").matcher(json);
        return m.find() ? m.group(1).replace("\\\"", "\"").replace("\\\\","\\") : "";
    }

    private static String safeUpper(String s){ return s == null ? "" : s.trim().toUpperCase(); }
    private static String safeLower(String s){ return s == null ? "" : s.trim().toLowerCase(); }
    private static String nz(String s){ return (s==null) ? "" : s.trim(); }

    private static String buildDeptDisplay(String code, String name, String status) {
        String s = (status==null || status.isEmpty()) ? "" : status.toLowerCase();
        String stat = s.isEmpty() ? "" : " (" + s + ")";
        String c = nz(code), n = nz(name);
        if (!c.isEmpty() && !n.isEmpty()) return c + " – " + n + stat;
        if (!n.isEmpty()) return n + stat;
        if (!c.isEmpty()) return "code " + c + stat;
        return "unknown" + stat;
    }

    private String clientIp(HttpServletRequest req) {
        String[] hdrs = {"X-Forwarded-For","X-Real-IP","CF-Connecting-IP","True-Client-IP"};
        for (String h : hdrs) {
            String v = req.getHeader(h);
            if (v != null && !v.isEmpty()) return v.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }
}
