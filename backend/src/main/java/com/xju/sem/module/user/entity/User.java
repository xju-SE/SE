package com.xju.sem.module.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xju.sem.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户账号（表 user）。id/deleted/createdAt/updatedAt 继承自 {@link BaseEntity}。
 * real_name/student_no 等强隐私字段不在本表，只存在于对应 profile。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user")
public class User extends BaseEntity {

    /** 登录名：学号或邮箱，注册后不可改。 */
    private String username;

    /** BCrypt 密码散列。 */
    private String passwordHash;

    /** STUDENT/ALUMNI/ADMIN，见 {@code AuthConst.RoleName}。 */
    private String role;

    /** UNVERIFIED/PENDING/VERIFIED/REJECTED，与 role 正交，控制写权限。 */
    private String authStatus;

    /** ACTIVE/DISABLED。 */
    private String status;

    /** SELF/SAME_MAJOR/PUBLIC，联系方式可见性。 */
    private String contactVisibility;

    /** SELF/SAME_MAJOR/PUBLIC，画像可见性。 */
    private String profileVisibility;
}
