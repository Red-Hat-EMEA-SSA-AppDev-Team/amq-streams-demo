#!/bin/bash

## Token
export GRAFANA_ACCESS_TOKEN=`oc create token grafana-serviceaccount --duration 720h`

## Config map
oc create configmap grafana-config --from-literal=datasource.yaml="$(cat k8s/monitor/01-datasource.yaml.template | envsubst)"