package com.iim.servlets.logs;

import com.iim.dao.ActivityLogDAO;
import com.iim.dao.DepartmentDAO;
import com.iim.dao.ItemDAO;
import com.iim.models.Department;
import com.iim.models.Item;
import com.iim.models.User;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@WebServlet(name="ActivityLogServlet", urlPatterns={"/log", "/logs"})
public class ActivityLogServlet extends HttpServlet {

  private static final TimeZone MYT = TimeZone.getTimeZone("Asia/Kuala_Lumpur");
  private static final SimpleDateFormat DF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
  static { DF.setTimeZone(MYT); }

  private final DepartmentDAO deptDAO = new DepartmentDAO();
  private final ItemDAO itemDAO = new ItemDAO();
  private final Map<Integer, String> deptCache = new HashMap<>();
  private final Map<Integer, Item> itemCache = new HashMap<>();

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
    User me = (User) req.getSession().getAttribute("user");
    if (me == null) { resp.sendRedirect("login.jsp"); return; }

    String path = req.getServletPath();
    if ("/log".equals(path)) {
      req.getRequestDispatcher("log.jsp").forward(req, resp);
      return;
    }

    String action = param(req, "action");
    String from   = param(req, "from");
    String to     = param(req, "to");
    String q      = param(req, "q");
    int page      = parseInt(param(req, "page"), 1);
    int size      = parseInt(param(req, "size"), 50);
    int offset    = Math.max(0, (page-1) * size);

    List<ActivityLogDAO.LogRow> rows = new ActivityLogDAO().list(action, from, to, q, size, offset);

    JSONArray out = new JSONArray();
    for (ActivityLogDAO.LogRow r : rows) {
      String t = (r.eventTime == null) ? "" : DF.format(r.eventTime);
      JSONObject o = new JSONObject();
      o.put("time", t);
      o.put("action", humanAction(nz(r.action)));
      o.put("detail", humanDetail(nz(r.action), nz(r.detail)));
      out.put(o);
    }

