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
  -e "MARIADB_ROOT_PASSWORD=rootpassword" \
  -e "MARIADB_DATABASE=test" \
  -e "MARIADB_USER=test" \
  -e "MARIADB_PASSWORD=test" \
  -p 127.0.0.1:33007:3306 \
  -d mariadb

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
# Oracle
if [[ $1 == "" || $1 == "oracle" ]]; then
docker run --name oracle_test \
  -e "ORACLE_PASSWORD=test" \
  -e "APP_USER=test" \
  -e "APP_USER_PASSWORD=test" \
  -p 127.0.0.1:31521:1521 \
  -d gvenzl/oracle-xe:21-slim
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
  sleep 60 # Wait until they are up
  docker exec -it sqlserver_test /opt/mssql-tools/bin/sqlcmd -S localhost -U SA -P "o2Aksm.A23asl" -Q "CREATE DATABASE test"
fi