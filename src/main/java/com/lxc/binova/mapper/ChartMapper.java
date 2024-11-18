package com.lxc.binova.mapper;

import com.lxc.binova.model.entity.Chart;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;
import java.util.Map;

/**
* @author lxc
* @description 针对表【chart(图表信息表)】的数据库操作Mapper
* @createDate 2024-06-03 18:16:09
* @Entity com.lxc.binova.model.entity.Chart
*/
public interface ChartMapper extends BaseMapper<Chart> {
	List<Map<String, Object>> queryChartData(String querySql);
}




