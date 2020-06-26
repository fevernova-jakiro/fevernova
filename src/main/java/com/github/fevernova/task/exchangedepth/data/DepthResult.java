package com.github.fevernova.task.exchangedepth.data;


import com.github.fevernova.framework.common.data.Data;
import com.github.fevernova.task.exchangedepth.engine.SymbolDepths;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


@Getter
@Setter
@ToString
public class DepthResult implements Data {


    private int symbolId;

    private long timestamp;

    private long lastSequence;

    private DepthGroup bidGroup;

    private DepthGroup askGroup;


    @Override public void clearData() {

        this.symbolId = 0;
        this.timestamp = 0L;
        this.bidGroup = null;
        this.askGroup = null;
    }


    @Override public byte[] getBytes() {

        return new byte[0];
    }


    public void dump(SymbolDepths symbolDepths, int maxDepthSize) {

        this.bidGroup = new DepthGroup(symbolDepths.getBids(), maxDepthSize);
        this.askGroup = new DepthGroup(symbolDepths.getAsks(), maxDepthSize);
    }
}
