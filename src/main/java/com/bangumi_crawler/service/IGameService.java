package com.bangumi_crawler.service;

import com.bangumi_crawler.pojo.Game;
import com.baomidou.mybatisplus.extension.service.IService;

public interface IGameService extends IService<Game> {
    void saveData(Game game);
}
