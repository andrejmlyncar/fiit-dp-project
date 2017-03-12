package com.mlyncar.dp.comparison.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mlyncar.dp.comparison.entity.Change;
import com.mlyncar.dp.comparison.entity.ChangeLog;

public class ComparisonTestHelper {
	
    private static final Logger logger = LoggerFactory.getLogger(ComparisonTestHelper.class);
    
	public static void printChanges(ChangeLog log) {
		logger.debug("List of found changes:");
		for(Change change : log.changes()) {
			logger.debug(change.getChangeType().getCode() + ":" + change.getNewValue());
		}
	}
}