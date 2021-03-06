package com.github.fevernova.task.logdist;


import com.github.fevernova.framework.common.Constants;
import com.github.fevernova.framework.common.context.JobTags;
import com.github.fevernova.framework.common.context.TaskContext;
import com.github.fevernova.framework.component.channel.selector.BytesSelector;
import com.github.fevernova.framework.metric.evaluate.NoMetricEvaluate;
import com.github.fevernova.framework.service.config.TaskConfig;
import com.github.fevernova.framework.task.BaseTask;
import com.github.fevernova.framework.task.TaskTopology;
import com.github.fevernova.io.kafka.data.KafkaDataFactory;
import com.github.fevernova.task.mirrormaker.JobParser;

import java.util.concurrent.atomic.AtomicInteger;


public class Task extends BaseTask {


    int parserInitParallelism = 0;

    int sinkInitParallelism = 0;


    public Task(TaskContext context, JobTags tags) {

        super(context, tags);
        context.put(Constants.INPUTCHANNEL_ + Constants.SIZE, "1024");
        context.put(Constants.OUTPUTCHANNEL_ + Constants.SIZE, "512");
        int unit = this.globalContext.getJobTags().getUnit();
        this.parserInitParallelism = context.getInteger(Constants.PARSER_ + Constants.PARALLELISM, Math.min(unit, 3));
        this.sinkInitParallelism = context.getInteger(Constants.SINK_ + Constants.PARALLELISM, unit);
    }


    @Override public BaseTask init() throws Exception {

        super.init();
        super.manager.register(new TaskTopology(super.globalContext, super.context, TaskConfig.builder()
                .sourceClass(JobSource.class)
                .parserClass(JobParser.class)
                .sinkClass(JobSink.class)
                .inputDataFactoryClass(KafkaDataFactory.class)
                .outputDataFactoryClass(KafkaDataFactory.class)
                .inputSelectorClass(BytesSelector.class)
                .outputSelectorClass(BytesSelector.class)
                .sourceParallelism(1)
                .parserParallelism(this.parserInitParallelism)
                .sinkParallelism(this.sinkInitParallelism + 1)
                .sourceAvailbleNum(new AtomicInteger(1))
                .parserAvailbleNum(new AtomicInteger(this.parserInitParallelism))
                .sinkAvailbleNum(new AtomicInteger(this.sinkInitParallelism))
                .inputDynamicBalance(false)
                .outputDynamicBalance(false)
                .metricEvaluateClass(NoMetricEvaluate.class)
                .build()));
        return this;
    }
}
