package com.ccbcc.charge.monitor.module.report.service;

import com.ccbcc.charge.monitor.module.report.vo.OverviewVO;

/**
 * 报表服务接口
 */
public interface ReportService {

    /**
     * 查询今日运行概览
     *
     * @return 今日运行概览
     */
    OverviewVO getOverview();
}