package com.bangumi_crawler.crawler;

import com.bangumi_crawler.config.CrawlerProperties;
import com.bangumi_crawler.mapper.GalGameMapper;
import com.bangumi_crawler.pojo.GalGame;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 抓取 galgame 标签的全部列表页（按收藏数排序），解析条目并 upsert 入库。
 * 启动后异步执行一次；重复条目按 subject_id 更新，重跑幂等。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "bangumi.list.enabled", havingValue = "true", matchIfMissing = true)
public class GalGameListCrawler {

    private static final String LIST_URL = "https://bangumi.tv/game/tag/galgame?sort=collects&page=";

    private final BangumiHttpClient httpClient;
    private final GalGameMapper mapper;
    private final CrawlerProperties props;

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        Thread crawler = new Thread(this::run, "galgame-list-crawler");
        crawler.setDaemon(true); // 不阻塞 /shutdown 退出
        crawler.start();
    }

    private void run() {
        int window = props.getList().getThreads();
        ExecutorService pool = Executors.newFixedThreadPool(window, r -> {
            Thread t = new Thread(r, "galgame-list-crawler");
            t.setDaemon(true);
            return t;
        });
        try {
            int page = 1;
            int total = 0;
            while (true) {
                // 并发窗口翻页：窗口内任一分页为空即视为列表已到末尾
                List<Future<Integer>> futures = new ArrayList<>();
                for (int i = 0; i < window; i++) {
                    int p = page + i;
                    futures.add(pool.submit((Callable<Integer>) () -> crawlPage(p)));
                }
                boolean end = false;
                for (Future<Integer> future : futures) {
                    int count = future.get();
                    if (count == 0) {
                        end = true;
                        break;
                    }
                    total += count;
                    page++;
                }
                log.info("已抓取 {} 页，累计 {} 条", page - 1, total);
                if (end) break;
            }
            log.info("galgame 列表抓取完成，共 {} 条", total);
        } catch (Exception e) {
            log.error("列表抓取中断: {}", e.getMessage(), e);
        } finally {
            pool.shutdownNow();
        }
    }

    /** 抓取单个列表页并入库，返回本页条目数（0 表示列表结束） */
    private int crawlPage(int page) throws Exception {
        Document doc = Jsoup.parse(httpClient.get(LIST_URL + page));
        List<Element> items = doc.select("li.item.odd.clearit, li.item.even.clearit");
        for (Element item : items) {
            GalGame game = parse(item);
            if (game != null) mapper.upsert(game);
        }
        Thread.sleep(props.getList().getSleepMs());
        return items.size();
    }

    /** 解析单个列表条目；主题链接缺失时返回 null（不入库） */
    GalGame parse(Element item) {
        String[] parts = item.select("a").attr("href").split("/"); // 条目内首个链接指向 /subject/{id}
        Long subjectId = parseLong(parts.length > 2 ? parts[2] : "");
        if (subjectId == null) return null;
        return GalGame.builder()
                .subjectId(subjectId)
                .translatedName(firstText(item, ".inner .l"))
                .info(firstText(item, ".info.tip"))
                .numberOfRatings(firstText(item, "span.tip_j"))
                .originalName(firstText(item, "small.grey"))
                .score(parseDouble(firstText(item, "small.fade")))
                .rank(parseDigits(firstText(item, ".rank")))
                .imgUrl(parseCover(item))
                .build();
    }

    /** 列表条目自带封面（登录态下无占位图）；no_icon 占位图视为无封面，留给封面任务补 */
    private static String parseCover(Element item) {
        String src = item.select("a.subjectCover img").attr("src");
        if (src.isBlank() || src.startsWith("/img/")) return null;
        return src.startsWith("//") ? "https:" + src : src;
    }

    private static String firstText(Element item, String css) {
        Element el = item.selectFirst(css);
        return el == null ? "" : el.text();
    }

    private static Double parseDouble(String s) {
        if (s.isBlank()) return null;
        try {
            return Double.valueOf(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Long parseDigits(String s) {
        if (s.isBlank()) return null;
        String digits = s.replaceAll("\\D+", "");
        return digits.isEmpty() ? null : Long.valueOf(digits);
    }

    private static Long parseLong(String s) {
        try {
            return Long.valueOf(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
