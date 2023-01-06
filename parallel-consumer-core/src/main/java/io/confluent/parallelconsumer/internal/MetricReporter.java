package io.confluent.parallelconsumer.internal;

/*-
 * Copyright (C) 2020-2023 Confluent, Inc.
 */

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;

import java.util.function.Supplier;

@RequiredArgsConstructor
final class MetricReporter {

    static final String PREFIX = "parallel.consumer.";
    static final String WORKING_POOL_METRIC_NAME = PREFIX + "record.poll";
    static final String WORKING_POOL_METRIC_DESCRIPTION = "The number of records which processed by broker.";
    static final String OFFSET_COMMIT_METRIC_NAME = PREFIX + "offset.commit";
    static final String OFFSET_COMMIT_METRIC_DESCRIPTION = "Last committed offset.";
    static final String WORKER_ACTIVE_METRIC_NAME = PREFIX + "worker.active";
    static final String WORKER_ACTIVE_METRIC_DESCRIPTION = "Active worker thread count.";
    static final String WORKER_QUEUE_METRIC_NAME = PREFIX + "worker.queue";
    static final String WORKER_QUEUE_METRIC_DESCRIPTION= "The number of entry in queue.";


    private final MeterRegistry meterRegistry;


    public void reportRecordPollCount(int count) {
        Gauge.builder(WORKING_POOL_METRIC_NAME, () -> count)
                .description(WORKING_POOL_METRIC_DESCRIPTION)
                .register(this.meterRegistry);
    }

    public void reportOffsetCommit(long offset, String topic, int partition) {
        Gauge.builder(OFFSET_COMMIT_METRIC_NAME, () -> offset)
                .description(OFFSET_COMMIT_METRIC_DESCRIPTION)
                .tags("topic", topic, "partition", Integer.toString(partition))
                .register(this.meterRegistry);
    }

    public void reportActiveWorkerCount(Supplier<Integer> countSupplier) {
        Gauge.builder(WORKER_ACTIVE_METRIC_NAME, countSupplier::get)
                .description(WORKER_ACTIVE_METRIC_DESCRIPTION)
                .register(this.meterRegistry);
    }

    public void reportWorkerQueueSize(Supplier<Integer> countSupplier) {
        Gauge.builder(WORKER_QUEUE_METRIC_NAME, countSupplier::get)
                .description(WORKER_QUEUE_METRIC_DESCRIPTION)
                .register(this.meterRegistry);
    }
}
