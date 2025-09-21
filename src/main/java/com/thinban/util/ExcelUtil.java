package com.thinban.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class ExcelUtil {
    /**
     * 导入Excel文件批量添加应用信息
     *
     * @param file 上传的Excel文件
     * @return 导入结果
     */
    public static ResponseEntity<Map<String, Object>> importExcel(MultipartFile file, Supplier f) {
        Map<String, Object> result = new HashMap<>();

        // 验证文件是否为空
        if (file.isEmpty()) {
            result.put("success", false);
            result.put("message", "请选择要上传的Excel文件");
            return ResponseEntity.badRequest().body(result);
        }

        // 验证文件格式
        String fileName = file.getOriginalFilename();
        if (!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls")) {
            result.put("success", false);
            result.put("message", "请上传Excel格式的文件");
            return ResponseEntity.badRequest().body(result);
        }

        try {
            //回调函数->获取数据
            Object o = f.get();
            Assert.isTrue(o != null, "");

            // 构建成功响应
            result.put("success", true);
            result.put("message", "Excel导入成功");
            result.put("count", ((List) o).size());
            result.put("data", (List) o);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            // 构建错误响应
            result.put("success", false);
            result.put("message", "Excel导入失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }


    /**
     * 导出应用信息为Excel文件
     *
     * @param response HTTP响应对象
     */
    public static void exportExcel(HttpServletResponse response, Runnable f) {
        try {
            // 设置响应头信息
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("UTF-8");
            // 生成文件名，包含当前时间戳防止重复
            String fileName = URLEncoder.encode("应用信息_" + System.currentTimeMillis(), "UTF-8").replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=UTF-8''" + fileName + ".xlsx");

            //回调函数->导出数据
            f.run();
        } catch (Exception e) {
            // 处理导出异常
            try {
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("success", false);
                errorResult.put("message", "Excel导出失败: " + e.getMessage());
                response.getWriter().write(new ObjectMapper().writeValueAsString(errorResult));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
