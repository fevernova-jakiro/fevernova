package com.github.fevernova.framework.task;


import com.github.fevernova.framework.common.Constants;
import com.github.fevernova.framework.common.Named;
import com.github.fevernova.framework.common.Util;
import com.github.fevernova.framework.common.context.GlobalContext;
import com.github.fevernova.framework.common.context.JobTags;
import com.github.fevernova.framework.common.context.TaskContext;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;


@Slf4j
public abstract class BaseTask {


    protected final GlobalContext globalContext;

    protected final TaskContext context;

    protected final Named named;

    protected final long createTime;

    protected Manager manager;


    public BaseTask(TaskContext context, JobTags tags) {

        this.context = context;
        this.named = Named.builder().taskName(Constants.PROJECT_NAME).moduleName(context.getName()).moduleType(getClass().getSimpleName()).build();
        EventBus eventBus = new EventBus(this.named.render(true));
        eventBus.register(this);
        this.globalContext = GlobalContext.builder().eventBus(eventBus).customContext(Maps.newConcurrentMap()).jobTags(tags).build();
        this.createTime = Util.nowMS();
        Validate.isTrue(this.globalContext.getJobTags().getUnit() >= 1 && this.globalContext.getJobTags().getUnit() <= 5);
    }


    public abstract BaseTask init() throws Exception;


    public BaseTask start() throws Exception {

        try {
            this.manager.execute();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            Validate.isTrue(false);
        }
        return this;
    }


    public synchronized void close() {

        Thread thread = new Thread() {


            @Override
            public void run() {

                try {
                    Thread.sleep(15 * 1000);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                } finally {
                    System.exit(0);
                }
            }
        };
        thread.start();
        try {
            this.globalContext.getEventBus().unregister(this);
            this.manager.close();
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            Validate.isTrue(false);
        } finally {
            System.exit(1);
        }
    }


    @Subscribe
    public void errorCalBack(String reason) {

        log.error(this.named.render(true) + " will be killed , " + reason);
        close();
    }
}