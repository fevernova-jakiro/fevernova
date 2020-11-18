package com.github.fevernova.task.markettracing;


import com.github.fevernova.framework.common.LogProxy;
import com.github.fevernova.framework.common.context.GlobalContext;
import com.github.fevernova.framework.common.context.TaskContext;
import com.github.fevernova.framework.common.data.Data;
import com.github.fevernova.io.kafka.AbstractKafkaSink;
import com.github.fevernova.task.exchange.data.result.OrderMatch;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.nio.ByteBuffer;


@Slf4j
public class JobSink extends AbstractKafkaSink {


    private ByteBuffer byteBuffer;


    public JobSink(GlobalContext globalContext, TaskContext taskContext, int index, int inputsNum) {

        super(globalContext, taskContext, index, inputsNum);
    }


    @Override
    protected void handleEvent(Data event) {

        OrderMatch data = (OrderMatch) event;
        if (LogProxy.LOG_DATA.isTraceEnabled()) {
            LogProxy.LOG_DATA.trace(data.toString());
        }

        this.byteBuffer = ByteBuffer.allocate(4);
        this.byteBuffer.putInt(data.getSymbolId());
        ProducerRecord<byte[], byte[]> record = new ProducerRecord<>(super.topic, this.byteBuffer.array(), data.getBytes());
        super.kafka.send(record, this);
    }
}
