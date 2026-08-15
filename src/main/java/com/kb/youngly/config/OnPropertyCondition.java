package com.kb.youngly.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

import java.util.Map;

public class OnPropertyCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Map<String, Object> attributes =
                metadata.getAnnotationAttributes(ConditionalOnProperty.class.getName());
        if (attributes == null) {
            return true;
        }

        String name = (String) attributes.get("name");
        String havingValue = (String) attributes.get("havingValue");
        boolean matchIfMissing = Boolean.TRUE.equals(attributes.get("matchIfMissing"));

        String actualValue = context.getEnvironment().getProperty(name);
        if (!StringUtils.hasText(actualValue)) {
            return matchIfMissing;
        }

        return actualValue.equalsIgnoreCase(havingValue);
    }
}
