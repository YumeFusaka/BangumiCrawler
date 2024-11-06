package com.bangumi_crawler.service.serviceImpl;

import com.bangumi_crawler.mapper.GalGameMapper;
import com.bangumi_crawler.pojo.GalGame;
import com.bangumi_crawler.service.IGalGameService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GalGameServiceImpl extends ServiceImpl<GalGameMapper, GalGame> implements IGalGameService {

    @Autowired
    private GalGameMapper gameMapper;

    @Override
    public void saveData(GalGame game) {
        QueryWrapper<GalGame> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("subject_id", game.getSubjectId());
        List<GalGame> games = gameMapper.selectList(queryWrapper);
        if (games.isEmpty())
            gameMapper.insert(game);
        else {
            UpdateWrapper<GalGame> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("subject_id", game.getSubjectId());
            gameMapper.update(game, updateWrapper);
        }

    }
}
