package com.bangumi_crawler.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

@Data
@TableName("game")
@Builder
public class Game {

    @TableId(value="id",type= IdType.AUTO)
    Long id;

    @TableField("name")
    String name;

    @TableField("info")
    String info;

    @TableField("score")
    String score;

    @TableField("`rank`")
    String rank;

    @TableField("votes")
    String votes;
}
