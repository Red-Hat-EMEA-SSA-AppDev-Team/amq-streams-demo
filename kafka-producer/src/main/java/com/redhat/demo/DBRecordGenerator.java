package com.redhat.demo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.temporal.ChronoUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.faulttolerance.Retry;

import io.agroal.api.AgroalDataSource;
import io.quarkus.arc.lookup.LookupIfProperty;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.reactive.messaging.kafka.KafkaRecord;

@ApplicationScoped
@LookupIfProperty(name = "sequence.db", stringValue = "true")
public class DBRecordGenerator extends AbstractRecordGenerator {

    @Inject
    AgroalDataSource dataSource;

    @Blocking
    @Retry(maxRetries = 2, delay = 1, delayUnit = ChronoUnit.SECONDS)
    @Override
    public KafkaRecord<Long, String> createRecord(Long tick) {
        Long lastKey = 0L;

        try (Connection connection = dataSource.getConnection()) {

            PreparedStatement stmt = connection.prepareStatement("SELECT NEXTVAL('event_seq')");
            stmt.execute();
            ResultSet resultSet = stmt.getResultSet();
            resultSet.next();
            lastKey = resultSet.getLong("nextval");
            resultSet.close();
            stmt.close();

            failureSimulation();

            return KafkaRecord.of(lastKey, "demo message " + lastKey);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}
