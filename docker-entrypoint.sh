#!/bin/sh
set -e

PORT="${PORT:-8080}"

JVM_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=65.0 -XX:InitialRAMPercentage=25.0 \
-XX:+UseG1GC -XX:+UseStringDeduplication -XX:MaxMetaspaceSize=192m -XX:ReservedCodeCacheSize=64m \
-XX:MaxGCPauseMillis=200 -Xss512k -Dspring.backgroundpreinitializer.ignore=true"

POSTGRES_HOST="${PGHOST:-${DB_HOST}}"

if [ -n "${SPRING_DATASOURCE_URL}" ] && printf '%s' "${SPRING_DATASOURCE_URL}" | grep -q '\${'; then
  echo " AVISO: SPRING_DATASOURCE_URL com placeholders - removendo"
  unset SPRING_DATASOURCE_URL
fi

if [ -n "${POSTGRES_HOST}" ] && [ "${POSTGRES_HOST}" != "localhost" ] && [ "${POSTGRES_HOST}" != "127.0.0.1" ]; then
  PG_PORT="${PGPORT:-${DB_PORT:-5432}}"
  PG_DB="${PGDATABASE:-${DB_DATABASE:-railway}}"
  PG_USER="${PGUSER:-${DB_USERNAME:-postgres}}"
  PG_PASS="${PGPASSWORD:-${DB_PASSWORD:-}}"
  export SPRING_DATASOURCE_URL="jdbc:postgresql://${POSTGRES_HOST}:${PG_PORT}/${PG_DB}"
  export SPRING_DATASOURCE_USERNAME="${PG_USER}"
  export SPRING_DATASOURCE_PASSWORD="${PG_PASS}"
  echo " JDBC: jdbc:postgresql://${POSTGRES_HOST}:${PG_PORT}/${PG_DB}"
fi

echo "=============================================="
echo " Fiscalimplify - iniciando no Railway"
echo " PORT=${PORT}"
echo " SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE:-<nao definido>}"

if [ -n "${POSTGRES_HOST}" ]; then
  echo " DB_HOST/PGHOST=${POSTGRES_HOST} (Postgres OK)"
elif [ -n "${DATABASE_PRIVATE_URL}" ] || [ -n "${DATABASE_URL}" ]; then
  echo " DATABASE_URL=<definido> (Postgres OK)"
else
  echo " ERRO: Postgres NAO configurado (defina DB_HOST ou PGHOST)"
  exit 1
fi

if [ -n "${JAVA_TOOL_OPTIONS}" ]; then
  echo " AVISO: remova JAVA_TOOL_OPTIONS das Variables do Railway."
fi
echo " Iniciando Java..."
echo "=============================================="

exec java ${JVM_OPTS} \
  -Dserver.port="${PORT}" \
  -Dserver.address=0.0.0.0 \
  -jar /app/app.jar
