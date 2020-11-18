package com.github.fevernova.task.markettracing.data;


import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.Validate;

import java.nio.ByteBuffer;


@Getter
@Builder
public class CandleMessage {


    private Integer pairCodeId;

    private Double open;

    private Double high;

    private Double low;

    private Double close;

    private Double volume;

    private Double amount;

    private Long count;

    private Long timeSequence;

    private Long timestamp;


    public void from(byte[] bytes) {

        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        byte version = byteBuffer.get();
        Validate.isTrue(version == 0);
        this.pairCodeId = byteBuffer.getInt();
        this.open = byteBuffer.getDouble();
        this.high = byteBuffer.getDouble();
        this.low = byteBuffer.getDouble();
        this.close = byteBuffer.getDouble();
        this.volume = byteBuffer.getDouble();
        this.amount = byteBuffer.getDouble();
        this.count = byteBuffer.getLong();
        this.timeSequence = byteBuffer.getLong();
        this.timestamp = byteBuffer.getLong();

    }
}
