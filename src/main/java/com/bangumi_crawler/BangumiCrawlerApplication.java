package com.bangumi_crawler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BangumiCrawlerApplication {

    public static void main(String[] args) {
        SpringApplication.run(BangumiCrawlerApplication.class, args);
    }

}
