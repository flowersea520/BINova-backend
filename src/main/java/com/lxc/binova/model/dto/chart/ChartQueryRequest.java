package com.lxc.binova.model.dto.chart;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.lxc.binova.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 查询请求
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ChartQueryRequest extends PageRequest implements Serializable {
    /**
     * id
     */
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
     * 图表类型
     */
    private String chartType;

    /**
     *  用户id（知道是哪个用户上传的图表）
     *  （根据用户id去查对应的图表）
     *
     */
    private Long userId;



    private static final long serialVersionUID = 1L;
}