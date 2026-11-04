#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")/.."
[ ! -e .env ] || { echo '.env already exists; refusing to replace secrets'; exit 1; }
umask 077
for name in DB_PASSWORD RABBIT_PASSWORD REDIS_PASSWORD JWT_SECRET INTERNAL_TOKEN ADMIN_PASSWORD; do
  printf '%s=%s\n' "$name" "$(openssl rand -hex 32)" >> .env
done
echo 'Created .env with random secrets. Read ADMIN_PASSWORD locally to sign in as admin.'
