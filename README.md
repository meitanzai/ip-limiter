# ip-limiter

#### 一、 IP限流平台介绍

1. 为什么要做IP限流

网络世界中，真实的用户与爬虫机器人等混杂在一起，正常的请求与异常请求也是相互交织，做为服务的提供方都希望请求来自于真实的用户的真实请求，这么才能够达到成本收益的最大化，但是总会有一些人来非法尝试获取提供方的信息，爬取有价值的数据供自己使用。作为数据的拥有者，肯定不希望自己的数据被非常利用，因为就需要做一些防御措施，达到数据保护的目的，IP限流就是其中一种比较有效的方案。因为通常用于爬取数据的爬虫机器人，都是在一台或者固定的几台服务器上执行的数据爬取操作（当然他可以购买更多的动态，只是会花更多的成本），针对这种访问量异常的ＩＰ进行记录和跟踪，然后就可以确认来自这些ＩＰ的请求是不是真实的用户请求了。

![输入图片说明](https://images.gitee.com/uploads/images/2020/1230/174416_6a9ef67b_306225.jpeg "b5acae60b9ae68da3938c84103187cf2.jpeg")

2. 要达到的目标
- 监控ＩＰ的访问行为，将其访问量及访问行为进行记录（完成）；
- 支持手动将单个IP、IP段加入到黑名单中，并对加入到黑名单中的IP进行过滤（完成）；
- 支持手动将单个IP、IP段加入到白名单中，并对加入到白名单中的IP进行过滤（完成）；
- 在基于一定的规则下，能够自动将异常访问的ＩＰ加入的ＩＰ黑名单中（完成）；
- 并对异常访问的ＩＰ进行告警，并支持应用自定义IP黑名单发现规则及告警实现（待实现）；
- 支持针对集群应用进行汇总统计（完成）；
- 高效的数据统计、存储及读取，减少对应用效率的影响，目前存在Redis中（完成）；
- 支持对规则不同存储的扩展支持，如可将规则持久化到DB中、ZK中等，实现已经抽象化，Redis的已经完成，其它存储逻辑可根据情况自定义（完成）；
- 能够支持按分纬度及秒纬度对访问的ＩＰ进行统计（完成）；
- 有一定的管理界面，能够对黑名单及TOP IP的访问情况进行查询及操作（完成）；
- 支持将统计数据输出到不同的存储，如Redis、Influxdb等，实现已经抽象化，Redis的已经完成，其它存储逻辑可根据情况自定义（完成）；
- 支持多平台对访问数据进行呈现，如以后数据可以在Grafana中呈现，目前有基本的管理界面，更精细的管理界面及Grafana中展示数据后期再考虑实现（部分完成）；
- 支持对ＩＰ及ＩＰ段访问最高TPS进行统一配置（完成），也支持对单个ＩＰ及ＩＰ段进行访问控制（完成）；
- 多应用集群统一流量管控平台，可管理所有接入的应用的访问情况（完成）；


#### 二、架构
1. 交互架构

![ＩＰ限流平台交互架构](https://images.gitee.com/uploads/images/2020/1231/181209_ad1ef1db_306225.png "962ebe8c87cfb79393e4faaa7e50b47a (1).png")

2. 实现架构

![IP限流平台架构图](https://images.gitee.com/uploads/images/2020/1230/174256_2b6b8f8c_306225.png "20201111144948719 (1).png")

3. 系统模块

- 收集访问纬度数据的Agent模块，需要应用集成；
- 用于查看及管理的控制台模块，控制台模块为Master/Slave架构，Master用于执行定时任务、对节点进行检查、检查系统收集的IP黑名单情况等；

#### 三、设计原则
1. 高性能
- 访问纬度数据的统计、过滤及收集，必须是高效的，并且尽可能的减少由于这些纬度数据的收集给集成应用带来的性能损耗；
- 访问纬度数据收集时，尽可能的采用全内存操作，减少或避免本地ＩＯ及远程ＩＯ的操作；
- 访问纬度数据的上报、从远程同步数据，使用异常操作；
2. 高可用
- 当IP限流系统的控制台不可用时，不能够影响到集成访问纬度数据的统计的应用端；
- 对集成访问纬度统计的应用定期健康检查，确保其可用性；
- IP限流系统的控制台支持水平扩展，且无状态；
3. 高可扩展
- 纬度数据的存储支持扩展，可以根据实际情况支持不同的后端存储结构，默认支持Redis，可扩展为支持MySQL、Elasticsearch、Influxdb等；
- 对超限访问ＩＰ处理规则的可扩展性，同时支持在控制台及集成客户端进行扩展，且提供默认的处理规则；
4. 简单易用
- 集成简单，尽可能的除了在pom.xml中引入Jar包以外，不需要做其它任何的操作；
- 使用简单，提供基本的管理界面给用户，可对IP的访问情况、IP白名单、IP黑白单、QPS设置等进行管理和查看；

#### 四、存储设计
系统默认设计只会保存最多一个小时的访问纬度数据，且每个应用的数据是存放在单的hashkey中的，因而单个应用数据量不大，且了访问上的高效性，此处就默认使用Redis做为存储，也支持扩展为使用其它的存储。

 **1. black-ips** 
用于存储全局黑名单IP的Key，存储结构为Hash，HashKey为IP，HashValue为对象com.laidian.limiter.common.vo.BlackIpVO，其定义的字段如下：

![输入图片说明](https://images.gitee.com/uploads/images/2020/1231/181916_7ec06ae2_306225.png "c7f707069f4ee8cba555071afc437162.png")

注：

每个应用的IP黑名单数据都会单独存储到不同的Key中，其命名规则为"应用名称-black-ips"，如IP限制平台控制台的IP黑名单数据，保存的Key为：ip-limiter-dashboard-black-ips；

 **2. white-ips** 
用于存储全局白名单IP的Key，存储结构为Hash，HashKey为IP，HashValue为对象com.laidian.limiter.common.vo.WhiteIpVO，其定义的字段如下：

![输入图片说明](https://images.gitee.com/uploads/images/2020/1231/181935_423ed591_306225.png "8e94d98285f75dbcee6b5dbfd1daa1a9.png")

注：

每个应用的IP白名单数据都会单独存储到不同的Key中，其命名规则为"应用名称-white-ips"，如IP限制平台控制台的IP白名单数据，保存的Key为：ip-limiter-dashboard-white-ips；

 **3. minute-access** 
用于存储所有接入了IP限流平台的应用客户端的每分钟的访问统计汇总的Key，存储结构为Hash，HashKey为代表访问的分钟，HashValue为对象List，com.laidian.limiter.common.vo.AccessVO定义的字段如下：

![输入图片说明](https://images.gitee.com/uploads/images/2020/1231/181952_2768bfae_306225.png "f9629ac5adb233a90a82686825374848.png")

注：

每个应用的分钟访问纬度数据都会单独存储到不同的Key中，其命名规则为"应用名称-minute-access"，如IP限制平台控制台的分钟访问纬度数据，保存的Key为：ip-limiter-dashboard-minute-access；

每个应用每个节点的的分钟访问纬度数据都会单独存储到不同的Key中，其命名规则为"应用名称-ip及端口-minute-access"，如IP限制平台控制台的分钟访问纬度数据，保存的Key为：ip-limiter-dashboard-127.0.0.1:20520-minute-access；

 **4. ip-limit** 
用于存储全局IP QPS设置的Key，存储结构为Hash，HashKey为IP，HashValue为对象com.laidian.limiter.common.vo.IpLimitVO，其定义的字段如下：

![输入图片说明](https://images.gitee.com/uploads/images/2020/1231/182017_514cc4cc_306225.png "bb44e9662ec695a2632ea2d538a17240.png")

注：

每个应用的IP QPS设置数据都会单独存储到不同的Key中，其命名规则为"应用名称-ip-limit"，如IP限制平台控制台的IP白名单数据，保存的Key为：ip-limiter-dashboard-ip-limit；

 **5. registered-clients** 
用于存储注册到IP限流平台管理控制台的Key，存储结构为Hash，HashKey为应用名称，HashValue为对象Map，用于存储该应用所有注册的客户端，Map的Key为应用客户端的IP+端口，com.laidian.limiter.common.vo.Client对定义的字段如下：

![输入图片说明](https://images.gitee.com/uploads/images/2020/1231/182034_dc06f189_306225.png "46b585047d9f13b93bdbb3a044fc789c.png")

 **6. ip-limiter-dashboard-master** 
用于存储IP限流平台的Master节点的Key，存储结构为普通的字符串，其值为当前master节点的IP+端口。

 **7. permitsPerSecondEachIp** 
用于存储每个ＩＰ默认的最大ＱＰＳ值的Key，存储结构为普通的数字，其值为当每个ＩＰ默认的最大ＱＰＳ值。

注：

每个应用都会单独存储IP默认值最大ＱＰＳ值，存储结构为普通的数字，其值为当前应用每个ＩＰ默认的最大ＱＰＳ值，存储每个应用默认访问QPS的Redis的Key命名规则为“应用名称-permitsPerSecondEachIp”，如ip-limiter-dashboard的key为"ip-limiter-dashboard-permitsPerSecondEachIp"。

#### 安装教程

1.  xxxx
2.  xxxx
3.  xxxx

#### 使用说明

1.  xxxx
2.  xxxx
3.  xxxx

#### 参与贡献

1.  Fork 本仓库
2.  新建 Feat_xxx 分支
3.  提交代码
4.  新建 Pull Request


#### 特技

1.  使用 Readme\_XXX.md 来支持不同的语言，例如 Readme\_en.md, Readme\_zh.md
2.  Gitee 官方博客 [blog.gitee.com](https://blog.gitee.com)
3.  你可以 [https://gitee.com/explore](https://gitee.com/explore) 这个地址来了解 Gitee 上的优秀开源项目
4.  [GVP](https://gitee.com/gvp) 全称是 Gitee 最有价值开源项目，是综合评定出的优秀开源项目
5.  Gitee 官方提供的使用手册 [https://gitee.com/help](https://gitee.com/help)
6.  Gitee 封面人物是一档用来展示 Gitee 会员风采的栏目 [https://gitee.com/gitee-stars/](https://gitee.com/gitee-stars/)
