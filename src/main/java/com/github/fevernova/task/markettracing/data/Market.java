package com.github.fevernova.task.markettracing.data;


import com.github.fevernova.task.exchange.engine.SerializationUtils;
import com.google.common.collect.Lists;
import lombok.Builder;
import lombok.Getter;
import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.bytes.BytesOut;
import net.openhft.chronicle.bytes.WriteBytesMarshallable;

import java.util.List;


@Getter
@Builder
public class Market implements WriteBytesMarshallable {


    private Long timestamp;

    private List<Double> tickers;


    public Market(BytesIn bytes) {

        this.timestamp = bytes.readLong();
        this.tickers = Lists.newLinkedList();
        SerializationUtils.readCollections(bytes, this.tickers, bytesIn -> bytesIn.readDouble());
    }


    @Override public void writeMarshallable(BytesOut bytes) {

        bytes.writeLong(this.timestamp);
        SerializationUtils.writeCollections(bytes, this.tickers, (bytesOut, d) -> bytesOut.writeDouble(d));
    }
}
