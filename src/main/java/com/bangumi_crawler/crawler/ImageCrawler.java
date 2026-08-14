package com.bangumi_crawler.crawler;

import com.bangumi_crawler.config.CrawlerProperties;
import com.bangumi_crawler.mapper.GalGameMapper;
import com.bangumi_crawler.pojo.GalGame;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 定时为缺少封面的条目抓取 subject 页封面并回写 img_url。
 * 每轮处理当前所有 img_url 为 NULL 的条目；失败的条目留待下轮重试。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ImageCrawler {

    private static final String SUBJECT_URL = "https://bangumi.tv/subject/";

    private final BangumiHttpClient httpClient;
    private final GalGameMapper mapper;
    private final CrawlerProperties props;

    @Scheduled(initialDelayString = "${bangumi.image.initial-delay-ms}",
               fixedDelayString = "${bangumi.image.fixed-delay-ms}")
    public void crawl() {
        List<GalGame> games = mapper.selectList(new QueryWrapper<GalGame>().isNull("img_url"));
        if (games.isEmpty()) return;
        log.info("待抓取封面的条目 {} 个，开始处理", games.size());

        ExecutorService pool = Executors.newFixedThreadPool(props.getImage().getThreads(), r -> {
            Thread t = new Thread(r, "image-crawler");
            t.setDaemon(true);
            return t;
        });
        try {
            pool.invokeAll(games.stream()
                    .map(g -> (Callable<Void>) () -> {
                        fetchImage(g);
                        return null;
                    })
                    .toList());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            pool.shutdownNow();
        }
        log.info("本轮封面抓取结束");
    }

    private void fetchImage(GalGame game) {
        try {
            String html = httpClient.get(SUBJECT_URL + game.getSubjectId());
            Element img = Jsoup.parse(html).selectFirst("a.thickbox.cover img");
            if (img == null) return; // 页面结构变化或无封面，留待下轮重试
            String src = img.attr("src");
            if (src.isBlank()) return;
            if (src.startsWith("//")) src = "https:" + src;
            mapper.update(null, new UpdateWrapper<GalGame>()
                    .eq("subject_id", game.getSubjectId())
                    .isNull("img_url")
                    .set("img_url", src));
            Thread.sleep(props.getImage().getSleepMs());
        } catch (Exception e) {
            log.warn("subject {} 封面抓取失败: {}", game.getSubjectId(), e.getMessage());
        }
    }
}
