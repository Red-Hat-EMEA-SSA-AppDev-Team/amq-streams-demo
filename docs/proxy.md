# Kafka Proxy Demo

> [!NOTE]  
> This demo is built on the product example, tailored to this repository's context.

## Prerequisites

* Administrative access to the OpenShift cluster used to evaluate Streams for Apache Kafka Proxy.
* The following Operators must be installed in the OpenShift cluster:
  * Red Hat Streams for Apache Kafka Operator
  * Red Hat Streams for Apache Kafka Proxy Operator
  * Cert-manager Operator for Red Hat OpenShift
* Kafka cluster installed with no authentication or authorization, as per the instruction in this repository.

## TLS certificates

This example uses TLS between clients and the proxy. The Kustomization creates
the required certificates using a namespace-scoped self-signed issuer to keep
the setup simple. A self-signed issuer is not suitable for production use.

## Cert-manager

Check that **cert-manager** is running:

```sh
oc get pods -n cert-manager
```

## Proxy setup

Kafka Proxy uses an API architecture inspired by the Kubernetes Gateway API. It
separates proxy functionality across five custom resources (CRs).

Depending on whether you enable protocol filters and metrics scraping, a working
proxy deployment needs three to five custom resources.


| Custom Resource           | Necessity                | Purpose                                                               |
| :------------------------ | :----------------------- | :-------------------------------------------------------------------------------- |
| **`KafkaProxy`**          | **Required**             | Top-level resource defining the proxy deployment instance (managing the proxy Pods/containers, CPU/memory limits, worker thread counts, and network timeouts). |
| **`VirtualKafkaCluster`** | **Required**             | Defines the logical Kafka cluster exposed to clients. It binds together the `KafkaProxy`, `KafkaProxyIngress`, `KafkaService`, and optional `KafkaProtocolFilter` resources. |
| **`KafkaProxyIngress`**   | **Required**             | Defines network access and ingress routing (`clusterIP`, `loadBalancer`, or `openShiftRoute`) and TLS transport settings. |
| **`KafkaService`**        | **Required**             | Defines the target backend Kafka cluster (bootstrap addresses, TLS trust, client authentication to upstream brokers). |
| **`KafkaProtocolFilter`** | **Optional** (0 or more) | Specifies Layer 7 filters applied to Kafka traffic (e.g., Record Encryption, Record Validation, Entity Isolation, SASL Inspection, or Authorization). If omitted or left empty, traffic passes through unchanged. |

> While it is not a proxy CR, a `PodMonitor` is also used when integrating the
> proxy with OpenShift user-workload Prometheus monitoring.

Let's start with the bare minimum deployment for a **Pass-through Proxy**:

- `KafkaProxy`
- `VirtualKafkaCluster`
- `KafkaProxyIngress`
- `KafkaService`

In addition to the core resources, this deploys the certificate issuer, the
proxy endpoint certificate, and the PodMonitor configuration.

Create the resources:

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

1. Wait for the virtual-cluster ingress to receive a bootstrap address:
   ```sh
   oc wait  -n kafka-proxy virtualkafkacluster my-cluster --for=jsonpath='{.status.ingresses[0].bootstrapServer}'
   ```

2. Store the bootstrap address:
   ```sh
   BOOTSTRAP_SERVER=$(oc get -n kafka-proxy virtualkafkacluster my-cluster -o=jsonpath='{.status.ingresses[0].bootstrapServer}')
   ```

3. Store the self-signed CA certificate:
   ```sh
   CA=$(oc get secret -n kafka-proxy my-cluster-server-certificate -o go-template='{{if index .data "ca.crt"}}{{index .data "ca.crt" | base64decode}}{{end}}')
   ```

4. Produce records to the topic:

   ```sh
   printf 'IBM:100\nAPPLE:99\n' | oc run -n kafka-proxy -qi proxy-producer --image=registry.redhat.io/amq-streams/kafka-42-rhel9:3.2.2-7 --rm=true --restart=Never -- ./bin/kafka-console-producer.sh --bootstrap-server "${BOOTSTRAP_SERVER}" --topic trades --producer-property ssl.truststore.type=PEM --producer-property security.protocol=SSL --producer-property ssl.truststore.certificates="$CA"
   ```

5. Consume records through the proxy:

   ```sh
   oc run -n kafka-proxy proxy-consumer -qi --image=registry.redhat.io/amq-streams/kafka-42-rhel9:3.2.2-7 --rm=true --restart=Never -- ./bin/kafka-console-consumer.sh --bootstrap-server "${BOOTSTRAP_SERVER}" --topic trades --from-beginning --timeout-ms 10000 --command-property ssl.truststore.type=PEM --command-property security.protocol=SSL --command-property ssl.truststore.certificates="$CA"
   ```   

## Install Key Management Service (KMS)

1. Create a Vault root token and save it locally:
   ```sh
   cat /dev/urandom | LC_ALL=C tr -dc 'a-zA-Z0-9' | fold -w 32 | head -n 1 > vault.root.token
   export VAULT_TOKEN=$(cat vault.root.token)
   ```

2. Install Vault using Helm:
   ```sh
   helm repo add hashicorp https://helm.releases.hashicorp.com
   helm install vault hashicorp/vault --create-namespace --namespace=vault --version v0.31.0 --values k8s/vault/helm-dev-values.yaml --set server.dev.devRootToken=$VAULT_TOKEN --wait
   ```

3. Set `VAULT_ADDR` to the new instance:
   ```
   export VAULT_ADDR=$(oc get route -n vault vault --template='https://{{.spec.host}}')
   ```

