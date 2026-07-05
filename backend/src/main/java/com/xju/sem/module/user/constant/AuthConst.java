package com.xju.sem.module.user.constant;

/**
 * M1 用户与认证模块的枚举取值常量（与 schema.sql 列注释严格对齐）。
 * 枚举列一律以 String 存储枚举名（不引入 MyBatis 枚举 TypeHandler，保持解耦）。
 */
public final class AuthConst {

    private AuthConst() {
    }

    /** user.role —— 注册身份类型（无持久化 GUEST）。 */
    public static final class RoleName {
        public static final String STUDENT = "STUDENT";
        public static final String ALUMNI = "ALUMNI";
        public static final String ADMIN = "ADMIN";
        private RoleName() {
        }
    }

    /** user.auth_status —— 与 role 正交，控制写权限。 */
    public static final class AuthStatus {
        public static final String UNVERIFIED = "UNVERIFIED";
        public static final String PENDING = "PENDING";
        public static final String VERIFIED = "VERIFIED";
        public static final String REJECTED = "REJECTED";
        private AuthStatus() {
        }
    }

    /** user.status —— 账号启用状态。 */
    public static final class UserStatus {
        public static final String ACTIVE = "ACTIVE";
        public static final String DISABLED = "DISABLED";
        private UserStatus() {
        }
    }

    /** user.contact_visibility / profile_visibility —— 隐私可见范围。 */
    public static final class Visibility {
        public static final String SELF = "SELF";
        public static final String SAME_MAJOR = "SAME_MAJOR";
        public static final String PUBLIC = "PUBLIC";
        public static boolean isValid(String v) {
            return SELF.equals(v) || SAME_MAJOR.equals(v) || PUBLIC.equals(v);
        }
        private Visibility() {
        }
    }

    /** auth_application.verify_method —— 四条分级认证路径。 */
    public static final class VerifyMethod {
        public static final String STUDENT_SSO = "STUDENT_SSO";
        public static final String STUDENT_MANUAL = "STUDENT_MANUAL";
        public static final String ALUMNI_INVITE_CODE = "ALUMNI_INVITE_CODE";
        public static final String ALUMNI_MANUAL_GUARANTEE = "ALUMNI_MANUAL_GUARANTEE";
        private VerifyMethod() {
        }
    }

    /** auth_application.status —— 认证申请状态机（见实现说明文档 §4 mermaid）。 */
    public static final class AppStatus {
        public static final String INVITE_ISSUED = "INVITE_ISSUED";
        public static final String PENDING = "PENDING";
        public static final String AWAITING_GUARANTEE = "AWAITING_GUARANTEE";
        public static final String UNDER_REVIEW = "UNDER_REVIEW";
        public static final String RETURNED = "RETURNED";
        public static final String APPROVED = "APPROVED";
        public static final String REJECTED = "REJECTED";
        public static final String WITHDRAWN = "WITHDRAWN";
        public static final String EXPIRED = "EXPIRED";
        private AppStatus() {
        }
    }

    /** auth_application.guarantor1_status / guarantor2_status —— 单个担保人的确认态（S3 双人担保）。 */
    public static final class GuarantorStatus {
        public static final String PENDING = "PENDING";
        public static final String CONFIRMED = "CONFIRMED";
        public static final String REJECTED = "REJECTED";
        private GuarantorStatus() {
        }
    }

    /** alumni_profile.degree_type。 */
    public static final class DegreeType {
        public static final String BACHELOR = "BACHELOR";
        public static final String MASTER = "MASTER";
        public static final String PHD = "PHD";
        private DegreeType() {
        }
    }
}
