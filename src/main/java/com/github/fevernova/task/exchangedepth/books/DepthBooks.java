package com.github.fevernova.task.exchangedepth.books;


import com.github.fevernova.task.exchangedepth.data.Depth;
import lombok.Getter;
import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.bytes.BytesOut;
import net.openhft.chronicle.bytes.ReadBytesMarshallable;
import net.openhft.chronicle.bytes.WriteBytesMarshallable;
import net.openhft.chronicle.core.io.IORuntimeException;

import java.util.Map;
import java.util.NavigableMap;


@Getter
public abstract class DepthBooks implements WriteBytesMarshallable, ReadBytesMarshallable {


    protected final NavigableMap<Long, Depth> priceTree;

    protected long cachePrice;

    protected Depth cacheDepth;


    public DepthBooks(NavigableMap<Long, Depth> priceTree) {

        this.priceTree = priceTree;
    }


    public void handle(long price, long size, int count) {

        if (size == 0) {
            this.priceTree.remove(price);
            if (this.cachePrice == price) {
                Map.Entry<Long, Depth> entry = this.priceTree.ceilingEntry(price);
                this.cachePrice = entry != null ? entry.getKey() : defaultPrice();
                this.cacheDepth = entry != null ? entry.getValue() : null;
            }
            return;
        }

        if (newEdgePrice(price)) {
            Depth depth = new Depth();
            depth.setSize(size);
            depth.setCount(count);
            this.priceTree.put(price, depth);
            this.cachePrice = price;
            this.cacheDepth = depth;
            return;
        }

        if (this.cachePrice == price) {
            this.cacheDepth.setSize(size);
            this.cacheDepth.setCount(count);
            return;
        }

        Depth depth = new Depth();
        depth.setSize(size);
        depth.setCount(count);
        this.priceTree.put(price, depth);
    }


    protected abstract long defaultPrice();

    public abstract boolean newEdgePrice(long tmpPrice);


    @Override public void readMarshallable(BytesIn bytes) throws IORuntimeException {

        int treeSize = bytes.readInt();
        for (int i = 0; i < treeSize; i++) {
            long price = bytes.readLong();
            Depth depth = new Depth();
            depth.readMarshallable(bytes);
            this.priceTree.put(price, depth);
        }
        this.cachePrice = bytes.readLong();
        this.cacheDepth = this.priceTree.get(this.cachePrice);
    }


    @Override public void writeMarshallable(BytesOut bytes) {

        bytes.writeInt(this.priceTree.size());
        this.priceTree.forEach((price, depth) -> {
            bytes.writeLong(price);
            depth.writeMarshallable(bytes);
        });
        bytes.writeLong(this.cachePrice);
    }
}
