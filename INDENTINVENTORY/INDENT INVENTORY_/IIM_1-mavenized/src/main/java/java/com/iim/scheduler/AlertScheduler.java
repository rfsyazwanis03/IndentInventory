package com.iim.scheduler;

import com.iim.dao.AlertDAO;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.util.concurrent.*;


@WebListener
public class AlertScheduler implements ServletContextListener {

    private ScheduledExecutorService exec;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        exec = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "alert-scheduler");
            t.setDaemon(true);
            return t;
        });

        AlertDAO dao = new AlertDAO();

        Runnable task = () -> {
            try {
                System.out.println("[AlertScheduler] ===== Reminder cycle started =====");
                dao.generateLowStockAlerts();
                dao.resendDueReminders();
                System.out.println("[AlertScheduler] ===== Reminder cycle completed =====\n");
            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        // Start immediately (0s) then repeat every 60s
        exec.scheduleAtFixedRate(task, 0, 60, TimeUnit.SECONDS);
        System.out.println("[AlertScheduler] Started — runs every 1 minute.");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (exec != null) exec.shutdownNow();
    }
}
