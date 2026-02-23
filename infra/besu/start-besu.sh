#!/usr/bin/env bash
# Besu node startup script

set -euo pipefail

BESU_HOME="${BESU_HOME:-.}"
NETWORK="${NETWORK:-development}"
NODE_NAME="${NODE_NAME:-besu-node}"
RPC_PORT="${RPC_PORT:-8545}"
WS_PORT="${WS_PORT:-8546}"
P2P_PORT="${P2P_PORT:-30303}"

echo "Starting Besu node: $NODE_NAME"
echo "Network: $NETWORK"
echo "RPC Port: $RPC_PORT"
echo "P2P Port: $P2P_PORT"

# Set JVM options
export BESU_OPTS="-Xmx2g -Xms1g"

# Start Besu with appropriate configuration
besu \
  --node-private-key-file="$BESU_HOME/node-key" \
  --genesis-file="$BESU_HOME/genesis.json" \
  --network-id=1337 \
  --p2p-port="$P2P_PORT" \
  --rpc-http-enabled \
  --rpc-http-interface=0.0.0.0 \
  --rpc-http-port="$RPC_PORT" \
  --rpc-http-api=ETH,NET,WEB3,ADMIN,DEBUG,MINER,TXPOOL \
  --rpc-ws-enabled \
  --rpc-ws-interface=0.0.0.0 \
  --rpc-ws-port="$WS_PORT" \
  --rpc-ws-api=ETH,NET,WEB3,ADMIN,DEBUG,MINER,TXPOOL \
  --data-path="$BESU_HOME/data" \
  --logging=INFO \
  --host-allowlist=localhost,127.0.0.1,besu-node \
  "$@"
