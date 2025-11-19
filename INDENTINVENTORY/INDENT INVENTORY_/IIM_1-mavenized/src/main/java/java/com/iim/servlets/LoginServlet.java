package com.iim.servlets;

import com.iim.dao.UserDAO;
import com.iim.models.User;
import com.iim.dao.ActivityLogDAO;   // ✅ import logger

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

@WebServlet(name="LoginServlet", urlPatterns={"/login"})
public class LoginServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setCharacterEncoding("UTF-8");

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (username != null) username = username.trim();
        if (password != null) password = password.trim();

        UserDAO dao = new UserDAO();
        User user = dao.authenticate(username, password);

        if (user != null) {
            HttpSession session = req.getSession(true);
            session.setAttribute("user", user);

            
            new ActivityLogDAO().log(
                user.getId(),                     // who
                "LOGIN",                          // action
                "User " + user.getUsername() + " logged in", // detail
                req.getRemoteAddr()               // IP
            );

            resp.sendRedirect("home.jsp");
        } else {
            req.setAttribute("error", "Invalid username or password.");
            req.getRequestDispatcher("login.jsp").forward(req, resp);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.getRequestDispatcher("login.jsp").forward(req, resp);
    }
}
