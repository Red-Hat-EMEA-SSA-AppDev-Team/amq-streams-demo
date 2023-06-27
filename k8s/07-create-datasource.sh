#!/bin/bash

## Service account
oc apply -f k8s/06-graphana-auth.yaml -n my-kafka

## Token
export GRAFANA_ACCESS_TOKEN=`oc create token grafana-serviceaccount --duration 720h`

## Config map
oc create configmap grafana-config -n my-kafka --from-literal=datasource.yaml="$(cat k8s/07-datasource.yaml.template | envsubst)"