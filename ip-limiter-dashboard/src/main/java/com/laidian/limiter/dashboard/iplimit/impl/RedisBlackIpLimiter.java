package com.laidian.limiter.dashboard.iplimit.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.laidian.erp.redis.spring.boot.RedisTemplateWrapper;
import com.laidian.limiter.common.util.AppUtil;
import com.laidian.limiter.common.vo.BlackIpVO;
import com.laidian.limiter.dashboard.constants.RedisKey;
import com.laidian.limiter.dashboard.iplimit.BlackIpLimiter;

/**
 * ＩＰ黑名单操作服务类
 * 
 * @author fenglibin
 *
 */
@Service("redisBlackIpLimiter")
public class RedisBlackIpLimiter implements BlackIpLimiter {

	@Override
	public BlackIpVO addBlackIp(String appName, String ip) {
		BlackIpVO vo = BlackIpVO.builder().ip(ip).build();
		RedisTemplateWrapper.hSet(AppUtil.getAppRedisKeyPrefix(appName) + RedisKey.BLACK_IPS_KEY, ip, vo);
		return vo;
	}
	

	@Override
	public BlackIpVO updateBlackIp(String appName, String ip, BlackIpVO blackIp) {
		RedisTemplateWrapper.hSet(AppUtil.getAppRedisKeyPrefix(appName) + RedisKey.BLACK_IPS_KEY, ip, blackIp);
		return blackIp;
	}

	@Override
	public BlackIpVO getBlackIp(String appName, String ip) {
		return (BlackIpVO) RedisTemplateWrapper
				.hGet(AppUtil.getAppRedisKeyPrefix(appName) + RedisKey.BLACK_IPS_KEY, ip);
	}

	@Override
	public BlackIpVO delBlackIp(String appName, String ip) {
		Object value = RedisTemplateWrapper.hGet(AppUtil.getAppRedisKeyPrefix(appName) + RedisKey.BLACK_IPS_KEY,
				ip);
		RedisTemplateWrapper.hDel(AppUtil.getAppRedisKeyPrefix(appName) + RedisKey.BLACK_IPS_KEY, ip);
		return (BlackIpVO) value;
	}

	@Override
	public void cleanAllBlackIps(String appName) {
		RedisTemplateWrapper.del(AppUtil.getAppRedisKeyPrefix(appName) + RedisKey.BLACK_IPS_KEY);
	}

	@Override
	public List<BlackIpVO> getAllBlackIps(String appName) {
		List<BlackIpVO> list = new ArrayList<BlackIpVO>();
		List<Object> blackIpKeysList = RedisTemplateWrapper
				.hGetHashKeys(AppUtil.getAppRedisKeyPrefix(appName) + RedisKey.BLACK_IPS_KEY);
		if (CollectionUtils.isEmpty(blackIpKeysList)) {
			return list;
		}
		List<Object> blackIpList = RedisTemplateWrapper
				.hGets(AppUtil.getAppRedisKeyPrefix(appName) + RedisKey.BLACK_IPS_KEY, blackIpKeysList);
		if (CollectionUtils.isEmpty(blackIpList)) {
			return list;
		}
		blackIpList.forEach(e -> {
			list.add((BlackIpVO) e);
		});
		return list;
	}

}
