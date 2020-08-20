package com.github.fevernova.task.exchangedepth;


import com.alibaba.fastjson.JSONObject;
import com.github.fevernova.framework.common.LogProxy;
import com.github.fevernova.framework.common.Util;
import com.github.fevernova.framework.common.context.GlobalContext;
import com.github.fevernova.framework.common.context.TaskContext;
import com.github.fevernova.framework.common.data.BarrierData;
import com.github.fevernova.framework.common.data.Data;
import com.github.fevernova.framework.component.channel.ChannelProxy;
import com.github.fevernova.framework.component.parser.AbstractParser;
import com.github.fevernova.framework.service.barrier.listener.BarrierCoordinatorListener;
import com.github.fevernova.framework.service.checkpoint.CheckPointSaver;
import com.github.fevernova.framework.service.checkpoint.ICheckPointSaver;
import com.github.fevernova.framework.service.checkpoint.MapCheckPoint;
import com.github.fevernova.framework.service.state.BinaryFileIdentity;
import com.github.fevernova.framework.service.state.StateService;
import com.github.fevernova.framework.service.state.StateValue;
import com.github.fevernova.framework.task.Manager;
import com.github.fevernova.io.kafka.data.KafkaData;
import com.github.fevernova.task.exchange.data.result.OrderMatch;
import com.github.fevernova.task.exchange.data.result.OrderMatchFactory;
import com.github.fevernova.task.exchangedepth.data.DepthResult;
import com.github.fevernova.task.exchangedepth.engine.DepthEngine;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;

import java.util.List;


@Slf4j
public class JobParser extends AbstractParser<Integer, DepthResult> implements BarrierCoordinatorListener {


    private ICheckPointSaver<MapCheckPoint> checkpoints = new CheckPointSaver<>();

    private OrderMatch orderMatch = (OrderMatch) new OrderMatchFactory().createData();

    private BinaryFileIdentity depthDataIdentity;

    private DepthEngine depthEngine;


    public JobParser(GlobalContext globalContext, TaskContext taskContext, int index, int inputsNum, ChannelProxy channelProxy) {

        super(globalContext, taskContext, index, inputsNum, channelProxy);
        this.depthDataIdentity = BinaryFileIdentity.builder().componentType(super.componentType).total(super.total).index(super.index)
                .identity(DepthEngine.CONS_NAME.toLowerCase()).build();
        int maxDepthSize = taskContext.getInteger("maxdepthsize", 500);
        long interval = taskContext.getLong("interval", 1000L);
        this.depthEngine = new DepthEngine(maxDepthSize, interval, this);
    }


    @Override protected void handleEvent(Data event) {

        KafkaData kafkaData = (KafkaData) event;
        this.orderMatch.from(kafkaData.getValue());
        if (LogProxy.LOG_DATA.isTraceEnabled()) {
            LogProxy.LOG_DATA.trace(this.orderMatch.toString());
        }
        long now = Util.nowMS();
        if (this.orderMatch.getOrderPart1().getOrderPriceDepthSize() >= 0L) {
            this.depthEngine.handle(this.orderMatch, now);
        }
        this.depthEngine.scan(now);
    }


    @Override protected void timeOut() {

        this.depthEngine.scan(Util.nowMS());
    }


    @Override protected void snapshotWhenBarrier(BarrierData barrierData) {

        super.snapshotWhenBarrier(barrierData);
        MapCheckPoint checkPoint = new MapCheckPoint();
        StateService stateService = Manager.getInstance().getStateService();
        if (stateService.isSupportRecovery()) {
            String path = stateService.saveBinary(this.depthDataIdentity, barrierData, this.depthEngine);
            checkPoint.getValues().put(this.depthDataIdentity.getIdentity(), path);
        }
        this.checkpoints.put(barrierData.getBarrierId(), checkPoint);
    }


    @Override public boolean collect(BarrierData barrierData) throws Exception {

        return this.checkpoints.getCheckPoint(barrierData.getBarrierId()) != null;
    }


    @Override public StateValue getStateForRecovery(BarrierData barrierData) throws Exception {

        MapCheckPoint checkPoint = this.checkpoints.getCheckPoint(barrierData.getBarrierId());
        StateValue stateValue = new StateValue();
        stateValue.setComponentType(super.componentType);
        stateValue.setComponentTotalNum(super.total);
        stateValue.setCompomentIndex(super.index);
        stateValue.setValue(checkPoint);
        return stateValue;
    }


    @Override public void result(boolean result, BarrierData barrierData) throws Exception {

        Validate.isTrue(result);
    }


    @Override public void onRecovery(List<StateValue> stateValueList) {

        super.onRecovery(stateValueList);
        stateValueList.forEach(stateValue -> {
            if (stateValue.getCompomentIndex() == index) {
                MapCheckPoint checkPoint = new MapCheckPoint();
                checkPoint.parseFromJSON((JSONObject) stateValue.getValue());
                Manager.getInstance().getStateService().recoveryBinary(checkPoint.getValues().get(depthDataIdentity.getIdentity()), depthEngine);
            }
        });
    }


    @Override public boolean needRecovery() {

        return true;
    }
}
