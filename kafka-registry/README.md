# kafka-registry Project

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: https://quarkus.io/ .

## Reference documentation

This example is based on the following guides:

- Apicurio Registry - Avro ([guide](https://quarkus.io/guides/kafka-schema-registry-avro)): Use Apicurio as Avro schema registry
- SmallRye Reactive Messaging - Kafka Connector ([guide](https://quarkus.io/guides/kafka-reactive-getting-started)): Connect to Kafka with Reactive Messaging

## Quick usage guide

Launch the quarkus service:

```sh
mvn quarkus:dev
```

Launch the consumer:

```sh
curl -N http://localhost:8080/consumed-movies
```

Produce a record:

```sh
curl --header "Content-Type: application/json" \
     --request POST \
     --data '{"title":"The Shawshank Redemption","year":1994}' \
     http://localhost:8080/movies
```

## Architecture

This application is composed by a Kafka producer and a consumer:

- the producer is triggered by the following REST endpoint: `/movies`. See source code in: [MovieResource.java](src/main/java/com/redhat/ssa/example/MovieResource.java)
- the consumer is triggered by the following REST endpoint: `/consumed-movies`. See source code in: [ConsumedMovieResource.java](src/main/java/com/redhat/ssa/example/ConsumedMovieResource.java)

The payload is defined by an Avro file: [movie.avsc](src/main/avro/movie.avsc)