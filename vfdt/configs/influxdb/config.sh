#!/bin/bash

ORG="my-org"
TOKEN="my-token"
USERNAME="admin"
PASSWORD="admin123"
HOST="http://influxdb:8086"

CONFIG_PATH="/etc/influxdb/configs"

influx setup --bucket data --host=$HOST -t $TOKEN -o $ORG --username=$USERNAME --password=$PASSWORD -f -r 4h

#influx bucket create --host=$HOST -t $TOKEN -o $ORG -n data-downsampled -r 24h
#
#influx bucket create --host=$HOST -t $TOKEN -o $ORG -n data-downsampled2
#
#influx bucket create --host=$HOST -t $TOKEN -o $ORG -n logs
#
#influx bucket create --host=$HOST -t $TOKEN -o $ORG -n hubs

influx bucket list --host=$HOST -t $TOKEN -o $ORG

#influx task create --host=$HOST -t $TOKEN -o $ORG --file $CONFIG_PATH/basic-downsampler.flux
#
#influx task create --host=$HOST -t $TOKEN -o $ORG --file $CONFIG_PATH/secondary-downsampler.flux
#
#influx task list --host=$HOST -t $TOKEN -o $ORG