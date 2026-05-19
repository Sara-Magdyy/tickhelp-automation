package com.tickhelp.automation.pages.assets;

import com.tickhelp.automation.base.BasePage;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.util.List;
import java.util.stream.Collectors;

public class AssetsSearchPage extends BasePage {

    // Screenshot confirms: search input has placeholder "Search"; submit is "Search assets"
    private final By byNameField    = By.cssSelector("input[placeholder='Search'], input[name='Search'], input[name='Name'], input[id='Name']");
    private final By byTypeFilter   = By.cssSelector("select[name='TypeId'], select[id='TypeId'], select[name='Type']");
    private final By bySearchBtn    = By.xpath("//button[contains(.,'Search assets')] | //input[@value='Search assets'] | //button[@type='submit'] | //input[@type='submit']");
    private final By byResultRows   = By.cssSelector("#tblTickets tbody tr, table tbody tr");
    private final By byNoResults    = By.xpath(
            "//*[contains(.,'No assets') or contains(.,'no results') or contains(.,'0 results') or contains(.,'Nothing found') or contains(.,'no matches')]");

    public AssetsSearchPage(WebDriver driver) {
        super(driver);
    }

    @Step("Enter search name: {name}")
    public AssetsSearchPage enterName(String name) {
        if (isPresent(byNameField)) sendKeys(byNameField, name);
        return this;
    }

    @Step("Select type filter: {type}")
    public AssetsSearchPage selectType(String type) {
        if (isPresent(byTypeFilter)) {
            try { selectByVisibleText(byTypeFilter, type); } catch (Exception ignored) {}
        }
        return this;
    }

    @Step("Submit search")
    public AssetsSearchPage search() {
        click(bySearchBtn);
        return this;
    }

    public boolean hasResults() {
        return !findAll(byResultRows).isEmpty();
    }

    public boolean isNoResultsDisplayed() {
        // Accept either an explicit "no results" message OR simply 0 result rows
        return isPresent(byNoResults) || findAll(byResultRows).isEmpty();
    }

    public boolean isSearchPageDisplayed() {
        // Search page is present when at least the search field or submit button is visible
        return isPresent(byNameField) || isPresent(bySearchBtn);
    }

    public List<String> getResultNames() {
        return findAll(byResultRows).stream()
                .map(r -> r.findElements(By.tagName("td")))
                .filter(cells -> !cells.isEmpty())
                .map(cells -> cells.get(0).getText().trim())
                .collect(Collectors.toList());
    }

}