    resp.setContentType("application/json; charset=UTF-8");
    resp.getWriter().write(out.toString());
  }

  private static String param(HttpServletRequest req, String k){
    String v = req.getParameter(k);
    return (v==null||v.trim().isEmpty())? null : v.trim();
  }
  private static int parseInt(String s, int def){ try{return Integer.parseInt(s);}catch(Exception e){return def;} }
  private static String nz(String s){ return s==null? "" : s.trim(); }

  private String humanAction(String raw){
    switch (raw) {
      case "LOGIN":            return "Login";
      case "LOGOUT":           return "Logout";
      case "ADD_ITEM":         return "Add Item";
      case "UPDATE_ITEM":      return "Update Item";
      case "SCAN_ITEM":        return "Scan Item";
      case "QUICK_SCAN_SAVE":  return "Stock Issued";
      case "DEPT_CREATE":    return "Create Department";
case "DEPT_UPDATE":    return "Update Department";
      case "REPORT_GENERATE":  return "Generate Report";
      case "REPORT_EXPORT":    return "Export Report";
      case "USER_CREATE":      return "Create User";
      case "USER_UPDATE":      return "Update User";
      
      default:
        String a = raw.replace('_',' ').toLowerCase(Locale.ENGLISH);
        StringBuilder sb = new StringBuilder();
        boolean cap = true;
        for(char c: a.toCharArray()){
          if (cap && Character.isLetter(c)) { sb.append(Character.toUpperCase(c)); cap=false; }
          else sb.append(c);
          if (c==' ') cap=true;
        }
        return sb.toString();
    }
  }

  private String humanDetail(String action, String detail){
    if (detail.isEmpty()) return "";

    String dl = detail.toLowerCase(Locale.ENGLISH);
    if ("UPDATE_ITEM".equals(action) && (
        dl.startsWith("restocked:") ||
        dl.startsWith("quantity adjusted:") ||
        dl.startsWith("quantity unchanged:") ||
        dl.startsWith("renamed item:") ||
        dl.startsWith("barcode updated:") ||
        dl.startsWith("status updated:") ||
        dl.startsWith("updated:")
    )) {
      return detail;
    }

    try {
      if ("QUICK_SCAN_SAVE".equals(action)) {
        Matcher m = Pattern.compile("department\\s+(\\d+)").matcher(detail);
        if (m.find()) {
          int depId = Integer.parseInt(m.group(1));
          String dept = deptDisplay(depId);
          if (dept != null) detail = detail.replaceAll("department\\s+\\d+", dept);
        }
        detail = detail.replaceFirst("^Saved\\s+(\\d+)\\s+item\\(s\\)\\s+for\\s+", "Issued $1 item(s) to ");
        return detail;
      }

      if ("ADD_ITEM".equals(action)) {
        Matcher m = Pattern.compile("Added\\s+'([^']+)'\\s*\\(([^)]*)\\)\\s*qty=(\\d+)\\s*min=(\\d+)").matcher(detail);
        if (m.find()) {
          String name = m.group(1), code = m.group(2), qty = m.group(3), min = m.group(4);
          return String.format("Item created: %s (barcode %s); opening qty %s, min %s", name, code, qty, min);
        }
        return detail;
      }

      if ("UPDATE_ITEM".equals(action)) {
        Matcher mOld = Pattern.compile("id=(\\d+)\\s+code=([^\\s]+)\\s+qty=(\\d+)\\s+status=([\\w-]+)").matcher(detail);
        if (mOld.find()) {
          int itemId = Integer.parseInt(mOld.group(1));
          String code = mOld.group(2);
          int newQty  = Integer.parseInt(mOld.group(3));
          Item it = item(itemId);
          String title = it==null? ("barcode " + code)
                          : ((nz(it.getItemName()).isEmpty()? "Item" : it.getItemName()) +
                             " (barcode " + (nz(it.getCodeBarcode()).isEmpty()? code : it.getCodeBarcode()) + ")");
          return String.format("Quantity updated to %d for %s", newQty, title);
        }
        return detail;
      }

      if ("SCAN_ITEM".equals(action)) {
        Matcher m1 = Pattern.compile("^Scanned\\s+(\\S+)(?:\\s+(.+))?$").matcher(detail);
        if (m1.find()) {
          String token = m1.group(1);
          if (token.matches("\\d+")) {
            Item it = item(Integer.parseInt(token));
            if (it != null) {
              String code = nz(it.getCodeBarcode()).isEmpty()? ("id="+it.getId()) : it.getCodeBarcode();
              String name = nz(it.getItemName());
              return "Item scanned: " + code + (name.isEmpty()? "" : " — " + name);
            }
          } else {
            Item it = byBarcode(token);
            if (it != null) {
              String name = nz(it.getItemName());
              return "Item scanned: " + token + (name.isEmpty()? "" : " — " + name);
            }
          }
        }
        return detail;
      }

      return detail;

    } catch (Exception ex) {
      return detail;
    }
  }

  private String deptDisplay(int id){
    if (deptCache.containsKey(id)) return deptCache.get(id);
    try {
      Department d = deptDAO.findById(id);
      if (d == null) { deptCache.put(id, null); return null; }
      String code = nz(d.getCode());
      String name = nz(d.getName());
      String disp = code.isEmpty()? name : (name.isEmpty()? code : code + " – " + name);
      deptCache.put(id, disp);
      return disp;
    } catch (Exception e) {
      deptCache.put(id, null);
      return null;
    }
  }

  private Item item(int id){
    if (itemCache.containsKey(id)) return itemCache.get(id);
    try { Item it = itemDAO.findById(id); itemCache.put(id, it); return it; }
    catch(Exception e){ itemCache.put(id, null); return null; }
  }
  private Item byBarcode(String code){
    try { return itemDAO.findByBarcode(code); } catch(Exception e){ return null; }
  }
}
