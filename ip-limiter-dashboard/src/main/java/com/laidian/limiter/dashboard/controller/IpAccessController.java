package com.laidian.limiter.dashboard.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.laidian.limiter.common.constant.Constants;
import com.laidian.limiter.common.util.DateUtil;
import com.laidian.limiter.common.util.HttpClientUtil;
import com.laidian.limiter.common.vo.AccessVO;
import com.laidian.limiter.core.cache.IpCacheHelper;
import com.laidian.limiter.core.config.SystemEnv;
import com.laidian.limiter.dashboard.client.IClientService;
import com.laidian.limiter.dashboard.config.LimiterResource;

import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping(value = "/limiter", produces = MediaType.APPLICATION_JSON_VALUE)
@Slf4j
public class IpAccessController {

	@Autowired
	private IpCacheHelper ipCacheHelper;

	@Autowired
	private LimiterResource limiterResource;

	@Autowired
	private IClientService clientService;

	/**
	 * 首页
	 * 
	 * @param model
	 * @return
	 */
	@RequestMapping(value = "", method = { RequestMethod.GET })
	public String index_0(Model model) {
		return index_1(model);
	}

	/**
	 * 首页
	 * 
	 * @param model
	 * @return
	 */
	@RequestMapping(value = "/", method = { RequestMethod.GET })
	public String index_1(Model model) {
		return getMinutesDataPretty(Constants.EMPTY_STRING, Constants.EMPTY_STRING, 10, model);
	}

	@ResponseBody
	@RequestMapping("/getIpSecondLongAccess")
	public Map<Long, AccessVO> getIpSecondAccess(@RequestParam(required = true) String ip) {
		return ipCacheHelper.getSecondIpCacheHelper().getVisit(ip);
	}

	@ResponseBody
	@RequestMapping("/getAllIpSecondAccess")
	public Map<String, TreeMap<String, AccessVO>> getAllIpSecondAccess() {
		Map<String, TreeMap<String, AccessVO>> result = new HashMap<String, TreeMap<String, AccessVO>>();
		Map<String, HashMap<Long, AccessVO>> map = ipCacheHelper.getSecondIpCacheHelper().getAllVisit();
		map.forEach((k, v) -> {
			TreeMap<String, AccessVO> treeMap = new TreeMap<String, AccessVO>();
			v.forEach((k1, v1) -> {
				treeMap.put(String.valueOf(k1), v1);
			});
			result.put(k, treeMap);
		});
		return result;
	}

	/**
	 * 获取指定服务器最近一段时间秒纬度的访问统计数据
	 * 
	 * @param model
	 * @param appName     需要获取访问统计的应用名称
	 * @param ip          应用的ＩＰ
	 * @param ts
	 * @param lastSeconds
	 * @return
	 */
	@RequestMapping("/getIpSecondAccessPretty")
	public String getIpSecondAccessPretty(Model model, @RequestParam(required = false) String appName,
			@RequestParam(required = false) String ip, @RequestParam(required = false) String ts,
			@RequestParam(required = false, defaultValue = "10") Integer lastSeconds,
			@RequestParam(required = false, defaultValue = "5") Integer refreshInterval) {
		log.debug("调用接口getIpSecondAccessPretty的参数,appName={},ip={},ts={},lastSeconds={}", appName, ip, ts, lastSeconds);
		Map<String, TreeMap<String, AccessVO>> sencondsAccessMetric = new HashMap<String, TreeMap<String, AccessVO>>();
		if (StringUtils.isEmpty(appName)) {
			sencondsAccessMetric.putAll(getAllIpSecondAccess());
		} else {
			String server = ip;
			if (StringUtils.isEmpty(server)) {
				List<String> appNameList = clientService.getAppRegisteredIps(appName);
				if (!CollectionUtils.isEmpty(appNameList)) {
					ip = server = appNameList.get(0);
				}
			}
			if (!StringUtils.isEmpty(server)) {
				StringBuilder url = new StringBuilder("http://");
				url.append(server).append("/ip-limiter/metric/getIpSecondAccess?lastSeconds=").append(lastSeconds);
				HttpClientUtil.doGet(url.toString(), r -> {
					if (r != null && r.getCode() == 0) {
						Map<String, TreeMap<String, AccessVO>> map = JSON.parseObject(r.getData().toString(),
								new TypeReference<Map<String, TreeMap<String, AccessVO>>>() {
								});
						if (CollectionUtils.isEmpty(map)) {
							log.warn("获取远程秒访问纬度的响应内容为空，url为" + url);
						}
						sencondsAccessMetric.putAll(map);
					} else {
						log.warn("获取远程秒访问纬度发生异常，响应code：" + r.getCode() + "，响应Msg：" + r.getMsg() + "，URL:" + url);
					}
				});
			}
		}
		model.addAttribute("secondsAccess", sencondsAccessMetric);
		model.addAttribute("ip", ip);
		model.addAttribute("lastSeconds", lastSeconds);
		model.addAttribute("refreshInterval", refreshInterval);
		model.addAttribute("appName", appName);

		return "localSecondsData";
	}

