# kafka-consumer Project

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: https://quarkus.io/ .

## Goal of this project

Read messages from Kafka and ensure the consistency:

- no missing messages
- no duplicate messages

It operates in two modes:

- in memory: keep track of the missing messages in a TreeMap
- persistence: store messages in a PostgreSQL DB

### DB operations

Check missing messages (gaps)

```sql
select key + 1 as gap_start, 
       next_nr - 1 as gap_end
from (
  select key, 
         lead(key) over (order by key) as next_nr
  from event
) nr
where key + 1 <> next_nr;
```

Check duplicated messages

```sql
select * from (
select key, count(id) as c From event group by key) count_key
where c > 1
```

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:
```shell script
./mvnw compile quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at http://localhost:8080/q/dev/.

### Disable DevServices

This application requires a Kafka broker up and running to work properly.
By default, quarkus dev mode initialize the kafka services, but you can override this behavior:

1. Disable dev services in `application.properties`:

```
quarkus.devservices.enabled=false
```

2. Start the kafka container:

```sh
podman run --rm -it -p 9092:9092 -e kafka.bootstrap.servers=OUTSIDE://localhost:9092 docker.io/vectorized/redpanda
```

## Packaging and running the application

The application can be packaged using:
```shell script
./mvnw package
```
It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:
```shell script
./mvnw package -Dquarkus.package.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using: 
```shell script
./mvnw package -Pnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using: 
```shell script
./mvnw package -Pnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/kafka-consumer-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult https://quarkus.io/guides/maven-tooling.

## Deploy in OpenShift

```sh
./mvnw install -Dquarkus.kubernetes.deploy=true
```

## Provided Code

### RESTEasy JAX-RS

Easily start your RESTful Web Services

[Related guide section...](https://quarkus.io/guides/getting-started#the-jax-rs-resources)
