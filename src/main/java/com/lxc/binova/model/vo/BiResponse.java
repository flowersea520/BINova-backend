package com.lxc.binova.model.vo;

import lombok.Data;

/**
 * @author mortal
 * @date 2024/6/6 20:32
 */
@Data
public class BiResponse {
	/**
	 *   ai模型生成的图表chart代码
	 */
	private String genChart;
	/**
	 *  ai模型生成的结论
	 */
	private String genResult;

	/**
	 *  每次ai生成的图表id
	 */
	private Long chartId;
}
