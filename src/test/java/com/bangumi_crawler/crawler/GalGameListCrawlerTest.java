package com.bangumi_crawler.crawler;

import com.bangumi_crawler.pojo.GalGame;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** 列表条目解析：HTML 片段取自 bangumi 现网列表页（2026-08 实测） */
class GalGameListCrawlerTest {

    private final GalGameListCrawler crawler = new GalGameListCrawler(null, null, null);

    private static final String ITEM = """
            <li id="item_1126" class="item odd clearit">
                <a href="/subject/1126" class="subjectCover cover ll coverPortrait">
                    <span class="image"><img src="//lain.bgm.tv/r/400/pic/cover/l/ff/ee/1126_wtoHO.jpg" class="cover"/></span>
                </a>
                <div class="inner">
                    <h3><a href="/subject/1126" class="l">时空轮回</a> <small class="grey">Ever17 -the out of infinity-</small></h3>
                    <span class="rank"><small>Rank </small>6</span>
                    <p class="info tip">2002-08-29 / DC、PS2、PC等 / AVG / KID</p>
                    <p class="rateInfo"><span class="starstop-s"><span class="starlight stars9"></span></span> <small class="fade">9.1</small> <span class="tip_j">(9766人评分)</span></p>
                </div>
            </li>""";

    @Test
    void parseFullItem() {
        GalGame game = crawler.parse(Jsoup.parse(ITEM).selectFirst("li"));

        assertThat(game.getSubjectId()).isEqualTo(1126L);
        assertThat(game.getTranslatedName()).isEqualTo("时空轮回");
        assertThat(game.getOriginalName()).isEqualTo("Ever17 -the out of infinity-");
        assertThat(game.getInfo()).isEqualTo("2002-08-29 / DC、PS2、PC等 / AVG / KID");
        assertThat(game.getScore()).isEqualTo(9.1);
        assertThat(game.getRank()).isEqualTo(6L);
        assertThat(game.getNumberOfRatings()).isEqualTo("(9766人评分)");
        assertThat(game.getImgUrl()).isEqualTo("https://lain.bgm.tv/r/400/pic/cover/l/ff/ee/1126_wtoHO.jpg");
    }

    @Test
    void collectedItemStatusDoesNotOverwriteName() {
        // 收藏过的条目：.inner 内标题 h3 之前有 collectBlock，状态链接 class="l" 文本为「已玩过」
        String collectBlock = """
                <div id="collectBlock_1126" class="collectBlock tip_i" data-subject-id="1126">
                    <p class="collectModify">
                        <a href="/update/1126?TB_iframe=true" title="修改收藏" class="thickbox l">已玩过</a> | <a href="#;" class="l">X</a>
                    </p>
                </div>
                """;
        String html = ITEM.replace("<h3>", collectBlock + "<h3>");

        GalGame game = crawler.parse(Jsoup.parse(html).selectFirst("li"));

        assertThat(game.getTranslatedName()).isEqualTo("时空轮回");
    }

    @Test
    void noIconPlaceholderMeansNoCover() {
        String html = ITEM.replace("src=\"//lain.bgm.tv/r/400/pic/cover/l/ff/ee/1126_wtoHO.jpg\"", "src=\"/img/no_icon_subject.png\"");

        GalGame game = crawler.parse(Jsoup.parse(html).selectFirst("li"));

        assertThat(game.getImgUrl()).isNull();
    }

    @Test
    void missingScoreAndRankAreNull() {
        String html = ITEM
                .replace("<span class=\"rank\"><small>Rank </small>6</span>", "")
                .replace("<span class=\"starstop-s\"><span class=\"starlight stars9\"></span></span> <small class=\"fade\">9.1</small> ", "");

        GalGame game = crawler.parse(Jsoup.parse(html).selectFirst("li"));

        assertThat(game.getScore()).isNull();
        assertThat(game.getRank()).isNull();
        assertThat(game.getTranslatedName()).isEqualTo("时空轮回");
    }

    @Test
    void itemWithoutSubjectLinkIsSkipped() {
        String html = ITEM.replace("href=\"/subject/1126\"", "");

        assertThat(crawler.parse(Jsoup.parse(html).selectFirst("li"))).isNull();
    }

    @Test
    void evenItemIsParsed() {
        String html = ITEM.replace("item odd", "item even");

        GalGame game = crawler.parse(Jsoup.parse(html).selectFirst("li"));

        assertThat(game.getSubjectId()).isEqualTo(1126L);
    }
}
