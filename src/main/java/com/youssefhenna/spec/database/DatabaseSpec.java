package com.youssefhenna.spec.database;

import com.youssefhenna.model.RegistryCredentials;
import com.youssefhenna.spec.CommonRegistrySpec;
import io.fabric8.generator.annotation.Required;

public class DatabaseSpec extends CommonRegistrySpec {
    @Required
    private MaxscaleSpec maxscale;

    @Required
    private MariadbSpec mariadb;

    public MaxscaleSpec getMaxscale() {
        return maxscale;
    }

    public void setMaxscale(MaxscaleSpec maxscale) {
        this.maxscale = maxscale;
    }

    public MariadbSpec getMariadb() {
        return mariadb;
    }

    public void setMariadb(MariadbSpec mariadb) {
        this.mariadb = mariadb;
    }
}
