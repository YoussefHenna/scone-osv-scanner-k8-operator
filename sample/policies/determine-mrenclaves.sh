#!/bin/bash
set -e

# usage: get_mrenclave image [cmd docker_run_modifiers]
# return: mrenclave, if successful.
#
# e.g. get_mrenclave image:tag mysqld "-e SCONE_HEAP=2G"
#
function get_mrenclave {
    mre=$(docker run $3 --rm -e SCONE_HASH=1 $1 $2)
    ret=$(echo $mre | grep -o -e "[0-9a-f]\{64\}")
    if [ -z "$ret" ]; then
        echo "[ERROR] could not determine mrenclave of "$1 $2
        exit 1
    fi
    echo $ret
}

# Script gets and displays all required MRENCLAVES
MARIADB_IMAGE=${MARIADB_IMAGE:-"registry.scontain.com/sconecuratedimages/apps:mariadb-alpine-scone5.0"}
MAXSCALE_IMAGE=${MAXSCALE_IMAGE:-"registry.scontain.com/sconecuratedimages/apps:maxscale-2.5-alpine-scone5"}
SOS_OSV_SCAN_IMAGE=${SOS_OSV_SCAN_IMAGE:-"registry.scontain.com/scone.cloud/sos-images/osvscan:5.10.0-rc.5"}
SOS_DB_MANAGER_IMAGE=${SOS_DB_MANAGER_IMAGE:-"registry.scontain.com/scone.cloud/sos-images/dbmanager:5.10.0-rc.5"}

echo "Pulling the latest images. Make sure you have access to all of them!"
for img in $MARIADB_IMAGE $SOS_OSV_SCAN_IMAGE $SOS_DB_MANAGER_IMAGE $MAXSCALE_IMAGE; do
    docker pull $img
done

echo "Determining the MRENCLAVES."

# Determine MRENCLAVE of latest images.
MRENCLAVE_MYSQLD=$(get_mrenclave $MARIADB_IMAGE mysqld "-e SCONE_HEAP=2G -e SCONE_ALLOW_DLOPEN=1 --entrypoint=""")
MRENCLAVE_MYSQL=$(get_mrenclave $MARIADB_IMAGE mysql "-e SCONE_HEAP=2G -e SCONE_ALLOW_DLOPEN=1 --entrypoint=""")
MRENCLAVE_PROBE=$(get_mrenclave $MARIADB_IMAGE mysqladmin "-e SCONE_HEAP=32M -e SCONE_ALLOW_DLOPEN=1 --entrypoint=""")
MRENCLAVE_BACKUP=$(get_mrenclave $MARIADB_IMAGE mysqldump "-e SCONE_HEAP=512M -e SCONE_ALLOW_DLOPEN=1 --entrypoint=""")
MRENCLAVE_MAXSCALE=$(get_mrenclave $MAXSCALE_IMAGE maxscale "-e SCONE_HASH=1 --entrypoint=""")
MRENCLAVE_SOS_OSV_SCAN=$(get_mrenclave $SOS_OSV_SCAN_IMAGE /bin/osvscan "-e SCONE_HASH=1")
MRENCLAVE_SOS_DB_MANAGER=$(get_mrenclave $SOS_DB_MANAGER_IMAGE /bin/osvdbmanager "-e SCONE_HASH=1")


cat << EOF
export MRENCLAVE_MYSQLD="$MRENCLAVE_MYSQLD" \\
export MRENCLAVE_MYSQL="$MRENCLAVE_MYSQL" \\
export MRENCLAVE_PROBE="$MRENCLAVE_PROBE" \\
export MRENCLAVE_MAXSCALE="$MRENCLAVE_MAXSCALE" \\
export MRENCLAVE_SOS_OSV_SCAN="$MRENCLAVE_SOS_OSV_SCAN" \\
export MRENCLAVE_SOS_DB_MANAGER="$MRENCLAVE_SOS_DB_MANAGER"
EOF


echo "OK."

