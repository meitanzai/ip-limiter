/*
 * Copyright (c) 2014 laidian. All Rights Reserved.
 * @author jiangchao
 * @date  2020-07-24 16:02
 */
package com.laidian.limiter.core.springgateway.filter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

/**
 * 对所有的WEB请求进行过滤
 * 
 * @author fenglibin
 *
 */
@Component
public class NormalWebRequestFilter {
	@Autowired
	private IpLimiterFilter ipLimiterFilter;

	@Bean
	@Order(-1)
	public WebFilter erpPrefixFilter() {
		return (ServerWebExchange exchange, WebFilterChain chain) -> {
			return ipLimiterFilter.filter(exchange, chain);
		};
	}

}
