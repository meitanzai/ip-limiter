package com.laidian.limiter.core.config;

import javax.annotation.Priority;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.laidian.limiter.common.constant.Constants;
import com.laidian.limiter.core.Application;
import com.laidian.limiter.core.cache.IpCacheHelper;
import com.laidian.limiter.core.interceptor.IpLimiterInterceptor;
import com.laidian.limiter.core.service.BlackIpService;
import com.laidian.limiter.core.service.WhiteIpService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@ComponentScan(basePackageClasses = { Application.class })
@EnableConfigurationProperties({ IpLimiterConfigurationProperties.class })
@Qualifier
@Primary
@Priority(0)
public class IpLimiterConfiguration implements WebMvcConfigurer {

	@Autowired
	private IpCacheHelper ipCacheHelper;

	@Autowired
	private BlackIpService blackIpService;

	@Autowired
	private WhiteIpService whiteIpService;

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		log.info("Add IpLimiter interceptor:" + this.getClass() + ", current appName is:" + SystemEnv.getAppName());
		// Add IpLimiter interceptor
		addSpringMvcInterceptor(registry);
	}

	private void addSpringMvcInterceptor(InterceptorRegistry registry) {
		registry.addInterceptor(new IpLimiterInterceptor(SystemEnv.getAppName(), ipCacheHelper,
				Constants.DEFAULT_IP_MAX_QPS, blackIpService, whiteIpService)).addPathPatterns("/**");
	}

}
