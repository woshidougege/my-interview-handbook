package com.noah.superagent.common.dto.request;

import lombok.Data;

@Data
public class ChangePasswordRequest {
    private String userId;
    private String newPwd;
    private String servicecode;
}