#!/usr/bin/env bash

set -e

# print an error message on error exit
trap 'last_command=$current_command; current_command=$BASH_COMMAND' DEBUG
trap 'if [ $? -ne 0 ]; then echo "\"${last_command}\" command failed - exiting."; fi' EXIT

which date || {
    echo "\"date\" is not installed! Exiting..."
    exit 1
}

# MariaDB data location.
export MYSQL_DATADIR=/var/lib/mysql

if [[ ! -z "${SCONE_HASH}" ]]; then
    exec mysqld
fi

mkdir -p $MYSQL_DATADIR
chown -R mysql:mysql $MYSQL_DATADIR

# We might need to switch between different SCONE_CONFIG_IDs
# during MariaDB's bootstrap and startup.
if [[ -z "$BASE_SCONE_CONFIG_ID" ]]; then
    echo "[ $(date) | MARIADB-SCONE INIT ] - BASE_SCONE_CONFIG_ID env var is not defined! Exiting."
    exit 1
fi
export BOOTSTRAP_CONFIG_ID="$BASE_SCONE_CONFIG_ID/bootstrap"
export PRE_CREATE_USER_CONFIG_ID="$BASE_SCONE_CONFIG_ID/db_before_setup"
export CREATEUSER_CONFIG_ID="$BASE_SCONE_CONFIG_ID/create_user"
export DB_SCONE_CONFIG_ID="$BASE_SCONE_CONFIG_ID/db"

# Each step of MariaDB startup is attested, so
# we need to switch between different SCONE_CONFIG_IDs.
#     - BOOTSTRAP_CONFIG_ID: bootstrap config ID
#     - PRE_CREATE_USER_CONFIG_ID: mysqld before user and SQL setup
#     - CREATEUSER_CONFIG_ID: mysql client with .sql for user creation and
#       database setup
#     - DB_SCONE_CONFIG_ID: Config ID used for the final mysqld to run. It runs
#       without socket plugin enabled

if [[ ! -e "${MYSQL_DATADIR}/ibdata1" ]]; then
    echo "[ $(date) | MARIADB-SCONE INIT | STEP 1 ] - bootstrap using mysql_install_db"
    export SCONE_CONFIG_ID=$BOOTSTRAP_CONFIG_ID

    rm -rf ${MYSQL_DATADIR}/* /tmp/* /var/tmp/*
    su mysql -c 'mysql_install_db --user=mysql --datadir=${MYSQL_DATADIR} --rpm --innodb-use-native-aio=0'
    rm -rf ${MYSQL_DATADIR}/test

    echo "[ $(date) | MARIADB-SCONE INIT | STEP 2.1 ] - setup server to create user config via sql script"
    export SCONE_CONFIG_ID=$PRE_CREATE_USER_CONFIG_ID
    su mysql -c 'mysqld --innodb-use-native-aio=0 --datadir=${MYSQL_DATADIR}' &
    MYSQLD_PID=$!

    echo "[ $(date) | MARIADB-SCONE INIT | STEP 2.2 ] - waiting for "/run/mysqld/mysqld.sock" to be available (120s max)";
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

    echo "[ $(date) | MARIADB-SCONE INIT | STEP 3 ] - executing mariadb-client to set mariadb up"
    export SCONE_CONFIG_ID=$CREATEUSER_CONFIG_ID
    # Configure with .sql script is injected by CAS!
    mysql

    kill $MYSQLD_PID
    wait $MYSQLD_PID

    echo "[ $(date) | MARIADB-SCONE INIT ] - database initialized"
else
    echo "[ $(date) | MARIADB-SCONE INIT ] - file '${MYSQL_DATADIR}/ibdata1' detected - this indicates an already boostrapped instance of mariadb"
fi

# Final phase of startup or bootstrap not needed. Use final config ID.
export SCONE_CONFIG_ID=$DB_SCONE_CONFIG_ID

echo "[ $(date) | MARIADB-SCONE INIT ] - starting bootstrapped mariadb"
exec su mysql -c 'mysqld --innodb-use-native-aio=0 --datadir=${MYSQL_DATADIR}'