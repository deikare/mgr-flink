#!/bin/bash

http_status=$(curl -s -o /dev/null -w "%{http_code}" "${FLINK_ADDRESS}")

#todo read from maven steps name of created jar instead of hardcoding it as env $JAR_FILE

if [ "${http_status}" != "200" ]; then
  "${FLINK_BIN_PATH}"/start-cluster.sh
fi

"$FLINK_BIN_PATH"/flink run "$JAR_FILE"
