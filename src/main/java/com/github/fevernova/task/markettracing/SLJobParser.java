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
import com.github.fevernova.task.exchange.data.cmd.OrderCommandType;
import com.github.fevernova.task.markettracing.data.CandleMessage;
import com.github.fevernova.task.markettracing.data.TriggerResult;
import com.github.fevernova.task.markettracing.data.order.SLOrder;
import com.github.fevernova.task.markettracing.engine.TracingEngine;
import com.github.fevernova.task.markettracing.engine.struct.SLFactory;
import com.github.fevernova.task.markettracing.engine.struct.SLOrderBook;
import com.google.common.primitives.Ints;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;

import java.util.List;


@Slf4j
public class SLJobParser extends AbstractParser<Integer, TriggerResult> implements BarrierCoordinatorListener {


    protected ICheckPointSaver<MapCheckPoint> checkpoints = new CheckPointSaver<>();

    private TracingEngine<SLOrderBook, SLOrder> tracingEngine;

    private BinaryFileIdentity matchIdentity;


    public SLJobParser(GlobalContext globalContext, TaskContext taskContext, int index, int inputsNum, ChannelProxy channelProxy) {

        super(globalContext, taskContext, index, inputsNum, channelProxy);
        this.tracingEngine = new TracingEngine<>(new SLFactory(), this);
        this.matchIdentity = BinaryFileIdentity.builder().componentType(super.componentType).total(super.total).index(super.index)
                .identity(TracingEngine.CONS_NAME.toLowerCase()).build();
    }


    @Override protected void handleEvent(Data event) {

        KafkaData kafkaData = (KafkaData) event;
        switch (kafkaData.getTopic()) {
            case "markets":
                CandleMessage candle = new CandleMessage();
                candle.from(kafkaData.getValue());
                this.tracingEngine.handleCandle(candle);
                break;
            case "sl-condition-order":
                int pairCodeId = Ints.fromByteArray(kafkaData.getKey());
                SLOrder order = new SLOrder();
                order.from(kafkaData.getValue());
                if (OrderCommandType.HEARTBEAT == order.getCommandType()) {
                    this.tracingEngine.heartbeat(pairCodeId, order.getTimestamp());
                } else if (OrderCommandType.PLACE_ORDER == order.getCommandType()) {
                    this.tracingEngine.handleOrder(pairCodeId, order);
                } else {
                    this.tracingEngine.cancelOrder(pairCodeId, order);
                }
                break;
            default:
                Validate.isTrue(false);
        }
    }


    @Override protected void snapshotWhenBarrier(BarrierData barrierData) {

        super.snapshotWhenBarrier(barrierData);
        MapCheckPoint checkPoint = new MapCheckPoint();
        StateService stateService = Manager.getInstance().getStateService();
        if (stateService.isSupportRecovery()) {
            String path4engine = stateService.saveBinary(this.matchIdentity, barrierData, this.tracingEngine);
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
                Manager.getInstance().getStateService().recoveryBinary(checkPoint.getValues().get(matchIdentity.getIdentity()), tracingEngine);
            }
        });
    }


    @Override public boolean needRecovery() {

        return true;
    }
}
