#!/bin/sh
set -e

PORT="${PORT:-8080}"

echo "=============================================="
echo " Fiscalimplify - iniciando no Railway"
echo " PORT=${PORT}"
echo " SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE:-<nao definido>}"
echo " PGHOST=${PGHOST:-<nao definido>}"
if [ -n "${DATABASE_URL}" ] || [ -n "${DATABASE_PRIVATE_URL}" ]; then
  echo " DATABASE_URL=<definido>"
else
  echo " DATABASE_URL=<nao definido - vincule o Postgres!>"
fi
echo "=============================================="

exec java \
  -Dserver.port="${PORT}" \
  -Dserver.address=0.0.0.0 \
  -jar /app/app.jar
