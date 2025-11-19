package com.iim.boot;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.util.TimeZone;

@WebListener
public class AppTimeZoneListener implements ServletContextListener {
  @Override
  public void contextInitialized(ServletContextEvent sce) {
    // Pastikan semua tarikh/masa dalam JVM ikut MYT
    TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kuala_Lumpur"));
    System.setProperty("user.timezone", "Asia/Kuala_Lumpur");
    System.out.println("[TZ] Default JVM timezone set to Asia/Kuala_Lumpur");
  }
  @Override public void contextDestroyed(ServletContextEvent sce) {}
}
