#!/usr/bin/env bash


which date || {
    echo "\"date\" is not installed! Exiting..."
    exit 1
}

# MariaDB data location.
export MYSQL_DATADIR=/var/lib/mysql


mkdir -p $MYSQL_DATADIR
chown -R mysql:mysql $MYSQL_DATADIR

# We might need to switch between different SCONE_CONFIG_IDs
# during MariaDB's bootstrap and startup.
if [[ -z "$BASE_SCONE_CONFIG_ID" ]]; then
    echo "[ $(date) | MARIADB-SCONE REPLICA SETUP ] - BASE_SCONE_CONFIG_ID env var is not defined! Exiting."
    exit 1
fi
export REPLICA_SETUP_SCONE_CONFIG_ID="$BASE_SCONE_CONFIG_ID/setup_replica_conf"

if [[ ! -e "${MYSQL_DATADIR}/ibdata1" ]]; then
    echo "[ $(date) | MARIADB-SCONE REPLICA SETUP ] - no file '${MYSQL_DATADIR}/ibdata1' detected - this indicates that mariadb was not initialized yet"
    exit 1
fi

echo "[ $(date) | MARIADB-SCONE REPLICA SETUP ] - starting replica setup"

echo "[ $(date) | MARIADB-SCONE REPLICA SETUP ] - waiting for "/run/mysqld/mysqld.sock" to be available (120s max)";
COUNTER=0;
while [[ ! -e /run/mysqld/mysqld.sock ]]; do
    COUNTER=$((COUNTER + 1));
    sleep 1;
    if [ $COUNTER -eq 120 ]; then
        echo -n "Limit of 120s waiting reached. Either MariaDB server did not come online in time to "
        echo -n "be configured by SQL script (injected in the security policy) or it could not"
        echo "expose the '/run/mysqld/mysqld.sock' used to verify it was up running."
        exit 1;
    fi;
done;

echo "[ $(date) | MARIADB-SCONE REPLICA SETUP ] - running mysql to configure replication on replica side"
# Configure with .sql script is injected by CAS!
if SCONE_CONFIG_ID="$REPLICA_SETUP_SCONE_CONFIG_ID" mysql; then
    echo "[ $(date) | MARIADB-SCONE REPLICA SETUP ] - replica configured - replica side - check the logs to ensure replica could connect to primary mariadb"
else
    echo "[ $(date) | MARIADB-SCONE REPLICA SETUP ] - something went wrong with replica side replication setup - check the log messages"
fi

echo "[ $(date) | MARIADB-SCONE REPLICA SETUP ] - running mysql to configure replication on primary side"

if SCONE_CONFIG_ID="$BASE_SCONE_CONFIG_ID/setup_replica_on_primary_conf" mysql; then
    echo "[ $(date) | MARIADB-SCONE REPLICA SETUP ] - replica configured - primary side - check the logs to ensure replica could connect to primary mariadb"
else
    echo "[ $(date) | MARIADB-SCONE REPLICA SETUP ] - something went wrong with primary side replication setup - check the log messages"
fi
