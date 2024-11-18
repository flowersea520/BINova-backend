package com.lxc.binova.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lxc.binova.model.entity.Chart;
import com.lxc.binova.service.ChartService;
import com.lxc.binova.mapper.ChartMapper;
import org.springframework.stereotype.Service;

/**
* @author lxc
* @description 针对表【chart(图表信息表)】的数据库操作Service实现
* @createDate 2024-06-03 18:16:09
*/
@Service
public class ChartServiceImpl extends ServiceImpl<ChartMapper, Chart>
    implements ChartService{

}




