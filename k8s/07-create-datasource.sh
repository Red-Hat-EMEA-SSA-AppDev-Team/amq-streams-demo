#!/bin/bash
export GRAFANA_ACCESS_TOKEN=`oc create token grafana-serviceaccount`

echo "apiVersion: 1
datasources:
- name: Prometheus
  type: prometheus
  url: https://thanos-querier.openshift-monitoring.svc.cluster.local:9091
  access: proxy
  basicAuth: false
  withCredentials: false
  isDefault: true
  jsonData:
    timeInterval: 5s
    tlsSkipVerify: true
    httpHeaderName1: \"Authorization\"
  secureJsonData:
    httpHeaderValue1: \"Bearer ${GRAFANA_ACCESS_TOKEN}\"
  editable: true" > datasource.yaml
  
  oc create configmap grafana-config -n my-kafka --from-file=datasource.yaml

  rm datasource.yaml