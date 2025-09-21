# 使用说明

套件基于下面技术栈开发：

- java17
- springboot3.5.6
- mybatisplus3.5.7
- mysql8
- hutool5.8.38

## pom.xml

```pom
        <dependency>
            <groupId>com.thinban</groupId>
            <artifactId>tb-mp-quick-spring-boot-starter</artifactId>
            <version>1.0.0</version>
        </dependency>
```

## Application.java (启动类配置包扫描)

包含SpringBootApplication注解的类中： @ComponentScan(basePackages = {"com.thinban", "你项目中的包名"})

## application.properties

```text
spring.application.name=demo0916
spring.jackson.date-format=yyyy-MM-dd HH:mm:ss
spring.jackson.time-zone=GMT+8
spring.datasource.url=jdbc:mysql://localhost:3306/mp0614
spring.datasource.username=root
spring.datasource.password=root
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
mybatis-plus.configuration.log-impl=org.apache.ibatis.logging.stdout.StdOutImpl
mybatis-plus.global-config.db-config.logic-delete-field=boolDel
mybatis-plus.global-config.db-config.logic-delete-value=1
mybatis-plus.global-config.db-config.logic-not-delete-value=0
tb.mp.mapper=com.example.mapper
tb.jwt.key=123456789
tb.jwt.excludes=/app/**
```

# 版本

## V1.0.1

- 新增StepMonitor注解，结合AOP监控方法使用耗时。
- 引入MDC，见 TraceIdUtils.java

<details><summary>StepMonitor注解+TraceIdUtils 使用方法</summary>

```
 try {
            TraceIdUtils.initTraceId();
            testService.consume();
            return 1;
        } finally {
            TraceIdUtils.clearTraceId();
        }


@Service
public class TestService {
    private static final Logger log = LoggerFactory.getLogger(TestService.class);
    @Resource
    TestService self;

    // 消费主方法（调用各步骤）
    @StepMonitor(stepName = "consume")
    public void consume() {
        // 步骤1：解析消息（标记步骤名为"parse_msg"）
        self.parseMsg();
        // 步骤2：业务处理（标记步骤名为"biz_process"）
        self.processBiz();
        // 步骤3：结果存储（标记步骤名为"save_result"）
        self.saveResult();
    }

    // 被监控的步骤1：解析消息
    @StepMonitor(stepName = "parse_msg")
    public void parseMsg() {
        log.info("parseMsg");
    }

    // 被监控的步骤2：业务处理
    @StepMonitor(stepName = "biz_process")
    public void processBiz() {
        log.info("biz_process");

    }

    // 被监控的步骤3：结果存储
    @StepMonitor(stepName = "save_result")
    public void saveResult() {
        log.info("save_result");
    }
}

```

```
</details>

## V1.0.0

- 统一的响应体 R.java
- 全局异常处理 GlobalExceptionHandler.java(暂时处理IllegalArgumentException和Exception，响应json)
- mybatis分页自动配置
- excel导出类 ExcelUtil.java
- 新增JwtFilter，JwtThreadLocalHolder 获取当前登录用户id
- RateLimitFilter
- SecurityHeaderFilter
- SensitiveInfoFilter
- XssFilter

<details><summary>ExcelUtil调用方法</summary> 

```text
/**
     * 导入Excel文件批量添加应用信息
     *
     * @param file 上传的Excel文件
     * @return 导入结果
     */
    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importApplications(@RequestParam("file") MultipartFile file) {
        return ExcelUtil.importExcel(file, () -> {
            // 使用EasyExcel读取文件
            List<App> applicationList = new ArrayList<>();

            try {
                EasyExcel.read(file.getInputStream(), App.class, new AnalysisEventListener<App>() {
                    // 每读取一行数据都会调用此方法
                    @Override
                    public void invoke(App App, AnalysisContext context) {
                        applicationList.add(App);
                    }

                    // 读取完成后调用
                    @Override
                    public void doAfterAllAnalysed(AnalysisContext context) {
                        // 可以在这里添加一些读取完成后的处理逻辑
                    }

                    // 处理异常
                    @Override
                    public void onException(Exception exception, AnalysisContext context) {
                        throw new RuntimeException("Excel解析错误: " + exception.getMessage());
                    }
                }).sheet().doRead();

                // 保存数据到数据库
                appService.saveBatch(applicationList);
                return applicationList;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }


    /**
     * 导出应用信息为Excel文件
     *
     * @param response HTTP响应对象
     */
    @GetMapping("/export")
    public void exportApplications(HttpServletResponse response) {
        ExcelUtil.exportExcel(response, () -> {
            try {
                // 获取要导出的数据
                List<App> applicationList = appService.list();
                // 使用EasyExcel写入Excel文件并通过响应流返回
                EasyExcel.write(response.getOutputStream(), App.class).sheet("应用信息").doWrite(applicationList);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

```

</details>