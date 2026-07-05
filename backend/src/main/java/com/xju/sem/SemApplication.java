package com.xju.sem;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 新疆大学校友圈与双圈成长导航平台 —— 后端启动类。
 * 单体应用，按业务模块分包（module/*），模块间仅通过 Service 接口弱耦合。
 */
@SpringBootApplication
@EnableAsync          // 采纳→候选等 AFTER_COMMIT 异步事件
@EnableScheduling     // 机会状态推进、知识过期降权、路由兜底等定时任务
@MapperScan("com.xju.sem.module.**.mapper")
public class SemApplication {
    public static void main(String[] args) {
        SpringApplication.run(SemApplication.class, args);
    }
}
