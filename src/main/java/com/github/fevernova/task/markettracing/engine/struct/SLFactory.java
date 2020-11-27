package com.github.fevernova.task.markettracing.engine.struct;


public class SLFactory implements Factory<SLOrderBook> {


    @Override
    public SLOrderBook create() {

        return new SLOrderBook();
    }
}
