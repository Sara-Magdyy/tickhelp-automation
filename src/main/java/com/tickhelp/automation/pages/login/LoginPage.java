package com.tickhelp.automation.pages.login;

import com.tickhelp.automation.base.BasePage;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class LoginPage extends BasePage {

    // Login page uses placeholder-based inputs (no id="Email" on this server)
    private final By emailField    = By.cssSelector("input[placeholder='Username or Email']");
    private final By passwordField = By.cssSelector("input[type='password']");
    // Server renders <input class="btn-primary" type="submit" value="Login" />
    private final By loginButton   = By.cssSelector("input[type='submit']");

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    @Step("Enter email: {email}")
    public LoginPage enterEmail(String email) {
        sendKeys(emailField, email);
        return this;
    }

    @Step("Enter password")
    public LoginPage enterPassword(String password) {
        sendKeys(passwordField, password);
        return this;
    }

    @Step("Click login button")
    public void clickLogin() {
        click(loginButton);
    }

    @Step("Login with credentials")
    public void login(String email, String password) {
        enterEmail(email);
        enterPassword(password);
        clickLogin();
        // Wait for redirect away from login — ensures session cookie is set
        // before the caller attempts to navigate to a protected page.
        wait.until(d -> !d.getCurrentUrl().contains("Login") && !d.getCurrentUrl().contains("login"));
    }

    public boolean isLoginPageDisplayed() {
        return isDisplayed(loginButton);
    }
}
