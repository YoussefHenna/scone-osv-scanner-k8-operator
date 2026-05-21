package com.youssefhenna.updates;

import com.youssefhenna.SconeOsvScanner;
import com.youssefhenna.spec.CommonDependantSpec;
import com.youssefhenna.spec.SconeOsvScannerSpec;
import com.youssefhenna.spec.database.DatabaseSpec;
import com.youssefhenna.spec.database.MariadbSpec;
import com.youssefhenna.spec.database.MaxscaleSpec;
import com.youssefhenna.spec.scanner.DbManagerSpec;
import com.youssefhenna.spec.scanner.FrontAppSpec;
import com.youssefhenna.spec.scanner.ScannerSpec;
import com.youssefhenna.status.UpdateStatus;
import com.youssefhenna.updates.model.DependantResourceType;
import com.youssefhenna.updates.model.RunUpdateResult;
import com.youssefhenna.updates.registry_read.RegistryImageVersionReader;
import io.quarkus.logging.Log;

import org.semver4j.Semver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ContainerUpdate {

    public static Map<DependantResourceType, RunUpdateResult> runContainerUpdates(SconeOsvScanner resource, RegistryImageVersionReader imageVersionReader, Runnable patchResource) {
        SconeOsvScannerSpec spec = resource.getSpec();
        ScannerSpec scannerSpec = spec.getScanner();
        DatabaseSpec databaseSpec = spec.getDatabase();

        FrontAppSpec frontAppSpec = scannerSpec.getFrontApp();
        DbManagerSpec dbManagerSpec = scannerSpec.getDbManager();
        MariadbSpec mariadbSpec = databaseSpec.getMariadb();
        MaxscaleSpec maxscaleSpec = databaseSpec.getMaxscale();

        Map<DependantResourceType, RunUpdateResult> results = new HashMap<>();

        if (frontAppSpec.isAutoUpdate()) {
            results.put(DependantResourceType.FRONT_APP, runUpdate(frontAppSpec, imageVersionReader));
        }

        if (dbManagerSpec.isAutoUpdate()) {
            results.put(DependantResourceType.DB_MANAGER, runUpdate(dbManagerSpec, imageVersionReader));
        }

        if (mariadbSpec.isAutoUpdate()) {
            results.put(DependantResourceType.MARIADB, runUpdate(mariadbSpec, imageVersionReader));
        }

        if (maxscaleSpec.isAutoUpdate()) {
            results.put(DependantResourceType.MAXSCALE, runUpdate(maxscaleSpec, imageVersionReader));
        }

        boolean anyUpdated = results.values().stream().anyMatch(r -> r.getLastUpdateStatus() == UpdateStatus.SUCCESS_UPDATED);
        if (anyUpdated) {
            // spec updated through runUpdate calls, patch resource with changes to trigger reconcile
            patchResource.run();
        }

        return results;
    }

    private static RunUpdateResult runUpdate(CommonDependantSpec dependantSpec, RegistryImageVersionReader imageVersionReader) {
        try {
            List<String> allTags = imageVersionReader.getAvailableVersions(dependantSpec, dependantSpec.getImageName());
            List<Semver> stableVersions = filterStableSemver(allTags);
            Semver highestVersion = findHighestVersion(stableVersions);

            if (highestVersion == null) {
                return new RunUpdateResult(dependantSpec.getImageVersion(), UpdateStatus.FAILED_HIGHEST_VERSION_NOT_FOUND);
            }

            Semver currentVersion = Semver.parse(dependantSpec.getImageVersion());
            if (currentVersion == null || highestVersion.isGreaterThan(currentVersion)) {
                String newVersion = highestVersion.getVersion();
                dependantSpec.setImageVersion(newVersion);
                Log.info("Updated " + dependantSpec.getImageName() + " to " + newVersion);
                return new RunUpdateResult(newVersion, UpdateStatus.SUCCESS_UPDATED);
            }

            Log.info("Skipped update for image, already up to date:" + dependantSpec.getImageName());
            return new RunUpdateResult(dependantSpec.getImageVersion(), UpdateStatus.SKIPPED_ALREADY_UPTODATE);
        } catch (Exception e) {
            Log.error("Fail to run update for " + dependantSpec.getImageName() + "->" + e.getMessage());
            return new RunUpdateResult(dependantSpec.getImageVersion(), UpdateStatus.FAILED_UNKNOWN_ERROR);
        }
    }

    private static List<Semver> filterStableSemver(List<String> tags) {
        return tags.stream()
                .map(Semver::parse)
                .filter(semver -> semver != null && semver.isStable())
                .collect(Collectors.toList());
    }

    private static Semver findHighestVersion(List<Semver> versions) {
        return versions.stream()
                .max(Semver::compareTo)
                .orElse(null);
    }
}
