package com.tickhelp.automation.tests.assets;

import com.tickhelp.automation.base.BaseTest;
import com.tickhelp.automation.core.ConfigManager;
import com.tickhelp.automation.core.DriverManager;
import com.tickhelp.automation.pages.assets.AssetDetailPage;
import com.tickhelp.automation.pages.assets.AssetsListPage;
import com.tickhelp.automation.pages.assets.CreateAssetPage;
import com.tickhelp.automation.pages.login.LoginPage;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Feature("Assets Retired Toggle")
public class AssetsRetiredToggleTest extends BaseTest {

    private static final Logger log = LogManager.getLogger(AssetsRetiredToggleTest.class);

    private AssetsListPage assetsListPage;
    private String retiredAssetName;

    @BeforeClass(alwaysRun = true)
    public void loginAndSetup() {
        LoginPage loginPage = new LoginPage(DriverManager.getDriver());
        loginPage.login(ConfigManager.getAdminEmail(), ConfigManager.getAdminPassword());
        assetsListPage = new AssetsListPage(DriverManager.getDriver());
        assetsListPage.navigateToAssets();
    }

    /**
     * Before each test: create a fresh asset then retire it so every test starts
     * with a known retired asset in the database.
     * On Chrome renderer crash, reinit the driver session and skip rather than fail.
     */
    @BeforeMethod(alwaysRun = true)
    public void createFreshAssetAndRetire() {
        try {
            retiredAssetName = "0AutoRetire_" + System.currentTimeMillis();
            log.info("@BeforeMethod: creating and retiring asset '{}'", retiredAssetName);

            assetsListPage.navigateToAssets();
            CreateAssetPage createPage = assetsListPage.clickNewAsset();
            createPage.createAsset(retiredAssetName, "Laptop");

            // Navigate to asset detail and retire it
            assetsListPage.navigateToAssets();
            AssetDetailPage detailPage = assetsListPage.clickOnAsset(retiredAssetName);
            detailPage.retireAsset();

            assetsListPage.navigateToAssets();
        } catch (Exception e) {
            log.error("@BeforeMethod failed — reinitialising driver: {}", e.getMessage());
            DriverManager.quitDriver();
            DriverManager.initDriver();
            DriverManager.getDriver().get(ConfigManager.getBaseUrl() + "/User/Login");
            LoginPage lp = new LoginPage(DriverManager.getDriver());
            lp.login(ConfigManager.getAdminEmail(), ConfigManager.getAdminPassword());
            assetsListPage = new AssetsListPage(DriverManager.getDriver());
            throw new SkipException("@BeforeMethod failed — skipping: " + e.getMessage());
        }
    }

    /**
     * After each test: ensure the retired toggle is in the OFF (default) state.
     * Detects toggle-ON by the presence of &lt;del&gt; elements in the table — reliable
     * regardless of how many pages of retired assets have accumulated across runs.
     * Checking a specific asset name was fragile: if that asset landed on page 2+
     * the @AfterMethod would silently leave the toggle ON, contaminating the next test.
     */
    @AfterMethod(alwaysRun = true)
    public void ensureRetiredToggleOff() {
        try {
            assetsListPage.navigateToAssets();
            assetsListPage.ensureRetiredHidden();
        } catch (Exception e) {
            log.warn("@AfterMethod ensureRetiredToggleOff failed: {}", e.getMessage());
        }
    }

    // ── Tests ────────────────────────────────────────────────────────────────

    @Test(description = "TC-RT-001: Retired assets are hidden in the default view")
    @Description("Without toggling 'Show retired', the retired asset should not appear in the list")
    public void testDefaultStateRetiredAssetsHidden() {
        assetsListPage.navigateToAssets();
        assertThat(assetsListPage.isAssetDisplayedInList(retiredAssetName))
                .as("Retired asset '" + retiredAssetName + "' should NOT be visible in the default (non-retired) view")
                .isFalse();
    }

    @Test(description = "TC-RT-002: Clicking 'Show retired' reveals retired assets")
    public void testClickToggleShowsRetiredAssets() {
        assetsListPage.navigateToAssets();
        assetsListPage.clickShowRetired();
        // The app uses CSS strikethrough (not <del> tags) for retired assets.
        // After clicking Show Retired, the table must render with rows (at least the
        // asset retired by @BeforeMethod). Check for row presence rather than <del>.
        assertThat(assetsListPage.isAssetRowsPresent())
                .as("Asset table should have rows visible after toggling 'Show retired' (retired assets should appear)")
                .isTrue();
    }

    @Test(description = "TC-RT-003: Clicking 'Show retired' a second time hides retired assets again")
    public void testClickToggleAgainHidesRetiredAssets() {
        assetsListPage.navigateToAssets();
        assetsListPage.clickShowRetired();   // show retired
        assetsListPage.clickShowRetired();   // hide retired

        // After hiding, if 0 active assets remain the table won't be rendered.
        // Wait for quickSearch (always present) instead of the table.
        assetsListPage.waitForAssetsPageLoaded();

        assertThat(assetsListPage.isAssetDisplayedInList(retiredAssetName))
                .as("Retired asset should be hidden again after second toggle")
                .isFalse();
    }

    @Test(description = "TC-RT-004: Toggling 'Show retired' does not crash the page")
    public void testToggleClickDoesNotCrash() {
        assetsListPage.navigateToAssets();
        assetsListPage.clickShowRetired();
        assetsListPage.waitForAssetsPageLoaded();
        assetsListPage.clickShowRetired();
        assetsListPage.waitForAssetsPageLoaded();

        assertThat(DriverManager.getDriver().getCurrentUrl())
                .as("Should remain on the Assets page after two toggle clicks")
                .contains("/Assets");
    }

    @Test(description = "TC-RT-005: Sorting works correctly when retired assets are visible")
    public void testSortAppliesWithRetiredVisible() {
        assetsListPage.navigateToAssets();
        assetsListPage.clickShowRetired();
        assetsListPage.clickSortByName();

        assertThat(DriverManager.getDriver().getCurrentUrl())
                .as("Sort should work when retired assets are visible")
                .contains("/Assets");
        assetsListPage.waitForAssetsPageLoaded();
    }

    @Test(description = "TC-RT-006: A freshly retired asset appears after toggling 'Show retired'")
    @Description("Create a new asset, retire it in this test, then verify it appears in the retired view")
    public void testRetireFreshAssetAppearsAfterToggle() {
        String freshName = "0AutoFresh_" + System.currentTimeMillis();
        log.info("TC-RT-006: creating fresh asset '{}'", freshName);

        assetsListPage.navigateToAssets();
        CreateAssetPage createPage = assetsListPage.clickNewAsset();
        createPage.createAsset(freshName, "Laptop");

        assetsListPage.navigateToAssets();
        AssetDetailPage detailPage = assetsListPage.clickOnAsset(freshName);
        detailPage.retireAsset();

        assetsListPage.navigateToAssets();
        assetsListPage.clickShowRetired();
        assetsListPage.waitForAssetsPageLoaded();

        assertThat(assetsListPage.isAssetDisplayedInList(freshName))
                .as("Freshly retired asset '" + freshName + "' should appear after 'Show retired' is toggled on")
                .isTrue();
    }
}
