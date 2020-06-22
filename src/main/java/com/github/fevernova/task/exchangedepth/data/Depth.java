package com.github.fevernova.task.exchangedepth.data;


import lombok.Getter;
import lombok.Setter;
import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.bytes.BytesOut;
import net.openhft.chronicle.bytes.ReadBytesMarshallable;
import net.openhft.chronicle.bytes.WriteBytesMarshallable;
import net.openhft.chronicle.core.io.IORuntimeException;


@Getter
@Setter
public class Depth implements WriteBytesMarshallable, ReadBytesMarshallable {


    private long size;

    private int count;


    @Override public void readMarshallable(BytesIn bytes) throws IORuntimeException {

        this.size = bytes.readLong();
        this.count = bytes.readInt();
    }


    @Override public void writeMarshallable(BytesOut bytes) {

        bytes.writeLong(this.size);
        bytes.writeInt(this.count);
    }
}
