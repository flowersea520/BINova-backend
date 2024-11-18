package com.lxc.binova.model.dto.chart;

import lombok.Data;

import java.io.Serializable;

/**
 * 创建请求
 *  前端传过来的 创建图表的参数
 */
@Data
public class ChartAddRequest implements Serializable {
    // 用户前端不用传用户id，因为从登录态中去拿就可以了，而不是让用户去拿
    /**
     *  图表名称
     */
    private String name;

    /**
     * 分析目标
     */
    private String goal;

    /**
     * 图表数据
     */
    private String chartData;

    /**
     * 图表类型
     */
    private String chartType;


    private static final long serialVersionUID = 1L;
}