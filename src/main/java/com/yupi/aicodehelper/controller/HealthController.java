package com.yupi.aicodehelper.controller;

import com.yupi.aicodehelper.common.BaseResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/health")
    public BaseResponse<String> health() {
        return BaseResponse.success("ok");
    }
}
