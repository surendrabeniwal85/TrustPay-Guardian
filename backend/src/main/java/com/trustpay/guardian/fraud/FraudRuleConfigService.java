package com.trustpay.guardian.fraud;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustpay.guardian.settings.SystemSetting;
import com.trustpay.guardian.settings.SystemSettingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service managing cached configuration, parameters, points, and enabled status for fraud rules.
 * Automatically cleared when an admin updates rules or settings.
 */
@Service
public class FraudRuleConfigService {

    private static final Logger log = LoggerFactory.getLogger(FraudRuleConfigService.class);

    private final FraudRuleRepository fraudRuleRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // In-memory thread-safe caches
    private final Map<String, FraudRule> ruleCache = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> parameterCache = new ConcurrentHashMap<>();
    private final AtomicInteger reviewThresholdCache = new AtomicInteger(-1);
    private final AtomicInteger blockThresholdCache = new AtomicInteger(-1);

    public FraudRuleConfigService(
            FraudRuleRepository fraudRuleRepository,
            SystemSettingRepository systemSettingRepository) {
        this.fraudRuleRepository = fraudRuleRepository;
        this.systemSettingRepository = systemSettingRepository;
    }

    /**
     * Clears all cached rules, parameters, and thresholds.
     */
    public synchronized void clearCache() {
        log.info("Evicting fraud rule and threshold caches.");
        ruleCache.clear();
        parameterCache.clear();
        reviewThresholdCache.set(-1);
        blockThresholdCache.set(-1);
    }

    private void ensureRulesLoaded() {
        if (ruleCache.isEmpty()) {
            synchronized (this) {
                if (ruleCache.isEmpty()) {
                    for (FraudRule rule : fraudRuleRepository.findAll()) {
                        ruleCache.put(rule.getRuleCode(), rule);
                        if (rule.getParametersJson() != null && !rule.getParametersJson().isBlank()) {
                            try {
                                Map<String, Object> params = objectMapper.readValue(
                                        rule.getParametersJson(),
                                        new TypeReference<>() {}
                                );
                                parameterCache.put(rule.getRuleCode(), params);
                            } catch (Exception e) {
                                log.warn("Failed to parse parametersJson for rule [{}]: {}", rule.getRuleCode(), e.getMessage());
                                parameterCache.put(rule.getRuleCode(), Collections.emptyMap());
                            }
                        } else {
                            parameterCache.put(rule.getRuleCode(), Collections.emptyMap());
                        }
                    }
                    log.debug("Loaded {} fraud rules into cache.", ruleCache.size());
                }
            }
        }
    }

    public boolean isRuleEnabled(String ruleCode) {
        ensureRulesLoaded();
        FraudRule rule = ruleCache.get(ruleCode);
        return rule != null && rule.isEnabled();
    }

    public int getRulePoints(String ruleCode, int defaultPoints) {
        ensureRulesLoaded();
        FraudRule rule = ruleCache.get(ruleCode);
        return rule != null ? rule.getPoints() : defaultPoints;
    }

    public BigDecimal getBigDecimalParameter(String ruleCode, String key, BigDecimal defaultValue) {
        ensureRulesLoaded();
        Map<String, Object> params = parameterCache.get(ruleCode);
        if (params != null && params.containsKey(key)) {
            Object val = params.get(key);
            if (val != null) {
                try {
                    return new BigDecimal(val.toString());
                } catch (Exception ignored) {
                }
            }
        }
        return defaultValue;
    }

    public int getIntParameter(String ruleCode, String key, int defaultValue) {
        ensureRulesLoaded();
        Map<String, Object> params = parameterCache.get(ruleCode);
        if (params != null && params.containsKey(key)) {
            Object val = params.get(key);
            if (val instanceof Number num) {
                return num.intValue();
            } else if (val != null) {
                try {
                    return Integer.parseInt(val.toString());
                } catch (Exception ignored) {
                }
            }
        }
        return defaultValue;
    }

    public int getReviewThreshold() {
        int cached = reviewThresholdCache.get();
        if (cached >= 0) {
            return cached;
        }
        int threshold = systemSettingRepository.findBySettingKey("REVIEW_THRESHOLD")
                .map(SystemSetting::getSettingValue)
                .map(Integer::parseInt)
                .orElse(40);
        reviewThresholdCache.set(threshold);
        return threshold;
    }

    public int getBlockThreshold() {
        int cached = blockThresholdCache.get();
        if (cached >= 0) {
            return cached;
        }
        int threshold = systemSettingRepository.findBySettingKey("BLOCK_THRESHOLD")
                .map(SystemSetting::getSettingValue)
                .map(Integer::parseInt)
                .orElse(70);
        blockThresholdCache.set(threshold);
        return threshold;
    }
}
