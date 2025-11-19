package com.iim.servlets;

import com.iim.dao.ReportDAO;
import com.iim.dao.ActivityLogDAO;
import com.iim.models.ReportRow;
import com.iim.models.Item;
import com.iim.models.User;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@WebServlet(name="ReportsServlet", urlPatterns={"/reports"})
public class ReportsServlet extends HttpServlet {
    private final ReportDAO dao = new ReportDAO();
    private final ActivityLogDAO logDAO = new ActivityLogDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Always provide items
        req.setAttribute("items", dao.listActiveItems());

        Integer itemId = parseInt(req.getParameter("item"));
        Integer year   = parseInt(req.getParameter("year"));

        if (itemId != null && year != null) {
            List<ReportRow> rows = dao.getUsageByDepartment(itemId, year);
            String itemName = dao.getItemName(itemId);

            // === Totals by month (columns) + grand total ===
            int[] colTotals = new int[12];
            int grandTotal = 0;
            for (ReportRow r : rows) {
                int[] m = r.getMonths();
                for (int i = 0; i < 12; i++) {
                    colTotals[i] += m[i];
                    grandTotal += m[i];
                }
            }

            req.setAttribute("rows", rows);
            req.setAttribute("chosenItemName", itemName);
            req.setAttribute("chosenYear", year);
            req.setAttribute("colTotals", colTotals);
            req.setAttribute("grandTotal", grandTotal);

            // ==== LOG: "user generated report for X (YYYY)" ====
            User me = (User) req.getSession().getAttribute("user");
            Integer userId = (me == null) ? null : me.getId();
            String username = (me == null || me.getUsername()==null || me.getUsername().trim().isEmpty())
                              ? "Unknown user" : me.getUsername().trim();
            String ip = clientIp(req);

            String detail = String.format("%s generated report for %s (%d)",
                    username, itemName, year);

            logDAO.log(userId, "REPORT_GENERATE", detail, ip);
            // ===================================================
        }

        req.getRequestDispatcher("reports.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String export = req.getParameter("export");
        Integer itemId = parseInt(req.getParameter("item"));
        Integer year   = parseInt(req.getParameter("year"));

        User me = (User) req.getSession().getAttribute("user");
        Integer userId = (me == null) ? null : me.getId();
        String username = (me == null || me.getUsername()==null || me.getUsername().trim().isEmpty())
                          ? "Unknown user" : me.getUsername().trim();
        String ip = clientIp(req);

        // ==== REPORT PDF EXPORT ====
        if ("pdf".equalsIgnoreCase(export) && itemId != null && year != null) {
            String itemName = dao.getItemName(itemId);
            String detail = String.format("%s exported PDF report for %s (%d)",
                    username, itemName, year);
            logDAO.log(userId, "REPORT_EXPORT_PDF", detail, ip);

            exportPdf(resp, itemId, year);
            return;
        }

        // ==== REPORT EXCEL (CSV) EXPORT ====
        if ("excel".equalsIgnoreCase(export) && itemId != null && year != null) {
            String itemName = dao.getItemName(itemId);
            String detail = String.format("%s exported Excel (CSV) report for %s (%d)",
                    username, itemName, year);
            logDAO.log(userId, "REPORT_EXPORT_EXCEL", detail, ip);

            exportExcel(resp, itemId, year);
            return;
        }

        // ==== INVENTORY PDF EXPORT ====
        if ("inventory".equalsIgnoreCase(export)) {
            String qParam = req.getParameter("q");
            String sortParam = req.getParameter("sort");
            String detail = username + " exported Inventory PDF list";
            logDAO.log(userId, "INVENTORY_EXPORT", detail, ip);

            exportInventoryPdf(resp, qParam, sortParam);
            return;
        }

        doGet(req, resp);
    }

    private Integer parseInt(String s) {
        try { return (s == null || s.isEmpty()) ? null : Integer.parseInt(s); }
        catch (Exception e) { return null; }
    }

