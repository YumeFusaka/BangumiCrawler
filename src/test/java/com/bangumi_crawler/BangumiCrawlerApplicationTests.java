package com.bangumi_crawler;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/** 上下文冒烟：能加载即说明依赖组合（MyBatis-Plus/数据源等）兼容；禁用列表爬虫防止测试触发真实抓取 */
@SpringBootTest(properties = "bangumi.list.enabled=false")
class BangumiCrawlerApplicationTests {

    @Test
    void contextLoads() {
    }
}
