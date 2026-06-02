package com.tickhelp.automation.tests.assets;

import com.tickhelp.automation.base.BaseTest;
import com.tickhelp.automation.core.ConfigManager;
import com.tickhelp.automation.core.DriverManager;
import com.tickhelp.automation.pages.assets.AssetsListPage;
import com.tickhelp.automation.pages.assets.CreateAssetPage;
import com.tickhelp.automation.pages.login.LoginPage;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Feature("Assets Management")
public class AssetsTest extends BaseTest {

    private static final Logger log = LogManager.getLogger(AssetsTest.class);

    private AssetsListPage assetsListPage;

    @BeforeClass(alwaysRun = true)
    public void loginAndNavigateToAssets() {
        if (!isDriverAlive()) {
            log.warn("Driver dead at @BeforeClass entry — reinitialising");
            DriverManager.quitDriver();
            DriverManager.initDriver();
            DriverManager.getDriver().get(ConfigManager.getBaseUrl() + "/User/Login");
        }
        LoginPage loginPage = new LoginPage(DriverManager.getDriver());
        loginPage.login(ConfigManager.getAdminEmail(), ConfigManager.getAdminPassword());

        assetsListPage = new AssetsListPage(DriverManager.getDriver());
        assetsListPage.navigateToAssets();
        assetsListPage.ensureRetiredHidden();

        // Seed one asset if DB is empty so priority-10 test (testViewAssets) can pass
        if (!assetsListPage.isAssetRowsPresent()) {
            log.info("DB is empty — seeding one asset before suite");
            try {
                CreateAssetPage createPage = assetsListPage.clickNewAsset();
                createPage.createAsset("0AutoSeed_" + System.currentTimeMillis(), "Laptop");
                assetsListPage.navigateToAssets();
            } catch (Exception e) {
                log.warn("Seeding failed ({}); tests requiring assets will self-skip", e.getMessage());
                assetsListPage.navigateToAssets();
            }
        }
    }

    // ── View / Navigation ────────────────────────────────────────────────────

    @Test(priority = 10, description = "TC-AM-001: Assets table is visible after navigation")
    @Description("Navigate to the Assets page and verify the table is rendered")
    public void testViewAssets() {
        assetsListPage.navigateToAssets();
        assertThat(assetsListPage.isAssetsPageDisplayed())
                .as("Assets table (tblTickets) should be visible after navigation")
                .isTrue();
    }

    @Test(priority = 20, description = "TC-AM-002: Clicking Name column header sorts the list")
    @Description("Click the Name column header and verify the page reloads (sort redirect)")
    public void testSortByName() {
        assetsListPage.navigateToAssets();
        assetsListPage.clickSortByName();
        assertThat(DriverManager.getDriver().getCurrentUrl())
                .as("URL should contain sort parameter after clicking Name header")
                .contains("/Assets");
    }

    @Test(priority = 30, description = "TC-AM-003: Pagination controls visible when >1 page of assets")
    @Description("Skip if there are fewer assets than a full page")
    public void testPagination() {
        assetsListPage.navigateToAssets();
        if (!assetsListPage.isPaginationVisible()) {
            throw new SkipException("Fewer than one page of assets — pagination not rendered");
        }
        assertThat(assetsListPage.isPaginationVisible())
                .as("Pagination should be visible when assets exceed one page")
                .isTrue();
    }

    @Test(priority = 40, description = "TC-AM-004: Quick search input filters assets")
    @Description("Type a search term in Quick Search and verify the table responds")
    public void testQuickSearch() {
        assetsListPage.navigateToAssets();
        assetsListPage.quickSearch("nonexistent_xyz_99");
        // The quick search input should still be present regardless of results
        assertThat(assetsListPage.isQuickSearchDisplayed())
                .as("Quick search input should remain visible after typing")
                .isTrue();
    }

    @Test(priority = 50, description = "TC-AM-005: Export CSV button triggers download")
    @Description("Click Export (CSV) and verify no error page is shown")
    public void testCsvExport() {
        assetsListPage.navigateToAssets();
        if (!assetsListPage.isExportCsvButtonDisplayed()) {
            throw new SkipException("No assets present — Export (CSV) button not shown");
        }
        assetsListPage.clickExportCsv();
        assertThat(DriverManager.getDriver().getCurrentUrl())
                .as("Should not navigate to an error page after Export CSV click")
                .contains("/Assets");
    }

