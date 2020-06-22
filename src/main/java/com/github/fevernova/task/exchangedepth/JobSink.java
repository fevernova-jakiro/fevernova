package com.github.fevernova.task.exchangedepth;


import com.github.fevernova.framework.common.context.GlobalContext;
import com.github.fevernova.framework.common.context.TaskContext;
import com.github.fevernova.framework.common.data.Data;
import com.github.fevernova.framework.component.sink.AbstractSink;
import com.github.fevernova.task.exchangedepth.data.DepthResult;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RTopic;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;

import java.util.Map;


@Slf4j
public class JobSink extends AbstractSink {


    private Redisson redis;

    private Map<Integer, RTopic> channels = Maps.newHashMap();

    private Integer currentSymbolId;

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
        this.redis = (Redisson) Redisson.create(redisConfig);
    }


    @Override protected void handleEvent(Data event) {

        DepthResult depthResult = (DepthResult) event;
        if (this.currentSymbolId != depthResult.getSymbolId()) {
            this.currentSymbolId = depthResult.getSymbolId();
            this.currentTopic = this.channels.get(this.currentSymbolId);
            if (this.currentTopic == null) {
                this.currentTopic = this.redis.getTopic("DepthData_" + depthResult.getSymbolId());
                this.channels.put(depthResult.getSymbolId(), this.currentTopic);
            }
        }
        this.currentTopic.publish(depthResult);
    }
}
