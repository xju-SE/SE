package com.xju.sem.module.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 模拟学籍库（表 mock_student_roster），在校生 SSO 认证的核验数据源（演示期 mock）。
 * 主键为 student_no（无自增 id / 逻辑删除），故不继承 BaseEntity。
 */
@Data
@TableName("mock_student_roster")
public class MockStudentRoster {

    @TableId(value = "student_no", type = IdType.INPUT)
    private String studentNo;

    private String realName;

    private String college;

    private String majorName;

    private Integer enrollYear;
}
