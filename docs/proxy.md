# Kafka Proxy Demo

> [!NOTE]  
> This demo is built on the product example, tailored to this repository's context.

## Prerequisites

* Administrative access to the OpenShift Cluster being used to evaluate Streams for Apache Kafka Proxy
* The following Operators must be installed in the OpenShift Cluster
  * Red Hat Streams for Apache Kafka Operator
  * Red Hat Streams for Apache Kafka Proxy Operator
  * Cert-manager Operator for Red Hat OpenShift

---
* CLI for your chosen KMS - either Vault CLI (`vault`).
---

* Apache Kafka CLI tools (`kafka-console-producer.sh`, and `kafka-console-consumer.sh`) (found in the `bin` directory of the Streams for Apache Kafka on RHEL distribution).



## TLS certificates

This examples rely on TLS communication between the applications and the proxy. 
Cert-manager is used to create the required certificates.
In order to keep the examples simple, Cert-manager uses a self-signed issuer.
This is NOT suitable for production use.

## Certificates Manager

1. Check that the **cert-manager** is running:

```sh
oc get pods -n cert-manager
```

2. **Create a Self-Signed ClusterIssuer:** Cluster-wide availability.
A `ClusterIssuer` configures cert-manager to generate self-signed certificates across all namespaces without per-namespace setup.

```bash
cat <<EOF | oc apply -f -
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: selfsigned-issuer
spec:
  selfSigned: {}
EOF
```

## Proxy set up

The Kafka Proxy configuration is uses an API architecture inspired by the Kubernetes Gateway API.
It decomposes proxy functionality across **five distinct custom resources (CRs)**. 

Depending on your requirements (such as whether protocol filters or metrics scraping are enabled), **three to five custom resources** are needed to configure and run a working proxy deployment.


| Custom Resource           | Necessity                | Purpose                                                               |
| :------------------------ | :----------------------- | :-------------------------------------------------------------------------------- |
| **`KafkaProxy`**          | **Required**             | Top-level resource defining the proxy deployment instance (managing the proxy Pods/containers, CPU/memory limits, worker thread counts, and network timeouts). |
| **`VirtualKafkaCluster`** | **Required**             | Defines the logical Kafka cluster exposed to clients. It binds together the `KafkaProxy`, `KafkaProxyIngress`, `KafkaService`, and optional `KafkaProtocolFilter` resources. |
| **`KafkaProxyIngress`**   | **Required**             | Defines network access and ingress routing (`clusterIP`, `loadBalancer`, or `openShiftRoute`) and TLS transport settings. |
| **`KafkaService`**        | **Required**             | Defines the target backend Kafka cluster (bootstrap addresses, TLS trust, client authentication to upstream brokers). |
| **`KafkaProtocolFilter`** | **Optional** (0 or more) | Specifies Layer 7 filters applied to Kafka traffic (e.g., Record Encryption, Record Validation, Entity Isolation, SASL Inspection, or Authorization). If omitted or left empty, traffic passes through unchanged. |

*(Note: While not a proxy CR, a **`PodMonitor`** custom resource is also used if you integrate the proxy with OpenShift user-workload Prometheus monitoring.)*

Let's start with the bare minimum deployment for a **Pass-through Proxy**:

- `KafkaProxy`
- `VirtualKafkaCluster`
- `KafkaProxyIngress`
- `KafkaService`

Along with the previous core definitions, we deploy the certificate issuer, the certificate (for the proxy endpoint) and the pod monitor configuration.

Run the following command to create everything in one shot: 
```sh
oc apply -k k8s/proxy
```

The Kustomize configuration deploys the proxy into the `kafka-proxy` namespace.
The generated bootstrap service is therefore named
`my-cluster-my-ingress-bootstrap.kafka-proxy.svc.cluster.local`; this name is
included in the server certificate's DNS subject alternative names (SANs).

Wait for that certificate to be issued before starting a client:

```sh
oc wait -n kafka-proxy certificate/my-cluster-server-certificate \
  --for=condition=Ready --timeout=5m
```

## Proxy interactions



1. Wait for the ingress of the virtual cluster to have a bootstrap address assigned
   ```sh
   oc wait  -n kafka-proxy virtualkafkacluster my-cluster --for=jsonpath='{.status.ingresses[0].bootstrapServer}'
   ```

2. Get the bootstrap address
   ```sh
   BOOTSTRAP_SERVER=$(oc get -n kafka-proxy virtualkafkacluster my-cluster -o=jsonpath='{.status.ingresses[0].bootstrapServer}')
   ```

3. Get the self-signed certificate
   ```sh
   CA=$(oc get secret -n kafka-proxy my-cluster-server-certificate -o go-template='{{if index .data "ca.crt"}}{{index .data "ca.crt" | base64decode}}{{end}}')
   ```

   To confirm the certificate contains the bootstrap hostname, run:

   ```sh
   oc get secret -n kafka-proxy my-cluster-server-certificate \
     -o go-template='{{index .data "tls.crt" | base64decode}}' |
     openssl x509 -noout -ext subjectAltName
   ```

   alternative:

   ```sh
   set CA (oc get secret -n kafka-proxy my-cluster-server-certificate -o go-template='{{if index .data "ca.crt"}}{{index .data "ca.crt" | base64decode}}{{end}}')
   ```

4. Produce a message to the topic:

   ```sh
   echo 'IBM:100\nAPPLE:99' | oc run -n kafka-proxy -qi proxy-producer --image=registry.redhat.io/amq-streams/kafka-42-rhel9:3.2.2-7 --rm=true --restart=Never -- ./bin/kafka-console-producer.sh --bootstrap-server ${BOOTSTRAP_SERVER} --topic trades --producer-property ssl.truststore.type=PEM --producer-property security.protocol=SSL --producer-property ssl.truststore.certificates="${CA}"
   ```

5. Consume messages from the *proxy*:

   ```sh
   oc run -n kafka-proxy proxy-consumer -qi --image=registry.redhat.io/amq-streams/kafka-42-rhel9:3.2.2-7 --rm=true --restart=Never -- ./bin/kafka-console-consumer.sh  --bootstrap-server ${BOOTSTRAP_SERVER} --topic trades --from-beginning --timeout-ms 10000  --command-property ssl.truststore.type=PEM --command-property security.protocol=SSL --command-property ssl.truststore.certificates="${CA}"
   ```   
