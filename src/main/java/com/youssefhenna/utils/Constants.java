package com.youssefhenna.utils;

public class Constants {

    public static final String DEPENDANT_LABEL_KEY = "app.kubernetes.io/managed-by";
    public static final String DEPENDANT_LABEL_VALUE = "scone-osv-scanner-operator";
    public static final String DEPENDANT_SELECTOR = DEPENDANT_LABEL_KEY + "=" + DEPENDANT_LABEL_VALUE;

    public static final int FRONT_APP_PORT = 8443;

    public static final String DB_MANAGER_DEPENDENT_NAME = "dbManager";
    public static final String FRONT_APP_DEPENDENT_NAME = "frontApp";
    public static final String FRONT_APP_SERVICE_DEPENDENT_NAME = "frontAppService";

    public static String getDbManagerDeploymentName(String primaryName) {
        return primaryName + "-db-manager";
    }

    public static String getFrontAppDeploymentName(String primaryName) {
        return primaryName + "-front-app";
    }

    public static String getFrontAppServiceName(String primaryName) {
        return primaryName + "-front-service";
    }

}
