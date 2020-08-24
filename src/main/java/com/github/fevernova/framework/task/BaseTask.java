package com.github.fevernova.framework.task;


import com.github.fevernova.framework.common.Constants;
import com.github.fevernova.framework.common.LogProxy;
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


    protected final TaskContext context;

    protected final Named named;

    protected final long createTime;

    protected final GlobalContext globalContext;

    protected Manager manager;


    public BaseTask(TaskContext context, JobTags tags) {

        this.context = context;
        this.named = Named.builder().taskName(Constants.PROJECT_NAME).moduleName(context.getName()).moduleType(getClass().getSimpleName()).build();
        this.createTime = Util.nowMS();
        EventBus eventBus = new EventBus(this.named.render(true));
        eventBus.register(this);
        this.globalContext = GlobalContext.builder().eventBus(eventBus).customContext(Maps.newConcurrentMap()).jobTags(tags).build();
        Validate.isTrue(this.globalContext.getJobTags().getUnit() >= 1 && this.globalContext.getJobTags().getUnit() <= 5);
    }


    public BaseTask init() throws Exception {

        log.info("Task Init .");
        LogProxy.LOG_CONF.info(this.globalContext.getJobTags().toString());
        LogProxy.LOG_CONF.info("taskcontext : " + this.context.toString());
        this.manager = Manager.getInstance(this.globalContext, this.context);
        return this;
    }


    public BaseTask recovery() {

        log.info("Task Recovery .");
        this.manager.recovery();
        return this;
    }


    public BaseTask start() throws Exception {

        log.info("Task Start .");
        try {
            this.manager.execute();
        } catch (Exception e) {
            log.error("Task start error : ", e);
            Validate.isTrue(false);
        }
        return this;
    }


    public synchronized void close() {

        log.info("Task Close .");
        Thread thread = new Thread(() -> {

            try {
                Thread.sleep(10 * 1000);
            } catch (Exception e) {
                log.error("Task close error : ", e);
            } finally {
                System.exit(0);
            }
        });
        thread.start();
        try {
            this.globalContext.getEventBus().unregister(this);
            this.manager.close();
        } catch (Throwable e) {
            log.error("Task close error : ", e);
            Validate.isTrue(false);
        }
    }


    @Subscribe
    public void callBack(String reason) {

        log.error(this.named.render(true) + " is closing : " + reason);
        close();
    }
}
