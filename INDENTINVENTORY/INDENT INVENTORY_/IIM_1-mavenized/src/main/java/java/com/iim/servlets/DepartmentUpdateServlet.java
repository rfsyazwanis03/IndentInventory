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

@WebServlet(name="DepartmentUpdateServlet", urlPatterns={"/department-update"})
public class DepartmentUpdateServlet extends HttpServlet {
    private final DepartmentDAO dao = new DepartmentDAO();
    private final ActivityLogDAO logDAO = new ActivityLogDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/plain; charset=UTF-8");
        String body = readBody(req);

        Integer id    = pickInt(body, "id");
        String code   = safeUpper(pickStr(body, "code"));
        String name   = safeUpper(pickStr(body, "name"));
        String status = safeLower(pickStr(body, "status"));

        if (id == null || id <= 0) { resp.setStatus(400); resp.getWriter().write("Invalid id."); return; }
        if (code.isEmpty() || name.isEmpty()) { resp.setStatus(400); resp.getWriter().write("Code and Name are required."); return; }
        if (!status.equals("active") && !status.equals("inactive")) status = "active";

        try {
            Department current = dao.findById(id);
            if (current == null) { resp.setStatus(404); resp.getWriter().write("Not found."); return; }

            // Check duplicate code (pass the normalized uppercase code)
            if (dao.codeExists(code, id)) { resp.setStatus(409); resp.getWriter().write("Code already exists."); return; }

            current.setCode(code);
            current.setName(name);
            current.setStatus(status);
            dao.update(current);

            // ===== LOG: DEPT_UPDATE (friendly) =====
            User me = (User) req.getSession().getAttribute("user");
            Integer userId = (me == null) ? null : me.getId();
            String username = (me == null || me.getUsername()==null || me.getUsername().trim().isEmpty())
                    ? "Unknown user" : me.getUsername().trim();
            String ip = clientIp(req);
            String display = buildDeptDisplay(code, name, status);
            String detail  = String.format("%s updated department %s", username, display);
            logDAO.log(userId, "DEPT_UPDATE", detail, ip);
            // =======================================

            resp.getWriter().write("ok");
        } catch (Exception e) {
            resp.setStatus(500); resp.getWriter().write(e.getMessage());
        }
    }

    /* ===== helpers ===== */
    private String readBody(HttpServletRequest req) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(req.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder(); String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }
    private static String pickStr(String json, String key){
        if (json == null) return "";
        String p = "\""+key+"\"\\s*:\\s*\"";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(p + "([^\"\\\\]*(?:\\\\.[^\"\\\\]*)*)\"").matcher(json);
        return m.find() ? m.group(1).replace("\\\"", "\"").replace("\\\\","\\") : "";
    }
    private static Integer pickInt(String json, String key){
        if (json == null) return null;
        java.util.regex.Matcher mNum = java.util.regex.Pattern.compile("\""+key+"\"\\s*:\\s*(\\d+)").matcher(json);
        if (mNum.find()) { try { return Integer.parseInt(mNum.group(1)); } catch (Exception ignore) {} }
        String s = pickStr(json, key);
        if (!s.isEmpty()) { try { return Integer.parseInt(s.trim()); } catch (Exception ignore) {} }
        return null;
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
