#!/bin/bash

http_status=$(curl -s -o /dev/null -w "%{http_code}" "${FLINK_ADDRESS}")

if [ "${http_status}" != "200" ]; then
  "${FLINK_BIN_PATH}"/start-cluster.sh
fi
