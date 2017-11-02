package com.rudra.aks.batch.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;

import com.rudra.aks.batch.model.UserBO;

public class UserItemProcessor implements ItemProcessor<UserBO, UserBO> {

	private static Logger logger = LoggerFactory.getLogger(UserItemProcessor.class);
	
	@Override
	public UserBO process(UserBO item) throws Exception {
		logger.info("Processing item>>> " + item.getUserid());
		return item;
	}

}
