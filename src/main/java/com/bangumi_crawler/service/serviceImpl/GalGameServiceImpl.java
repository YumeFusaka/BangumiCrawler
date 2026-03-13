package com.bangumi_crawler.service.serviceImpl;

import com.bangumi_crawler.mapper.GalGameMapper;
import com.bangumi_crawler.pojo.GalGame;
import com.bangumi_crawler.service.IGalGameService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GalGameServiceImpl extends ServiceImpl<GalGameMapper, GalGame> implements IGalGameService {

    @Autowired
    private GalGameMapper gameMapper;
    
    @Override
    public void saveData(GalGame game) {
        if (game.getSubjectId() == null) return;
        gameMapper.insertOrUpdate(game);
    }
}
