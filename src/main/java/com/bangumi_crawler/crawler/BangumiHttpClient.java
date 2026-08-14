package com.bangumi_crawler.crawler;

import com.bangumi_crawler.config.CrawlerProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/** bangumi 请求客户端：统一代理、UA、超时与退避重试 */
@Slf4j
@Component
public class BangumiHttpClient {

    public static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36";

    private static final int MAX_RETRIES = 3;

    private final HttpClient client;
    private final String cookie;

    public BangumiHttpClient(CrawlerProperties props) {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL);
        String host = props.getProxy().getHost();
        if (host != null && !host.isBlank()) {
            // ProxySelector.of 会先按 SOCKS 再按 HTTP 协商（Clash 的 7890 实测只支持 SOCKS）
            builder.proxy(ProxySelector.of(new InetSocketAddress(host, props.getProxy().getPort())));
            log.info("bangumi 请求走代理 {}:{}", host, props.getProxy().getPort());
        } else {
            log.info("bangumi 请求直连");
        }
        this.client = builder.build();
        this.cookie = props.getCookie();
        if (cookie == null || cookie.isBlank()) {
            log.info("未配置登录 cookie，以匿名身份访问");
        } else {
            log.info("已配置登录 cookie（{} 字符，不打印内容）", cookie.length());
        }
    }

    /** GET 并返回 HTML。网络异常与 429/5xx 退避重试（1s/2s/4s），耗尽后抛 IOException */
    public String get(String url) throws IOException, InterruptedException {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", USER_AGENT)
                .timeout(Duration.ofSeconds(15))
                .GET();
        if (cookie != null && !cookie.isBlank()) {
            requestBuilder.header("Cookie", cookie);
        }
        HttpRequest request = requestBuilder.build();
        for (int attempt = 0; ; attempt++) {
            HttpResponse<String> resp;
            try {
                resp = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            } catch (IOException e) {
                if (attempt >= MAX_RETRIES) throw e;
                log.warn("{} 网络异常: {}，第 {} 次重试", url, e.getMessage(), attempt + 1);
                Thread.sleep(1000L << attempt);
                continue;
            }
            if (resp.statusCode() == 200) return resp.body();
            if (resp.statusCode() == 429 || resp.statusCode() >= 500) {
                if (attempt >= MAX_RETRIES) throw new IOException(url + " -> HTTP " + resp.statusCode());
                log.warn("{} 返回 {}，第 {} 次退避重试", url, resp.statusCode(), attempt + 1);
                Thread.sleep(1000L << attempt);
                continue;
            }
            throw new IOException(url + " -> HTTP " + resp.statusCode());
        }
    }
}