4. Enable the Transit secrets engine:
   ```sh
   oc exec -n vault vault-0 -- vault secrets enable transit
   ```

5. Log in to Vault:
   ```sh
   oc exec -n vault vault-0 -- vault login $(cat vault.root.token)
   ```

6. Write the policy from the file:

   Note: Since the policy file is on your local machine, pipe its content directly into the pod:
   ```sh
   cat k8s/vault/proxy_encryption_filter_policy.hcl | oc exec -i -n vault vault-0 -- vault policy write proxy_encryption_filter_policy -
   ```

7. Create a token for the proxy:
   ```sh
   oc exec -n vault vault-0 -- vault token create -display-name "proxy encryption filter" -policy=proxy_encryption_filter_policy -no-default-policy -orphan -field=token > vault.encryption.token
   ```

8. Create a secret containing the Vault token:
   ```sh
   oc create secret generic proxy-encryption-kms-secret -n kafka-proxy --from-file=encryption-vault-token.txt=vault.encryption.token
   ```

## Add the Record Encryption filter

The Record Encryption filter provides encryption at rest for Apache Kafka while
remaining transparent to clients and brokers. It encrypts records before they
reach the broker and decrypts them before they are returned to consumers.

1. Create the key for the `trades` topic using your KMS provider's instructions:

   Vault:
   ```sh
   oc exec -n vault vault-0 -- vault write -f transit/keys/KEK_trades
   ```

2. Create the Kafka protocol filter:

   ```sh
   oc apply -f k8s/proxy/base/02.KafkaProtocolFilter.encryption.yaml
   ```

3. Attach the filter to the virtual Kafka cluster:

   ```sh
   oc patch -n kafka-proxy virtualkafkacluster my-cluster --type=merge \
     -p '{"spec":{"filterRefs":[{"name":"encryption"}]}}'
   ```

   The proxy operator reconciles this change and rolls out the proxy Pods. Wait
   for the rollout to complete before producing encrypted records:

   ```sh
   oc rollout status -n kafka-proxy deployment/my-proxy --timeout=5m
   ```


## Proxy interactions with encryption active

Use a unique marker so that you can distinguish the encrypted test record from
existing records in `trades`:

```sh
MESSAGE="encryption-check-$(date +%s)"
printf '%s\n' "$MESSAGE" | oc run -n kafka-proxy -qi encrypted-producer \
  --image=registry.redhat.io/amq-streams/kafka-42-rhel9:3.2.2-7 --rm=true --restart=Never -- \
  ./bin/kafka-console-producer.sh --bootstrap-server "${BOOTSTRAP_SERVER}" --topic trades \
  --producer-property ssl.truststore.type=PEM \
  --producer-property security.protocol=SSL \
  --producer-property ssl.truststore.certificates="$CA"
```

Consume through the proxy. The filter decrypts the record, so the marker is
visible in the output:

```sh
oc run -n kafka-proxy -qi encrypted-proxy-consumer \
  --image=registry.redhat.io/amq-streams/kafka-42-rhel9:3.2.2-7 --rm=true --restart=Never -- \
  ./bin/kafka-console-consumer.sh --bootstrap-server "${BOOTSTRAP_SERVER}" --topic trades \
  --from-beginning --timeout-ms 10000 \
  --command-property ssl.truststore.type=PEM \
  --command-property security.protocol=SSL \
  --command-property ssl.truststore.certificates="$CA" | grep -F "$MESSAGE"
```

To verify that the broker stores ciphertext, consume the same topic directly,
bypassing the proxy. The backend listener in this demo is plaintext on port
9092. The marker must not appear in this output; it is encrypted before it
reaches the broker:

```sh
set -o pipefail
oc run -n my-kafka -qi encrypted-direct-consumer \
  --image=registry.redhat.io/amq-streams/kafka-42-rhel9:3.2.2-7 --rm=true --restart=Never -- \
  ./bin/kafka-console-consumer.sh \
  --bootstrap-server my-cluster-kafka-bootstrap.my-kafka.svc.cluster.local:9092 \
  --topic trades --from-beginning --timeout-ms 10000 | grep -Fq "$MESSAGE"
statuses=("${PIPESTATUS[@]}")

if [ "${statuses[0]}" -ne 0 ]; then
  echo "The direct consumer failed."
  exit "${statuses[0]}"
elif [ "${statuses[1]}" -eq 0 ]; then
  echo "ERROR: plaintext reached the broker."
  exit 1
else
  echo "Success: the broker does not expose the plaintext marker."
fi
```

## Clean up

Delete the `trades` topic before removing the proxy. This topic is created on
the backend Kafka cluster, so deleting the proxy does not remove it:

```sh
oc exec -n my-kafka my-cluster-kafka-0 -- \
  bin/kafka-topics.sh --bootstrap-server localhost:9092 --delete --topic trades
```

Remove the encryption filter and its token secret, then uninstall Vault:

```sh
oc delete -n kafka-proxy kafkaprotocolfilter encryption --ignore-not-found
oc delete -n kafka-proxy secret proxy-encryption-kms-secret --ignore-not-found
helm uninstall vault --namespace vault --ignore-not-found
oc delete namespace vault --ignore-not-found
```

Remove the proxy resources, including the `kafka-proxy` namespace:

```sh
oc delete -k k8s/proxy
```

Finally, remove the local Vault tokens:

```sh
rm -f vault.root.token vault.encryption.token
```

