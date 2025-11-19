package com.iim.servlets.user;

import com.iim.dao.UserDAO;
import com.iim.dao.ActivityLogDAO;
import com.iim.models.User;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name="UserServlet", urlPatterns={"/users"})
public class UserServlet extends HttpServlet {

    private final UserDAO dao = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Forward ke JSP (JSP sendiri akan panggil dao.listAll())
        req.getRequestDispatcher("user.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        req.setCharacterEncoding("UTF-8");
        String action = req.getParameter("action");

        // Info pelaku untuk log
        HttpSession s = req.getSession(false);
        User me = (s == null) ? null : (User) s.getAttribute("user");
        Integer actorId = (me == null) ? null : me.getId();
        String ip = req.getRemoteAddr();
        ActivityLogDAO logger = new ActivityLogDAO();

        if ("add".equalsIgnoreCase(action)) {
            // ambil & trim
            String category = nz(req.getParameter("category"));
            String username = nz(req.getParameter("username"));
            String password = nz(req.getParameter("password")); // required on add
            String status   = nz(req.getParameter("status"));
            String desc     = nz(req.getParameter("description"));

            // VALIDATION: all required on Add
            if (category.isEmpty() || username.isEmpty() || password.isEmpty() || status.isEmpty()) {
                req.setAttribute("error", "Please fill in Category, Username, Password and Status to add a user.");
                req.getRequestDispatcher("user.jsp").forward(req, resp);
                return;
            }

            // BLOCK another admin on ADD
            if (isAdmin(category)) {
                if (dao.countAdmins() >= 1) {
                    req.setAttribute("error", "There must be only 1 admin. Please choose another category/role.");
                    req.getRequestDispatcher("user.jsp").forward(req, resp);
                    return;
                }
            }

            User u = new User();
            u.setCategory(category);
            u.setUsername(username);
            u.setPassword(password); // plaintext (ikut setup sedia ada)
            u.setStatus(status);
            u.setDescription(desc);

            boolean ok = dao.insert(u);

            if (ok) {
                // ✅ log create user
                String detail = String.format(
                        "Created user '%s' (category %s, status %s)",
                        nz(u.getUsername()), emptyDash(u.getCategory()), emptyDash(u.getStatus())
                );
                logger.log(actorId, "USER_CREATE", detail, ip);
            }

        } else if ("update".equalsIgnoreCase(action)) {
            int id = parseInt(req.getParameter("id"), 0);
            if (id <= 0) {
                req.setAttribute("error", "Invalid user id.");
                req.getRequestDispatcher("user.jsp").forward(req, resp);
                return;
            }

            User current = dao.findById(id); // nilai ASAL sebelum ubah
            if (current != null) {
                // Simpan nilai asal
                String oldCategory = nz(current.getCategory());
                String oldUsername = nz(current.getUsername());
                String oldStatus   = nz(current.getStatus());
                String oldDesc     = nz(current.getDescription());

                // Nilai baharu dari form
                String newCategory = nz(req.getParameter("category"));
                String newUsername = nz(req.getParameter("username"));
                String newPassword = nz(req.getParameter("password")); // optional on update
                String newStatus   = nz(req.getParameter("status"));
                String newDesc     = nz(req.getParameter("description"));

                // VALIDATION: required fields on Update (except password)
                if (newCategory.isEmpty() || newUsername.isEmpty() || newStatus.isEmpty()) {
                    req.setAttribute("error", "Please fill in Category, Username and Status.");
                    req.getRequestDispatcher("user.jsp").forward(req, resp);
                    return;
                }

                // If trying to set this user as admin, ensure no other admin exists.
                if (isAdmin(newCategory)) {
                    // allowed ONLY if this user is the sole admin (i.e., no other admin except possibly itself)
                    if (dao.hasAnotherAdmin(id)) {
                        req.setAttribute("error", "There must be only 1 admin. Please choose another category/role.");
                        req.getRequestDispatcher("user.jsp").forward(req, resp);
                        return;
                    }
                }

                boolean changePwd = !newPassword.isEmpty();

                // Apply nilai baharu untuk disimpan
                current.setCategory(newCategory);
                current.setUsername(newUsername);
                if (changePwd) current.setPassword(newPassword);
                current.setStatus(newStatus);
                current.setDescription(newDesc);

                boolean ok = dao.update(current, changePwd);

                if (ok) {
                    // ✅ bina mesej perubahan
                    List<String> parts = new ArrayList<>();
                    if (!oldUsername.equals(newUsername)) {
                        parts.add(String.format("Username '%s' → '%s'", oldUsername, newUsername));
                    }
                    if (!oldCategory.equals(newCategory)) {
                        parts.add(String.format("Category %s → %s", emptyDash(oldCategory), emptyDash(newCategory)));
                    }
                    if (!oldStatus.equals(newStatus)) {
                        parts.add(String.format("Status %s → %s", emptyDash(oldStatus), emptyDash(newStatus)));
                    }
                    if (!oldDesc.equals(newDesc)) {
                        parts.add("Description updated");
                    }
                    if (changePwd) {
                        parts.add("Password changed");
                    }

                    String subject = newUsername.isEmpty() ? ("ID " + id) : ("'" + newUsername + "'");
                    String detail;
                    if (parts.isEmpty()) {
                        detail = String.format("No changes detected for user %s", subject);
                    } else if (parts.size() == 1) {
                        detail = "Updated user " + subject + " — " + parts.get(0);
                    } else {
                        detail = "Updated user " + subject + " — " + String.join("; ", parts);
                    }

                    logger.log(actorId, "USER_UPDATE", detail, ip);
                }
            }
        }

        // kembali ke senarai
        resp.sendRedirect("users");
    }

    /* ---------- helpers ---------- */
    private static String nz(String s) { return (s == null) ? "" : s.trim(); }
    private static boolean isAdmin(String s){ return "admin".equalsIgnoreCase(nz(s)); }
    private static String emptyDash(String s){ s = nz(s); return s.isEmpty()? "-" : s; }
    private static int parseInt(String s, int def){ try { return Integer.parseInt(s); } catch (Exception e){ return def; } }
}
