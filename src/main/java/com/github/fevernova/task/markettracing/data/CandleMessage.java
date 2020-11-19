package com.github.fevernova.task.markettracing.data;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.bytes.BytesOut;
import net.openhft.chronicle.bytes.WriteBytesMarshallable;
import org.apache.commons.lang3.Validate;

import java.nio.ByteBuffer;


@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CandleMessage implements WriteBytesMarshallable {


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


    public CandleMessage(BytesIn bytes) {

        this.pairCodeId = bytes.readInt();
        this.open = bytes.readDouble();
        this.high = bytes.readDouble();
        this.low = bytes.readDouble();
        this.close = bytes.readDouble();
        this.volume = bytes.readDouble();
        this.amount = bytes.readDouble();
        this.count = bytes.readLong();
        this.timeSequence = bytes.readLong();
        this.timestamp = bytes.readLong();
    }


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


    @Override public void writeMarshallable(BytesOut bytes) {

        bytes.writeInt(this.pairCodeId);
        bytes.writeDouble(this.open);
        bytes.writeDouble(this.high);
        bytes.writeDouble(this.low);
        bytes.writeDouble(this.close);
        bytes.writeDouble(this.volume);
        bytes.writeDouble(this.amount);
        bytes.writeLong(this.count);
        bytes.writeLong(this.timeSequence);
        bytes.writeLong(this.timestamp);
    }
}
