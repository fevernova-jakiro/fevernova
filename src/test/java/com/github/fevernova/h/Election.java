package com.github.fevernova.h;


import com.github.fevernova.framework.common.Util;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RBucket;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;


@Slf4j
public class Election implements Runnable {


    private final static long DURATION_TIME = 60;

    private final static long DISPUTE_TIME = 20;

    private final static AtomicInteger index = new AtomicInteger(0);

    private final AtomicBoolean master = new AtomicBoolean(false);

    private final AtomicLong deadLineTS = new AtomicLong(Util.nowMS());

    private final String lockName;

    private final String deviceId;

    private final Redisson redis;


    public Election(String lockName, String deviceId, String address) {

        this.lockName = "LOCK_" + lockName;
        this.deviceId = deviceId;
        Config redisConfig = new Config();
        SingleServerConfig singleServerConfig = redisConfig.useSingleServer();
        singleServerConfig.setAddress(address);
        singleServerConfig.setDatabase(0);
        singleServerConfig.setConnectionMinimumIdleSize(2);
        singleServerConfig.setConnectionPoolSize(4);
        singleServerConfig.setClientName("Job-" + index.getAndAdd(1));
        this.redis = (Redisson) Redisson.create(redisConfig);
    }


    @Override public void run() {

        while (true) {
            try {
                RBucket<String> currentLock = this.redis.getBucket(this.lockName, StringCodec.INSTANCE);
                String currentLockValue = currentLock.get();

                if (StringUtils.isBlank(currentLockValue)) {
                    if (log.isDebugEnabled()) {
                        log.debug("start election .");
                    }
                    boolean success = currentLock.trySet(this.deviceId, DURATION_TIME, TimeUnit.SECONDS);
                    if (log.isDebugEnabled()) {
                        log.debug("election result : " + success);
                    }
                    update(success);
                    Util.sleepSec(10);
                } else {
                    long remainTime = currentLock.remainTimeToLive();
                    if (remainTime <= 0) {
                        continue;
                    }
                    String tValue = currentLock.get();
                    if (!currentLockValue.equals(tValue)) {
                        continue;
                    }

                    if (this.deviceId.equals(currentLockValue)) {
                        if (log.isDebugEnabled()) {
                            log.debug("I am master .");
                        }
                        if (remainTime >= TimeUnit.SECONDS.toMillis(DISPUTE_TIME)) {
                            currentLock.expire(DURATION_TIME, TimeUnit.SECONDS);
                            if (log.isDebugEnabled()) {
                                log.debug("renew lock ttl .");
                            }
                            update(true);
                        } else {
                            update(false);
                            if (log.isDebugEnabled()) {
                                log.debug("unstable .");
                            }
                        }
                        Util.sleepSec(10);
                    } else {
                        update(false);
                        if (log.isDebugEnabled()) {
                            log.debug("wait for next time .");
                        }
                        Util.sleepMS(remainTime);
                    }
                }
            } catch (Throwable e) {
                log.error("Election Error : ", e);
                Util.sleepSec(1);
            }
        }
    }


    private void update(boolean s) {

        this.master.set(s);
        long time = Util.nowMS() + (s ? TimeUnit.SECONDS.toMillis(DURATION_TIME - DISPUTE_TIME) : TimeUnit.SECONDS.toMillis(DURATION_TIME));
        this.deadLineTS.set(time);
    }


    public boolean isMaster() {

        return this.master.get() && this.deadLineTS.get() > Util.nowMS();
    }
}
