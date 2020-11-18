package com.github.fevernova.task.markettracing.data;


import org.apache.commons.lang3.Validate;

import static org.apache.commons.lang3.time.DateUtils.MILLIS_PER_MINUTE;
import static org.apache.commons.lang3.time.DateUtils.MILLIS_PER_SECOND;


public class SQTimeUtil {


    public static Long toSequenceTime(Long timeSequence, Long timestamp) {

        timeSequence = timeSequence * MILLIS_PER_SECOND;
        Validate.isTrue(timestamp >= timeSequence);
        if (timestamp - timeSequence > MILLIS_PER_MINUTE) {
            return timeSequence + 999L;
        }
        return timestamp;
    }
}
