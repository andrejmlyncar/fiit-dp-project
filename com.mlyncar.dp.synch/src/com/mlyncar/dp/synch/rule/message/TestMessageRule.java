package com.mlyncar.dp.synch.rule.message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mlyncar.dp.comparison.entity.Change;
import com.mlyncar.dp.synch.rule.SynchRule;

public class TestMessageRule implements SynchRule {

    private final Logger logger = LoggerFactory.getLogger(TestMessageRule.class);

    @Override
    public boolean validateChange(Change change) {
        logger.debug("Validating change with rule {}", this.getClass().getName());
        return true;
    }

}