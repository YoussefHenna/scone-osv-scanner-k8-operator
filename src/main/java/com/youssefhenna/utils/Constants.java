package com.youssefhenna.utils;

public class Constants {

    public static final String DEPENDANT_LABEL_KEY = "app.kubernetes.io/managed-by";
    public static final String DEPENDANT_LABEL_VALUE = "scone-osv-scanner-operator";
    public static final String DEPENDANT_SELECTOR = DEPENDANT_LABEL_KEY + "=" + DEPENDANT_LABEL_VALUE;

    public static final int FRONT_APP_PORT = 8443;

    public static final String DB_MANAGER_DEPENDENT_NAME = "dbManager";
    public static final String FRONT_APP_DEPENDENT_NAME = "frontApp";
    public static final String FRONT_APP_SERVICE_DEPENDENT_NAME = "frontAppService";

    public static final String MARIADB_PRIMARY_PDB_DEPENDENT_NAME = "mariadbPrimaryPdb";
    public static final String MARIADB_REPLICA_PDB_DEPENDENT_NAME = "mariadbReplicaPdb";
    public static final String MARIADB_INIT_SCRIPTS_CONFIGMAP_DEPENDENT_NAME = "mariadbInitScriptsConfigMap";
    public static final String MAXSCALE_ADMIN_SERVICE_DEPENDENT_NAME = "maxscaleAdminService";
    public static final String MAXSCALE_SERVICE_DEPENDENT_NAME = "maxscaleService";
    public static final String MARIADB_PRIMARY_SERVICE_DEPENDENT_NAME = "mariadbPrimaryService";
    public static final String MARIADB_REPLICA_SERVICE_DEPENDENT_NAME = "mariadbReplicaService";
    public static final String MAXSCALE_DEPLOYMENT_DEPENDENT_NAME = "maxscaleDeployment";
    public static final String MARIADB_PRIMARY_STATEFULSET_DEPENDENT_NAME = "mariadbPrimaryStatefulSet";
    public static final String MARIADB_REPLICA_STATEFULSET_DEPENDENT_NAME = "mariadbReplicaStatefulSet";

    public static final int MAXSCALE_PORT = 3306;
    public static final int MAXSCALE_ADMIN_PORT = 8989;
    public static final int MARIADB_PRIMARY_PORT = 3306;
    public static final int MARIADB_REPLICA_PORT = 3307;

    public static final String MARIADB_PRIMARY_COMPONENT = "primary";
    public static final String MARIADB_REPLICA_COMPONENT = "replica";
    public static final String MAXSCALE_COMPONENT = "maxscale";

    public static String getDbManagerDeploymentName(String primaryName) {
        return primaryName + "-db-manager";
    }

    public static String getFrontAppDeploymentName(String primaryName) {
        return primaryName + "-front-app";
    }

    public static String getFrontAppServiceName(String primaryName) {
        return primaryName + "-front-service";
    }

    public static String getMariadbPrimaryName(String primaryName) {
        return primaryName + "-mariadb-primary";
    }

    public static String getMariadbReplicaName(String primaryName) {
        return primaryName + "-mariadb-replica";
    }

    public static String getMaxscaleDeploymentName(String primaryName) {
        return primaryName + "-maxscale";
    }

    public static String getMaxscaleServiceName(String primaryName) {
        return primaryName + "-maxscale-service";
    }

    public static String getMaxscaleAdminServiceName(String primaryName) {
        return primaryName + "-maxscale-admin-service";
    }

    public static String getMariadbPrimaryServiceName(String primaryName) {
        return primaryName + "-mariadb-primary-service";
    }

    public static String getMariadbReplicaServiceName(String primaryName) {
        return primaryName + "-mariadb-replica-service";
    }

    public static String getMariadbInitScriptsConfigMapName(String primaryName) {
        return primaryName + "-mariadb-init-scripts";
    }

}
