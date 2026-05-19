package com.tickhelp.automation.pages.assets;

import com.tickhelp.automation.base.BasePage;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class AssetDetailPage extends BasePage {

    // The retire/disable button text varies: "Retire", "Disable", "Mark as retired"
    private final By byRetireBtn = By.xpath(
            "//a[contains(.,'Retire') or contains(.,'Disable') or contains(.,'retire')] | " +
            "//button[contains(.,'Retire') or contains(.,'Disable')] | " +
            "//input[@value='Retire' or @value='Disable']");

    // Confirmation dialog / link that appears after clicking retire
    private final By byConfirmBtn = By.xpath(
            "//a[contains(.,'Confirm') or contains(.,'Yes') or contains(.,'OK')] | " +
            "//button[contains(.,'Confirm') or contains(.,'Yes') or contains(.,'OK')] | " +
            "//input[@value='Confirm' or @value='Yes' or @value='OK']");

    public AssetDetailPage(WebDriver driver) {
        super(driver);
    }

    @Step("Retire / disable this asset")
    public void retireAsset() {
        waitForClickable(byRetireBtn);
        click(byRetireBtn);
        // Accept browser confirm dialog if present
        try {
            driver.switchTo().alert().accept();
        } catch (Exception ignored) {}
        // Click in-page confirmation if present
        try {
            if (isPresent(byConfirmBtn)) click(byConfirmBtn);
        } catch (Exception ignored) {}
    }

    public boolean isDetailPageDisplayed() {
        return isPresent(byRetireBtn);
    }
}
