package com.tickhelp.automation.tests.login;

import com.tickhelp.automation.base.BaseTest;
import com.tickhelp.automation.core.ConfigManager;
import com.tickhelp.automation.core.DriverManager;
import com.tickhelp.automation.pages.login.LoginPage;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Feature("Authentication")
public class LoginTest extends BaseTest {

    private static final Logger log = LogManager.getLogger(LoginTest.class);

    @Test(description = "TC-LG-001: Valid admin login redirects to home/dashboard")
    @Description("Login with valid admin credentials and verify successful authentication")
    public void testValidAdminLogin() {
        LoginPage loginPage = new LoginPage(DriverManager.getDriver());
        loginPage.login(ConfigManager.getAdminEmail(), ConfigManager.getAdminPassword());

        String currentUrl = DriverManager.getDriver().getCurrentUrl();
        assertThat(currentUrl)
                .as("Should be redirected away from login page after successful login")
                .doesNotContain("/User/Login");

        log.info("Login successful. Redirected to: {}", currentUrl);
    }
}
