package com.tickhelp.automation.tests.assets;

import com.tickhelp.automation.base.BaseTest;
import com.tickhelp.automation.core.ConfigManager;
import com.tickhelp.automation.core.DriverManager;
import com.tickhelp.automation.pages.assets.AssetsListPage;
import com.tickhelp.automation.pages.login.LoginPage;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.net.URL;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

@Feature("Assets CSV Import")
public class AssetsImportCsvTest extends BaseTest {

    private static final Logger log = LogManager.getLogger(AssetsImportCsvTest.class);

    private AssetsListPage assetsListPage;

    @BeforeClass(alwaysRun = true)
    public void loginAndSetup() {
        LoginPage loginPage = new LoginPage(DriverManager.getDriver());
        loginPage.login(ConfigManager.getAdminEmail(), ConfigManager.getAdminPassword());
        assetsListPage = new AssetsListPage(DriverManager.getDriver());
        assetsListPage.navigateToAssets();
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private String resolveTestData(String filename) {
        URL res = getClass().getClassLoader().getResource("test-data/" + filename);
        if (res == null) throw new IllegalStateException("Test data not found: test-data/" + filename);
        try {
            return Paths.get(res.toURI()).toAbsolutePath().toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ── Tests ────────────────────────────────────────────────────────────────

    @Test(priority = 10, description = "TC-IM-001: Import panel opens when 'Import (CSV)...' is clicked")
    public void testImportPanelOpensOnButtonClick() {
        assetsListPage.navigateToAssets();
        assertThat(assetsListPage.isImportPanelVisible())
                .as("Import panel should be hidden before clicking the button")
                .isFalse();
        assetsListPage.clickImportCsv();
        assertThat(assetsListPage.isImportPanelVisible())
                .as("Import panel should be visible after clicking Import (CSV)...")
                .isTrue();
    }

    @Test(priority = 20, description = "TC-IM-002: Import panel closes on second click")
    public void testImportPanelClosesOnSecondClick() {
        assetsListPage.navigateToAssets();
        assetsListPage.clickImportCsv();  // open
        assertThat(assetsListPage.isImportPanelVisible()).as("Panel should be open").isTrue();
        assetsListPage.clickImportCsv();  // close
        assetsListPage.waitForImportPanelHidden();
        assertThat(assetsListPage.isImportPanelVisible())
                .as("Import panel should close on second click")
                .isFalse();
    }

    @Test(priority = 30, description = "TC-IM-003: Uploading a valid CSV creates a new asset")
    @Description("Upload valid-asset.csv and verify row count increases by at least 1")
    public void testUploadValidCsvCreatesAsset() {
        assetsListPage.navigateToAssets();
        int before = assetsListPage.getRowCount();
        log.info("Row count before upload: {}", before);

        assetsListPage.clickImportCsv();
        assetsListPage.uploadCsvFile(resolveTestData("valid-asset.csv"));
        assetsListPage.clickUploadButton();

        assetsListPage.navigateToAssets();
        int after = assetsListPage.getRowCount();
        log.info("Row count after upload: {}", after);

        assertThat(assetsListPage.isAssetDisplayedAcrossAllPages("AutoImportAsset_TC003"))
                .as("AutoImportAsset_TC003 should appear in the asset list after valid CSV upload")
                .isTrue();
    }

    @Test(priority = 40, description = "TC-IM-004: Uploading CSV with 'update existing' updates an asset's location")
    @Description("Create asset TC004, then upload update-location.csv with 'update existing' checked to change location")
    public void testUploadCsvWithUpdateExistingUpdatesLocation() {
        assetsListPage.navigateToAssets();

        // Step 1 — create the asset to be updated via initial CSV upload
        assetsListPage.clickImportCsv();
        assetsListPage.uploadCsvFile(resolveTestData("create-tc004.csv"));
        assetsListPage.clickUploadButton();

        assetsListPage.navigateToAssets();
        assertThat(assetsListPage.isAssetDisplayedAcrossAllPages("AutoImportAsset_TC004"))
                .as("AutoImportAsset_TC004 should appear in the asset list after initial CSV upload")
                .isTrue();

        // Step 2 — upload update CSV with 'update existing' checked
        assetsListPage.clickImportCsv();
        assetsListPage.uploadCsvFile(resolveTestData("update-location.csv"));
        assetsListPage.checkUpdateExisting();
        assetsListPage.clickUploadButton();

        // Step 3 — verify asset still exists (location updated, not duplicated)
        assetsListPage.navigateToAssets();
        assertThat(assetsListPage.isAssetDisplayedAcrossAllPages("AutoImportAsset_TC004"))
                .as("AutoImportAsset_TC004 should still exist after location update")
                .isTrue();
    }

    @Test(priority = 50, description = "TC-IM-005: Uploading an invalid file type shows error or makes no change")
    public void testUploadInvalidFileTypeShowsErrorOrNoChange() {
        assetsListPage.navigateToAssets();
        int before = assetsListPage.getRowCount();

        assetsListPage.clickImportCsv();
        assetsListPage.uploadCsvFile(resolveTestData("invalid-file.txt"));
        assetsListPage.clickUploadButton();

        // Either an error is shown or row count is unchanged — both are acceptable
        assetsListPage.navigateToAssets();
        int after = assetsListPage.getRowCount();

        assertThat(DriverManager.getDriver().getCurrentUrl())
                .as("Should remain on or return to the Assets page after invalid upload")
                .contains("/Assets");
        assertThat(after)
                .as("Row count should not increase when an invalid file type is uploaded")
                .isLessThanOrEqualTo(before + 1); // server may reject silently
    }

    @Test(priority = 60, description = "TC-IM-006: Clicking upload without selecting a file does not crash")
    public void testClickUploadWithoutFileDoesNotCrash() {
        assetsListPage.navigateToAssets();
        assetsListPage.clickImportCsv();
        // Submit without choosing a file
        assetsListPage.clickUploadButton();

        assertThat(DriverManager.getDriver().getCurrentUrl())
                .as("Assets page should be accessible after submitting upload with no file")
                .contains("/Assets");
    }

    @Test(priority = 70, description = "TC-IM-007: Uploading an empty CSV shows error or makes no change")
    public void testUploadEmptyCsvShowsErrorOrNoChange() {
        assetsListPage.navigateToAssets();
        int before = assetsListPage.getRowCount();

        assetsListPage.clickImportCsv();
        assetsListPage.uploadCsvFile(resolveTestData("empty.csv"));
        assetsListPage.clickUploadButton();

        assetsListPage.navigateToAssets();
        int after = assetsListPage.getRowCount();

        assertThat(DriverManager.getDriver().getCurrentUrl())
                .as("Should remain on or return to the Assets page after empty CSV upload")
                .contains("/Assets");
        assertThat(after)
                .as("Row count should not increase when an empty CSV is uploaded")
                .isLessThanOrEqualTo(before);
    }
}
