package com.iim.filters;

import com.iim.dao.AlertDAO;
import com.iim.models.Alert;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import java.io.IOException;
import java.util.List;

@WebFilter("/*")
public class HeaderInitFilter implements Filter {
    private final AlertDAO alertDAO = new AlertDAO();

    @Override public void init(FilterConfig filterConfig) { }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            // Hanya proses reminder; JANGAN generate alert low-stock di sini
            alertDAO.resendDueReminders();

            List<Alert> feed = alertDAO.headerFeed();
            int unreadCount = alertDAO.unreadCount();

            request.setAttribute("notifList", feed);
            request.setAttribute("unreadCount", unreadCount);
        } catch (Exception ignored) {}
        chain.doFilter(request, response);
    }

    @Override public void destroy() { }
}
