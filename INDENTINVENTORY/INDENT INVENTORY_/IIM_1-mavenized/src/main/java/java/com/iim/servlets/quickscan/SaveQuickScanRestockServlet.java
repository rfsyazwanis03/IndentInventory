package com.iim.servlets.quickscan;

import com.iim.dao.*;
import com.iim.models.User;
import com.iim.models.Item;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.*;
import java.sql.Connection;
import java.sql.Timestamp;
import java.time.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.iim.dao.AlertDAO;

import org.json.*;

@WebServlet(name="SaveQuickScanRestockServlet", urlPatterns={"/save-quickscan-restock"})
public class SaveQuickScanRestockServlet extends HttpServlet {

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    resp.setContentType("text/plain; charset=UTF-8");
    User me = (User) req.getSession().getAttribute("user");
    if (me == null) {
      resp.setStatus(401);
      resp.getWriter().write("Not logged in");
      return;
    }

    String body = new BufferedReader(new InputStreamReader(req.getInputStream(), "UTF-8"))
            .lines().reduce("", (a,b)->a+b);

    try {
      JSONObject j = new JSONObject(body);

      // Date handling
      final String dateStr = j.optString("date", null);
      ZoneId ZONE = ZoneId.of("Asia/Kuala_Lumpur");
      LocalDate pickedLocalDate;
      try {
        pickedLocalDate = (dateStr == null || dateStr.isEmpty())
                ? LocalDate.now(ZONE)
                : LocalDate.parse(dateStr);
      } catch (Exception bad) {
        pickedLocalDate = LocalDate.now(ZONE);
      }
      LocalTime nowLocalTime = LocalTime.now(ZONE);
      ZonedDateTime zdt = ZonedDateTime.of(pickedLocalDate, nowLocalTime, ZONE);
      Timestamp transTime = Timestamp.from(zdt.toInstant());

      JSONArray items = j.getJSONArray("items");

      InventoryDAO invDao = new InventoryDAO();
      IndentTransactionDAO trxDao = new IndentTransactionDAO();
      ActivityLogDAO logDao = new ActivityLogDAO();
      ItemDAO itemDao = new ItemDAO();

      int totalQty = 0;
      List<String> itemLines = new ArrayList<>();
      Set<Integer> touchedItemIds = new HashSet<>();

      try (Connection c = com.iim.db.DBConnection.getConnection()) {
        c.setAutoCommit(false);
        try {
          for (int i=0; i<items.length(); i++) {
            JSONObject it = items.getJSONObject(i);
            int itemId = it.getInt("id");
            int qty    = it.getInt("qty");
            String code = it.optString("code", null);

            if (qty <= 0) throw new IllegalArgumentException("Invalid qty for item " + itemId);

            // RESTOCK
            invDao.increase(c, itemId, qty);
            trxDao.insertRestock(c, itemId, qty, code, me.getId(), null, transTime);

            totalQty += qty;
            touchedItemIds.add(itemId);

            try {
              Item item = itemDao.findById(itemId);
              String name = (item != null && item.getItemName()!=null) ? item.getItemName() : ("Item " + itemId);
              itemLines.add(name + " ×" + qty);
            } catch (Exception ignore) {
              String fallback = (code==null||code.isEmpty()? ("Item " + itemId) : code) + " ×" + qty;
              itemLines.add(fallback);
            }
          }
          c.commit();

          // Trigger alert check untuk setiap item yang disentuh
          AlertDAO alertDao = new AlertDAO();
          for (Integer id : touchedItemIds) {
            try { alertDao.generateLowStockAlertForItem(id); } catch (Exception ignore) {}
          }

        } catch (Exception ex) {
          c.rollback();
          throw ex;
        } finally {
          c.setAutoCommit(true);
        }
      }

      String itemsText;
      if (itemLines.size() <= 6) {
        itemsText = String.join("; ", itemLines);
      } else {
        int remain = itemLines.size() - 6;
        itemsText = String.join("; ", itemLines.subList(0, 6)) + " … +" + remain + " more";
      }

      String actor = (me.getUsername()!=null && !me.getUsername().trim().isEmpty())
              ? me.getUsername().trim() : ("ID#" + me.getId());
      String detail = "Restocked (Quick Scan): " + itemsText +
                      " (total " + totalQty + " unit" + (totalQty==1?"": "s") + ") — by " + actor;

      try {
        // 🔴 DULU: "QUICK_SCAN_RESTOCK_SAVE"
        // 🟢 SEKARANG: standard action "RESTOCK"
        logDao.log(me.getId(), "RESTOCK", detail, req.getRemoteAddr());
      } catch (Exception logEx) { logEx.printStackTrace(); }

      resp.setStatus(200);
      resp.getWriter().write("OK");

    } catch (Exception ex) {
      ex.printStackTrace();
      resp.setStatus(400);
      resp.getWriter().write(ex.getMessage() == null ? "Invalid payload" : ex.getMessage());
    }
  }
}
