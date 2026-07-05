package com.xju.sem.module.user.service.impl;

import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 邀请码生成器：产出 32 位大写字母数字串，基于 UUID 随机，冲突概率可忽略。
 */
@Component
public class InviteCodeAllocator {

    /** 生成一个 32 位邀请码。 */
    public String next() {
        return (UUID.randomUUID().toString() + UUID.randomUUID())
                .replace("-", "")
                .substring(0, 32)
                .toUpperCase();
    }
}
