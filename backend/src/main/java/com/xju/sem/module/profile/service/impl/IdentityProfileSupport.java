package com.xju.sem.module.profile.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xju.sem.common.exception.BusinessException;
import com.xju.sem.module.profile.enums.Visibility;
import com.xju.sem.module.user.constant.AuthConst;
import com.xju.sem.module.user.entity.AlumniProfile;
import com.xju.sem.module.user.entity.StudentProfile;
import com.xju.sem.module.user.mapper.AlumniProfileMapper;
import com.xju.sem.module.user.mapper.StudentProfileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 身份档案访问支撑（<b>M2 与 M1 之间受控的单一接缝</b>）。
 *
 * <p><b>为什么直接持有 M1 的 Mapper</b>：schema §3.1/§3.2 把 student_profile / alumni_profile 归属
 * M2 的画像域，但这两张表的 entity + BaseMapper 在 M1 认证流程中已建（认证时写入身份字段）。为不
 * <b>重复</b>其 entity（会造成两个 @TableName 指向同一物理表），M2 复用 M1 的
 * {@link StudentProfileMapper}/{@link AlumniProfileMapper}（二者均为无业务逻辑的裸 BaseMapper，
 * 复用不会绕过任何封装规则）。全部跨模块表访问收敛在本类这一处，便于将来 M1 若下沉出
 * ProfileRepository 契约时集中替换。M1 内部亦有同类受控例外（MajorTagMapper 只读 M2 的 tag 表）。
 */
@Component
@RequiredArgsConstructor
public class IdentityProfileSupport {

    private final StudentProfileMapper studentProfileMapper;
    private final AlumniProfileMapper alumniProfileMapper;

    // ---------------- 查询 ----------------

    public StudentProfile findStudentProfile(Long userId) {
        return studentProfileMapper.selectOne(new LambdaQueryWrapper<StudentProfile>()
                .eq(StudentProfile::getUserId, userId));
    }

    public AlumniProfile findAlumniProfile(Long userId) {
        return alumniProfileMapper.selectOne(new LambdaQueryWrapper<AlumniProfile>()
                .eq(AlumniProfile::getUserId, userId));
    }

    /** 画像不存在按 40202 处理（未完成认证或未初始化画像）。 */
    public StudentProfile requireStudentProfile(Long userId) {
        StudentProfile p = findStudentProfile(userId);
        if (p == null) {
            throw new BusinessException(40202, "在校生画像不存在，请先完成身份认证");
        }
        return p;
    }

    public AlumniProfile requireAlumniProfile(Long userId) {
        AlumniProfile p = findAlumniProfile(userId);
        if (p == null) {
            throw new BusinessException(40202, "毕业生档案不存在，请先完成身份认证");
        }
        return p;
    }

    /**
     * 取用户所属专业标签 id（先查在校生档案，再查毕业生档案）。用于 §6.2 SAME_MAJOR 判定。
     * 两者皆无返回 null（视为无法命中同专业，SAME_MAJOR 对其恒不可见）。
     */
    public Long majorTagIdOf(Long userId) {
        StudentProfile sp = findStudentProfile(userId);
        if (sp != null) {
            return sp.getMajorTagId();
        }
        AlumniProfile ap = findAlumniProfile(userId);
        return ap == null ? null : ap.getMajorTagId();
    }

    /** 按专业（+可选学院、+可选最低年级档）列出在校生档案，供 listVerifiedUsersByMajor。 */
    public java.util.List<StudentProfile> listStudentProfilesByMajor(Long majorTagId, String college,
                                                                     Integer minGradeLevel) {
        LambdaQueryWrapper<StudentProfile> w = new LambdaQueryWrapper<StudentProfile>()
                .eq(StudentProfile::getMajorTagId, majorTagId);
        if (college != null && !college.isBlank()) {
            w.eq(StudentProfile::getCollege, college);
        }
        if (minGradeLevel != null) {
            w.ge(StudentProfile::getGradeLevel, minGradeLevel);
        }
        w.last("limit 200");
        return studentProfileMapper.selectList(w);
    }

    /** 按专业（+可选学院）列出毕业生档案，供 listVerifiedUsersByMajor。 */
    public java.util.List<AlumniProfile> listAlumniProfilesByMajor(Long majorTagId, String college) {
        LambdaQueryWrapper<AlumniProfile> w = new LambdaQueryWrapper<AlumniProfile>()
                .eq(AlumniProfile::getMajorTagId, majorTagId);
        if (college != null && !college.isBlank()) {
            w.eq(AlumniProfile::getCollege, college);
        }
        w.last("limit 200");
        return alumniProfileMapper.selectList(w);
    }

    /**
     * 取用户所属学院（先查在校生档案，再查毕业生档案）。用于 §6.4 冷启动"同学院跨专业"收窄（C16）。
     * 两者皆无返回 null（视为无法定位学院，调用方据此决定退回策略）。
     */
    public String collegeOf(Long userId) {
        StudentProfile sp = findStudentProfile(userId);
        if (sp != null && sp.getCollege() != null && !sp.getCollege().isBlank()) {
            return sp.getCollege();
        }
        AlumniProfile ap = findAlumniProfile(userId);
        return ap == null ? null : ap.getCollege();
    }

    /** 该用户毕业档案是否命中指定专业（供 hasMajorTag 兜底判断）。 */
    public boolean alumniHasMajor(Long userId, Long majorTagId) {
        AlumniProfile ap = findAlumniProfile(userId);
        return ap != null && majorTagId != null && majorTagId.equals(ap.getMajorTagId());
    }

    /** 全量在校生档案（供 grade_level 年度批量重算定时任务；本期数据量小直接全表扫描）。 */
    public java.util.List<StudentProfile> listAllStudentProfiles() {
        return studentProfileMapper.selectList(null);
    }

    // ---------------- 写入 ----------------

    public void updateStudentProfile(StudentProfile profile) {
        studentProfileMapper.updateById(profile);
    }

    public void updateAlumniProfile(AlumniProfile profile) {
        alumniProfileMapper.updateById(profile);
    }

    /** 贡献者徽章写入（幂等）：is_contributor_badge=1 + honor_cert_url。 */
    public void grantContributorBadge(Long userId, String honorCertUrl) {
        AlumniProfile p = requireAlumniProfile(userId);
        p.setIsContributorBadge(1);
        if (honorCertUrl != null) {
            p.setHonorCertUrl(honorCertUrl);
        }
        alumniProfileMapper.updateById(p);
    }

    /** helped/adopted 计数 DB 侧原子自增（不做"读出再写回"，避免并发丢更新）。 */
    public void incrementHelpedCount(Long userId) {
        alumniProfileMapper.update(null, new LambdaUpdateWrapper<AlumniProfile>()
                .eq(AlumniProfile::getUserId, userId)
                .setSql("helped_count = helped_count + 1"));
    }

    public void incrementAdoptedCount(Long userId) {
        alumniProfileMapper.update(null, new LambdaUpdateWrapper<AlumniProfile>()
                .eq(AlumniProfile::getUserId, userId)
                .setSql("adopted_count = adopted_count + 1"));
    }

    // ---------------- 常量 ----------------

    /** 校验可见级别取值合法（复用全局 SELF/SAME_MAJOR/PUBLIC 口径）。 */
    public boolean isValidVisibility(String v) {
        return Visibility.isValid(v);
    }

    public boolean isAlumni(String role) {
        return AuthConst.RoleName.ALUMNI.equals(role);
    }

    public boolean isStudent(String role) {
        return AuthConst.RoleName.STUDENT.equals(role);
    }
}
