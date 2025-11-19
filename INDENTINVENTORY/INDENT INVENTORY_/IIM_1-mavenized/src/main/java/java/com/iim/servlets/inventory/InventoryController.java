package com.iim.servlets.inventory;

import com.iim.dao.ItemDAO;
import com.iim.dao.ActivityLogDAO;
import com.iim.dao.AlertDAO;
import com.iim.models.Item;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.util.*;
import org.json.*;

public class InventoryController extends HttpServlet {

  private String readBody(HttpServletRequest req) throws IOException {
    req.setCharacterEncoding("UTF-8");
    try (BufferedReader br = req.getReader()) {
      StringBuilder sb = new StringBuilder();
      String line;
      while((line = br.readLine()) != null) sb.append(line);
      return sb.toString();
    }
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    com.iim.models.User me = (com.iim.models.User) req.getSession().getAttribute("user");
    if (me == null) { resp.sendRedirect("login.jsp"); return; }

    String path = req.getServletPath();   // /items or /item
    ItemDAO dao = new ItemDAO();
    resp.setCharacterEncoding("UTF-8");

    try {
      if ("/items".equals(path)) {
        String q = Optional.ofNullable(req.getParameter("q")).orElse("");
        String sort = Optional.ofNullable(req.getParameter("sort")).orElse("name_asc");
        List<Item> list = dao.listForInventory(q, sort);
        JSONArray arr = new JSONArray();
        for (Item it : list) {
          JSONObject o = new JSONObject();
          o.put("id", it.getId());
          o.put("code", Optional.ofNullable(it.getCodeBarcode()).orElse(""));
          o.put("name", Optional.ofNullable(it.getItemName()).orElse(""));
          o.put("quantity", it.getQuantity());
          o.put("status", Optional.ofNullable(it.getStatus()).orElse(""));
          o.put("min_quantity", it.getMinQuantity());
          o.put("critical_min_quantity", it.getCriticalMinQuantity());
          arr.put(o);
        }
        resp.setContentType("application/json");
        resp.getWriter().write(arr.toString());
        return;
      }

      if ("/item".equals(path)) {
        String sid = req.getParameter("id");
        int id = Integer.parseInt(sid);
        Item it = dao.findDetailForInventory(id);
        if (it == null) { resp.sendError(404, "Not found"); return; }
        JSONObject o = new JSONObject();
        o.put("id", it.getId());
        o.put("code", Optional.ofNullable(it.getCodeBarcode()).orElse(""));
        o.put("name", Optional.ofNullable(it.getItemName()).orElse(""));
        o.put("quantity", it.getQuantity());
        o.put("status", Optional.ofNullable(it.getStatus()).orElse(""));
        o.put("min_quantity", it.getMinQuantity());
        o.put("critical_min_quantity", it.getCriticalMinQuantity());
        resp.setContentType("application/json");
        resp.getWriter().write(o.toString());
        return;
      }

      // default forward
      req.getRequestDispatcher("inventory.jsp").forward(req, resp);

    } catch (Exception e) {
      e.printStackTrace();
      resp.sendError(500, e.getMessage());
    }
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    com.iim.models.User me = (com.iim.models.User) req.getSession().getAttribute("user");
    if (me == null) { resp.sendRedirect("login.jsp"); return; }

    String path = req.getServletPath(); // /item-create, /item-update, /item-qty
    ItemDAO dao = new ItemDAO();

    try {
      JSONObject body = new JSONObject(readBody(req));

      // -------- CREATE (opening stock) --------
      if ("/item-create".equals(path)) {
        String code   = body.optString("code", "").trim().toUpperCase();
        String name   = body.optString("name", "").trim().toUpperCase();
        int qty       = Math.max(0, body.optInt("quantity", 0));
        int minQty    = Math.max(0, body.optInt("min_quantity", 0));
        int critMin   = Math.max(0, body.optInt("critical_min_quantity", 0));

        if (code.isEmpty() || name.isEmpty()) { resp.sendError(400, "code and name required"); return; }
        try {
          dao.addWithOpeningStock(name, code, qty, minQty, critMin);

          String actor = (me.getUsername()!=null && !me.getUsername().trim().isEmpty())
                  ? me.getUsername().trim() : ("ID#" + me.getId());
          new ActivityLogDAO().log(
              me.getId(),
              "ADD_ITEM",
              "Item created: " + name + " (barcode " + code + "); opening qty " + qty +
                  ", min " + minQty + (critMin>0? (", critical min " + critMin) : "") +
                  " — by " + actor,
              req.getRemoteAddr()
          );

          // REAL-TIME low-stock scan for all items
          new AlertDAO().generateLowStockAlerts();

          resp.setStatus(200); resp.getWriter().write("OK");
        } catch (java.sql.SQLIntegrityConstraintViolationException dup) {
          resp.setStatus(409); resp.getWriter().write("Duplicate barcode");
        } catch (Exception e) {
          e.printStackTrace(); resp.sendError(500, e.getMessage());
        }
        return;
      }

      // -------- STATUS UPDATE ONLY --------
      if ("/item-update".equals(path)) {
        int id       = body.getInt("id");
        String st    = body.optString("status", "").trim().toLowerCase();
        if (!"active".equals(st) && !"inactive".equals(st)) {
          resp.sendError(400, "invalid status"); return;
        }

        Item before = dao.findDetailForInventory(id);
        if (before == null) { resp.sendError(404, "Not found"); return; }

        String name = before.getItemName();
        String code = before.getCodeBarcode();
        int qty     = Math.max(0, before.getQuantity());
        String oldStatus = Optional.ofNullable(before.getStatus()).orElse("");

        dao.updateItemAndQuantity(id, name, code, st, qty);

        String actor = (me.getUsername()!=null && !me.getUsername().trim().isEmpty())
                ? me.getUsername().trim() : ("ID#" + me.getId());
        String detailMsg = String.format(
            "Status updated: %s (barcode %s) • %s → %s — by %s",
            nz(name).isEmpty()? "Item" : nz(name),
            nz(code).isEmpty()? "-" : nz(code),
            nz(oldStatus).isEmpty()? "-" : nz(oldStatus),
            st,
            actor
        );
        new ActivityLogDAO().log(me.getId(), "UPDATE_ITEM_STATUS", detailMsg, req.getRemoteAddr());

        resp.setStatus(200); resp.getWriter().write("OK"); return;
      }

      // -------- QUANTITY UPDATE (SET / ADJUST) --------
      // POST /item-qty  { id, mode:"set"|"adjust", value:int }
      if ("/item-qty".equals(path)) {
        int id = body.getInt("id");
        String mode = body.optString("mode","adjust").trim().toLowerCase();
        int val = body.optInt("value", 0);

        Item it = dao.findDetailForInventory(id);
        if (it == null) { resp.sendError(404, "Not found"); return; }

        int oldQty = Math.max(0, it.getQuantity());
        int newQty = oldQty;

        if ("set".equals(mode)) newQty = Math.max(0, val);
        else newQty = Math.max(0, oldQty + val);

        dao.updateItemAndQuantity(
            it.getId(),
            Optional.ofNullable(it.getItemName()).orElse(""),
            Optional.ofNullable(it.getCodeBarcode()).orElse(""),
            Optional.ofNullable(it.getStatus()).orElse("active"),
            newQty
        );

        String actor = (me.getUsername()!=null && !me.getUsername().trim().isEmpty())
                ? me.getUsername().trim() : ("ID#" + me.getId());

        // 🟢 Bezakan jenis action untuk log
        String logAction;
        String verb;
        if ("set".equals(mode)) {
          logAction = "SET_STOCK";
          verb      = "set";
        } else if (val > 0) {
          // adjust + value positif = RESTOCK
          logAction = "RESTOCK";
          verb      = "increment";
        } else if (val < 0) {
          logAction = "ADJUST_DECREMENT";
          verb      = "decrement";
        } else {
          logAction = "UPDATE_ITEM_QUANTITY";
          verb      = "adjust";
        }

        String msg = String.format(
            "Quantity %s: %s (barcode %s) • %d → %d — by %s",
            verb,
            nz(it.getItemName()), nz(it.getCodeBarcode()), oldQty, newQty, actor
        );
        new ActivityLogDAO().log(me.getId(), logAction, msg, req.getRemoteAddr());

        // Trigger alert segera untuk item ini
        new AlertDAO().generateLowStockAlertForItem(id);

        JSONObject out = new JSONObject();
        out.put("ok", true);
        out.put("new_quantity", newQty);
        resp.setContentType("application/json; charset=UTF-8");
        resp.getWriter().write(out.toString());
        return;
      }

      resp.sendError(404, "Unknown path");

    } catch (Exception e) {
      e.printStackTrace();
      resp.sendError(500, e.getMessage());
    }
  }

  private static String nz(String s){ return s==null? "" : s.trim(); }
}
