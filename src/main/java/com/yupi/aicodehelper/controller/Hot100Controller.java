package com.yupi.aicodehelper.controller;

import com.yupi.aicodehelper.common.BaseResponse;
import com.yupi.aicodehelper.hot100.Hot100ProblemDetailView;
import com.yupi.aicodehelper.hot100.Hot100ProblemSummaryView;
import com.yupi.aicodehelper.hot100.Hot100Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/hot100")
public class Hot100Controller {

    private final Hot100Service hot100Service;

    public Hot100Controller(Hot100Service hot100Service) {
        this.hot100Service = hot100Service;
    }

    @GetMapping("/problems")
    public BaseResponse<List<Hot100ProblemSummaryView>> listProblems(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String difficulty) {
        return BaseResponse.success(hot100Service.listProblems(keyword, tag, difficulty));
    }

    @GetMapping("/problems/{slug}")
    public BaseResponse<Hot100ProblemDetailView> getProblem(@PathVariable String slug) {
        return BaseResponse.success(hot100Service.getProblem(slug));
    }

    @GetMapping("/tags")
    public BaseResponse<List<String>> listTags() {
        return BaseResponse.success(hot100Service.listTags());
    }
}
