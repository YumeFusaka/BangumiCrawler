package com.bangumi_crawler.task;

import com.bangumi_crawler.controller.ShutdownController;
import com.bangumi_crawler.pojo.Game;
import com.bangumi_crawler.service.IGameService;
import com.bangumi_crawler.utils.BeanUtils;
import lombok.Data;
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

@Component
public class BangumiProcessor implements PageProcessor {

    // private final IGameService gameService=BeanUtils.getBean(IGameService.class);

    public static int count = 1;

    @Override
    public void process(Page page) {
        boolean isOver = true;
        List<Selectable> oddList = page.getHtml().css(".item.odd.clearit").nodes();
        for (Selectable oddSelectable : oddList) {
            this.saveInfo(oddSelectable);
            isOver = false;
        }
        List<Selectable> evenList = page.getHtml().css(".item.even.clearit").nodes();
        for (Selectable evenSelectable : evenList) {
            this.saveInfo(evenSelectable);
            isOver = false;
        }
        if (isOver) {
            ShutdownController shutdownController = BeanUtils.getBean(ShutdownController.class);
            shutdownController.shutdownContext();
            return;
        }
        page.addTargetRequest(url + (BangumiProcessor.count++));
    }

    public void saveInfo(Selectable selectable) {
        Game game = Game.builder()
                .name(selectable.css(".inner .l", "text").toString())
                .info(selectable.css(".info.tip", "text").toString())
                .score(selectable.css("small.fade", "text").toString())
                .votes(selectable.css("span.tip_j", "text").toString())
                .rank(selectable.css(".rank", "text").toString())
                .nick(selectable.css("small.grey", "text").toString())
                .subjectId(Long.valueOf(selectable.css("a", "href").toString().split("/")[2]))
                .build();
        IGameService gameService = BeanUtils.getBean(IGameService.class);
        gameService.saveData(game);
    }

    private Site site = Site.me()
            .setCharset("UTF-8")
            .setSleepTime(1)
            .setTimeOut(10 * 1000)
            .setRetrySleepTime(3000)
            .setRetryTimes(3);

    @Override
    public Site getSite() {
        return site;
    }

    private String url = "https://bangumi.tv/game/tag/galgame?sort=collects&page=";

    @Autowired
    private BangumiPipeline bangumiPipeline;

    @Scheduled(initialDelay = 1000, fixedDelay = 100 * 1000)
    public void process() {
        HttpClientDownloader httpClientDownloader = new HttpClientDownloader();
        Spider.create(new BangumiProcessor())
                .addUrl(url + (BangumiProcessor.count++))
                .setScheduler(new QueueScheduler().setDuplicateRemover(new BloomFilterDuplicateRemover(1000000)))
                .thread(10)
                .setDownloader(httpClientDownloader)
                .addPipeline(bangumiPipeline)
                .run();
    }
}
