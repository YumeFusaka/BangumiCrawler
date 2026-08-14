package com.bangumi_crawler.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** application.yml 中 bangumi.* 配置 */
@ConfigurationProperties(prefix = "bangumi")
@Data
public class CrawlerProperties {

    private Proxy proxy = new Proxy();
    private Task list = new Task();
    private Task image = new Task();
    /** 登录 cookie（如 chii_auth=...），置空则匿名访问 */
    private String cookie;

    @Data
    public static class Proxy {
        /** 代理主机，置空则直连 */
        private String host;
        private int port;
    }

    @Data
    public static class Task {
        /** 并发线程数 */
        private int threads;
        /** 每个请求后的休眠毫秒数 */
        private long sleepMs;
    }
}
