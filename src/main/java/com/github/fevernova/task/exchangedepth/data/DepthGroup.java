package com.github.fevernova.task.exchangedepth.data;


import com.github.fevernova.task.exchangedepth.books.DepthBooks;
import lombok.Getter;

import java.util.Map;


@Getter
public class DepthGroup {


    private long[] price;

    private long[] size;


    public DepthGroup(DepthBooks books, int maxDepthSize) {

        int size = Math.min(maxDepthSize, books.getPriceTree().size());
        this.price = new long[size];
        this.size = new long[size];
        int cursor = 0;
        for (Map.Entry<Long, Depth> entry : books.getPriceTree().entrySet()) {
            if (cursor == size) {
                break;
            }
            this.price[cursor] = entry.getKey();
            this.size[cursor] = entry.getValue().getSize();
            cursor++;
        }
    }
}
