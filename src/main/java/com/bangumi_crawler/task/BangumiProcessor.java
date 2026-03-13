package com.bangumi_crawler.task;

import com.bangumi_crawler.controller.ShutdownController;
import com.bangumi_crawler.pojo.GalGame;
import com.bangumi_crawler.service.IGalGameService;
import com.bangumi_crawler.utils.BeanUtils;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.downloader.HttpClientDownloader;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.scheduler.BloomFilterDuplicateRemover;
import us.codecraft.webmagic.scheduler.QueueScheduler;
import us.codecraft.webmagic.selector.Selectable;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class BangumiProcessor implements PageProcessor {

    private final IGalGameService gameService;

    public BangumiProcessor(IGalGameService gameService) {
        this.gameService = gameService;
    }

    private static AtomicInteger count = new AtomicInteger(1);


    private final String url = "https://bangumi.tv/game/tag/galgame?sort=collects&page=";

    private Site site = Site.me()
            .setCharset("UTF-8")
            .setSleepTime(500)
            .setUserAgent(
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120 Safari/537.36"
            )
            .setRetryTimes(3)
            .setTimeOut(10000);

    @Override
    public Site getSite() {
        return site;
    }

    @Override
    public void process(Page page) {
        List<Selectable> oddList = page.getHtml().css(".item.odd.clearit").nodes();
        List<Selectable> evenList = page.getHtml().css(".item.even.clearit").nodes();

        if (oddList.isEmpty() && evenList.isEmpty()) {
            page.setSkip(true);
            return;
        }

        for (Selectable s : oddList) saveInfo(s);
        for (Selectable s : evenList) saveInfo(s);

        page.addTargetRequest(url + count.getAndIncrement());
    }

    private void saveInfo(Selectable s) {
        GalGame game = GalGame.builder()
                .translatedName(s.css(".inner .l", "text").toString())
                .info(s.css(".info.tip", "text").toString())
                .numberOfRatings(s.css("span.tip_j", "text").toString())
                .originalName(s.css("small.grey", "text").toString())
                .subjectId(parseSubjectId(s))
                .build();

        // 处理 score
        String scoreText = s.css("small.fade", "text").toString();
        game.setScore(scoreText == null || scoreText.isEmpty() ? null : Double.valueOf(scoreText));

        // 处理 rank
        String rankText = s.css(".rank", "text").toString();
        if (rankText == null || rankText.isEmpty()) {
            game.setRank(null);
        } else {
            // 去掉非数字字符，只保留数字
            String digits = rankText.replaceAll("\\D+", "");
            game.setRank(digits.isEmpty() ? null : Long.valueOf(digits));
        }

        // 保存数据
        gameService.saveData(game);
    }

    // 辅助方法：安全解析 subjectId
    private Long parseSubjectId(Selectable s) {
        String href = s.css("a", "href").toString();
        if (href != null && !href.isEmpty()) {
            String[] parts = href.split("/");
            if (parts.length >= 3) {
                try {
                    return Long.valueOf(parts[2]);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return null;
    }

    @Autowired
    private BangumiPipeline bangumiPipeline;

    @PostConstruct
    public void startSpider() {
        HttpClientDownloader downloader = new HttpClientDownloader();
        Spider.create(this)
                .addUrl(url + count.getAndIncrement())
                .setScheduler(new QueueScheduler().setDuplicateRemover(new BloomFilterDuplicateRemover(10000000)))
                .thread(8)
                .setDownloader(downloader)
                .addPipeline(bangumiPipeline)
                .run();
    }
}
