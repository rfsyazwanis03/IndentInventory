package com.iim.scheduler;

import com.iim.dao.AlertDAO;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.util.concurrent.*;

@WebListener
public class AlertReminderScheduler implements ServletContextListener {
    private ScheduledExecutorService exec;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        exec = Executors.newSingleThreadScheduledExecutor();
        AlertDAO dao = new AlertDAO();
        Runnable task = () -> { try { dao.resendDueReminders(); } catch (Exception ignored) {} };
        exec.scheduleAtFixedRate(task, 5, 30, TimeUnit.SECONDS);
    }
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (exec != null) exec.shutdownNow();
    }
}
