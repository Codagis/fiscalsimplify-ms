package com.fiscalimplify.fiscalimplify.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class RailwayDeploymentCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        var env = context.getEnvironment();
        return RailwayDeployment.isRailwayDeployment(env)
                && RailwayDeployment.hasRailwayPostgresConfig(env);
    }
}
