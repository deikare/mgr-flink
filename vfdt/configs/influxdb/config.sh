#!/bin/bash

ORG="my-org"
TOKEN="my-token"
USERNAME="admin"
PASSWORD="admin123"
HOST="http://influxdb:8086"

CONFIG_PATH="/etc/influxdb/configs"

influx setup --bucket data --host $HOST -t $TOKEN -o $ORG --username=$USERNAME --password=$PASSWORD -f


influx bucket list --host $HOST -t $TOKEN -o $ORG

influx template validate --file $CONFIG_PATH/templates -R -e json --host $HOST

# TODO add new template + make start time dynamic
influx apply --host $HOST -o $ORG -t $TOKEN --file $CONFIG_PATH/templates -R --force yes #todo add check whether applied template already exists

influx dashboards --host $HOST -o $ORG -t $TOKEN