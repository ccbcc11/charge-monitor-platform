package com.ccbcc.charge.monitor.module.report.controller;

import com.ccbcc.charge.monitor.common.result.Result;
import com.ccbcc.charge.monitor.module.report.service.ReportService;
import com.ccbcc.charge.monitor.module.report.vo.OverviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 报表接口
 */
@Tag(name = "报表模块")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/report")
public class ReportController {

    private final ReportService reportService;

    /**
     * 今日运行概览
     */
    @Operation(summary = "今日运行概览")
    @GetMapping("/overview")
    public Result<OverviewVO> getOverview() {
        return Result.success(reportService.getOverview());
    }
}