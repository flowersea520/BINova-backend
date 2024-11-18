package com.lxc.binova.model.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 图表信息表
 * @TableName chart
 */
@TableName(value ="chart")
@Data
public class Chart implements Serializable{
    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     *  图表名称
     */
    private String name;


    /**
     * 分析目标
     */
    private String goal;

    /**
     * 图表数据 (会传 excel转换后 的 csv类型）
     */
    private String chartData;

    /**
     * 图表类型
     */
    private String chartType;

    /**
     *  任务状态
     */
    private String status;

    /**
     *  执行信息
     */
    private String execMessage;



    /**
     * 生成的图表数据
     */
    private String genChart;

    /**
     * 生成的分析结论
     */
    private String genResult;

    /**
     * 创建用户id（知道是哪个用户上传的图表） -- 一般都是登录的用户id
     */
    private Long userId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}