    private String clientIp(HttpServletRequest req) {
        String[] hdrs = {"X-Forwarded-For","X-Real-IP","CF-Connecting-IP","True-Client-IP"};
        for (String h : hdrs) {
            String v = req.getHeader(h);
            if (v != null && !v.isEmpty()) return v.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }

    /** ==============================
     *  INVENTORY PDF EXPORT
     *  ============================== */
    private void exportInventoryPdf(HttpServletResponse resp, String qParam, String sortParam) throws IOException {
        try {
            List<Item> items = dao.listInventoryForReport(qParam, sortParam, 2000);

            resp.setContentType("application/pdf");
            resp.setHeader("Content-Disposition", "attachment; filename=\"inventory-list.pdf\"");

            com.lowagie.text.Document doc = new com.lowagie.text.Document(com.lowagie.text.PageSize.A4, 36, 36, 36, 36);
            com.lowagie.text.pdf.PdfWriter.getInstance(doc, resp.getOutputStream());
            doc.open();

            com.lowagie.text.Font titleF = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 14, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font headF  = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 10, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font cellF  = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 9,  com.lowagie.text.Font.NORMAL);
            com.lowagie.text.Font exF    = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 10, com.lowagie.text.Font.BOLD);

            doc.add(new com.lowagie.text.Paragraph("Indent Inventory Items\n\n", titleF));

            com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(4);
            table.setWidthPercentage(100f);
            table.setWidths(new float[]{2.2f, 5.3f, 1.2f, 0.8f});

            addHeader(table, headF, "CODE", "ITEM", "QUANTITY", "STATUS");

            for (Item it : items) {
                addCell(table, cellF, nullSafe(it.getCodeBarcode()), com.lowagie.text.Element.ALIGN_LEFT);
                addCell(table, cellF, nullSafe(it.getItemName()), com.lowagie.text.Element.ALIGN_LEFT);
                addCell(table, cellF, String.valueOf(it.getQuantity()), com.lowagie.text.Element.ALIGN_CENTER);

                int qty = it.getQuantity();
                int crit = it.getCriticalMinQuantity();
                int minq = it.getMinQuantity();

                if (crit > 0 && qty <= crit) {
                    java.awt.Color red = new java.awt.Color(220,30,30);
                    com.lowagie.text.pdf.PdfPCell statusCell = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase("!", exF));
                    statusCell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
                    statusCell.setVerticalAlignment(com.lowagie.text.Element.ALIGN_MIDDLE);
                    statusCell.setBackgroundColor(red);
                    table.addCell(statusCell);
                } else if (minq > 0 && qty <= minq) {
                    java.awt.Color yellow = new java.awt.Color(250,200,30);
                    com.lowagie.text.pdf.PdfPCell statusCell = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase("!", exF));
                    statusCell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
                    statusCell.setVerticalAlignment(com.lowagie.text.Element.ALIGN_MIDDLE);
                    statusCell.setBackgroundColor(yellow);
                    table.addCell(statusCell);
                } else {
                    com.lowagie.text.pdf.PdfPCell emptyCell = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase("", cellF));
                    emptyCell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
                    table.addCell(emptyCell);
                }
            }

            doc.add(table);
            doc.close();
        } catch (Exception e) {
            resp.reset();
            resp.setContentType("text/plain; charset=UTF-8");
            resp.getWriter().println("Inventory PDF generation failed: " + e.getMessage());
        }
    }

    private String nullSafe(String s) {
        return (s == null) ? "" : s.trim();
    }

    private void addHeader(com.lowagie.text.pdf.PdfPTable t, com.lowagie.text.Font f, String... labels) {
        for (String s : labels) {
            com.lowagie.text.pdf.PdfPCell c = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(s, f));
            c.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            c.setGrayFill(0.9f);
            t.addCell(c);
        }
    }

    private void addCell(com.lowagie.text.pdf.PdfPTable t, com.lowagie.text.Font f, String val, int align) {
        com.lowagie.text.pdf.PdfPCell c = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(val, f));
        c.setHorizontalAlignment(align);
        t.addCell(c);
    }

    /** Export landscape A4 PDF using OpenPDF WITH a bottom TOTAL row */
    private void exportPdf(HttpServletResponse resp, int itemId, int year) throws IOException {
        List<ReportRow> rows = dao.getUsageByDepartment(itemId, year);
        String itemName = dao.getItemName(itemId);

        // compute totals for PDF
        int[] colTotals = new int[12];
        int grandTotal = 0;
        for (ReportRow r : rows) {
            int[] m = r.getMonths();
            for (int i=0;i<12;i++) { colTotals[i] += m[i]; grandTotal += m[i]; }
        }

        resp.setContentType("application/pdf");
        resp.setHeader("Content-Disposition", "attachment; filename=\"Report_" + year + "_" + itemId + ".pdf\"");

        try {
            com.lowagie.text.Document doc = new com.lowagie.text.Document(com.lowagie.text.PageSize.A4.rotate(), 26, 26, 26, 26);
            com.lowagie.text.pdf.PdfWriter.getInstance(doc, resp.getOutputStream());
            doc.open();

            com.lowagie.text.Font titleF = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 14, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font headF  = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 10, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font cellF  = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 9,  com.lowagie.text.Font.NORMAL);
            com.lowagie.text.Font cellFB = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 9,  com.lowagie.text.Font.BOLD);

            doc.add(new com.lowagie.text.Paragraph("Indent Inventory – Reports & Analytics", titleF));
            doc.add(new com.lowagie.text.Paragraph("Item: " + itemName + "    Year: " + year + "\n\n", cellF));

            com.lowagie.text.pdf.PdfPTable tbl = new com.lowagie.text.pdf.PdfPTable(14);
            tbl.setWidthPercentage(100);
            tbl.setWidths(new float[]{ 3.6f, 1.1f,1.1f,1.1f,1.1f,1.1f,1.1f,1.1f,1.1f,1.1f,1.1f,1.1f,1.1f,1.2f });

            addHeader(tbl, headF, "DEPARTMENT","JAN","FEB","MAR","APR","MAY","JUN","JUL","AUG","SEP","OCT","NOV","DEC","TOTAL");

            for (ReportRow r : rows) {
                addCell(tbl, cellFB, r.getDepartmentName(), com.lowagie.text.Element.ALIGN_LEFT);
                int[] m = r.getMonths();
                for (int i=0;i<12;i++) {
                    addCell(tbl, cellF, m[i]==0 ? "" : String.valueOf(m[i]), com.lowagie.text.Element.ALIGN_CENTER);
                }
                addCell(tbl, cellF, String.valueOf(r.getTotal()), com.lowagie.text.Element.ALIGN_RIGHT);
            }

            // === Footer TOTAL row ===
            com.lowagie.text.pdf.PdfPCell totLabel = new com.lowagie.text.pdf.PdfPCell(
                    new com.lowagie.text.Phrase("TOTAL", headF));
            totLabel.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_LEFT);
            totLabel.setGrayFill(0.92f);
            tbl.addCell(totLabel);

            for (int i=0;i<12;i++) {
                String v = (colTotals[i]==0 ? "" : String.valueOf(colTotals[i]));
                com.lowagie.text.pdf.PdfPCell c = new com.lowagie.text.pdf.PdfPCell(
                        new com.lowagie.text.Phrase(v, headF));
                c.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
                c.setGrayFill(0.92f);
                tbl.addCell(c);
            }

            com.lowagie.text.pdf.PdfPCell g = new com.lowagie.text.pdf.PdfPCell(
                    new com.lowagie.text.Phrase(String.valueOf(grandTotal), cellFB));
            g.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_RIGHT);
            g.setGrayFill(0.88f);
            tbl.addCell(g);

            doc.add(tbl);
            doc.close();
        } catch (Exception e) {
            resp.reset();
            resp.setContentType("text/plain; charset=UTF-8");
            resp.getWriter().println("PDF generation failed: " + e.getMessage());
        }
    }

    /** Export CSV for Excel (same layout: dept vs months + total). */
    private void exportExcel(HttpServletResponse resp, int itemId, int year) throws IOException {
        List<ReportRow> rows = dao.getUsageByDepartment(itemId, year);
        String itemName = dao.getItemName(itemId);

        int[] colTotals = new int[12];
        int grandTotal = 0;
        for (ReportRow r : rows) {
            int[] m = r.getMonths();
            for (int i = 0; i < 12; i++) {
                colTotals[i] += m[i];
                grandTotal += m[i];
            }
        }

        resp.setContentType("text/csv; charset=UTF-8");
        resp.setHeader("Content-Disposition", "attachment; filename=\"Report_" + year + "_" + itemId + ".csv\"");

        PrintWriter out = resp.getWriter();

        // UTF-8 BOM so Excel reads encoding correctly
        out.write('\uFEFF');

        // Header info (optional)
        out.println("Item," + csv(itemName));
        out.println("Year," + year);
        out.println();

        // Table header
        out.println("DEPARTMENT,JAN,FEB,MAR,APR,MAY,JUN,JUL,AUG,SEP,OCT,NOV,DEC,TOTAL");

        // Rows
        for (ReportRow r : rows) {
            StringBuilder sb = new StringBuilder();
            sb.append(csv(r.getDepartmentName()));
            int[] m = r.getMonths();
            for (int i = 0; i < 12; i++) {
                sb.append(',').append(m[i] == 0 ? "" : m[i]);
            }
            sb.append(',').append(r.getTotal());
            out.println(sb.toString());
        }

        // Totals row
        StringBuilder tot = new StringBuilder();
        tot.append("TOTAL");
        for (int i = 0; i < 12; i++) {
            tot.append(',').append(colTotals[i] == 0 ? "" : colTotals[i]);
        }
        tot.append(',').append(grandTotal);
        out.println(tot.toString());

        out.flush();
    }

    private String csv(String v) {
        if (v == null) return "";
        String s = v.trim().replace("\"", "\"\"");
        if (s.indexOf(',') >= 0 || s.indexOf('"') >= 0 || s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0) {
            return "\"" + s + "\"";
        }
        return s;
    }
}
