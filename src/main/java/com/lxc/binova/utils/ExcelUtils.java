package com.lxc.binova.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.support.ExcelTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ResourceUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Excel相关的工具类
 *
 * @author mortal
 * @date 2024/6/5 8:39
 */
@Slf4j
public class ExcelUtils {
	/**
	 * excel 转 csv
	 * 在 Web 开发中，通常使用 multipart/form-data 格式进行文件上传。
	 * 而 @RequestPart 注解可以解析这个格式，将文件上传的部分映射为相应类型 的参数。
	 *
	 * @param multipartFile 文件对象  这个参数类型允许你在方法中操作上传的文件。
	 * @return
	 */
	public static String excelToCsv(MultipartFile multipartFile) {
//		File file = null;
//		try {
//			file = ResourceUtils.getFile("classpath:网站数据.xlsx");
//		} catch (FileNotFoundException e) {
//			throw new RuntimeException(e);
//		}
		List<Map<Integer, String>> list = null;
		try {
			// 输入流：读文件 ； 输出流：写文件
			// 要读取一个文件中的内容，你需要将这个文件与程序连接起来。
			list = EasyExcel.read(multipartFile.getInputStream())
					.excelType(ExcelTypeEnum.XLSX)
					.sheet()
					.headRowNumber(0)
					.doReadSync();
		} catch (Exception e) {
			log.error("表格处理错误：", e);
			e.printStackTrace();
		}
		System.out.println(list);

		if (CollUtil.isEmpty(list)) {
			return "";
		}

		// 转换为 csv
		// StringBuilder 是 Java 中一个用于创建和操作可变字符串的类，它提供了各种方法来高效地拼接、修改字符串内容。
		// 可以将stringBuilder理解为：字符串构造器，然后一直对字符串操作，最后返回最终的字符串
		StringBuilder stringBuilder = new StringBuilder();

		// 读取表头
		// LinkedHashMap 会按照元素插入的顺序进行迭代，这样可以确保读取时的顺序与插入时的顺序一致。
		// {0=日期, 1=用户数, 2=null, 3=null}
		LinkedHashMap<Integer, String> headerMap = (LinkedHashMap) list.get(0);

		// 将 表头元素中，value为null的过滤掉（换句话说：将不为null的元素，单独弄成集合）
		List<String> headerList = headerMap.values().stream().filter(ObjectUtil::isNotNull).collect(Collectors.toList());

		//  将 headerList 中的元素用逗号连接成一个字符串，并添加到 stringBuilder 中，最后加上一个换行符
		stringBuilder.append(StrUtil.join(",", headerList)).append("\n");

		// String 工具类中的join方法是：是将多个元素拼接成一个字符串，可以指定连接的分隔符。
		System.out.println(StrUtil.join(",", headerList));
		// 读取数据（除去表头）
		for (int i = 1; i < list.size(); i++) {
			LinkedHashMap dataMap = (LinkedHashMap) list.get(i);
			// 将（除表头）数据中的元素， 拼接成字符串，用逗号
//			{0=1号, 1=10个, 2=null, 3=null}, {0=2号, 1=20个}, {0=3号, 1=30个}
			//  将 数据 元素中，value为null的过滤掉（换句话说：将不为null的元素，单独弄成集合）
			List<String> dataList = (List<String>) dataMap.values().stream().filter(obj -> ObjUtil.isNotNull(obj)).collect(Collectors.toList());

			stringBuilder.append(StrUtil.join(",", dataList)).append("\n");

			System.out.println(StrUtil.join(",", dataList));
		}
		// StringBuilder 是可变的，它可以有效地在内存中操作字符串，而不会频繁创建新的字符串对象。
		// 下面就是标准的csv格式：CSV 文件通常是纯文本文件，每行代表一条记录，字段之间用逗号分隔
// 日期,用户数
//1号,10个
//2号,20个
//3号,30个
		return stringBuilder.toString();
	}


	public static void main(String[] args) {
		System.out.println(excelToCsv(null));
	}



}
