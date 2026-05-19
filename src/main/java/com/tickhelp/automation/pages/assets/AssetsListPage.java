package com.tickhelp.automation.pages.assets;

import com.tickhelp.automation.base.BasePage;
import com.tickhelp.automation.core.ConfigManager;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.stream.Collectors;

public class AssetsListPage extends BasePage {

    // Always-present elements (visible even with 0 assets)
    // Use placeholder as fallback in case id attribute differs in this build
    private final By byQuickSearch     = By.cssSelector("#quickSearch, input[placeholder='Quick search']");
    private final By byNewAssetBtn     = By.xpath("//a[contains(.,'New Asset')] | //button[contains(.,'New Asset')] | //input[@value[contains(.,'New Asset')]]");
    private final By bySearchBtn       = By.xpath("//a[contains(.,'Search') and not(contains(.,'Quick'))] | //button[contains(.,'Search') and not(contains(.,'Quick'))] | //input[@type='button' and contains(@value,'Search') and not(contains(@value,'Quick'))]");
    // Import (CSV) toggle button — exclude any element inside the upload form so that
    // clicking "close" when the panel is open doesn't accidentally submit the form.
    private final By byImportCsvBtn    = By.xpath(
            "(//a[contains(text(),'Import')] | //button[contains(text(),'Import')] |" +
            "//input[@type='button' and contains(@value,'Import')] |" +
            "//input[@type='submit' and contains(@value,'Import')])" +
            "[not(ancestor::form[.//input[@type='file']])]");
    private final By byShowRetiredLink = By.xpath("//a[contains(.,'Show retired') or contains(.,'retired') or contains(.,'disabled') and contains(.,'assets')]");

    // Present only when assets exist
    private final By byAssetsTable   = By.id("tblTickets");
    private final By byExportCsvBtn  = By.xpath(
            "//a[contains(translate(.,'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ'),'EXPORT')" +
            " or contains(@href,'xport') or contains(@href,'Export') or contains(@href,'export')] |" +
            "//button[contains(translate(.,'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ'),'EXPORT')] |" +
            "//input[(@type='button' or @type='submit') and contains(translate(@value,'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ'),'EXPORT')]");
    private final By byAssetRows     = By.cssSelector("#tblTickets tbody tr");
    // Name column header — ALL-CAPS in screenshot; sort link inside th
    private final By byNameHeader    = By.xpath("//th[contains(.,'NAME') or contains(.,'Name')][1]");
    // Select-all checkbox is in the last <th> of the header row
    private final By bySelectAll     = By.cssSelector("#tblTickets thead input[type='checkbox']");
    // Row checkboxes are in the last <td> of each body row
    private final By byRowCheckboxes = By.cssSelector("#tblTickets tbody tr td:last-child input[type='checkbox']");
    private final By byDeleteBtn     = By.xpath(
            "//button[contains(.,'Delete')] | //a[contains(.,'Delete')] | //input[@value='Delete']");

    // Import panel elements (visible when panel is toggled open)
    private final By byFileInput         = By.cssSelector("input[type='file']");
    private final By byUpdateExistingChk = By.xpath(
            "(//form[.//input[@type='file']]//input[@type='checkbox'])[1]");
    // Upload submit button: match input[type='submit'], button[@type='submit'],
    // and button with no type attribute (HTML5 default is submit).
    private final By byUploadBtn         = By.xpath(
            "//form[.//input[@type='file']]//input[@type='submit'] |" +
            "//form[.//input[@type='file']]//button[@type='submit'] |" +
            "//form[.//input[@type='file']]//button[not(@type)]");

    // Pagination
    private final By byPagination = By.cssSelector(".pagination, ul.pagination");

    public AssetsListPage(WebDriver driver) {
        super(driver);
    }

    // ── Navigation ──────────────────────────────────────────────────────────

    @Step("Navigate to Assets page")
    public AssetsListPage navigateToAssets() {
        driver.get(ConfigManager.getBaseUrl() + "/Assets/Index");
        // #quickSearch is always present regardless of asset count — safe wait target.
        waitForVisible(byQuickSearch);
        return this;
    }

    // ── Page-loaded checks ───────────────────────────────────────────────────

    /** Waits for #quickSearch — always present, even when 0 assets. */
    @Step("Wait for assets page to load")
    public void waitForAssetsPageLoaded() {
        waitForVisible(byQuickSearch);
    }

    /** Waits for #tblTickets — only present when assets exist. */
    @Step("Wait for assets table to be visible")
    public void waitForTableVisible() {
        waitForVisible(byAssetsTable);
    }

