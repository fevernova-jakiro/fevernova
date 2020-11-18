package com.github.fevernova.task.markettracing;


import com.alibaba.fastjson.JSONObject;
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
import com.github.fevernova.task.exchange.data.cmd.OrderCommand;
import com.github.fevernova.task.exchange.data.order.OrderMode;
import com.github.fevernova.task.exchange.data.result.OrderMatch;
import com.github.fevernova.task.exchange.engine.OrderBooksEngine;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;

import java.util.List;


@Slf4j
public class JobParser extends AbstractParser<Integer, OrderMatch> implements BarrierCoordinatorListener {


    protected ICheckPointSaver<MapCheckPoint> checkpoints = new CheckPointSaver<>();

    private OrderBooksEngine matchEngine;

    private BinaryFileIdentity matchIdentity;


    public JobParser(GlobalContext globalContext, TaskContext taskContext, int index, int inputsNum, ChannelProxy channelProxy) {

        super(globalContext, taskContext, index, inputsNum, channelProxy);
        TaskContext matchEngineContext =
                new TaskContext(OrderBooksEngine.CONS_NAME, taskContext.getSubProperties(OrderBooksEngine.CONS_NAME.toLowerCase() + "."));
        this.matchEngine = new OrderBooksEngine(globalContext, matchEngineContext);
        this.matchIdentity = BinaryFileIdentity.builder().componentType(super.componentType).total(super.total).index(super.index)
                .identity(OrderBooksEngine.CONS_NAME.toLowerCase()).build();
    }


    @Override protected void handleEvent(Data event) {

        KafkaData kafkaData = (KafkaData) event;
        OrderCommand orderCommand = new OrderCommand();
        orderCommand.from(kafkaData.getValue());

        if (OrderMode.SIMPLE == orderCommand.getOrderMode()) {
            switch (orderCommand.getOrderCommandType()) {
                case PLACE_ORDER:
                    this.matchEngine.placeOrder(orderCommand, this);
                    break;
                case CANCEL_ORDER:
                    this.matchEngine.cancelOrder(orderCommand, this);
                    break;
                case LOCATE_MATCH_PRICE:
                    this.matchEngine.locateMatchPrice(orderCommand, this);
                    break;
                case HEARTBEAT:
                    this.matchEngine.heartBeat(orderCommand, this);
                    break;
            }
        } else {
            switch (orderCommand.getOrderCommandType()) {
                case PLACE_ORDER:
                    this.matchEngine.placeConditionOrder(orderCommand, this);
                    break;
                case CANCEL_ORDER:
                    this.matchEngine.cancelConditionOrder(orderCommand, this);
                    break;
            }
        }
    }


    @Override protected void snapshotWhenBarrier(BarrierData barrierData) {

        super.snapshotWhenBarrier(barrierData);
        MapCheckPoint checkPoint = new MapCheckPoint();
        StateService stateService = Manager.getInstance().getStateService();
        if (stateService.isSupportRecovery()) {
            String path4engine = stateService.saveBinary(this.matchIdentity, barrierData, this.matchEngine);
            checkPoint.getValues().put(this.matchIdentity.getIdentity(), path4engine);
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
                Manager.getInstance().getStateService().recoveryBinary(checkPoint.getValues().get(matchIdentity.getIdentity()), matchEngine);
            }
        });
    }


    @Override public boolean needRecovery() {

        return true;
    }
}
