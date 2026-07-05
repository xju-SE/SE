package com.xju.sem.module.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xju.sem.common.exception.BusinessException;
import com.xju.sem.common.result.PageResult;
import com.xju.sem.common.result.ResultCode;
import com.xju.sem.module.notification.service.NotificationService;
import com.xju.sem.module.user.constant.AuthConst.AppStatus;
import com.xju.sem.module.user.constant.AuthConst.AuthStatus;
import com.xju.sem.module.user.constant.AuthConst.DegreeType;
import com.xju.sem.module.user.constant.AuthConst.GuarantorStatus;
import com.xju.sem.module.user.constant.AuthConst.RoleName;
import com.xju.sem.module.user.constant.AuthConst.VerifyMethod;
import com.xju.sem.module.user.dto.AuthApplicationDTO;
import com.xju.sem.module.user.dto.AuthApplicationQuery;
import com.xju.sem.module.user.dto.BatchInviteCodeRequest;
import com.xju.sem.module.user.dto.InviteCodeCheckDTO;
import com.xju.sem.module.user.dto.ResubmitAuthApplicationRequest;
import com.xju.sem.module.user.dto.SubmitAuthApplicationRequest;
import com.xju.sem.module.user.entity.AlumniProfile;
import com.xju.sem.module.user.entity.AuthApplication;
import com.xju.sem.module.user.entity.MockStudentRoster;
import com.xju.sem.module.user.entity.StudentProfile;
import com.xju.sem.module.user.entity.User;
import com.xju.sem.module.user.event.AuthApplicationSubmittedEvent;
import com.xju.sem.module.user.mapper.AlumniProfileMapper;
import com.xju.sem.module.user.mapper.AuthApplicationMapper;
import com.xju.sem.module.user.mapper.StudentProfileMapper;
import com.xju.sem.module.user.mapper.UserMapper;
import com.xju.sem.module.user.service.AuthApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 认证申请服务实现（M1 核心）。三条分级认证路径 + 邀请码机制 + 终审状态机。
 * 审核类更新一律用状态 CAS（UPDATE ... WHERE status=期望前置状态，影响行数=0 抛冲突）；
 * 提交后经 ApplicationEventPublisher 发布 AuthApplicationSubmittedEvent，事务提交后由 M7 建 audit_task。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthApplicationServiceImpl implements AuthApplicationService {

    private final AuthApplicationMapper authApplicationMapper;
    private final UserMapper userMapper;
    private final StudentProfileMapper studentProfileMapper;
    private final AlumniProfileMapper alumniProfileMapper;
    private final SsoMockService ssoMockService;
    private final MajorTagResolver majorTagResolver;
    private final InviteCodeAllocator inviteCodeAllocator;
    private final ApplicationEventPublisher eventPublisher;
    /** 跨模块契约依赖：由通知/M7 模块提供，弱依赖（缺失时担保/结果通知降级为不发送）。 */
    private final ObjectProvider<NotificationService> notificationServiceProvider;

    // ==================== 提交入口 ====================

    @Override
    @Transactional
    public AuthApplicationDTO submit(Long userId, SubmitAuthApplicationRequest request) {
        User user = requireUser(userId);
        if (AuthStatus.VERIFIED.equals(user.getAuthStatus())) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "当前账号已通过认证");
        }
        String vm = request.getVerifyMethod();
        return switch (vm == null ? "" : vm) {
            case VerifyMethod.STUDENT_SSO -> handleStudentSso(user, request);
            case VerifyMethod.STUDENT_MANUAL -> handleStudentManual(user, request);
            case VerifyMethod.ALUMNI_INVITE_CODE -> handleInviteClaim(user, request);
            case VerifyMethod.ALUMNI_MANUAL_GUARANTEE -> handleGuarantee(user, request);
            default -> throw new BusinessException(ResultCode.PARAM_INVALID, "不支持的认证方式");
        };
    }

    private AuthApplicationDTO handleStudentSso(User user, SubmitAuthApplicationRequest req) {
        requireRole(user, RoleName.STUDENT);
        requireText(req.getRealName(), "真实姓名");
        requireText(req.getStudentNo(), "学号");
        AuthApplication app = newApplication(user.getId(), RoleName.STUDENT, VerifyMethod.STUDENT_SSO, req);
        app.setStatus(AppStatus.PENDING);
        authApplicationMapper.insert(app);

        MockStudentRoster roster = ssoMockService.verify(req.getStudentNo(), req.getRealName());
        if (roster != null) {
            app.setStatus(AppStatus.APPROVED);
            app.setAutoApproved(1);
            authApplicationMapper.updateById(app);
            upsertStudentProfile(user.getId(), req.getRealName(), req.getStudentNo(),
                    coalesce(req.getCollege(), roster.getCollege()),
                    coalesce(req.getMajorText(), roster.getMajorName()), roster.getEnrollYear());
            setUserAuthStatus(user, AuthStatus.VERIFIED);
            eventPublisher.publishEvent(new AuthApplicationSubmittedEvent(app.getId(), true));
            return toDTO(app, "认证成功，学号核验通过（请重新登录以解锁完整功能）");
        }
        app.setStatus(AppStatus.UNDER_REVIEW);
        authApplicationMapper.updateById(app);
        setUserAuthStatus(user, AuthStatus.PENDING);
        eventPublisher.publishEvent(new AuthApplicationSubmittedEvent(app.getId(), false));
        return toDTO(app, "学号核验未通过，已转人工审核");
    }

    private AuthApplicationDTO handleStudentManual(User user, SubmitAuthApplicationRequest req) {
        requireRole(user, RoleName.STUDENT);
        requireText(req.getRealName(), "真实姓名");
        requireText(req.getStudentNo(), "学号");
        AuthApplication app = newApplication(user.getId(), RoleName.STUDENT, VerifyMethod.STUDENT_MANUAL, req);
        app.setStatus(AppStatus.UNDER_REVIEW);
        authApplicationMapper.insert(app);
        setUserAuthStatus(user, AuthStatus.PENDING);
        eventPublisher.publishEvent(new AuthApplicationSubmittedEvent(app.getId(), false));
        return toDTO(app, "已提交人工审核");
    }

    private AuthApplicationDTO handleInviteClaim(User user, SubmitAuthApplicationRequest req) {
        requireRole(user, RoleName.ALUMNI);
        requireText(req.getInviteCode(), "邀请码");
        // CAS 认领：仅当邀请码存在且仍为 INVITE_ISSUED（未被抢先认领）时成功
        AuthApplication set = new AuthApplication();
        set.setUserId(user.getId());
        set.setStatus(AppStatus.APPROVED);
        set.setAutoApproved(1);
        if (StringUtils.hasText(req.getRealName())) {
            set.setRealName(req.getRealName());
        }
        LambdaUpdateWrapper<AuthApplication> uw = new LambdaUpdateWrapper<AuthApplication>()
                .eq(AuthApplication::getInviteCode, req.getInviteCode())
                .eq(AuthApplication::getStatus, AppStatus.INVITE_ISSUED);
        int rows = authApplicationMapper.update(set, uw);
        if (rows == 0) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "邀请码无效、已过期或已被认领");
        }
        AuthApplication app = authApplicationMapper.selectOne(new LambdaQueryWrapper<AuthApplication>()
                .eq(AuthApplication::getInviteCode, req.getInviteCode())
                .eq(AuthApplication::getUserId, user.getId()));
        upsertAlumniProfile(user.getId(), app.getRealName(), app.getCollege(), app.getMajorText(),
                LocalDate.now().getYear(), DegreeType.BACHELOR);
        setUserAuthStatus(user, AuthStatus.VERIFIED);
        eventPublisher.publishEvent(new AuthApplicationSubmittedEvent(app.getId(), true));
        return toDTO(app, "邀请码认领成功，认证通过（请重新登录以解锁完整功能）");
    }

    private AuthApplicationDTO handleGuarantee(User user, SubmitAuthApplicationRequest req) {
        requireRole(user, RoleName.ALUMNI);
        requireText(req.getRealName(), "真实姓名");
        Long g1 = req.getGuarantor1Id();
        Long g2 = req.getGuarantor2Id();
        if (g1 == null || g2 == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "需选择两名担保人");
        }
        if (g1.equals(g2)) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "两名担保人不可重复");
        }
        if (g1.equals(user.getId()) || g2.equals(user.getId())) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "担保人不可为本人");
        }
        Long majorTagId = majorTagResolver.resolve(req.getMajorText());
        validateGuarantor(g1, majorTagId);
        validateGuarantor(g2, majorTagId);

        AuthApplication app = newApplication(user.getId(), RoleName.ALUMNI, VerifyMethod.ALUMNI_MANUAL_GUARANTEE, req);
        app.setStatus(AppStatus.AWAITING_GUARANTEE);
        app.setGuarantor1Id(g1);
        app.setGuarantor2Id(g2);
        app.setGuarantor1Status(GuarantorStatus.PENDING);
        app.setGuarantor2Status(GuarantorStatus.PENDING);
        authApplicationMapper.insert(app);
        setUserAuthStatus(user, AuthStatus.PENDING);

        notifyGuarantor(g1, user, app);
        notifyGuarantor(g2, user, app);
        // 担保阶段不建 audit_task，待担保确认转 UNDER_REVIEW 时再发事件
        return toDTO(app, "已提交，等待担保人确认");
    }

    // ==================== 担保确认 ====================

    @Override
    @Transactional
    public AuthApplicationDTO confirmGuarantee(Long id, Long guarantorUserId, boolean approve) {
        AuthApplication app = requireApp(id);
        if (!AppStatus.AWAITING_GUARANTEE.equals(app.getStatus())) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "该申请当前不在担保确认阶段");
        }
        boolean isG1 = guarantorUserId.equals(app.getGuarantor1Id());
        boolean isG2 = guarantorUserId.equals(app.getGuarantor2Id());
        if (!isG1 && !isG2) {
            throw new BusinessException(ResultCode.FORBIDDEN, "非本申请的担保人");
        }
        // 幂等/重复表态防护：该担保人当前必须仍为 PENDING
        String selfStatus = isG1 ? app.getGuarantor1Status() : app.getGuarantor2Status();
        if (!GuarantorStatus.PENDING.equals(selfStatus)) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "您已对该担保请求表态");
        }

        // S3 双人担保：任一担保人拒绝 → 立即否决整份申请（退回申请人）
        if (!approve) {
            int gr = casGuarantorStatus(id, isG1, GuarantorStatus.PENDING, GuarantorStatus.REJECTED);
            if (gr == 0) {
                throw new BusinessException(ResultCode.STATE_CONFLICT, "申请状态已变更");
            }
            int rows = casStatus(id, AppStatus.AWAITING_GUARANTEE, AppStatus.REJECTED, "担保人拒绝担保");
            if (rows == 0) {
                throw new BusinessException(ResultCode.STATE_CONFLICT, "申请状态已变更");
            }
            User applicant = userMapper.selectById(app.getUserId());
            if (applicant != null) {
                setUserAuthStatus(applicant, AuthStatus.REJECTED);
            }
            return toDTO(requireApp(id), "担保被拒绝，认证未通过");
        }

        // 确认：仅置本担保人 CONFIRMED，持久化“半确认”态；两人均 CONFIRMED 才转人工终审
        int gr = casGuarantorStatus(id, isG1, GuarantorStatus.PENDING, GuarantorStatus.CONFIRMED);
        if (gr == 0) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "申请状态已变更");
        }
        AuthApplication fresh = requireApp(id);
        boolean bothConfirmed = GuarantorStatus.CONFIRMED.equals(fresh.getGuarantor1Status())
                && GuarantorStatus.CONFIRMED.equals(fresh.getGuarantor2Status());
        if (!bothConfirmed) {
            return toDTO(fresh, "已记录您的确认，等待另一位担保人确认");
        }
        int rows = casStatus(id, AppStatus.AWAITING_GUARANTEE, AppStatus.UNDER_REVIEW, null);
        if (rows == 0) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "申请状态已变更");
        }
        eventPublisher.publishEvent(new AuthApplicationSubmittedEvent(id, false));
        return toDTO(requireApp(id), "两位担保人均已确认，进入人工终审");
    }

    // ==================== 撤回 / 重新提交 ====================

    @Override
    @Transactional
    public AuthApplicationDTO withdraw(Long id, Long userId) {
        AuthApplication app = requireApp(id);
        requireOwner(app, userId);
        if (!AppStatus.PENDING.equals(app.getStatus()) && !AppStatus.AWAITING_GUARANTEE.equals(app.getStatus())) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "当前状态不允许撤回");
        }
        int rows = casStatus(id, app.getStatus(), AppStatus.WITHDRAWN, null);
        if (rows == 0) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "申请状态已变更");
        }
        User user = userMapper.selectById(userId);
        if (user != null && !AuthStatus.VERIFIED.equals(user.getAuthStatus())) {
            setUserAuthStatus(user, AuthStatus.UNVERIFIED);
        }
        return toDTO(requireApp(id), "已撤回");
    }

    @Override
    @Transactional
    public AuthApplicationDTO resubmit(Long id, Long userId, ResubmitAuthApplicationRequest req) {
        AuthApplication app = requireApp(id);
        requireOwner(app, userId);
        if (!AppStatus.RETURNED.equals(app.getStatus())) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "仅退回状态的申请可重新提交");
        }
        // 覆盖可编辑字段
        if (StringUtils.hasText(req.getRealName())) app.setRealName(req.getRealName());
        if (StringUtils.hasText(req.getStudentNo())) app.setStudentNo(req.getStudentNo());
        if (StringUtils.hasText(req.getCollege())) app.setCollege(req.getCollege());
        if (StringUtils.hasText(req.getMajorText())) app.setMajorText(req.getMajorText());
        if (StringUtils.hasText(req.getEvidenceUrl())) app.setEvidenceUrl(req.getEvidenceUrl());
        if (req.getGuarantor1Id() != null) app.setGuarantor1Id(req.getGuarantor1Id());
        if (req.getGuarantor2Id() != null) app.setGuarantor2Id(req.getGuarantor2Id());
        app.setRejectReason(null);

        User user = requireUser(userId);
        String vm = app.getVerifyMethod();
        if (VerifyMethod.STUDENT_SSO.equals(vm)) {
            MockStudentRoster roster = ssoMockService.verify(app.getStudentNo(), app.getRealName());
            if (roster != null) {
                app.setStatus(AppStatus.APPROVED);
                app.setAutoApproved(1);
                authApplicationMapper.updateById(app);
                upsertStudentProfile(userId, app.getRealName(), app.getStudentNo(),
                        coalesce(app.getCollege(), roster.getCollege()),
                        coalesce(app.getMajorText(), roster.getMajorName()), roster.getEnrollYear());
                setUserAuthStatus(user, AuthStatus.VERIFIED);
                eventPublisher.publishEvent(new AuthApplicationSubmittedEvent(app.getId(), true));
                return toDTO(app, "重新核验通过，认证成功");
            }
            app.setStatus(AppStatus.UNDER_REVIEW);
            authApplicationMapper.updateById(app);
            setUserAuthStatus(user, AuthStatus.PENDING);
            eventPublisher.publishEvent(new AuthApplicationSubmittedEvent(app.getId(), false));
            return toDTO(app, "已重新提交，转人工审核");
        } else if (VerifyMethod.STUDENT_MANUAL.equals(vm)) {
            app.setStatus(AppStatus.UNDER_REVIEW);
            authApplicationMapper.updateById(app);
            setUserAuthStatus(user, AuthStatus.PENDING);
            eventPublisher.publishEvent(new AuthApplicationSubmittedEvent(app.getId(), false));
            return toDTO(app, "已重新提交人工审核");
        } else if (VerifyMethod.ALUMNI_MANUAL_GUARANTEE.equals(vm)) {
            Long majorTagId = majorTagResolver.resolve(app.getMajorText());
            validateGuarantor(app.getGuarantor1Id(), majorTagId);
            validateGuarantor(app.getGuarantor2Id(), majorTagId);
            app.setStatus(AppStatus.AWAITING_GUARANTEE);
            app.setGuarantor1Status(GuarantorStatus.PENDING);
            app.setGuarantor2Status(GuarantorStatus.PENDING);
            authApplicationMapper.updateById(app);
            setUserAuthStatus(user, AuthStatus.PENDING);
            notifyGuarantor(app.getGuarantor1Id(), user, app);
            notifyGuarantor(app.getGuarantor2Id(), user, app);
            return toDTO(app, "已重新提交，等待担保人确认");
        } else {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "该认证方式不支持重新提交");
        }
    }

    // ==================== 跨模块契约：终审（M7 调用） ====================

    @Override
    @Transactional
    public void approve(Long appId, Long reviewerId) {
        AuthApplication app = requireApp(appId);
        int rows = casStatus(appId, AppStatus.UNDER_REVIEW, AppStatus.APPROVED, null);
        if (rows == 0) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "申请当前状态不允许终审通过");
        }
        User user = requireUser(app.getUserId());
        if (RoleName.STUDENT.equals(app.getApplyRole())) {
            Integer enrollYear = parseEnrollYear(app.getStudentNo());
            upsertStudentProfile(user.getId(), app.getRealName(), app.getStudentNo(),
                    app.getCollege(), app.getMajorText(), enrollYear);
        } else {
            upsertAlumniProfile(user.getId(), app.getRealName(), app.getCollege(), app.getMajorText(),
                    LocalDate.now().getYear(), DegreeType.BACHELOR);
        }
        setUserAuthStatus(user, AuthStatus.VERIFIED);
        log.info("认证终审通过 appId={} userId={} reviewerId={}", appId, user.getId(), reviewerId);
        notifyResult(user.getId(), appId, "身份认证已通过", "您的身份认证已通过，请重新登录以解锁完整功能。");
    }

    @Override
    @Transactional
    public void reject(Long appId, Long reviewerId, String reason) {
        AuthApplication app = requireApp(appId);
        int rows = casStatus(appId, AppStatus.UNDER_REVIEW, AppStatus.REJECTED, reason);
        if (rows == 0) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "申请当前状态不允许终审拒绝");
        }
        User user = userMapper.selectById(app.getUserId());
        if (user != null) {
            setUserAuthStatus(user, AuthStatus.REJECTED);
        }
        log.info("认证终审拒绝 appId={} reviewerId={} reason={}", appId, reviewerId, reason);
        notifyResult(app.getUserId(), appId, "身份认证未通过",
                "很抱歉，您的身份认证未通过。原因：" + safe(reason));
    }

    @Override
    @Transactional
    public void returnForSupplement(Long appId, Long reviewerId, String reason) {
        AuthApplication app = requireApp(appId);
        int rows = casStatus(appId, AppStatus.UNDER_REVIEW, AppStatus.RETURNED, reason);
        if (rows == 0) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "申请当前状态不允许退回");
        }
        log.info("认证申请退回补充 appId={} reviewerId={} reason={}", appId, reviewerId, reason);
        notifyResult(app.getUserId(), appId, "身份认证需补充材料",
                "您的认证申请被退回，请补充后重新提交。说明：" + safe(reason));
    }

    // ==================== 查询 ====================

    @Override
    public AuthApplicationDTO getById(Long id) {
        return toDTO(requireApp(id), null);
    }

    @Override
    public PageResult<AuthApplicationDTO> pageMine(Long userId, long page, long size) {
        Page<AuthApplication> p = authApplicationMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<AuthApplication>()
                        .eq(AuthApplication::getUserId, userId)
                        .orderByDesc(AuthApplication::getCreatedAt));
        return toPageResult(p);
    }

    @Override
    public PageResult<AuthApplicationDTO> pageForReview(AuthApplicationQuery query) {
        Page<AuthApplication> p = authApplicationMapper.selectPage(new Page<>(query.getPage(), query.getSize()),
                new LambdaQueryWrapper<AuthApplication>()
                        .eq(StringUtils.hasText(query.getStatus()), AuthApplication::getStatus, query.getStatus())
                        .eq(StringUtils.hasText(query.getApplyRole()), AuthApplication::getApplyRole, query.getApplyRole())
                        .eq(StringUtils.hasText(query.getVerifyMethod()), AuthApplication::getVerifyMethod, query.getVerifyMethod())
                        .orderByAsc(AuthApplication::getCreatedAt));
        return toPageResult(p);
    }

    // ==================== 邀请码 ====================

    @Override
    public InviteCodeCheckDTO checkInviteCode(String code) {
        AuthApplication app = authApplicationMapper.selectOne(new LambdaQueryWrapper<AuthApplication>()
                .eq(AuthApplication::getInviteCode, code)
                .eq(AuthApplication::getStatus, AppStatus.INVITE_ISSUED));
        if (app == null) {
            return new InviteCodeCheckDTO(false, null);
        }
        return new InviteCodeCheckDTO(true, app.getMajorText());
    }

    @Override
    public boolean validateInviteCode(String code) {
        Long cnt = authApplicationMapper.selectCount(new LambdaQueryWrapper<AuthApplication>()
                .eq(AuthApplication::getInviteCode, code)
                .eq(AuthApplication::getStatus, AppStatus.INVITE_ISSUED));
        return cnt != null && cnt > 0;
    }

    @Override
    @Transactional
    public List<String> batchCreateInviteCodes(BatchInviteCodeRequest request) {
        List<String> codes = new ArrayList<>();
        for (int i = 0; i < request.getCount(); i++) {
            AuthApplication app = new AuthApplication();
            app.setApplyRole(RoleName.ALUMNI);
            app.setVerifyMethod(VerifyMethod.ALUMNI_INVITE_CODE);
            app.setStatus(AppStatus.INVITE_ISSUED);
            app.setAutoApproved(0);
            app.setInviteCode(inviteCodeAllocator.next());
            app.setMajorText(request.getMajor());
            app.setCollege(request.getCollege());
            authApplicationMapper.insert(app);
            codes.add(app.getInviteCode());
        }
        return codes;
    }

    // ==================== 内部辅助 ====================

    private AuthApplication newApplication(Long userId, String applyRole, String verifyMethod,
                                           SubmitAuthApplicationRequest req) {
        AuthApplication app = new AuthApplication();
        app.setUserId(userId);
        app.setApplyRole(applyRole);
        app.setVerifyMethod(verifyMethod);
        app.setRealName(req.getRealName());
        app.setStudentNo(req.getStudentNo());
        app.setCollege(req.getCollege());
        app.setMajorText(req.getMajorText());
        app.setEvidenceUrl(req.getEvidenceUrl());
        app.setAutoApproved(0);
        return app;
    }

    private void validateGuarantor(Long guarantorId, Long majorTagId) {
        if (guarantorId == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "担保人不能为空");
        }
        User g = userMapper.selectById(guarantorId);
        if (g == null) {
            throw new BusinessException(ResultCode.BIZ_ERROR, "担保人不存在");
        }
        if (!AuthStatus.VERIFIED.equals(g.getAuthStatus())) {
            throw new BusinessException(ResultCode.BIZ_ERROR, "担保人需为已认证用户");
        }
        Long gMajor = profileMajorTagId(g);
        if (gMajor == null || !gMajor.equals(majorTagId)) {
            throw new BusinessException(ResultCode.BIZ_ERROR, "担保人需与申请人同专业");
        }
    }

    private Long profileMajorTagId(User user) {
        if (RoleName.STUDENT.equals(user.getRole())) {
            StudentProfile p = studentProfileMapper.selectOne(new LambdaQueryWrapper<StudentProfile>()
                    .eq(StudentProfile::getUserId, user.getId()));
            return p == null ? null : p.getMajorTagId();
        }
        if (RoleName.ALUMNI.equals(user.getRole())) {
            AlumniProfile p = alumniProfileMapper.selectOne(new LambdaQueryWrapper<AlumniProfile>()
                    .eq(AlumniProfile::getUserId, user.getId()));
            return p == null ? null : p.getMajorTagId();
        }
        return null;
    }

    private void upsertStudentProfile(Long userId, String realName, String studentNo,
                                      String college, String majorText, Integer enrollYear) {
        StudentProfile p = studentProfileMapper.selectOne(new LambdaQueryWrapper<StudentProfile>()
                .eq(StudentProfile::getUserId, userId));
        boolean isNew = (p == null);
        if (isNew) {
            p = new StudentProfile();
            p.setUserId(userId);
        }
        p.setRealName(coalesce(realName, "未知"));
        p.setStudentNo(studentNo);
        p.setCollege(coalesce(college, "未知学院"));
        p.setMajorTagId(majorTagResolver.resolve(majorText));
        int year = enrollYear != null ? enrollYear : parseEnrollYear(studentNo);
        p.setEnrollYear(year);
        p.setGradeLevel(computeGradeLevel(year));
        if (isNew) {
            studentProfileMapper.insert(p);
        } else {
            studentProfileMapper.updateById(p);
        }
    }

    private void upsertAlumniProfile(Long userId, String realName, String college, String majorText,
                                     Integer gradYear, String degreeType) {
        AlumniProfile p = alumniProfileMapper.selectOne(new LambdaQueryWrapper<AlumniProfile>()
                .eq(AlumniProfile::getUserId, userId));
        boolean isNew = (p == null);
        if (isNew) {
            p = new AlumniProfile();
            p.setUserId(userId);
        }
        p.setRealName(coalesce(realName, "未知"));
        p.setCollege(coalesce(college, "未知学院"));
        p.setMajorTagId(majorTagResolver.resolve(majorText));
        p.setGradYear(gradYear);
        p.setDegreeType(degreeType);
        if (isNew) {
            alumniProfileMapper.insert(p);
        } else {
            alumniProfileMapper.updateById(p);
        }
    }

    /**
     * 单个担保人确认态 CAS：SET guarantor{n}_status=to
     * WHERE id=? AND status=AWAITING_GUARANTEE AND guarantor{n}_status=from。
     * 用前置态并发守卫，防止同一担保人重复表态导致的竞态。
     */
    private int casGuarantorStatus(Long id, boolean isG1, String from, String to) {
        AuthApplication set = new AuthApplication();
        LambdaUpdateWrapper<AuthApplication> uw = new LambdaUpdateWrapper<AuthApplication>()
                .eq(AuthApplication::getId, id)
                .eq(AuthApplication::getStatus, AppStatus.AWAITING_GUARANTEE);
        if (isG1) {
            set.setGuarantor1Status(to);
            uw.eq(AuthApplication::getGuarantor1Status, from);
        } else {
            set.setGuarantor2Status(to);
            uw.eq(AuthApplication::getGuarantor2Status, from);
        }
        return authApplicationMapper.update(set, uw);
    }

    /** 状态 CAS：UPDATE ... SET status=to[, reject_reason=reason] WHERE id=? AND status=from。 */
    private int casStatus(Long id, String from, String to, String reason) {
        AuthApplication set = new AuthApplication();
        set.setStatus(to);
        if (reason != null) {
            set.setRejectReason(reason);
        }
        LambdaUpdateWrapper<AuthApplication> uw = new LambdaUpdateWrapper<AuthApplication>()
                .eq(AuthApplication::getId, id)
                .eq(AuthApplication::getStatus, from);
        return authApplicationMapper.update(set, uw);
    }

    private void setUserAuthStatus(User user, String status) {
        user.setAuthStatus(status);
        userMapper.updateById(user);
    }

    private int computeGradeLevel(int enrollYear) {
        int g = LocalDate.now().getYear() - enrollYear + 1;
        return Math.min(10, Math.max(1, g));
    }

    /** 无学籍来源时，尝试从学号前 4 位解析入学年份，失败回退当前年份。 */
    private int parseEnrollYear(String studentNo) {
        if (studentNo != null && studentNo.length() >= 4) {
            try {
                int y = Integer.parseInt(studentNo.substring(0, 4));
                if (y >= 1990 && y <= LocalDate.now().getYear()) {
                    return y;
                }
            } catch (NumberFormatException ignored) {
                // 落回默认
            }
        }
        return LocalDate.now().getYear();
    }

    private void notifyGuarantor(Long guarantorId, User applicant, AuthApplication app) {
        NotificationService ns = notificationServiceProvider.getIfAvailable();
        if (ns == null) {
            return;
        }
        try {
            ns.send(guarantorId, "SYSTEM", "担保确认请求",
                    "校友「" + safe(app.getRealName()) + "」(" + safe(app.getMajorText()) + ") 申请认证，邀请您担保确认。",
                    "AUTH_APPLICATION", app.getId());
        } catch (Exception e) {
            log.warn("担保通知发送失败 guarantorId={} appId={}", guarantorId, app.getId(), e);
        }
    }

    private void notifyResult(Long userId, Long appId, String title, String content) {
        NotificationService ns = notificationServiceProvider.getIfAvailable();
        if (ns == null) {
            return;
        }
        try {
            ns.send(userId, "AUDIT_RESULT", title, content, "AUTH_APPLICATION", appId);
        } catch (Exception e) {
            log.warn("认证结果通知发送失败 userId={} appId={}", userId, appId, e);
        }
    }

    private User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    private AuthApplication requireApp(Long id) {
        AuthApplication app = authApplicationMapper.selectById(id);
        if (app == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "认证申请不存在");
        }
        return app;
    }

    private void requireOwner(AuthApplication app, Long userId) {
        if (app.getUserId() == null || !app.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能操作本人的认证申请");
        }
    }

    private void requireRole(User user, String role) {
        if (!role.equals(user.getRole())) {
            throw new BusinessException(ResultCode.STATE_CONFLICT, "身份类型与认证方式不匹配");
        }
    }

    private void requireText(String v, String field) {
        if (!StringUtils.hasText(v)) {
            throw new BusinessException(ResultCode.PARAM_MISSING, field + "不能为空");
        }
    }

    private String coalesce(String a, String b) {
        return StringUtils.hasText(a) ? a : b;
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private PageResult<AuthApplicationDTO> toPageResult(Page<AuthApplication> p) {
        List<AuthApplicationDTO> records = new ArrayList<>();
        for (AuthApplication a : p.getRecords()) {
            records.add(toDTO(a, null));
        }
        return new PageResult<>(records, p.getTotal(), p.getCurrent(), p.getSize());
    }

    private AuthApplicationDTO toDTO(AuthApplication app, String hint) {
        AuthApplicationDTO dto = new AuthApplicationDTO();
        dto.setId(app.getId());
        dto.setUserId(app.getUserId());
        dto.setApplyRole(app.getApplyRole());
        dto.setVerifyMethod(app.getVerifyMethod());
        dto.setRealName(app.getRealName());
        dto.setStudentNo(app.getStudentNo());
        dto.setMajorText(app.getMajorText());
        dto.setCollege(app.getCollege());
        dto.setEvidenceUrl(app.getEvidenceUrl());
        dto.setInviteCode(app.getInviteCode());
        dto.setGuarantor1Id(app.getGuarantor1Id());
        dto.setGuarantor2Id(app.getGuarantor2Id());
        dto.setGuarantor1Status(app.getGuarantor1Status());
        dto.setGuarantor2Status(app.getGuarantor2Status());
        dto.setStatus(app.getStatus());
        dto.setAutoApproved(app.getAutoApproved());
        dto.setRejectReason(app.getRejectReason());
        dto.setCreatedAt(app.getCreatedAt());
        dto.setUpdatedAt(app.getUpdatedAt());
        dto.setStatusHint(hint);
        return dto;
    }
}
