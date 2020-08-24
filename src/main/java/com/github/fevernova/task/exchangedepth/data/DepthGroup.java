package com.github.fevernova.task.exchangedepth.data;


import com.github.fevernova.task.exchangedepth.books.DepthBooks;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.nio.ByteBuffer;
import java.util.Map;


@Getter
@NoArgsConstructor
@ToString
public class DepthGroup {


    private long[] price;

    private long[] volume;


    public DepthGroup(DepthBooks books, int maxDepthSize) {

        int arraySize = Math.min(maxDepthSize, books.getPriceTree().size());
        this.price = new long[arraySize];
        this.volume = new long[arraySize];
        int cursor = 0;
        for (Map.Entry<Long, Depth> entry : books.getPriceTree().entrySet()) {
            if (cursor == arraySize) {
                break;
            }
            this.price[cursor] = entry.getKey();
            this.volume[cursor] = entry.getValue().getVolume();
            cursor++;
        }
    }


    public void from(ByteBuffer byteBuffer) {

        int arraySize = byteBuffer.getInt();
        this.price = new long[arraySize];
        this.volume = new long[arraySize];
        for (int i = 0; i < arraySize; i++) {
            this.price[i] = byteBuffer.getLong();
            this.volume[i] = byteBuffer.getLong();
        }
    }


    public void getBytes(ByteBuffer byteBuffer) {

        int arraySize = this.price == null ? 0 : this.price.length;
        byteBuffer.putInt(arraySize);
        for (int i = 0; i < arraySize; i++) {
            byteBuffer.putLong(this.price[i]);
            byteBuffer.putLong(this.volume[i]);
        }
    }


    public int countBytes() {

        if (this.price == null) {
            return 4;
        }
        return 4 + this.price.length * 16;
    }
}
