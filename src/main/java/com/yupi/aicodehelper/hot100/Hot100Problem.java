package com.yupi.aicodehelper.hot100;

import lombok.Data;

import java.util.List;

@Data
public class Hot100Problem {

    private Integer problemId;

    private String title;

    private String slug;

    private String difficulty;

    private List<String> tags;

    private String pattern;

    private String summary;

    private String coreIdea;

    private String pitfalls;

    private String complexity;

    private String markdownContent;
}
