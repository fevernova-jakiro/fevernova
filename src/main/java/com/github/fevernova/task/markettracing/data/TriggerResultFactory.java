package com.github.fevernova.task.markettracing.data;


import com.github.fevernova.framework.common.data.Data;
import com.github.fevernova.framework.common.data.DataFactory;
import com.github.fevernova.task.exchange.data.result.MatchPart;
import com.github.fevernova.task.exchange.data.result.OrderMatch;
import com.github.fevernova.task.exchange.data.result.OrderPart;


public class TriggerResultFactory implements DataFactory {


    @Override public Data createData() {

        return new TriggerResult();
    }
}
