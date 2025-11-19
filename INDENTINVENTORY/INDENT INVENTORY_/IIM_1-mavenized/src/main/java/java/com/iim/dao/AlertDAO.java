package com.iim.dao;

import com.iim.db.DBConnection;
import com.iim.models.Alert;

import java.sql.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;

public class AlertDAO {

    // Gunakan Asia/Kuala_Lumpur (MYT)
    private static final Calendar KL_CAL
            = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kuala_Lumpur"));

    // Reminder interval (minutes) — set to 24*60 for production; change for testing if needed
    private static final int REMIND_INTERVAL_MINUTES = 24 * 60;

    private Alert map(ResultSet rs) throws SQLException {
        Alert a = new Alert();
        a.setId(rs.getInt("id"));
        a.setItemId(rs.getInt("item_id"));
        a.setTitle(rs.getString("title"));
        a.setDetail(rs.getString("detail"));
        a.setRead(rs.getBoolean("is_read"));
        a.setDeleted(rs.getBoolean("deleted"));
        a.setSeverity(rs.getString("severity"));

        Timestamp c = rs.getTimestamp("created_at", KL_CAL);
        a.setCreatedAt(c == null ? null : new java.util.Date(c.getTime()));
        Timestamp r = rs.getTimestamp("remind_at", KL_CAL);
        a.setRemindAt(r == null ? null : new java.util.Date(r.getTime()));
        return a;
    }

