package com.yupi.aicodehelper.hot100;

import com.yupi.aicodehelper.common.ErrorCode;
import com.yupi.aicodehelper.exception.BusinessException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

@Service
public class Hot100Service {

    private final Hot100ProblemLoader hot100ProblemLoader;

    public Hot100Service(Hot100ProblemLoader hot100ProblemLoader) {
        this.hot100ProblemLoader = hot100ProblemLoader;
    }

    @Cacheable(
            cacheNames = Hot100CacheNames.PROBLEM_LIST,
            key = "T(java.util.Objects).toString(#keyword,'') + '|' + T(java.util.Objects).toString(#tag,'') + '|' + T(java.util.Objects).toString(#difficulty,'')"
    )
    public List<Hot100ProblemSummaryView> listProblems(String keyword, String tag, String difficulty) {
        return hot100ProblemLoader.listAll().stream()
                .filter(problem -> matchKeyword(problem, keyword))
                .filter(problem -> matchTag(problem, tag))
                .filter(problem -> matchDifficulty(problem, difficulty))
                .map(Hot100ProblemSummaryView::from)
                .toList();
    }

    @Cacheable(cacheNames = Hot100CacheNames.PROBLEM_DETAIL, key = "#slug")
    public Hot100ProblemDetailView getProblem(String slug) {
        if (slug == null || slug.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "slug cannot be blank");
        }
        Hot100Problem problem = hot100ProblemLoader.getBySlug(slug);
        if (problem == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Hot100 problem not found: " + slug);
        }
        return Hot100ProblemDetailView.from(problem);
    }

    @Cacheable(cacheNames = Hot100CacheNames.TAG_LIST)
    public List<String> listTags() {
        return hot100ProblemLoader.listAll().stream()
                .flatMap(problem -> problem.getTags() == null ? Stream.empty() : problem.getTags().stream())
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    private boolean matchKeyword(Hot100Problem problem, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String needle = keyword.toLowerCase(Locale.ROOT).trim();
        return contains(problem.getTitle(), needle)
                || contains(problem.getSlug(), needle)
                || contains(problem.getSummary(), needle);
    }

    private boolean matchTag(Hot100Problem problem, String tag) {
        if (tag == null || tag.isBlank()) {
            return true;
        }
        String expected = tag.toLowerCase(Locale.ROOT).trim();
        return problem.getTags() != null && problem.getTags().stream()
                .anyMatch(t -> t != null && t.toLowerCase(Locale.ROOT).trim().equals(expected));
    }

    private boolean matchDifficulty(Hot100Problem problem, String difficulty) {
        if (difficulty == null || difficulty.isBlank()) {
            return true;
        }
        String expected = normalizeDifficulty(difficulty);
        String actual = normalizeDifficulty(problem.getDifficulty());
        return !actual.isEmpty() && expected.equals(actual);
    }

    private boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }

    private String normalizeDifficulty(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT)
                .replace(" ", "")
                .replace("_", "")
                .replace("-", "");
    }
}
