package com.bangumi_crawler.service.serviceImpl;

import com.bangumi_crawler.mapper.GameMapper;
import com.bangumi_crawler.pojo.Game;
import com.bangumi_crawler.service.IGameService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Wrapper;
import java.util.List;

@Service
public class GameServiceImpl extends ServiceImpl<GameMapper, Game> implements IGameService {

    @Autowired
    private GameMapper gameMapper;

    @Override
    public void saveData(Game game) {
        QueryWrapper<Game> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("subject_id", game.getSubjectId());
        List<Game> games = gameMapper.selectList(queryWrapper);
        if (games.isEmpty())
            gameMapper.insert(game);
        else {
            UpdateWrapper<Game> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("subject_id", game.getName());
            gameMapper.update(game, updateWrapper);
        }

    }
}
