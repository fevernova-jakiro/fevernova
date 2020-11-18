package com.github.fevernova.task.markettracing.data;


import lombok.Builder;
import lombok.Data;

import java.util.List;


@Data
@Builder
public class Market {


    private Long timestamp;

    private List<Double> tickers;

}
