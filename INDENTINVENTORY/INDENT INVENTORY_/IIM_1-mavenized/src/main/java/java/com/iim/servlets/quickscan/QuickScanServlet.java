// com.iim.servlets.quickscan.QuickScanServlet
package com.iim.servlets.quickscan;

import com.iim.dao.DepartmentDAO;
import com.iim.models.Department;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.List;

public class QuickScanServlet extends HttpServlet {
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    HttpSession s = req.getSession(false);
    if (s == null || s.getAttribute("user") == null) {
      resp.sendRedirect(req.getContextPath() + "/login.jsp"); // ✅ betul
      return;
    }

    List<Department> deps = new DepartmentDAO().listActive();
    req.setAttribute("departments", deps);
    req.getRequestDispatcher("/quickscan.jsp").forward(req, resp); // ✅ leading slash selamat
  }
}
