package com.tickhelp.automation.pages.assets;

import com.tickhelp.automation.base.BasePage;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class CreateAssetPage extends BasePage {

    // Use name attribute as primary — more stable than id in MVC scaffolded forms
    private final By byNameField   = By.cssSelector(
            "[name='Name'], #Name, [name='name'], [id='name'], " +
            "[placeholder*='name' i]:not([type='search']), " +
            "form input[type='text']:first-of-type");
    private final By byTypeField   = By.cssSelector("[name='TypeId'], #TypeId, [name='Type'], #Type");
    private final By byLocationFld = By.cssSelector("[name='Location'], #Location");
    private final By bySubmitBtn   = By.cssSelector("input[type='submit'], button[type='submit']");

    public CreateAssetPage(WebDriver driver) {
        super(driver);
    }

    @Step("Fill asset name: {name}")
    public CreateAssetPage enterName(String name) {
        sendKeys(byNameField, name);
        return this;
    }

    @Step("Fill asset type: {type}")
    public CreateAssetPage enterType(String type) {
        // Try dropdown first; if not a <select>, treat byTypeField as a text input
        if (isPresent(byTypeField)) {
            try {
                selectByVisibleText(byTypeField, type);
                return this;
            } catch (Exception ignored) {
                // Not a <select> — fall through to sendKeys
            }
            try {
                sendKeys(byTypeField, type);
            } catch (Exception ignored) {}
        }
        return this;
    }

    @Step("Fill location: {location}")
    public CreateAssetPage enterLocation(String location) {
        if (isPresent(byLocationFld)) {
            sendKeys(byLocationFld, location);
        }
        return this;
    }

    @Step("Submit create asset form")
    public void submit() {
        click(bySubmitBtn);
    }

    /**
     * Creates an asset with the given name and type then waits for redirect.
     * Returns the URL of the page landed on (detail or list).
     */
    @Step("Create asset: name={name}, type={type}")
    public String createAsset(String name, String type) {
        // Caller must already be on the create form (reached via "New Asset..." button click)
        waitForVisible(byNameField);
        enterName(name);
        enterType(type);
        String beforeUrl = driver.getCurrentUrl();
        submit();
        // Wait for URL change (redirect to detail/list); ignore timeout for modal-based forms
        try {
            new org.openqa.selenium.support.ui.WebDriverWait(driver,
                    java.time.Duration.ofSeconds(15))
                    .until(d -> !d.getCurrentUrl().equals(beforeUrl));
        } catch (Exception ignored) {}
        return driver.getCurrentUrl();
    }
}