    /** ID tertinggi untuk alert yang visible (tidak deleted & bukan pending remind_at). */
    public Integer latestVisibleId() {
        String sql = """
            SELECT MAX(id)
            FROM alerts
            WHERE deleted = 0
              AND (remind_at IS NULL OR remind_at <= NOW())
        """;
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                int v = rs.getInt(1);
                return rs.wasNull() ? null : v;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Cipta alert low-stock untuk semua item:
     * - Satu alert serta-merta (remind_at = NULL)
     * - Satu reminder tertunda (is_read=1, remind_at = NOW() + INTERVAL)
     *
     * Logik NOT EXISTS disusun supaya:
     * - tidak duplikasi jika visible alert sedia ada
     * - tidak buat reminder jika reminder telah ada dalam 1 hari akan datang
     */
    public int generateLowStockAlerts() {
        String sql = """
            INSERT INTO alerts (item_id, title, detail, is_read, severity, remind_at)
            /* ALERT SERTA-MERTA (visible) */
            SELECT  i.id,
                    CASE 
                      WHEN (i.critical_min_quantity IS NOT NULL AND i.critical_min_quantity > 0 
                            AND inv.quantity <= i.critical_min_quantity)
                        THEN CONCAT('CRITICAL STOCK: "', i.item_name, '"')
                      ELSE CONCAT('LOW STOCK: "', i.item_name, '"')
                    END AS title,
                    CASE 
                      WHEN (i.critical_min_quantity IS NOT NULL AND i.critical_min_quantity > 0 
                            AND inv.quantity <= i.critical_min_quantity)
                        THEN CONCAT('Item \"', i.item_name, '\" (Barcode: ', COALESCE(i.code_barcode,'-'),
                                    ') has reached the CRITICAL threshold. Current qty: ', inv.quantity,
                                    ', Critical min: ', i.critical_min_quantity, 
                                    '. Immediate restock is required.')
                      ELSE CONCAT('Item \"', i.item_name, '\" (Barcode: ', COALESCE(i.code_barcode,'-'),
                                    ') is below the minimum level. Current qty: ', inv.quantity,
                                    ', Min qty: ', i.min_quantity, 
                                    '. Please restock via Indent.')
                    END AS detail,
                    0 AS is_read,
                    CASE 
                      WHEN (i.critical_min_quantity IS NOT NULL AND i.critical_min_quantity > 0 
                            AND inv.quantity <= i.critical_min_quantity)
                        THEN 'critical'
                      ELSE 'normal'
                    END AS severity,
                    NULL AS remind_at
            FROM items i
            JOIN inventory inv ON inv.item_id = i.id
            WHERE i.status = 'active'
              AND (
                    inv.quantity <= i.min_quantity
                    OR (i.critical_min_quantity IS NOT NULL AND i.critical_min_quantity > 0
                        AND inv.quantity <= i.critical_min_quantity)
                  )
              /* pastikan tiada visible alert sedia ada untuk item ini */
              AND NOT EXISTS (
                    SELECT 1 FROM alerts a
                    WHERE a.item_id = i.id
                      AND a.deleted = 0
                      AND (a.remind_at IS NULL OR a.remind_at <= NOW())
                  )

            UNION ALL

            /* REMINDER PERTAMA (DIPENDAM SAMPAI DUE) — untuk kedua2 normal & critical */
            SELECT  i.id,
                    CASE 
                      WHEN (i.critical_min_quantity IS NOT NULL AND i.critical_min_quantity > 0 
                            AND inv.quantity <= i.critical_min_quantity)
                        THEN CONCAT('CRITICAL STOCK: "', i.item_name, '"')
                      ELSE CONCAT('LOW STOCK: "', i.item_name, '"')
                    END AS title,
                    CASE 
                      WHEN (i.critical_min_quantity IS NOT NULL AND i.critical_min_quantity > 0 
                            AND inv.quantity <= i.critical_min_quantity)
                        THEN CONCAT('Item \"', i.item_name, '\" (Barcode: ', COALESCE(i.code_barcode,'-'),
                                    ') has reached the CRITICAL threshold. Current qty: ', inv.quantity,
                                    ', Critical min: ', i.critical_min_quantity, 
                                    '. Immediate restock is required.')
                      ELSE CONCAT('Item \"', i.item_name, '\" (Barcode: ', COALESCE(i.code_barcode,'-'),
                                    ') is below the minimum level. Current qty: ', inv.quantity,
                                    ', Min qty: ', i.min_quantity, 
                                    '. Please restock via Indent.')
                    END AS detail,
                    1 AS is_read,
                    CASE 
                      WHEN (i.critical_min_quantity IS NOT NULL AND i.critical_min_quantity > 0 
                            AND inv.quantity <= i.critical_min_quantity)
                        THEN 'critical'
                      ELSE 'normal'
                    END AS severity,
                    DATE_ADD(NOW(), INTERVAL %d MINUTE) AS remind_at
            FROM items i
            JOIN inventory inv ON inv.item_id = i.id
            WHERE i.status = 'active'
              AND (
                    inv.quantity <= i.min_quantity
                    OR (i.critical_min_quantity IS NOT NULL AND i.critical_min_quantity > 0
                        AND inv.quantity <= i.critical_min_quantity)
                  )
              /* hanya buat pending reminder jika tiada remind yang akan datang */
              AND NOT EXISTS (
                    SELECT 1 FROM alerts a
                    WHERE a.item_id = i.id
                      AND a.deleted = 0
                      AND a.remind_at IS NOT NULL
                      AND a.remind_at > NOW()
                  )
            """.formatted(REMIND_INTERVAL_MINUTES);

        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            int affected = ps.executeUpdate();
            System.out.println("generateLowStockAlerts: inserted rows = " + affected);
            return affected;
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    /** Versi single item. (Tidak diubah kecuali perbaikan parity dengan versi total) */
    public int generateLowStockAlertForItem(int itemId) {
        String sql = """
            INSERT INTO alerts (item_id, title, detail, is_read, severity, remind_at)
            /* ALERT SERTA-MERTA */
            SELECT  i.id,
                    CASE 
                      WHEN (i.critical_min_quantity IS NOT NULL AND i.critical_min_quantity > 0 
                            AND inv.quantity <= i.critical_min_quantity)
                        THEN CONCAT('CRITICAL STOCK: "', i.item_name, '"')
                      ELSE CONCAT('LOW STOCK: "', i.item_name, '"')
                    END AS title,
                    CASE 
                      WHEN (i.critical_min_quantity IS NOT NULL AND i.critical_min_quantity > 0 
                            AND inv.quantity <= i.critical_min_quantity)
                        THEN CONCAT('Item \"', i.item_name, '\" (Barcode: ', COALESCE(i.code_barcode,'-'),
                                    ') has reached the CRITICAL threshold. Current qty: ', inv.quantity,
                                    ', Critical min: ', i.critical_min_quantity, 
                                    '. Immediate restock is required.')
                      ELSE CONCAT('Item \"', i.item_name, '\" (Barcode: ', COALESCE(i.code_barcode,'-'),
                                    ') is below the minimum level. Current qty: ', inv.quantity,
                                    ', Min qty: ', i.min_quantity, 
                                    '. Please restock via Indent.')
                    END AS detail,
                    0 AS is_read,
                    CASE 
                      WHEN (i.critical_min_quantity IS NOT NULL AND i.critical_min_quantity > 0 
                            AND inv.quantity <= i.critical_min_quantity)
                        THEN 'critical'
                      ELSE 'normal'
                    END AS severity,
                    NULL AS remind_at
            FROM items i
            JOIN inventory inv ON inv.item_id = i.id
            WHERE i.id = ?
              AND i.status='active'
              AND (
                    inv.quantity <= i.min_quantity
                    OR (i.critical_min_quantity IS NOT NULL AND i.critical_min_quantity > 0
                        AND inv.quantity <= i.critical_min_quantity)
                  )
              /* pastikan tiada visible alert sedia ada untuk item ini */
              AND NOT EXISTS (
                    SELECT 1 FROM alerts a
                    WHERE a.item_id = i.id
                      AND a.deleted = 0
                      AND (a.remind_at IS NULL OR a.remind_at <= NOW())
                  )

            UNION ALL

            /* REMINDER PERTAMA (DIPENDAM) */
            SELECT  i.id,
                    CASE 
                      WHEN (i.critical_min_quantity IS NOT NULL AND i.critical_min_quantity > 0 
                            AND inv.quantity <= i.critical_min_quantity)
                        THEN CONCAT('CRITICAL STOCK: "', i.item_name, '"')
                      ELSE CONCAT('LOW STOCK: "', i.item_name, '"')
                    END AS title,
                    CASE 
                      WHEN (i.critical_min_quantity IS NOT NULL AND i.critical_min_quantity > 0 
                            AND inv.quantity <= i.critical_min_quantity)
                        THEN CONCAT('Item \"', i.item_name, '\" (Barcode: ', COALESCE(i.code_barcode,'-'),
                                    ') has reached the CRITICAL threshold. Current qty: ', inv.quantity,
                                    ', Critical min: ', i.critical_min_quantity, 
                                    '. Immediate restock is required.')
                      ELSE CONCAT('Item \"', i.item_name, '\" (Barcode: ', COALESCE(i.code_barcode,'-'),
                                    ') is below the minimum level. Current qty: ', inv.quantity,
                                    ', Min qty: ', i.min_quantity, 
                                    '. Please restock via Indent.')
                    END AS detail,
                    1 AS is_read,
                    CASE 
                      WHEN (i.critical_min_quantity IS NOT NULL AND i.critical_min_quantity > 0 
                            AND inv.quantity <= i.critical_min_quantity)
                        THEN 'critical'
                      ELSE 'normal'
                    END AS severity,
                    DATE_ADD(NOW(), INTERVAL %d MINUTE) AS remind_at
            FROM items i
            JOIN inventory inv ON inv.item_id = i.id
            WHERE i.id = ?
              AND i.status='active'
              AND (
                    inv.quantity <= i.min_quantity
                    OR (i.critical_min_quantity IS NOT NULL AND i.critical_min_quantity > 0
                        AND inv.quantity <= i.critical_min_quantity)
                  )
              AND NOT EXISTS (
                    SELECT 1 FROM alerts a
                    WHERE a.item_id = i.id
                      AND a.deleted = 0
                      AND a.remind_at IS NOT NULL
                      AND a.remind_at > NOW()
                  )
            """.formatted(REMIND_INTERVAL_MINUTES);

        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            ps.setInt(2, itemId);
            int affected = ps.executeUpdate();
            System.out.println("generateLowStockAlertForItem(" + itemId + "): inserted rows = " + affected);
            return affected;
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Sync alerts' severity & title for a single item based on current inventory.
     * Panggil setiap kali inventory.quantity atau items.critical_min_quantity/min_quantity berubah.
     * Returns number of alert rows updated.
     */
    public int syncAlertsForItem(int itemId) {
        String sql = """
            UPDATE alerts a
            JOIN items i ON i.id = a.item_id
            JOIN inventory inv ON inv.item_id = i.id
            SET
              a.severity = CASE
                WHEN (i.critical_min_quantity IS NOT NULL AND i.critical_min_quantity > 0
                      AND inv.quantity <= i.critical_min_quantity) THEN 'critical'
                WHEN (inv.quantity <= i.min_quantity) THEN 'normal'
                ELSE a.severity
              END,
              a.title = CASE
                WHEN a.title LIKE 'REMINDER! %%' THEN CONCAT('REMINDER! ',
                     CASE
                       WHEN (i.critical_min_quantity IS NOT NULL AND i.critical_min_quantity > 0
                             AND inv.quantity <= i.critical_min_quantity)
                         THEN REPLACE(SUBSTRING(a.title, 11), 'LOW STOCK:', 'CRITICAL STOCK:')
                       ELSE REPLACE(SUBSTRING(a.title, 11), 'CRITICAL STOCK:', 'LOW STOCK:')
                     END)
                ELSE
                     CASE
                       WHEN (i.critical_min_quantity IS NOT NULL AND i.critical_min_quantity > 0
                             AND inv.quantity <= i.critical_min_quantity)
                         THEN REPLACE(a.title, 'LOW STOCK:', 'CRITICAL STOCK:')
                       ELSE REPLACE(a.title, 'CRITICAL STOCK:', 'LOW STOCK:')
                     END
              END
            WHERE a.item_id = ?
              AND a.deleted = 0
        """;

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            int updated = ps.executeUpdate();
            System.out.println("syncAlertsForItem(" + itemId + "): updated " + updated + " rows");
            return updated;
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public List<Alert> list(String sort, String readFilter, int limit, int offset) {
        String orderBy = switch (sort == null ? "" : sort) {
            case "oldest" -> "a.created_at ASC";
            case "title"  -> "a.title ASC, a.created_at DESC";
            default       -> "a.created_at DESC";
        };
        String filter = switch (readFilter == null ? "" : readFilter) {
            case "read"   -> "AND a.is_read = 1";
            case "unread" -> "AND a.is_read = 0";
            default       -> "";
        };

        String sql = """
            SELECT a.id, a.item_id, a.title, a.detail, a.is_read, a.deleted, a.severity, a.created_at, a.remind_at
            FROM alerts a
            WHERE a.deleted = 0
              AND (a.remind_at IS NULL OR a.remind_at <= NOW())
            """ + filter + " ORDER BY " + orderBy + " LIMIT ? OFFSET ?";

        List<Alert> out = new ArrayList<>();
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, Math.max(1, limit));
            ps.setInt(2, Math.max(0, offset));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return out;
    }

    public int count(String readFilter) {
        String filter = switch (readFilter == null ? "" : readFilter) {
            case "read"   -> "AND is_read = 1";
            case "unread" -> "AND is_read = 0";
            default       -> "";
        };
        String sql = """
            SELECT COUNT(*)
            FROM alerts
            WHERE deleted = 0
              AND (remind_at IS NULL OR remind_at <= NOW())
            """ + filter;
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int unreadCount() { return count("unread"); }

    public Alert findById(int id) {
        String sql = """
            SELECT id, item_id, title, detail, is_read, deleted, severity, created_at, remind_at
            FROM alerts
            WHERE id = ? AND deleted = 0
              AND (remind_at IS NULL OR remind_at <= NOW())
            """;
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return map(rs); }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public void markRead(List<Integer> ids, boolean read) {
        if (ids == null || ids.isEmpty()) return;
        String placeholders = ids.stream().map(x -> "?").collect(Collectors.joining(","));
        String sql = "UPDATE alerts SET is_read=? WHERE id IN (" + placeholders + ")";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBoolean(1, read);
            int idx = 2;
            for (Integer id : ids) ps.setInt(idx++, id);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void markSingleRead(int id, boolean read) {
        String sql = "UPDATE alerts SET is_read=? WHERE id=? AND deleted=0";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBoolean(1, read);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void delete(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return;
        String placeholders = ids.stream().map(x -> "?").collect(Collectors.joining(","));
        String sql = "UPDATE alerts SET deleted=1 WHERE id IN (" + placeholders + ")";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            int idx = 1;
            for (Integer id : ids) ps.setInt(idx++, id);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    /** Manual snooze (MYT) — kekalkan created_at asal. */
    public int scheduleDuplicateReminder(int originalId, Timestamp remindAtMyt) {
        String sql = """
            INSERT INTO alerts (item_id, title, detail, is_read, deleted, severity, remind_at, created_at)
            SELECT item_id, title, detail, 1, 0, severity, ?, created_at
            FROM alerts WHERE id = ? AND deleted = 0
        """;
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setTimestamp(1, remindAtMyt, KL_CAL);
            ps.setInt(2, originalId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * TERAS REMINDER:
     * - Publish semua pending reminder yang DUE → jadikan visible/unread (remind_at=NULL, prefix "REMINDER!")
     * - Queue next reminder (DATE_ADD NOW + INTERVAL REMIND_INTERVAL_MINUTES)
     * - Jika stok pulih → hentikan kitaran (clear remind_at)
     *
     * Nota: sqlQueueNext akan enqueue next reminder untuk setiap REMINDER! yang baru saja dipublish
     * asalkan item masih di bawah ambang dan tiada pending remind yang akan datang.
     */
    public int resendDueReminders() {
        int totalPublished = 0;

        String sqlPublishDue = """
            UPDATE alerts a
            JOIN items i   ON i.id = a.item_id
            JOIN inventory inv ON inv.item_id = i.id
            SET a.is_read = 0,
                a.created_at = NOW(),
                a.remind_at = NULL,
                a.title = CASE WHEN a.title LIKE 'REMINDER! %%'
                               THEN a.title
                               ELSE CONCAT('REMINDER! ', a.title)
                          END,
                /* regenerate severity from current inventory */
                a.severity = CASE
                               WHEN (i.critical_min_quantity IS NOT NULL AND i.critical_min_quantity > 0
                                     AND inv.quantity <= i.critical_min_quantity) THEN 'critical'
                               WHEN (inv.quantity <= i.min_quantity) THEN 'normal'
                               ELSE a.severity
                             END,
                /* regenerate detail from current inventory so reminder shows live qty */
                a.detail = CASE
                             WHEN (i.critical_min_quantity IS NOT NULL AND i.critical_min_quantity > 0
                                   AND inv.quantity <= i.critical_min_quantity)
                               THEN CONCAT('Item \"', i.item_name, '\" (Barcode: ', COALESCE(i.code_barcode,'-'),
                                           ') has reached the CRITICAL threshold. Current qty: ', inv.quantity,
                                           ', Critical min: ', i.critical_min_quantity,
                                           '. Immediate restock is required.')
                             WHEN (inv.quantity <= i.min_quantity)
                               THEN CONCAT('Item \"', i.item_name, '\" (Barcode: ', COALESCE(i.code_barcode,'-'),
                                           ') is below the minimum level. Current qty: ', inv.quantity,
                                           ', Min qty: ', i.min_quantity,
                                           '. Please restock via Indent.')
                             ELSE a.detail
                           END
            WHERE a.deleted = 0
              AND a.remind_at IS NOT NULL
              AND a.remind_at <= NOW()
              AND (
                    inv.quantity <= i.min_quantity
                    OR (i.critical_min_quantity IS NOT NULL AND i.critical_min_quantity > 0
                        AND inv.quantity <= i.critical_min_quantity)
                  )
        """;

        String sqlQueueNext = """
            INSERT INTO alerts (item_id, title, detail, is_read, deleted, severity, remind_at, created_at)
            SELECT i.id,
                   /* keep the REMINDER! prefixed title as-is */
                   a.title,
                   /* generate up-to-date detail from inventory (do not reuse a.detail) */
                   CASE
                     WHEN (i.critical_min_quantity IS NOT NULL AND i.critical_min_quantity > 0
                           AND inv.quantity <= i.critical_min_quantity)
                       THEN CONCAT('Item \"', i.item_name, '\" (Barcode: ', COALESCE(i.code_barcode,'-'),
                                   ') has reached the CRITICAL threshold. Current qty: ', inv.quantity,
                                   ', Critical min: ', i.critical_min_quantity,
                                   '. Immediate restock is required.')
                     WHEN (inv.quantity <= i.min_quantity)
                       THEN CONCAT('Item \"', i.item_name, '\" (Barcode: ', COALESCE(i.code_barcode,'-'),
                                   ') is below the minimum level. Current qty: ', inv.quantity,
                                   ', Min qty: ', i.min_quantity,
                                   '. Please restock via Indent.')
                     ELSE a.detail
                   END AS detail,
                   1, 0,
                   /* regenerate severity for the queued reminder based on current inventory */
                   CASE
                     WHEN (i.critical_min_quantity IS NOT NULL AND i.critical_min_quantity > 0
                           AND inv.quantity <= i.critical_min_quantity) THEN 'critical'
                     ELSE 'normal'
                   END AS severity,
                   DATE_ADD(NOW(), INTERVAL %d MINUTE) AS remind_at,
                   a.created_at
            FROM alerts a
            JOIN items i ON i.id = a.item_id
            JOIN inventory inv ON inv.item_id = i.id
            WHERE a.deleted = 0
              AND a.remind_at IS NULL
              AND a.title LIKE 'REMINDER! %%'
              /* ensure we only queue once per item by using the latest REMINDER! row for that item */
              AND a.id = (
                  SELECT MAX(a2.id) FROM alerts a2
                  WHERE a2.item_id = a.item_id
                    AND a2.deleted = 0
                    AND a2.remind_at IS NULL
                    AND a2.title LIKE 'REMINDER! %%'
              )
              /* only queue if the item still below threshold */
              AND (
                    inv.quantity <= i.min_quantity
                    OR (i.critical_min_quantity IS NOT NULL AND i.critical_min_quantity > 0
                        AND inv.quantity <= i.critical_min_quantity)
                  )
              /* avoid creating duplicate pending reminder if one already exists */
              AND NOT EXISTS (
                    SELECT 1 FROM alerts x
                    WHERE x.item_id = a.item_id
                      AND x.remind_at IS NOT NULL
                      AND x.deleted = 0
                      AND x.remind_at > NOW()
              )
        """.formatted(REMIND_INTERVAL_MINUTES);

        String sqlStopIfRecovered = """
            UPDATE alerts a
            JOIN items i   ON i.id = a.item_id
            JOIN inventory inv ON inv.item_id = i.id
            SET a.remind_at = NULL
            WHERE a.deleted = 0
              AND a.remind_at IS NOT NULL
              AND a.remind_at <= NOW()
              AND inv.quantity > i.min_quantity
              AND (i.critical_min_quantity IS NULL OR i.critical_min_quantity = 0 OR inv.quantity > i.critical_min_quantity)
        """;

        try (Connection c = DBConnection.getConnection()) {
            // 1) publish due reminders (make visible)
            try (PreparedStatement ps1 = c.prepareStatement(sqlPublishDue)) {
                totalPublished += ps1.executeUpdate();
            }
            // 2) queue next reminders for published REMINDER! rows (only for items still below threshold)
            try (PreparedStatement ps2 = c.prepareStatement(sqlQueueNext)) {
                int queued = ps2.executeUpdate();
                System.out.println("resendDueReminders: queued next reminders = " + queued);
            }
            // 3) if item recovered, clear any pending remind_at that may be obsolete (safety)
            try (PreparedStatement ps3 = c.prepareStatement(sqlStopIfRecovered)) {
                int cleared = ps3.executeUpdate();
                System.out.println("resendDueReminders: cleared pending reminders for recovered items = " + cleared);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        System.out.println("resendDueReminders: published visible reminders = " + totalPublished);
        return totalPublished;
    }

    /** Untuk dropdown header: semua unread (max 15). Jika tiada, 2 terkini. */
    public List<Alert> headerFeed() {
        List<Alert> unread = latestUnread(15);
        if (!unread.isEmpty()) return unread;
        return latest(2);
    }

    public List<Alert> latest(int limit) {
        String sql = """
            SELECT id, item_id, title, detail, is_read, deleted, severity, created_at, remind_at
            FROM alerts
            WHERE deleted = 0 AND (remind_at IS NULL OR remind_at <= NOW())
            ORDER BY created_at DESC
            LIMIT ?
        """;
        List<Alert> out = new ArrayList<>();
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return out;
    }

    public List<Alert> latestUnread(int limit) {
        String sql = """
            SELECT id, item_id, title, detail, is_read, deleted, severity, created_at, remind_at
            FROM alerts
            WHERE deleted = 0 AND is_read = 0
              AND (remind_at IS NULL OR remind_at <= NOW())
            ORDER BY created_at DESC
            LIMIT ?
        """;
        List<Alert> out = new ArrayList<>();
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return out;
    }
}
