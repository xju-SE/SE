package com.xju.sem.module.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/** access / refresh 令牌对。 */
@Data
@AllArgsConstructor
public class TokenPair {
    private String accessToken;
    private String refreshToken;
}
