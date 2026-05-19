package com.tickhelp.automation.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigManager {
    private static final Properties props = new Properties();

    static {
        try (InputStream in = ConfigManager.class.getClassLoader()
                .getResourceAsStream("config/config.properties")) {
            if (in == null) throw new RuntimeException("config/config.properties not found on classpath");
            props.load(in);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config.properties", e);
        }
    }

    public static String getBaseUrl()             { return props.getProperty("base.url"); }
    public static String getAdminEmail()          { return props.getProperty("admin.email"); }
    public static String getAdminPassword()       { return props.getProperty("admin.password"); }
    public static String getBrowser()             { return props.getProperty("browser", "chrome"); }
    public static boolean isHeadless()            { return Boolean.parseBoolean(props.getProperty("headless", "false")); }
    public static int getImplicitWait()           { return Integer.parseInt(props.getProperty("implicit.wait", "10")); }
    public static int getExplicitWait()           { return Integer.parseInt(props.getProperty("explicit.wait", "30")); }
    public static int getPageLoadTimeout()        { return Integer.parseInt(props.getProperty("page.load.timeout", "30")); }
    public static boolean isScreenshotOnFailure() { return Boolean.parseBoolean(props.getProperty("screenshot.on.failure", "true")); }
    public static String getScreenshotDir()       { return props.getProperty("screenshot.dir", "screenshots"); }
}
