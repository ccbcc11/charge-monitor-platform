package com.ccbcc.charge.monitor.task;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DailyReportTask {

    @Scheduled(cron = "0 5 0 * * ?")
    public void generateDailyReport() {
        // TODO: Aggregate device, alarm, and work order metrics into daily_report.
    }
}
