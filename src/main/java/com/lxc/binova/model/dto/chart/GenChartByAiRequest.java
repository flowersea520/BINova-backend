package com.lxc.binova.model.dto.chart;

import lombok.Data;

import java.io.Serializable;

/**
 * 生成图表类型 请求
 */
@Data
public class GenChartByAiRequest implements Serializable {
    // 可以根据以下三个属性去生成对应的图表

    /**
     * 图表名称
     */
    private String name;

    /**
     * 分析目标
     */
    private String goal;

    /**
     * 图表类型
     */
    private String chartType;

    private static final long serialVersionUID = 1L;
}