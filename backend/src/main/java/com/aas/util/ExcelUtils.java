package com.aas.util;

import com.aas.common.exception.BizException;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Excel 导入导出工具 (基于 EasyExcel)
 */
public final class ExcelUtils {

    private ExcelUtils() {
    }

    /**
     * 导出 Excel 到响应流
     *
     * @param response  响应
     * @param fileName  文件名(不含扩展名)
     * @param sheetName sheet 名称
     * @param headers   表头
     * @param data      数据集合
     * @param rowMapper 行映射函数
     */
    public static <T> void export(HttpServletResponse response, String fileName, String sheetName,
                                  String[] headers, List<T> data, Function<T, Object[]> rowMapper) {
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
            response.setHeader("Content-Disposition",
                    "attachment;filename=" + encoded + ".xlsx;filename*=utf-8''" + encoded + ".xlsx");

            List<List<String>> head = Arrays.stream(headers)
                    .map(List::of)
                    .collect(Collectors.toList());

            List<List<Object>> rows = new ArrayList<>();
            for (T item : data) {
                Object[] values = rowMapper.apply(item);
                rows.add(new ArrayList<>(Arrays.asList(values)));
            }

            try (ExcelWriter writer = EasyExcel.write(response.getOutputStream()).build()) {
                WriteSheet sheet = EasyExcel.writerSheet(sheetName).head(head).build();
                writer.write(rows, sheet);
            }
        } catch (IOException e) {
            throw new BizException("导出失败：" + e.getMessage());
        }
    }

    /**
     * 读取上传的 Excel 为行数据(按列索引)
     */
    public static List<Map<Integer, String>> read(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException("请选择要上传的文件");
        }
        String name = file.getOriginalFilename();
        if (name == null || !(name.toLowerCase().endsWith(".xlsx") || name.toLowerCase().endsWith(".xls"))) {
            throw new BizException("仅支持 .xlsx / .xls 格式的文件");
        }
        try {
            return EasyExcel.read(file.getInputStream()).sheet().doReadSync();
        } catch (IOException e) {
            throw new BizException("文件解析失败：" + e.getMessage());
        }
    }

    /** 安全取值 */
    public static String cell(Map<Integer, String> row, int index) {
        if (row == null) {
            return null;
        }
        String value = row.get(index);
        return value == null ? null : value.trim();
    }
}
