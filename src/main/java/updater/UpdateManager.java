package updater;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import prefs.Preferences;
import prefs.UpdateConfig;
import ui.UpdateProgressWindow;
import util.JsonSerializationConverter;
import util.Version;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The class that will handle updating of HubTurbo application
 */
public class UpdateManager {
    private static final Logger logger = LogManager.getLogger(UpdateManager.class.getName());
    private final ExecutorService pool = Executors.newSingleThreadExecutor();

    // Error messages
    private static final String ERROR_INIT_UPDATE = "Failed to initialize update";
    private static final String ERROR_DOWNLOAD_UPDATE_DATA = "Failed to download update data";
    private static final String ERROR_DOWNLOAD_UPDATE_APP = "Failed to download updated application";

    // Directories and file names
    private static final String UPDATE_DIRECTORY = "updates";
    // TODO change to release version on merging with master
    private static final String UPDATE_SERVER_DATA_NAME =
            "https://raw.githubusercontent.com/HubTurbo/HubTurbo/1271-updater-data/HubTurboUpdate.json";
    private static final String UPDATE_LOCAL_DATA_NAME = UPDATE_DIRECTORY + File.separator + "HubTurbo.json";
    private static final String UPDATE_APP_NAME = UPDATE_DIRECTORY + File.separator + "HubTurbo.jar";
    public static final String UPDATE_CONFIG_FILENAME = "updateConfig.json";


    // Class member variables
    private final UpdateProgressWindow updateProgressWindow;
    private UpdateConfig updateConfig;


    public UpdateManager(UpdateProgressWindow updateProgressWindow) {
        this.updateProgressWindow = updateProgressWindow;
        loadUpdateConfig();
    }

    /**
     * Driver method to trigger UpdateManager to run. Update will be run on another thread.
     *
     * - Run is not automatic upon instancing the class in case there would like to be conditions on when to run update,
     *   e.g. only if user is logged in
     */
    public void run() {
        pool.execute(() -> runUpdate());
    }

    /**
     * Runs update sequence.
     */
    private void runUpdate() {
        // Fail if folder cannot be created
        if (!initUpdate()) {
            logger.error(ERROR_INIT_UPDATE);
            return;
        }

        if (!downloadUpdateData()) {
            logger.error(ERROR_DOWNLOAD_UPDATE_DATA);
            return;
        }

        // Checks if there is a new update since last update
        Optional<UpdateDownloadLink> updateDownloadLink = getUpdateDownloadLinkOfUpdate();

        if (!updateDownloadLink.isPresent() ||
            !checkIfNewVersionAvailableToDownload(updateDownloadLink.get().version)) {
            return;
        }

        markStartOfAppUpdateDownload();

        if (!downloadUpdateForApplication(updateDownloadLink.get().applicationFileLocation)) {
            logger.error(ERROR_DOWNLOAD_UPDATE_APP);
            return;
        }

        markEndOfAppUpdateDownload(updateDownloadLink.get().version);

        // TODO prompt user for restarting application to apply update
        // If yes, quit application and run jar updater with execution

    }

    /**
     * Initializes system for updates
     * - Creates directory(ies) for updates
     */
    private boolean initUpdate() {
        File updateDir = new File(UPDATE_DIRECTORY);

        if (!updateDir.exists() && !updateDir.mkdirs()) {
            logger.error("Failed to create update directories");
            return false;
        }

        return true;
    }

    /**
     * Downloads update data to check if update is present.
     *
     * @return true if download successful, false otherwise
     */
    private boolean downloadUpdateData() {
        try {
            FileDownloader fileDownloader = new FileDownloader(
                    new URI(UPDATE_SERVER_DATA_NAME),
                    new File(UPDATE_LOCAL_DATA_NAME),
                    a -> {});
            return fileDownloader.download();
        } catch (URISyntaxException e) {
            logger.error(ERROR_DOWNLOAD_UPDATE_DATA, e);
            return false;
        }
    }

    /**
     * Downloads application update based on update data.
     *
     * @return true if download successful, false otherwise
     */
    private boolean downloadUpdateForApplication(URL downloadURL) {
        URI downloadUri;

        try {
            downloadUri = downloadURL.toURI();
        } catch (URISyntaxException e) {
            logger.error("Download URI is not correct", e);
            return false;
        }

        LabeledDownloadProgressBar downloadProgressBar = updateProgressWindow.createNewDownloadProgressBar(
                downloadUri, "Downloading HubTurbo Application...");

        FileDownloader fileDownloader = new FileDownloader(
                downloadUri,
                new File(UPDATE_APP_NAME),
                downloadProgressBar::setProgress);
        boolean result = fileDownloader.download();

        updateProgressWindow.removeDownloadProgressBar(downloadUri);

        return result;
    }

    /**
     * Checks if a given version is a new version that can be downloaded.
     * If that version was previously downloaded (even if newer than current), we will not download it again.
     *
     * Scenario: on V0.0.0, V1.0.0 was downloaded. However, user is still using V0.0.0 and there is no newer update
     *           than V1.0.0. HT won't download V1.0.0 again because the fact that user is still in V0.0.0 means he
     *           does not want to use V0.0.0 (either V1.0.0 is broken or due to other reasons).
     *
     * @param version version to be checked if it is an update
     * @return true if the given version can be downloaded, false otherwise
     */
    private boolean checkIfNewVersionAvailableToDownload(Version version) {
        return Version.getCurrentVersion().compareTo(version) < 0 &&
                !updateConfig.checkIfVersionWasPreviouslyDownloaded(version);
    }

    private Optional<UpdateDownloadLink> getUpdateDownloadLinkOfUpdate() {
        File updateDataFile = new File(UPDATE_LOCAL_DATA_NAME);
        JsonSerializationConverter jsonUpdateDataConverter = new JsonSerializationConverter(updateDataFile);
        UpdateData updateData = (UpdateData) jsonUpdateDataConverter.loadFromFile(UpdateData.class)
                .orElse(new UpdateData());

        return updateData.getUpdateDownloadLink();
    }

    private void markStartOfAppUpdateDownload() {
        updateConfig.setLastUpdateDownloadStatus(false);
        saveUpdateConfig();
    }

    private void markEndOfAppUpdateDownload(Version versionDownloaded) {
        updateConfig.setLastUpdateDownloadStatus(true);
        updateConfig.addToVersionPreviouslyDownloaded(versionDownloaded);
        saveUpdateConfig();
    }

    private void loadUpdateConfig() {
        File updateConfigFile = new File(Preferences.DIRECTORY + File.separator + UPDATE_CONFIG_FILENAME);
        JsonSerializationConverter jsonConverter = new JsonSerializationConverter(updateConfigFile);
        this.updateConfig = (UpdateConfig) jsonConverter.loadFromFile(UpdateConfig.class).orElse(new UpdateConfig());
    }

    private void saveUpdateConfig() {
        File updateConfigFile = new File(Preferences.DIRECTORY + File.separator + UPDATE_CONFIG_FILENAME);
        JsonSerializationConverter jsonConverter = new JsonSerializationConverter(updateConfigFile);
        jsonConverter.saveToFile(updateConfig);
    }

    public void showUpdateProgressWindow() {
        updateProgressWindow.showWindow();
    }

    public void hideUpdateProgressWindow() {
        updateProgressWindow.hideWindow();
    }
}
