package com.xju.sem.module.profile.service.impl;

import com.xju.sem.module.user.entity.StudentProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 在校生年级档年度重算（设计 §9 定时任务）。每年 9 月 1 日按 enroll_year 批量重算 grade_level，
 * 不由用户手填。{@code @EnableScheduling} 已在 SemApplication 开启。
 *
 * <p>简化算法：gradeLevel = clamp(currentYear - enrollYear + 1, 1, 10)，把连续学年映射到
 * 1..10 档（1 大一 .. 4 大四，5 研一 ..）。因 student_profile 无学历层次列，无法据本硕博程长精确
 * 分段，采用连续学年近似（见实现说明"假设与简化"）；数值仅用于路由/推荐的高年级判定，容错足够。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GradeLevelRecalcJob {

    private static final int MIN_LEVEL = 1;
    private static final int MAX_LEVEL = 10;

    private final IdentityProfileSupport identitySupport;

    /** 每年 9 月 1 日 03:00 触发。 */
    @Scheduled(cron = "0 0 3 1 9 ?")
    public void recompute() {
        int year = LocalDate.now().getYear();
        int updated = 0;
        for (StudentProfile p : identitySupport.listAllStudentProfiles()) {
            if (p.getEnrollYear() == null) {
                continue;
            }
            int level = clamp(year - p.getEnrollYear() + 1);
            if (!Integer.valueOf(level).equals(p.getGradeLevel())) {
                p.setGradeLevel(level);
                identitySupport.updateStudentProfile(p);
                updated++;
            }
        }
        log.info("年级档年度重算完成：year={}, updated={}", year, updated);
    }

    private int clamp(int v) {
        return Math.max(MIN_LEVEL, Math.min(MAX_LEVEL, v));
    }
}
