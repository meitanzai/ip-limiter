package com.laidian.limiter.dashboard.iplimit.impl;

import java.util.List;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.laidian.limiter.common.vo.AccessVO;
import com.laidian.limiter.dashboard.iplimit.OverLimitAccessHandler;

@Service(value = "defaultOverLimitAccessHandler")
public class DefaultOverLimitAccessHandler extends OverLimitAccessHandler {

	@Override
	@Async
	public void handleOverLimitAccess(String appName, List<AccessVO> topAccessMetricList) {
		// 什么都不做

	}

}