	@ResponseBody
	@RequestMapping("/cleanIpSecondAccess")
	public boolean cleanIpSecondAccess(@RequestParam(required = true) String ip) {
		return ipCacheHelper.getSecondIpCacheHelper().cleanVisit(ip);
	}

	@ResponseBody
	@RequestMapping("/getIpMinuteAccess")
	public Map<Long, AccessVO> getIpMinuteAccess(@RequestParam(required = true) String ip) {
		HashMap<Long, AccessVO> result = ipCacheHelper.getMinuteIpCacheHelper().getVisit(ip);
		result.forEach((k, v) -> {
			v.setCurrentDate(DateUtil.formatDate(v.getCurrentMinutes() * 60 * 1000, DateUtil.ISO_DATE_TIME_FORMAT));
		});
		return result;
	}

	@ResponseBody
	@RequestMapping("/getAllIpMinuteAccess")
	public Map<String, HashMap<Long, AccessVO>> getAllIpMinuteAccess() {
		Map<String, HashMap<Long, AccessVO>> result = ipCacheHelper.getMinuteIpCacheHelper().getAllVisit();
		result.forEach((k, v) -> {
			v.forEach((k1, v1) -> {
				v1.setCurrentDate(
						DateUtil.formatDate(v1.getCurrentMinutes() * 60 * 1000, DateUtil.ISO_DATE_TIME_FORMAT));
			});
		});
		return result;
	}

	@ResponseBody
	@RequestMapping("/cleanIpMinuteAccess")
	public boolean cleanIpMinuteAccess(@RequestParam(required = true) String ip) {
		return ipCacheHelper.getMinuteIpCacheHelper().cleanVisit(ip);
	}

	/**
	 * 获取所有以分钟为统计纬度、以分钟为hashkey的这些hashkey的列表
	 * 
	 * @return
	 */
	@ResponseBody
	@RequestMapping("/getMinuteKeys")
	public List<Object> getMinuteKeys(String appName) {
		if (StringUtils.isEmpty(appName)) {
			appName = SystemEnv.getAppName();
		}
		return limiterResource.getMetric().getMinuteDataKeys(appName);
	}

	/**
	 * 获取指定分钟的统计数据
	 * 
	 * @return
	 */
	@ResponseBody
	@RequestMapping("/getOneMinuteData")
	public List<AccessVO> getOneMinuteData(String appName, String minute) {
		if (StringUtils.isEmpty(appName)) {
			appName = SystemEnv.getAppName();
		}
		List<AccessVO> result = limiterResource.getMetric().getOneMinuteData(appName, minute);
		if (CollectionUtils.isEmpty(result)) {
			return result;
		}
		return result;
	}

	/**
	 * 获取多个指定分钟的统计数据
	 * 
	 * @return
	 */
	@ResponseBody
	@RequestMapping("/getMultiMinutesData")
	public List<List<AccessVO>> getMultiMinutesData(String appName, List<Object> minutes) {
		if (StringUtils.isEmpty(appName)) {
			appName = SystemEnv.getAppName();
		}
		return limiterResource.getMetric().getMultiMinutesData(appName, minutes);
	}

	/**
	 * 查看当前应用集群以分钟为纬度统计的TOP访问ＩＰ的数据
	 * 
	 * @param model
	 * @return
	 */
	@RequestMapping("/getMinutesDataPretty")
	public String getMinutesDataPretty(String appName, String ip,
			@RequestParam(required = false, defaultValue = "10") int lastMinutes, Model model) {
		if (StringUtils.isEmpty(appName)) {
			appName = SystemEnv.getAppName();
		}
		List<List<AccessVO>> result = limiterResource.getMetric().getMinutesData(appName, ip, lastMinutes);
		model.addAttribute("minutesDatas", result);
		model.addAttribute("appName", appName);
		model.addAttribute("ip", ip);
		model.addAttribute("lastMinutes", lastMinutes);
		return "minutesData";
	}

	/**
	 * 获取所有注册的客户端应用的名称列表
	 * 
	 * @return
	 */
	@ResponseBody
	@RequestMapping("/getAllAppNames")
	public List<Object> getAllAppNames() {
		return limiterResource.getClientService().getAllAppNames();
	}

	/**
	 * 获取指定的客户端应用的所有节点ＩＰ及端口
	 * 
	 * @return
	 */
	@ResponseBody
	@RequestMapping("/getAppRegisteredIps")
	public List<String> getAppRegisteredIps(@RequestParam(required = true) String appName) {
		return limiterResource.getClientService().getAppRegisteredIps(appName);
	}
}
