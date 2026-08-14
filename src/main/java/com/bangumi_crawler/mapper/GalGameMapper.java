package com.bangumi_crawler.mapper;

import com.bangumi_crawler.pojo.GalGame;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GalGameMapper extends BaseMapper<GalGame> {
    /** 按 subject_id 唯一键插入或更新（SQL 见 GalGameMapper.xml） */
    void upsert(GalGame game);
}
