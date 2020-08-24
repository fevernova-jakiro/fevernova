package com.github.fevernova.task.exchangedepth;


import com.github.fevernova.framework.common.LogProxy;
import com.github.fevernova.framework.common.context.GlobalContext;
import com.github.fevernova.framework.common.context.TaskContext;
import com.github.fevernova.framework.common.data.Data;
import com.github.fevernova.framework.component.sink.AbstractSink;
import com.github.fevernova.task.exchangedepth.data.DepthResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import org.redisson.Redisson;
import org.redisson.api.RTopic;
import org.redisson.client.codec.ByteArrayCodec;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;


@Slf4j
public class JobSink extends AbstractSink {


    private Redisson redis;

    private RTopic currentTopic;


    public JobSink(GlobalContext globalContext, TaskContext taskContext, int index, int inputsNum) {

        super(globalContext, taskContext, index, inputsNum);
        Config redisConfig = new Config();
        SingleServerConfig singleServerConfig = redisConfig.useSingleServer();
        singleServerConfig.setAddress(taskContext.get("address"));
        singleServerConfig.setPassword(taskContext.get("password"));
        singleServerConfig.setDatabase(taskContext.getInteger("dbnum", 0));
        singleServerConfig.setConnectionPoolSize(taskContext.getInteger("poolsize", 64));
        singleServerConfig.setClientName(super.named.render(true));
        redisConfig.setCodec(ByteArrayCodec.INSTANCE);
        this.redis = (Redisson) Redisson.create(redisConfig);
        String topicName = taskContext.get("topic");
        Validate.notNull(topicName);
        this.currentTopic = this.redis.getTopic(topicName);
    }


    @Override protected void handleEvent(Data event) {

        DepthResult depthResult = (DepthResult) event;
        if (LogProxy.LOG_DATA.isTraceEnabled()) {
            LogProxy.LOG_DATA.trace(depthResult.toString());
        }
        this.currentTopic.publish(depthResult.getBytes());
    }
}
