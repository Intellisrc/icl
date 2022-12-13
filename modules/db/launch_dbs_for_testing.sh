#!/bin/bash
if [[ $1 == "clean" ]]; then
    dbs="mysql_test mariadb_test firebird_test oracle_test postgres_test sqlserver_test"
    docker stop $dbs && docker rm $dbs
    exit
fi
# MySQL
if [[ $1 == "" || $1 == "mysql" ]]; then
docker run --name mysql_test \
  -e "MYSQL_ROOT_PASSWORD=rootpassword" \
  -e "MYSQL_DATABASE=test" \
  -e "MYSQL_USER=test" \
  -e "MYSQL_PASSWORD=test" \
  -p 127.0.0.1:33006:3306 \
  -d mysql
fi
# MariaDB
if [[ $1 == "" || $1 == "mariadb" ]]; then
docker run --name mariadb_test \
  -e "MYSQL_ROOT_PASSWORD=rootpassword" \
  -e "MYSQL_DATABASE=test" \
  -e "MYSQL_USER=test" \
  -e "MYSQL_PASSWORD=test" \
  -p 127.0.0.1:33007:3306 \
  -d mariadb
fi
# Oracle
if [[ $1 == "" || $1 == "oracle" ]]; then
docker run --name oracle_test \
  -e "ORACLE_PASSWORD=test" \
  -e "APP_USER=test" \
  -e "APP_USER_PASSWORD=test" \
  -p 127.0.0.1:31521:1521 \
  -d gvenzl/oracle-xe:21-slim
echo "Oracle may take a few minutes to be available. You can check the logs with:"
echo "docker logs -f oracle_test"
fi
# Oracle Standard Edition
if [[ $1 == "oracle-12" ]]; then
# You must login into container-registry.oracle.com and go to Database
# and accept the licence terms before be able to download
# It requires 25GB of HDD and 4GB of memory
docker login container-registry.oracle.com
docker run --name oracle_test \
  --shm-size="4g" \
  -e "DB_SID=XEPDB" \
  -e "DB_PASSWD=test" \
  -e "DB_PDB=XEPDB1" \
  -e "DB_BUDLE=basic" \
  -e "ORACLE_SID=XEPDB" \
  -e "ORACLE_PWD=test" \
  -e "ORACLE_PDB=XEPDB1" \
  -p 127.0.0.1:31521:1521 \
  -d container-registry.oracle.com/database/standard:12.1.0.2
echo "Oracle may take a few minutes to be available (Wait until it displays it is ready): "
echo "docker logs -f oracle_test"
read -p "Press ENTER when oracle is ready, to create initial database."
docker exec -it oracle_test sh -c 'echo "CREATE USER test IDENTIFIED BY test;"|/u01/app/oracle/product/12.1.0/dbhome_1/bin/sqlplus system/test@XEPDB1'
docker exec -it oracle_test sh -c 'echo "GRANT ALL PRIVILEGES TO test;"|/u01/app/oracle/product/12.1.0/dbhome_1/bin/sqlplus system/test@XEPDB1'
fi
# PostgresSQL
if [[ $1 == "" || $1 == "postgres" ]]; then
docker run --name postgres_test \
  -e "POSTGRES_PASSWORD=randompass" \
  -e "POSTGRES_USER=test" \
  -e "POSTGRES_PASSWORD=test" \
  -p 127.0.0.1:35432:5432 \
  -d postgres
fi
# SQL Server
if [[ $1 == "" || $1 == "sqlserver" ]]; then
docker run --name sqlserver_test \
  -e "ACCEPT_EULA=Y" \
  -e "SA_PASSWORD=o2Aksm.A23asl" \
  -p 127.0.0.1:31433:1433 \
  -d mcr.microsoft.com/mssql/server:2019-latest
  echo "Waiting to SQL Server to start ...."
  sleep 30 # Wait until they are up
  while ! docker exec -it sqlserver_test /opt/mssql-tools/bin/sqlcmd -S localhost -U SA -P "o2Aksm.A23asl" -Q "CREATE DATABASE test"
  do
    sleep 10
  done
fi
# Firebird
if [[ $1 == "" || $1 == "firebird" ]]; then
docker run --name firebird_test \
  -e "ISC_PASSWORD=test" \
  -e "FIREBIRD_DATABASE=test" \
  -e "FIREBIRD_USER=test" \
  -e "FIREBIRD_PASSWORD=test" \
  -p 127.0.0.1:33050:3050 \
  -d jacobalberty/firebird:3.0
fi
# H2
if [[ $1 == "" || $1 == "h2" ]]; then
docker run --name h2_test \
  -e "ISC_PASSWORD=test" \
  -e "FIREBIRD_DATABASE=test" \
  -e "FIREBIRD_USER=test" \
  -e "FIREBIRD_PASSWORD=test" \
  -p 127.0.0.1:33050:3050 \
  -d jacobalberty/firebird:3.0
fi
# HyperSQL
if [[ $1 == "" || $1 == "hsqldb" ]]; then
docker run --name hsqldb_test \
  -e "HSQLDB_DATABASE_NAME=test" \
  -e "HSQLDB_USER=test" \
  -e "HSQLDB_PASSWORD=test" \
  -e "HSQLDB_TRACE=true" \
  -e "HSQLDB_REMOTE=true" \
  -e "HSQLDB_SILENT=false" \
  -p 127.0.0.1:39001:9001 \
  -d datagrip/hsqldb  # 3years old
fi
# Informix
#  "USER=informix"
#  "PASSWORD=in4mix"
if [[ $1 == "" || $1 == "informix" ]]; then
docker run --name informix_test \
  -e "LICENSE=accept" \
  -p 127.0.0.1:39088:9088 \
  -d ibmcom/informix-developer-database:latest
fi
