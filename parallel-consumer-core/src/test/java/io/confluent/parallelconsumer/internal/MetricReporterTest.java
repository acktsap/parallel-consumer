package io.confluent.parallelconsumer.internal;

/*-
 * Copyright (C) 2020-2023 Confluent, Inc.
 */

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static io.confluent.parallelconsumer.internal.MetricReporter.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class MetricReporterTest {

    private MeterRegistry meterRegistry;

    private MetricReporter metricReporter;

    @BeforeEach
    void setup() {
        this.meterRegistry = new SimpleMeterRegistry();
        this.metricReporter = new MetricReporter(this.meterRegistry);
    }

    @Test
    void reportRecordPollCount() {
        int count = ThreadLocalRandom.current().nextInt();
        metricReporter.reportRecordPollCount(count);

        final Optional<Double> actual = meterRegistry.getMeters().stream()
                .filter(it -> it.getId().getName().equals(WORKING_POOL_METRIC_NAME))
                .map(Gauge.class::cast)
                .map(Gauge::value)
                .findFirst();
        assertThat(actual).hasValue((double) count);
    }

    @Test
    void reportOffsetCommit() {
        long offset = ThreadLocalRandom.current().nextLong();
        String topic = UUID.randomUUID().toString();
        int partition = ThreadLocalRandom.current().nextInt();
        metricReporter.reportOffsetCommit(offset, topic, partition);

        final Optional<Double> actual = meterRegistry.getMeters().stream()
                .filter(it -> it.getId().getName().equals(OFFSET_COMMIT_METRIC_NAME)
                        && Objects.equals(it.getId().getTag("topic"), topic)
                        && Objects.equals(it.getId().getTag("partition"), String.valueOf(partition))
                )
                .map(Gauge.class::cast)
                .map(Gauge::value)
                .findFirst();
        assertThat(actual).hasValue((double) offset);
    }

    @Test
    void reportActiveWorkerCount() {
        int count = ThreadLocalRandom.current().nextInt();
        metricReporter.reportActiveWorkerCount(() -> count);

        final Optional<Double> actual = meterRegistry.getMeters().stream()
                .filter(it -> it.getId().getName().equals(WORKER_ACTIVE_METRIC_NAME))
                .map(Gauge.class::cast)
                .map(Gauge::value)
                .findFirst();
        assertThat(actual).hasValue((double) count);
    }

    @Test
    void reportWorkerQueueSize() {
        int count = ThreadLocalRandom.current().nextInt();
        metricReporter.reportWorkerQueueSize(() -> count);

        final Optional<Double> actual = meterRegistry.getMeters().stream()
                .filter(it -> it.getId().getName().equals(WORKER_QUEUE_METRIC_NAME))
                .map(Gauge.class::cast)
                .map(Gauge::value)
                .findFirst();
        assertThat(actual).hasValue((double) count);
    }
}