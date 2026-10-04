#!/usr/bin/env bash
# Prepares an Amazon Linux 2023 EC2 instance and starts the whole stack.
# Use it as EC2 "User data" or run it once over SSH:   sudo bash deploy/ec2-setup.sh
#
# Variables (all optional):
#   REPO_URL      git repository to clone   (default: https://github.com/CarlosJRF/kafka_S8.git)
#   REPO_BRANCH   branch to deploy          (default: main)
#   APP_DIR       where to clone it         (default: /opt/kafka-s8)
#   GATEWAY_PORT  public port of the gateway (default: 80)
set -euxo pipefail

REPO_URL="${REPO_URL:-https://github.com/CarlosJRF/kafka_S8.git}"
REPO_BRANCH="${REPO_BRANCH:-main}"
APP_DIR="${APP_DIR:-/opt/kafka-s8}"
GATEWAY_PORT="${GATEWAY_PORT:-80}"

# --- Docker Engine + Compose v2 plugin
dnf install -y docker git
systemctl enable --now docker
usermod -aG docker ec2-user || true

ARCH="$(uname -m)"   # x86_64 or aarch64 (Graviton)
PLUGINS=/usr/local/lib/docker/cli-plugins
mkdir -p "$PLUGINS"
if ! docker compose version > /dev/null 2>&1; then
  curl -fsSL "https://github.com/docker/compose/releases/latest/download/docker-compose-linux-${ARCH}" \
    -o "$PLUGINS/docker-compose"
  chmod +x "$PLUGINS/docker-compose"
fi

# --- 2 GB swap: seven JVMs + Kafka + PostgreSQL are tight on 4 GB instances
if ! swapon --show | grep -q /swapfile; then
  dd if=/dev/zero of=/swapfile bs=1M count=2048
  chmod 600 /swapfile
  mkswap /swapfile
  swapon /swapfile
  echo '/swapfile none swap defaults 0 0' >> /etc/fstab
fi

# --- Code
if [ -d "$APP_DIR/.git" ]; then
  git -C "$APP_DIR" fetch origin "$REPO_BRANCH"
  git -C "$APP_DIR" checkout -B "$REPO_BRANCH" "origin/$REPO_BRANCH"
else
  git clone --branch "$REPO_BRANCH" "$REPO_URL" "$APP_DIR"
fi
cd "$APP_DIR"
[ -f .env ] || cp .env.example .env
grep -q '^GATEWAY_PORT=' .env && sed -i "s/^GATEWAY_PORT=.*/GATEWAY_PORT=${GATEWAY_PORT}/" .env \
  || echo "GATEWAY_PORT=${GATEWAY_PORT}" >> .env

# --- Build the images (multistage, inside Docker) and start everything
docker compose up -d --build --wait
docker compose ps
