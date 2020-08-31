package com.github.fevernova.io.kafka;


import com.alibaba.fastjson.JSON;
import com.github.fevernova.framework.common.Util;
import com.github.fevernova.framework.common.context.GlobalContext;
import com.github.fevernova.framework.common.context.TaskContext;
import com.github.fevernova.framework.common.data.BarrierData;
import com.github.fevernova.framework.component.channel.ChannelProxy;
import com.github.fevernova.framework.component.source.AbstractSource;
import com.github.fevernova.framework.service.checkpoint.ICheckPointSaver;
import com.github.fevernova.framework.service.state.StateValue;
import com.github.fevernova.io.kafka.data.KafkaCheckPoint;
import com.github.fevernova.io.kafka.data.KafkaData;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;


@Slf4j
public abstract class AbstractKafkaSource extends AbstractSource<byte[], KafkaData> implements ConsumerRebalanceListener {


    protected final TaskContext kafkaContext;

    protected final List<String> topics;

    protected final long pollTimeOut;

    protected final ReentrantLock kafkaLock = new ReentrantLock();

    protected final boolean commit2Kafka;

    protected KafkaConsumer<byte[], byte[]> kafkaConsumer;

    protected ICheckPointSaver<KafkaCheckPoint> checkpoints;


    public AbstractKafkaSource(GlobalContext globalContext, TaskContext taskContext, int index, int inputsNum, ChannelProxy channelProxy) {

        super(globalContext, taskContext, index, inputsNum, channelProxy);
        this.kafkaContext = new TaskContext(KafkaConstants.KAFKA, super.taskContext.getSubProperties(KafkaConstants.KAFKA_));
        this.topics = Util.splitStringWithFilter(taskContext.get(KafkaConstants.TOPICS), ",", null);
        this.pollTimeOut = taskContext.getLong(KafkaConstants.POLLTIMEOUT, 5000L);
        this.commit2Kafka = taskContext.getBoolean("commit2kafka", true);
    }


    @Override public void init() {

        super.init();
        this.kafkaConsumer = KafkaUtil.createConsumer(this.kafkaContext);
    }


    @Override public void work() {

        ConsumerRecords<byte[], byte[]> records;
        this.kafkaLock.lock();
        try {
            records = this.kafkaConsumer.poll(this.pollTimeOut);
        } finally {
            this.kafkaLock.unlock();
        }

        if (records != null && !records.isEmpty()) {
            kafkaRecords(records);
        } else {
            Util.sleepMS(1);
        }
    }


    protected void kafkaRecords(ConsumerRecords<byte[], byte[]> records) {

        if (records != null && !records.isEmpty()) {
            Set<TopicPartition> tmpPartitions = records.partitions();
            for (TopicPartition topicPartition : tmpPartitions) {
                List<ConsumerRecord<byte[], byte[]>> recordList = records.records(topicPartition);
                recordList.forEach(ele -> {
                    final KafkaData data = feedOne(ele.key());
                    data.setTopic(ele.topic());
                    data.setKey(ele.key());
                    data.setValue(ele.value());
                    data.setPartitionId(ele.partition());
                    data.setTimestamp(ele.timestamp());
                    push();
                });
                super.handleRows.inc(recordList.size());
            }
        }
    }


    @Override protected void snapshotWhenBarrier(BarrierData barrierData) {

        KafkaCheckPoint checkPoint = new KafkaCheckPoint();
        this.kafkaLock.lock();
        try {
            this.kafkaConsumer.assignment().forEach(topicPartition -> checkPoint.put(topicPartition.topic(), topicPartition.partition(), kafkaConsumer
                    .position(new TopicPartition(topicPartition.topic(), topicPartition.partition()))));
        } catch (Throwable e) {
            log.error("JobSource Snapshot Error : ", e);
        } finally {
            this.kafkaLock.unlock();
        }
        this.checkpoints.put(barrierData.getBarrierId(), checkPoint);
    }


    @Override public void onPartitionsRevoked(Collection<TopicPartition> partitions) {

        if (log.isInfoEnabled()) {
            log.info("kafka partition update : " + JSON.toJSONString(partitions));
        }
    }


    @Override public void onPartitionsAssigned(Collection<TopicPartition> partitions) {

        if (log.isInfoEnabled()) {
            log.info("kafka partition update : " + JSON.toJSONString(partitions));
        }
    }


    protected StateValue checkpoint2StateValue(KafkaCheckPoint checkPoint) {

        StateValue stateValue = new StateValue();
        stateValue.setComponentType(super.componentType);
        stateValue.setComponentTotalNum(super.total);
        stateValue.setCompomentIndex(super.index);
        stateValue.setValue(checkPoint);
        return stateValue;
    }


    protected Map<TopicPartition, OffsetAndMetadata> checkpoint2Params(KafkaCheckPoint checkPoint) {

        Map<String, Map<Integer, Long>> offsets = checkPoint.getOffsets();
        if (log.isInfoEnabled()) {
            log.info("commit offset : " + JSON.toJSONString(offsets));
        }
        Map<TopicPartition, OffsetAndMetadata> params = Maps.newHashMap();
        if (!offsets.isEmpty()) {
            offsets.forEach((topic, offset) -> offset.forEach((k, v) -> params.put(new TopicPartition(topic, k), new OffsetAndMetadata(v))));
        }
        return params;
    }


    protected void commitKafkaOffset(Map<TopicPartition, OffsetAndMetadata> params) {

        if (!this.commit2Kafka) {
            return;
        }

        this.kafkaLock.lock();
        try {
            this.kafkaConsumer.commitSync(params);
        } finally {
            this.kafkaLock.unlock();
        }
    }
}
