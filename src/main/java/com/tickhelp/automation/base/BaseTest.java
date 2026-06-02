package com.tickhelp.automation.base;

import com.tickhelp.automation.core.ConfigManager;
import com.tickhelp.automation.core.DriverManager;
import io.qameta.allure.Attachment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.testng.ITestResult;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public abstract class BaseTest {
    protected static final Logger log = LogManager.getLogger(BaseTest.class);

    @BeforeClass(alwaysRun = true)
    public void setUp() {
        try {
            DriverManager.initDriver();
        } catch (Exception e) {
            log.warn("Chrome start failed ({}), retrying once", e.getMessage());
            DriverManager.quitDriver();
            DriverManager.initDriver();
        }
        DriverManager.getDriver().get(ConfigManager.getBaseUrl() + "/User/Login");
    }

    protected boolean isDriverAlive() {
        return DriverManager.isAlive();
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() {
        DriverManager.quitDriver();
    }

    @AfterMethod(alwaysRun = true)
    public void captureOnFailure(ITestResult result) {
        String status = switch (result.getStatus()) {
            case ITestResult.SUCCESS -> "PASSED";
            case ITestResult.FAILURE -> "FAILED";
            case ITestResult.SKIP    -> "SKIPPED";
            default                  -> "UNKNOWN";
        };
        if (result.getStatus() == ITestResult.FAILURE) {
            log.error("TEST FAILED: {} — capturing screenshot", result.getName());
            takeScreenshot(result.getName());
        }
        log.info("TEST: {} | Status: {}", result.getName(), status);
    }

    private void takeScreenshot(String testName) {
        try {
            var driver = DriverManager.getDriver();
            if (driver == null) return;
            byte[] png = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            String dir = ConfigManager.getScreenshotDir();
            Files.createDirectories(Paths.get(dir));
            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            Files.write(Paths.get(dir, testName + "_" + ts + ".png"), png);
            attachScreenshot(png);
        } catch (Exception e) {
            log.warn("Screenshot failed: {}", e.getMessage());
        }
    }

    @Attachment(value = "Screenshot", type = "image/png")
    private byte[] attachScreenshot(byte[] png) {
        return png;
    }
}
