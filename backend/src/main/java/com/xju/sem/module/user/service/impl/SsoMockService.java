package com.xju.sem.module.user.service.impl;

import com.xju.sem.module.user.entity.MockStudentRoster;
import com.xju.sem.module.user.mapper.MockStudentRosterMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 模拟统一身份认证核验（演示期 mock）。
 * 实现：以 mock_student_roster 作为“学号-姓名”种子对照表——学号命中且真实姓名一致即视为核验通过。
 * 接口签名与真实教务/统一身份认证对接预留一致，未来替换实现即可。
 */
@Service
@RequiredArgsConstructor
public class SsoMockService {

    private final MockStudentRosterMapper rosterMapper;

    /**
     * 核验学号与姓名。
     * @return 命中且姓名一致返回学籍记录（含 college/major_name/enroll_year 供档案回写）；否则 null。
     */
    public MockStudentRoster verify(String studentNo, String realName) {
        if (!StringUtils.hasText(studentNo)) {
            return null;
        }
        MockStudentRoster roster = rosterMapper.selectById(studentNo.trim());
        if (roster == null) {
            return null;
        }
        if (StringUtils.hasText(realName) && !realName.trim().equals(roster.getRealName())) {
            return null;
        }
        return roster;
    }
}
