package com.xju.sem.module.timeline.util;

import com.xju.sem.module.timeline.enums.Stage;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

/**
 * 学期日历换算纯函数工具（§6.1）：不依赖数据库、可独立单元测试。
 *
 * <p>本实现相对 06 详细设计 §6.1 的裁剪与对齐：设计稿以"周次"（suggested_time 数字）+ 学期起始
 * 日精确到天换算建议截止日；而 schema.sql 落地为 {@code suggested_time VARCHAR}（人类可读展示串，
 * 如"大一上第 8 周"）+ {@code suggested_month TINYINT(1-12)}（供逾期比对的机读月份）。因此本工具
 * 以 <b>stage + suggested_month + enrollYear</b> 换算出"建议完成的自然年月"，逾期判定按
 * <b>月粒度</b>比对（对齐本阶段任务书"按 suggested_month vs 当前月算已过期节点"）；月内不再细分
 * 到具体某天，规避 schema 未存周次带来的伪精度。
 */
public final class TimelineCalendarUtil {

    /** 秋季学期开学月（9 月）。 */
    private static final int FALL_START_MONTH = 9;
    /** 春季学期开学月（2 月）。 */
    private static final int SPRING_START_MONTH = 2;

    private TimelineCalendarUtil() {
    }

    /**
     * 换算节点建议完成的自然年月：由 {@code stage} 定位学期所在自然年，再落到 {@code month}。
     * 秋季学期跨自然年——8~12 月归属学期起始年，1~7 月视为寒假后溢出到次年。
     */
    public static YearMonth suggestedYearMonth(Stage stage, int month, int enrollYear) {
        int baseYear = enrollYear + stage.getYearOffset();
        int year;
        if (stage.isFall()) {
            year = (month >= FALL_START_MONTH - 1) ? baseYear : baseYear + 1;
        } else {
            year = baseYear;
        }
        return YearMonth.of(year, month);
    }

    /**
     * 建议完成的"截止日"= 建议年月的当月最后一天（把"某月内完成"读作"月末前完成"，供 daysOverdue
     * 展示；逾期是否成立仍以月粒度为准，见 {@link #isOverdue}）。
     */
    public static LocalDate suggestedDeadline(Stage stage, int month, int enrollYear) {
        return suggestedYearMonth(stage, month, enrollYear).atEndOfMonth();
    }

    /** 某学期的开学年月（秋季 9 月 / 春季 2 月），供当前学期反查与逾期窗口界定。 */
    public static YearMonth semesterStart(Stage stage, int enrollYear) {
        int baseYear = enrollYear + stage.getYearOffset();
        return YearMonth.of(baseYear, stage.isFall() ? FALL_START_MONTH : SPRING_START_MONTH);
    }

    /**
     * 反查 {@code today} 落在 8 个学期窗口的哪一个：
     * 早于大一上开学 → 钳位为 {@link Stage#GRADE1_1}；晚于大四下结束（大四下开学约 7 个月后）
     * → 返回 {@code null}（已毕业/超龄，不在 P16 服务范围）。
     */
    public static Stage currentStage(LocalDate today, int enrollYear) {
        YearMonth cur = YearMonth.from(today);
        YearMonth firstStart = semesterStart(Stage.GRADE1_1, enrollYear);
        if (cur.isBefore(firstStart)) {
            return Stage.GRADE1_1;
        }
        YearMonth graduationGate = semesterStart(Stage.GRADE4_2, enrollYear).plusMonths(7);
        if (!cur.isBefore(graduationGate)) {
            return null;
        }
        Stage result = Stage.GRADE1_1;
        for (Stage s : Stage.values()) {
            if (!semesterStart(s, enrollYear).isAfter(cur)) {
                result = s;
            }
        }
        return result;
    }

    /** 月粒度逾期判定：当前年月严格晚于建议年月即逾期。 */
    public static boolean isOverdue(YearMonth suggested, YearMonth current) {
        return current.isAfter(suggested);
    }

    /** 逾期月数（未逾期返回 0）。 */
    public static int monthsOverdue(YearMonth suggested, YearMonth current) {
        if (!current.isAfter(suggested)) {
            return 0;
        }
        return (int) ChronoUnit.MONTHS.between(suggested, current);
    }

    /** 逾期天数（相对建议截止日 = 建议月月末；未逾期返回 0）。供展示层"已逾期 N 天"。 */
    public static long daysOverdue(LocalDate suggestedDeadline, LocalDate today) {
        long d = ChronoUnit.DAYS.between(suggestedDeadline, today);
        return Math.max(d, 0);
    }
}
