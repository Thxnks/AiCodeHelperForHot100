package com.yupi.aicodehelper.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.cache")
public class AppCacheProperties {

    /**
     * Whether Redis cache is enabled.
     */
    private boolean redisEnabled = true;

    /**
     * Hot100 problem list/query cache ttl seconds.
     */
    private long hot100ProblemTtlSeconds = 300;

    /**
     * Hot100 recommendation cache ttl seconds.
     */
    private long hot100RecommendationTtlSeconds = 180;

    /**
     * Hot100 study plan cache ttl seconds.
     */
    private long hot100StudyPlanTtlSeconds = 180;

    /**
     * Hot100 tag mastery cache ttl seconds.
     */
    private long hot100TagMasteryTtlSeconds = 180;
}
