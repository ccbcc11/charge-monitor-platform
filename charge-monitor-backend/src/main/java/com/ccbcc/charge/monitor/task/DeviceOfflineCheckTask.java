package com.ccbcc.charge.monitor.task;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DeviceOfflineCheckTask {

    @Scheduled(fixedDelay = 60_000)
    public void checkOfflineDevices() {
        // TODO: Scan device heartbeat keys and generate offline alarms.
    }
}
