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


    private long volume;

    private int count;


    @Override public void readMarshallable(BytesIn bytes) throws IORuntimeException {

        this.volume = bytes.readLong();
        this.count = bytes.readInt();
    }


    @Override public void writeMarshallable(BytesOut bytes) {

        bytes.writeLong(this.volume);
        bytes.writeInt(this.count);
    }
}