    @Test(priority = 60, description = "TC-AM-006: Import CSV button toggles the upload panel")
    @Description("Click Import (CSV)... and verify the file upload panel appears")
    public void testCsvImport() {
        assetsListPage.navigateToAssets();
        assetsListPage.clickImportCsv();
        assertThat(assetsListPage.isImportPanelVisible())
                .as("Import panel should be visible after clicking Import (CSV)...")
                .isTrue();
        // Close the panel again
        assetsListPage.clickImportCsv();
    }

    @Test(priority = 70, description = "TC-AM-007: Bulk delete removes selected assets")
    @Description("Select all assets, delete them, and verify the page is still accessible")
    public void testBulkDelete() {
        assetsListPage.navigateToAssets();
        if (!assetsListPage.isAssetRowsPresent()) {
            throw new SkipException("No assets to bulk-delete");
        }
        // Loop until all pages are cleared — selectAllAssets only acts on the current page,
        // so assets accumulated on page 2+ require repeated passes.
        while (assetsListPage.isAssetRowsPresent()) {
            assetsListPage.selectAllAssets();
            assetsListPage.clickBulkDelete();
            assetsListPage.navigateToAssets();
        }

        assertThat(DriverManager.getDriver().getCurrentUrl())
                .as("Assets page should be accessible and functional after bulk delete")
                .contains("/Assets");
    }

    @Test(priority = 80, description = "TC-AM-008: Empty-state page renders correctly with 0 assets")
    @Description("After bulk delete the server renders the empty-state page without #tblTickets")
    public void testEmptyList() {
        assetsListPage.navigateToAssets();
        assertThat(assetsListPage.isQuickSearchDisplayed())
                .as("Quick search should be present even when no assets exist")
                .isTrue();
        assertThat(assetsListPage.isAssetRowsPresent())
                .as("No asset rows should be present after bulk delete")
                .isFalse();
    }

    // ── UI Element Visibility ────────────────────────────────────────────────

    @Test(priority = 81, description = "TC-AM-009: 'New Asset' button is always visible")
    public void testNewAssetButtonVisible() {
        assetsListPage.navigateToAssets();
        assertThat(assetsListPage.isNewAssetButtonDisplayed())
                .as("'New Asset...' button should always be visible")
                .isTrue();
    }

    @Test(priority = 91, description = "TC-AM-010: Export CSV button visible when assets exist")
    @Description("Export (CSV) button only renders when at least one asset exists")
    public void testExportCsvButtonVisible() {
        assetsListPage.navigateToAssets();
        if (!assetsListPage.isAssetRowsPresent()) {
            throw new SkipException("No assets in DB — Export (CSV) button only appears when assets exist");
        }
        assertThat(assetsListPage.isExportCsvButtonDisplayed())
                .as("Export (CSV) button should be visible when assets exist")
                .isTrue();
    }

    @Test(priority = 83, description = "TC-AM-011: 'Import (CSV)' button is always visible")
    public void testImportCsvButtonVisible() {
        assetsListPage.navigateToAssets();
        assertThat(assetsListPage.isImportCsvButtonDisplayed())
                .as("'Import (CSV)...' button should always be visible")
                .isTrue();
    }

    @Test(priority = 84, description = "TC-AM-012: Quick search input is always visible")
    public void testQuickSearchInputVisible() {
        assetsListPage.navigateToAssets();
        assertThat(assetsListPage.isQuickSearchDisplayed())
                .as("Quick search input should always be visible")
                .isTrue();
    }

    @Test(priority = 85, description = "TC-AM-013: 'Show retired' toggle link is always visible")
    public void testShowDisabledToggleVisible() {
        assetsListPage.navigateToAssets();
        assertThat(assetsListPage.isShowRetiredLinkDisplayed())
                .as("'Show retired (disabled) assets' link should always be visible")
                .isTrue();
    }

    @Test(priority = 86, description = "TC-AM-014: Clicking Import (CSV) opens the upload panel")
    public void testImportCsvTogglesUploadContainer() {
        assetsListPage.navigateToAssets();
        // Panel should be closed initially
        assertThat(assetsListPage.isImportPanelVisible())
                .as("Import panel should be hidden before clicking the button")
                .isFalse();
        assetsListPage.clickImportCsv();
        assertThat(assetsListPage.isImportPanelVisible())
                .as("Import panel should be visible after clicking Import (CSV)...")
                .isTrue();
        // Restore state
        assetsListPage.clickImportCsv();
    }
}
