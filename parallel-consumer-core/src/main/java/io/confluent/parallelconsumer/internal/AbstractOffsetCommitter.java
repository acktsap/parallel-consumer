package io.confluent.parallelconsumer.internal;

/*-
 * Copyright (C) 2020-2023 Confluent, Inc.
 */

import io.confluent.parallelconsumer.ParallelConsumerOptions;
import io.confluent.parallelconsumer.state.WorkManager;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerGroupMetadata;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

import java.util.Map;
import java.util.concurrent.TimeoutException;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractOffsetCommitter<K, V> implements OffsetCommitter {

    protected final ConsumerManager<K, V> consumerMgr;
    protected final WorkManager<K, V> wm;
    protected final MetricReporter metricReporter;

    public AbstractOffsetCommitter(
            ConsumerManager<K, V> consumerMgr,
            WorkManager<K, V> wm,
            ParallelConsumerOptions<K, V> options) {
        this.consumerMgr = consumerMgr;
        this.wm = wm;
        this.metricReporter = new MetricReporter(options.getMeterRegistry());
    }

    /**
     * Get offsets from {@link WorkManager} that are ready to commit
     */
    @Override
    public void retrieveOffsetsAndCommit() throws TimeoutException, InterruptedException {
        log.debug("Find completed work to commit offsets");
        preAcquireOffsetsToCommit();
        try {
            var offsetsToCommit = wm.collectCommitDataForDirtyPartitions();
            if (offsetsToCommit.isEmpty()) {
                log.debug("No offsets ready");
            } else {
                log.debug("Will commit offsets for {} partition(s): {}", offsetsToCommit.size(), offsetsToCommit);
                ConsumerGroupMetadata groupMetadata = consumerMgr.groupMetadata();

                log.debug("Begin commit offsets");
                commitOffsets(offsetsToCommit, groupMetadata);

                log.debug("On commit success");
                onOffsetCommitSuccess(offsetsToCommit);
            }
        } finally {
            postCommit();
        }
    }

    protected void postCommit() {
        // default noop
    }

    protected void preAcquireOffsetsToCommit() throws TimeoutException, InterruptedException {
        // default noop
    }

    private void onOffsetCommitSuccess(final Map<TopicPartition, OffsetAndMetadata> committed) {
        wm.onOffsetCommitSuccess(committed);
        for (final Map.Entry<TopicPartition, OffsetAndMetadata> topicPartitionOffsetAndMetadataEntry : committed.entrySet()) {
            TopicPartition topicPartition = topicPartitionOffsetAndMetadataEntry.getKey();
            String topic = topicPartition.topic();
            int partition = topicPartition.partition();

            OffsetAndMetadata offsetAndMetadata = topicPartitionOffsetAndMetadataEntry.getValue();
            long offset = offsetAndMetadata.offset();

            metricReporter.reportOffsetCommit(offset, topic, partition);
        }
    }

    protected abstract void commitOffsets(final Map<TopicPartition, OffsetAndMetadata> offsetsToSend, final ConsumerGroupMetadata groupMetadata);

}
