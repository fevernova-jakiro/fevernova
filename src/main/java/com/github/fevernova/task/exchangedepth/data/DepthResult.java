package com.github.fevernova.task.exchangedepth.data;


import com.github.fevernova.framework.common.data.Data;
import com.github.fevernova.task.exchangedepth.engine.SymbolDepths;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.Validate;

import java.nio.ByteBuffer;


@Getter
@Setter
@ToString
public class DepthResult implements Data {


    private int symbolId;

    private long timestamp;

    private long lastSequence;

    private DepthGroup bidGroup;

    private DepthGroup askGroup;


    public void dump(SymbolDepths symbolDepths, int maxDepthSize) {

        this.bidGroup = new DepthGroup(symbolDepths.getBids(), maxDepthSize);
        this.askGroup = new DepthGroup(symbolDepths.getAsks(), maxDepthSize);
    }


    @Override public void clearData() {

        this.symbolId = 0;
        this.timestamp = 0L;
        this.bidGroup = null;
        this.askGroup = null;
    }


    @Override public byte[] getBytes() {

        ByteBuffer byteBuffer = ByteBuffer.allocate(21 + this.bidGroup.countBytes() + this.askGroup.countBytes());
        byteBuffer.put((byte) 0);
        byteBuffer.putInt(this.symbolId);
        byteBuffer.putLong(this.timestamp);
        byteBuffer.putLong(this.lastSequence);
        this.bidGroup.getBytes(byteBuffer);
        this.askGroup.getBytes(byteBuffer);
        return byteBuffer.array();
    }


    public void from(byte[] bytes) {

        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        byte version = byteBuffer.get();
        Validate.isTrue(version == 0);
        this.symbolId = byteBuffer.getInt();
        this.timestamp = byteBuffer.getLong();
        this.lastSequence = byteBuffer.getLong();
        this.bidGroup = new DepthGroup();
        this.bidGroup.from(byteBuffer);
        this.askGroup = new DepthGroup();
        this.askGroup.from(byteBuffer);
    }
}
