package com.bangumi_crawler.service;

import com.bangumi_crawler.pojo.GalGame;
import com.baomidou.mybatisplus.extension.service.IService;

public interface IGalGameService extends IService<GalGame> {
    void saveData(GalGame game);
}
