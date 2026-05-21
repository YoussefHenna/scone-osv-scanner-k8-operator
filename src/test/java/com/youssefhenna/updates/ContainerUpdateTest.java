package com.youssefhenna.updates;

import com.youssefhenna.SconeOsvScanner;
import com.youssefhenna.spec.CommonRegistrySpec;
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
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class ContainerUpdateTest {

    private StubRegistryImageVersionReader versionReader;
    private AtomicInteger patchCallCount;
    private Runnable patchResource;

    @BeforeEach
    void setUp() {
        versionReader = new StubRegistryImageVersionReader();
        patchCallCount = new AtomicInteger(0);
        patchResource = patchCallCount::incrementAndGet;
    }

    @Test
    void when_autoUpdateEnabled_andHigherVersionAvailable_versionIsUpdatedAndPatchCalled() {
        SconeOsvScanner resource = buildResource(spec -> {
            spec.getScanner().getFrontApp().setAutoUpdate(true);
            spec.getScanner().getFrontApp().setImageVersion("1.0.0");
        });
        versionReader.versions.put("front-app", List.of("1.0.0", "1.1.0", "2.0.0"));

        Map<DependantResourceType, RunUpdateResult> results = ContainerUpdate.runContainerUpdates(resource, versionReader, patchResource);

        assertEquals(UpdateStatus.SUCCESS_UPDATED, results.get(DependantResourceType.FRONT_APP).getLastUpdateStatus());
        assertEquals("2.0.0", results.get(DependantResourceType.FRONT_APP).getCurrentVersion());
        assertEquals("2.0.0", resource.getSpec().getScanner().getFrontApp().getImageVersion());
        assertEquals(1, patchCallCount.get());
    }

    @Test
    void when_autoUpdateEnabled_andAlreadyAtHighestVersion_updateIsSkipped() {
        SconeOsvScanner resource = buildResource(spec -> {
            spec.getScanner().getFrontApp().setAutoUpdate(true);
            spec.getScanner().getFrontApp().setImageVersion("2.0.0");
        });
        versionReader.versions.put("front-app", List.of("1.0.0", "2.0.0"));

        Map<DependantResourceType, RunUpdateResult> results = ContainerUpdate.runContainerUpdates(resource, versionReader, patchResource);

        assertEquals(UpdateStatus.SKIPPED_ALREADY_UPTODATE, results.get(DependantResourceType.FRONT_APP).getLastUpdateStatus());
        assertEquals(0, patchCallCount.get());
    }

    @Test
    void when_autoUpdateDisabled_componentIsNotIncludedInResults() {
        SconeOsvScanner resource = buildResource(spec -> {});

        Map<DependantResourceType, RunUpdateResult> results = ContainerUpdate.runContainerUpdates(resource, versionReader, patchResource);

        assertTrue(results.isEmpty());
        assertEquals(0, patchCallCount.get());
    }

    @Test
    void when_readerThrowsException_failedUnknownErrorIsReturned() {
        SconeOsvScanner resource = buildResource(spec -> spec.getScanner().getFrontApp().setAutoUpdate(true));
        versionReader.throwingImages.add("front-app");

        Map<DependantResourceType, RunUpdateResult> results = ContainerUpdate.runContainerUpdates(resource, versionReader, patchResource);

        assertEquals(UpdateStatus.FAILED_UNKNOWN_ERROR, results.get(DependantResourceType.FRONT_APP).getLastUpdateStatus());
        assertEquals(0, patchCallCount.get());
    }

    @Test
    void when_noStableVersionsAvailable_failedHighestVersionNotFoundReturned() {
        SconeOsvScanner resource = buildResource(spec -> {
            spec.getScanner().getFrontApp().setAutoUpdate(true);
            spec.getScanner().getFrontApp().setImageVersion("1.0.0");
        });
        versionReader.versions.put("front-app", List.of("2.0.0-alpha", "3.0.0-beta.1", "latest"));

        Map<DependantResourceType, RunUpdateResult> results = ContainerUpdate.runContainerUpdates(resource, versionReader, patchResource);

        assertEquals(UpdateStatus.FAILED_HIGHEST_VERSION_NOT_FOUND, results.get(DependantResourceType.FRONT_APP).getLastUpdateStatus());
        assertEquals(0, patchCallCount.get());
    }

    @Test
    void when_onlyAutoUpdateEnabledComponents_areIncludedInResults() {
        SconeOsvScanner resource = buildResource(spec -> {
            spec.getScanner().getFrontApp().setAutoUpdate(true);
            spec.getDatabase().getMariadb().setAutoUpdate(true);
        });
        versionReader.versions.put("front-app", List.of("1.0.0"));
        versionReader.versions.put("mariadb", List.of("1.0.0"));

        Map<DependantResourceType, RunUpdateResult> results = ContainerUpdate.runContainerUpdates(resource, versionReader, patchResource);

        assertTrue(results.containsKey(DependantResourceType.FRONT_APP));
        assertTrue(results.containsKey(DependantResourceType.MARIADB));
        assertFalse(results.containsKey(DependantResourceType.DB_MANAGER));
        assertFalse(results.containsKey(DependantResourceType.MAXSCALE));
    }

    @Test
    void when_noComponentsUpdated_patchResourceIsNotCalled() {
        SconeOsvScanner resource = buildResource(spec -> {
            spec.getScanner().getFrontApp().setAutoUpdate(true);
            spec.getScanner().getFrontApp().setImageVersion("2.0.0");
            spec.getScanner().getDbManager().setAutoUpdate(true);
            spec.getScanner().getDbManager().setImageVersion("3.0.0");
        });
        versionReader.versions.put("front-app", List.of("2.0.0"));
        versionReader.versions.put("db-manager", List.of("3.0.0"));

        ContainerUpdate.runContainerUpdates(resource, versionReader, patchResource);

        assertEquals(0, patchCallCount.get());
    }

    @Test
    void when_allFourComponentsAutoUpdateEnabled_allAreUpdatedAndPatchCalledOnce() {
        SconeOsvScanner resource = buildResource(spec -> {
            spec.getScanner().getFrontApp().setAutoUpdate(true);
            spec.getScanner().getFrontApp().setImageVersion("1.0.0");
            spec.getScanner().getDbManager().setAutoUpdate(true);
            spec.getScanner().getDbManager().setImageVersion("1.0.0");
            spec.getDatabase().getMariadb().setAutoUpdate(true);
            spec.getDatabase().getMariadb().setImageVersion("1.0.0");
            spec.getDatabase().getMaxscale().setAutoUpdate(true);
            spec.getDatabase().getMaxscale().setImageVersion("1.0.0");
        });
        versionReader.versions.put("front-app", List.of("2.0.0"));
        versionReader.versions.put("db-manager", List.of("2.0.0"));
        versionReader.versions.put("mariadb", List.of("2.0.0"));
        versionReader.versions.put("maxscale", List.of("2.0.0"));

        Map<DependantResourceType, RunUpdateResult> results = ContainerUpdate.runContainerUpdates(resource, versionReader, patchResource);

        assertEquals(4, results.size());
        assertTrue(results.values().stream().allMatch(r -> r.getLastUpdateStatus() == UpdateStatus.SUCCESS_UPDATED));
        assertEquals(1, patchCallCount.get());
    }

    @Test
    void when_oneComponentFailsAndAnotherSucceeds_patchIsStillCalled() {
        SconeOsvScanner resource = buildResource(spec -> {
            spec.getScanner().getFrontApp().setAutoUpdate(true);
            spec.getScanner().getFrontApp().setImageVersion("1.0.0");
            spec.getScanner().getDbManager().setAutoUpdate(true);
            spec.getScanner().getDbManager().setImageVersion("1.0.0");
        });
        versionReader.throwingImages.add("front-app");
        versionReader.versions.put("db-manager", List.of("2.0.0"));

        Map<DependantResourceType, RunUpdateResult> results = ContainerUpdate.runContainerUpdates(resource, versionReader, patchResource);

        assertEquals(UpdateStatus.FAILED_UNKNOWN_ERROR, results.get(DependantResourceType.FRONT_APP).getLastUpdateStatus());
        assertEquals(UpdateStatus.SUCCESS_UPDATED, results.get(DependantResourceType.DB_MANAGER).getLastUpdateStatus());
        assertEquals(1, patchCallCount.get());
    }


    private SconeOsvScanner buildResource(Consumer<SconeOsvScannerSpec> configure) {
        FrontAppSpec frontApp = new FrontAppSpec();
        frontApp.setImageName("front-app");
        frontApp.setImageVersion("1.0.0");
        frontApp.setMemory("256Mi");
        frontApp.setSconeConfigId("config-id");

        DbManagerSpec dbManager = new DbManagerSpec();
        dbManager.setImageName("db-manager");
        dbManager.setImageVersion("1.0.0");
        dbManager.setMemory("256Mi");
        dbManager.setSconeConfigId("config-id");

        MariadbSpec mariadb = new MariadbSpec();
        mariadb.setImageName("mariadb");
        mariadb.setImageVersion("1.0.0");
        mariadb.setMemory("256Mi");
        mariadb.setSconeConfigId("config-id");
        mariadb.setStorageSize("1Gi");
        mariadb.setStorageClassName("standard");
        mariadb.setReplicaSconeConfigId("replica-config-id");

        MaxscaleSpec maxscale = new MaxscaleSpec();
        maxscale.setImageName("maxscale");
        maxscale.setImageVersion("1.0.0");
        maxscale.setMemory("256Mi");
        maxscale.setSconeConfigId("config-id");

        ScannerSpec scanner = new ScannerSpec();
        scanner.setFrontApp(frontApp);
        scanner.setDbManager(dbManager);

        DatabaseSpec database = new DatabaseSpec();
        database.setMariadb(mariadb);
        database.setMaxscale(maxscale);

        SconeOsvScannerSpec spec = new SconeOsvScannerSpec();
        spec.setCasAddress("localhost");
        spec.setScanner(scanner);
        spec.setDatabase(database);

        configure.accept(spec);

        SconeOsvScanner resource = new SconeOsvScanner();
        resource.setSpec(spec);
        return resource;
    }

    public class StubRegistryImageVersionReader implements RegistryImageVersionReader {
        public final Map<String, List<String>> versions = new HashMap<>();
        public final Set<String> throwingImages = new HashSet<>();

        @Override
        public List<String> getAvailableVersions(CommonRegistrySpec registrySpec, String imageName) throws Exception {
            if (throwingImages.contains(imageName)) {
                throw new Exception("Registry unavailable for " + imageName);
            }
            return versions.getOrDefault(imageName, List.of());
        }
    }
}
