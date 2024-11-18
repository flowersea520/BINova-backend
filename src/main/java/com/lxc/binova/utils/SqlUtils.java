package com.lxc.binova.utils;

import org.apache.commons.lang3.StringUtils;

/**
 * SQL 工具
 */
public class SqlUtils {

    /**
     * 校验排序字段是否合法（防止 SQL 注入）
     * 这个静态方法 validSortField 接受一个字符串类型参数 sortField，
     * 并返回一个布尔值。其功能是验证排序字段是否  有效（valid），如果 sortField 为空，则返回 false；
     * 如果 sortField 中包含特殊字符（"=", "(", ")", " "），则返回 false；否则返回 true。
     *
     * @param sortField
     * @return
     */
    public static boolean validSortField(String sortField) {
        if (StringUtils.isBlank(sortField)) {
            return false;
        }
        return !StringUtils.containsAny(sortField, "=", "(", ")", " ");
    }
}
