package com.iim.servlets.quickscan;

import com.iim.dao.ItemDAO;
import com.iim.models.Item;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.List;

@WebServlet(name="SearchItemsServlet", urlPatterns={"/search-items"})
public class SearchItemsServlet extends HttpServlet {
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    resp.setContentType("application/json; charset=UTF-8");
    String q = req.getParameter("q");
    if (q==null || q.trim().isEmpty()){ resp.getWriter().write("[]"); return; }
    List<Item> items = new ItemDAO().searchByName(q.trim(), 12);
    StringBuilder sb = new StringBuilder("[");
    for (int i=0;i<items.size();i++){
      Item it = items.get(i);
      sb.append(String.format("{\"id\":%d,\"code\":%s,\"name\":\"%s\"}",
        it.getId(),
        it.getCodeBarcode()==null? "null" : "\"" + it.getCodeBarcode().replace("\"","\\\"") + "\"",
        it.getItemName().replace("\"","\\\"")));
      if(i<items.size()-1) sb.append(",");
    }
    sb.append("]");
    resp.getWriter().write(sb.toString());
  }
}
