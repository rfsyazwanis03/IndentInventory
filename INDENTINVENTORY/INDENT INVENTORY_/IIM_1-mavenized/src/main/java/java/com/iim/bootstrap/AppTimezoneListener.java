package com.iim.bootstrap;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.util.TimeZone;

@WebListener
public class AppTimezoneListener implements ServletContextListener {
  @Override public void contextInitialized(ServletContextEvent sce) {
    // Paksa JVM pakai MYT (UTC+08)
    TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kuala_Lumpur"));
  }
  @Override public void contextDestroyed(ServletContextEvent sce) {}
}
