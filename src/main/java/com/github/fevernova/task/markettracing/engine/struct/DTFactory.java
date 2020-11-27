package com.github.fevernova.task.markettracing.engine.struct;


public class DTFactory implements Factory<DTOrderBook> {


    @Override
    public DTOrderBook create() {

        return new DTOrderBook();
    }
}
