package com.github.fevernova.task.exchange;


import com.github.fevernova.framework.common.Constants;
import com.github.fevernova.framework.common.context.JobTags;
import com.github.fevernova.framework.common.context.TaskContext;
import com.github.fevernova.framework.component.channel.selector.BytesSelector;
import com.github.fevernova.framework.component.channel.selector.IntSelector;
import com.github.fevernova.framework.metric.evaluate.NoMetricEvaluate;
import com.github.fevernova.framework.service.config.TaskConfig;
import com.github.fevernova.framework.task.BaseTask;
import com.github.fevernova.framework.task.Manager;
import com.github.fevernova.framework.task.TaskTopology;
import com.github.fevernova.task.exchange.data.cmd.OrderCommandFactory;
import com.github.fevernova.task.exchange.data.result.OrderMatchFactory;

import java.util.concurrent.atomic.AtomicInteger;


public class Task extends BaseTask {


    int parserInitParallelism = 0;

    int sinkInitParallelism = 0;


    public Task(TaskContext context, JobTags tags) {

        super(context, tags);
        context.put(Constants.INPUTCHANNEL_ + Constants.SIZE, "1024");
        context.put(Constants.OUTPUTCHANNEL_ + Constants.SIZE, "512");
        this.parserInitParallelism = context.getInteger(Constants.PARSER_ + Constants.PARALLELISM, this.globalContext.getJobTags().getUnit());
        this.sinkInitParallelism = context.getInteger(Constants.SINK_ + Constants.PARALLELISM, this.globalContext.getJobTags().getUnit());
    }


    @Override public BaseTask init() throws Exception {

        super.init();
        super.manager = Manager.getInstance(this.globalContext, this.context);
        TaskConfig taskConfig = TaskConfig.builder()
                .sourceClass(JobSource.class)
                .parserClass(JobParser.class)
                .sinkClass(JobSink.class)
                .inputDataFactoryClass(OrderCommandFactory.class)
                .outputDataFactoryClass(OrderMatchFactory.class)
                .inputSelectorClass(BytesSelector.class)
                .outputSelectorClass(IntSelector.class)
                .sourceParallelism(2)
                .parserParallelism(this.globalContext.getJobTags().getUnit())
                .sinkParallelism(this.globalContext.getJobTags().getUnit() + 1)
                .sourceAvailbleNum(new AtomicInteger(2))
                .parserAvailbleNum(new AtomicInteger(this.parserInitParallelism))
                .sinkAvailbleNum(new AtomicInteger(this.sinkInitParallelism))
                .inputDynamicBalance(false)
                .outputDynamicBalance(false)
                .metricEvaluateClass(NoMetricEvaluate.class)
                .build();
        TaskTopology taskTopology = new TaskTopology(globalContext, this.context, taskConfig);
        super.manager.register(taskTopology);
        return this;
    }
}
