package com.github.fevernova.task.exchange;


import com.google.common.collect.Maps;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;
import java.util.NavigableMap;


public class T_TreeMap {


    private NavigableMap<Long, Long> tree0 = Maps.newTreeMap(Long::compareTo);

    private NavigableMap<Long, Long> tree1 = Maps.newTreeMap((l1, l2) -> l2.compareTo(l1));


    @Test
    public void T_treemap() {

        for (long i = 10L; i <= 20L; i += 2) {
            tree0.put(i, i);
            tree1.put(i, i);
        }
        Map.Entry<Long, Long> entry1 = tree0.ceilingEntry(5L);
        Assert.assertEquals(entry1.getKey().longValue(), 10L);

        Map.Entry<Long, Long> entry2 = tree1.ceilingEntry(25L);
        Assert.assertEquals(entry2.getKey().longValue(), 20L);
    }


    @Test
    public void T_treemap2() {

        tree0.put(2L, 2L);
        tree0.put(4L, 4L);
        tree0.put(1L, 1L);
        tree0.put(5L, 5L);
        tree0.put(3L, 3L);

        System.out.println(tree0.firstKey());
        System.out.println(tree0.lastKey());

    }

}
