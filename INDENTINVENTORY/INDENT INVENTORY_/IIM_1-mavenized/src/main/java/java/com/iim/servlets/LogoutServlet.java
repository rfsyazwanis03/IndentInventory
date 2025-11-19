package com.iim.servlets;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.iim.dao.ActivityLogDAO;
import com.iim.models.User;

@WebServlet("/LogoutServlet")
public class LogoutServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session != null) {
            User me = (User) session.getAttribute("user");
            Integer uid = (me == null ? null : me.getId());
            String uname = (me == null ? "Unknown" : me.getUsername());

            // ✅ Log logout event with username
            new ActivityLogDAO().log(uid, "LOGOUT", 
                "User " + uname + " logged out", 
                request.getRemoteAddr());

            session.invalidate(); // clear everything
        }

        response.sendRedirect("login.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }
}