    /** Returns true if the assets table is visible (i.e. at least 1 asset exists). */
    @Step("Check if Assets page is displayed")
    public boolean isAssetsPageDisplayed() {
        try {
            waitForVisible(byAssetsTable);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ── Row / asset presence ─────────────────────────────────────────────────

    public boolean isAssetRowsPresent() {
        return isPresent(byAssetsTable) && !findAll(byAssetRows).isEmpty();
    }

    public int getRowCount() {
        if (!isPresent(byAssetsTable)) return 0;
        return findAll(byAssetRows).size();
    }

    /**
     * Returns true if an asset with the given name appears in the table.
     * Uses contains(.) to handle retired assets whose names are wrapped in <del>.
     * Returns false (not throws) when the table is absent.
     */
    @Step("Check if asset '{name}' is displayed in the list")
    public boolean isAssetDisplayedInList(String name) {
        try {
            List<WebElement> rows = findAll(byAssetRows);
            return rows.stream().anyMatch(r -> r.getText().contains(name));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Searches all pages for an asset with the given name.
     * Follows pagination "next" links until the asset is found or pages are exhausted.
     */
    @Step("Check if asset '{name}' is displayed across all pages")
    public boolean isAssetDisplayedAcrossAllPages(String name) {
        if (isAssetDisplayedInList(name)) return true;
        By byNextPage = By.xpath(
                "//ul[contains(@class,'pagination')]//li[not(contains(@class,'disabled'))]" +
                "/a[normalize-space(.)='»' or normalize-space(.)='>' or normalize-space(.)='›'" +
                " or contains(translate(@aria-label,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'next')]");
        int pageLimit = 20;
        while (isPresent(byNextPage) && pageLimit-- > 0) {
            try {
                click(byNextPage);
                waitForAssetsPageLoaded();
                if (isAssetDisplayedInList(name)) return true;
            } catch (Exception e) {
                break;
            }
        }
        return false;
    }

    public List<String> getAssetNames() {
        return findAll(byAssetRows).stream()
                .map(r -> {
                    // Name is the first data cell (after the row header / checkbox column)
                    List<WebElement> cells = r.findElements(By.tagName("td"));
                    return cells.isEmpty() ? "" : cells.get(0).getText().trim();
                })
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    // ── Button / control visibility ──────────────────────────────────────────

    public boolean isNewAssetButtonDisplayed()   { return isDisplayed(byNewAssetBtn); }
    public boolean isImportCsvButtonDisplayed() {
        return findAll(byImportCsvBtn).stream()
                .anyMatch(e -> { try { return e.isDisplayed(); } catch (Exception ex) { return false; } });
    }
    public boolean isShowRetiredLinkDisplayed()  { return isDisplayed(byShowRetiredLink); }
    public boolean isQuickSearchDisplayed()      { return isDisplayed(byQuickSearch); }
    public boolean isPaginationVisible()         { return isPresent(byPagination); }

    public boolean isExportCsvButtonDisplayed() {
        return isPresent(byExportCsvBtn) && driver.findElement(byExportCsvBtn).isDisplayed();
    }

    // ── Actions ──────────────────────────────────────────────────────────────

    @Step("Click 'New Asset' button")
    public CreateAssetPage clickNewAsset() {
        click(byNewAssetBtn);
        return new CreateAssetPage(driver);
    }

    @Step("Click 'Search' button")
    public AssetsSearchPage clickSearch() {
        click(bySearchBtn);
        return new AssetsSearchPage(driver);
    }

    @Step("Click 'Import (CSV)' to toggle import panel")
    public void clickImportCsv() {
        java.util.List<WebElement> candidates = findAll(byImportCsvBtn);
        for (WebElement el : candidates) {
            try {
                if (el.isDisplayed()) {
                    el.click();
                    return;
                }
            } catch (Exception ignored) {}
        }
        click(byImportCsvBtn);
    }

    @Step("Wait for import panel to be hidden")
    public void waitForImportPanelHidden() {
        waitForInvisible(byFileInput);
    }

    @Step("Click 'Export (CSV)' button")
    public void clickExportCsv() {
        click(byExportCsvBtn);
    }

    /**
     * Ensures the "Show retired" toggle is in the OFF (default) state.
     * Detects toggle-ON by checking for &lt;del&gt; elements in the table body —
     * the server wraps retired asset names in &lt;del&gt; only when the toggle is ON.
     * Safe to call when there are no assets (table absent → no &lt;del&gt; → no-op).
     */
    @Step("Ensure retired assets are hidden (toggle OFF)")
    public void ensureRetiredHidden() {
        By byDel = By.cssSelector("#tblTickets tbody del");
        if (isPresent(byDel)) {
            clickShowRetired();
        }
    }

    @Step("Click 'Show retired' toggle")
    public void clickShowRetired() {
        click(byShowRetiredLink);
        waitForAssetsPageLoaded();
    }

    @Step("Type '{text}' in Quick Search")
    public void quickSearch(String text) {
        sendKeys(byQuickSearch, text);
    }

    @Step("Click sort by Name column header")
    public void clickSortByName() {
        click(byNameHeader);
        waitForAssetsPageLoaded();
    }

    // ── Import panel ─────────────────────────────────────────────────────────

    public boolean isImportPanelVisible() {
        return isPresent(byFileInput) && driver.findElement(byFileInput).isDisplayed();
    }

    @Step("Upload CSV file: {absolutePath}")
    public void uploadCsvFile(String absolutePath) {
        waitForVisible(byFileInput);
        driver.findElement(byFileInput).sendKeys(absolutePath);
    }

    @Step("Check 'Update existing assets' checkbox")
    public void checkUpdateExisting() {
        WebElement chk = driver.findElement(byUpdateExistingChk);
        if (!chk.isSelected()) {
            jsClick(chk);
        }
    }

    @Step("Click upload/submit button in import panel")
    public void clickUploadButton() {
        // form.submit() bypasses jQuery submit handlers and may not send file data correctly.
        // Instead: strip onclick="return false" from the button and click it natively so the
        // browser submits the form with proper multipart/form-data encoding.
        List<WebElement> qs = findAll(byQuickSearch);
        WebElement qsBefore = qs.isEmpty() ? null : qs.get(0);

        List<WebElement> candidates = findAll(byUploadBtn);
        WebElement submitBtn = candidates.stream()
                .filter(e -> { try { return e.isDisplayed(); } catch (Exception ex) { return false; } })
                .findFirst().orElse(null);

        if (submitBtn != null) {
            ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                    "var f = arguments[0].closest('form');" +
                    "if (f) f.setAttribute('enctype','multipart/form-data');" +
                    "arguments[0].onclick = null;" +
                    "arguments[0].removeAttribute('onclick');",
                    submitBtn);
            submitBtn.click();
        } else {
            // Fallback: explicit enctype + form.submit()
            try {
                WebElement fileInput = driver.findElement(byFileInput);
                WebElement form = (WebElement) ((org.openqa.selenium.JavascriptExecutor) driver)
                        .executeScript("return arguments[0].closest('form')", fileInput);
                if (form != null) {
                    ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                            "arguments[0].enctype='multipart/form-data'; arguments[0].submit();", form);
                }
            } catch (Exception e) {
                click(byUploadBtn);
            }
        }

        if (qsBefore != null) {
            try {
                new org.openqa.selenium.support.ui.WebDriverWait(driver,
                        java.time.Duration.ofSeconds(15))
                        .until(org.openqa.selenium.support.ui.ExpectedConditions.stalenessOf(qsBefore));
            } catch (Exception ignored) {}
        }
        waitForAssetsPageLoaded();
    }

    // ── Bulk delete ──────────────────────────────────────────────────────────

    @Step("Select all asset checkboxes")
    public void selectAllAssets() {
        // Native click (not jsClick) to trigger the browser's check/change events
        // that the app's jQuery handlers rely on to register selections server-side.
        WebElement selectAll = driver.findElement(bySelectAll);
        if (!selectAll.isSelected()) {
            selectAll.click();
        }
        findAll(byRowCheckboxes).forEach(chk -> {
            try { if (!chk.isSelected()) chk.click(); } catch (Exception ignored) {}
        });
    }

    /** Returns true if any retired asset (shown with &lt;del&gt; strikethrough) is visible on the current page. */
    public boolean isAnyRetiredAssetVisible() {
        return isPresent(By.cssSelector("#tblTickets tbody del"));
    }

    @Step("Click bulk delete button")
    public void clickBulkDelete() {
        click(byDeleteBtn);
        // Handle native browser alert
        try {
            driver.switchTo().alert().accept();
        } catch (Exception ignored) {}
        // Handle custom JS/Bootstrap modal confirmation (common in ASP.NET MVC apps)
        try {
            By byModalConfirm = By.xpath(
                    "//button[not(contains(.,'Cancel')) and not(contains(.,'No'))]" +
                    "[contains(.,'Yes') or contains(.,'OK') or contains(.,'Confirm') or contains(.,'Delete')]");
            new org.openqa.selenium.support.ui.WebDriverWait(driver,
                    java.time.Duration.ofSeconds(3))
                    .until(org.openqa.selenium.support.ui.ExpectedConditions
                            .elementToBeClickable(byModalConfirm));
            driver.findElement(byModalConfirm).click();
        } catch (Exception ignored) {}
        waitForAssetsPageLoaded();
    }

    // ── Asset detail navigation ──────────────────────────────────────────────

    @Step("Click on asset '{name}' to open detail page")
    public AssetDetailPage clickOnAsset(String name) {
        By assetLink = By.xpath(
                "//table[@id='tblTickets']//td[contains(.,'" + name + "')]//a | " +
                "//table[@id='tblTickets']//a[contains(.,'" + name + "')]");
        click(assetLink);
        return new AssetDetailPage(driver);
    }
}
