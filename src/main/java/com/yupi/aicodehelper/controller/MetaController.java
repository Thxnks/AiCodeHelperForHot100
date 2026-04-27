package com.yupi.aicodehelper.controller;

import com.yupi.aicodehelper.common.BaseResponse;
import com.yupi.aicodehelper.common.ErrorCode;
import com.yupi.aicodehelper.common.ErrorCodeView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/meta")
@Tag(name = "Meta", description = "Metadata APIs")
public class MetaController {

    @GetMapping("/error-codes")
    @Operation(summary = "Get API error code dictionary")
    public BaseResponse<List<ErrorCodeView>> listErrorCodes() {
        List<ErrorCodeView> codes = Arrays.stream(ErrorCode.values())
                .map(ErrorCodeView::from)
                .toList();
        return BaseResponse.success(codes);
    }
}
