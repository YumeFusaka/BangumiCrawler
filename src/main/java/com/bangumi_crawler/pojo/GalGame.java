package com.bangumi_crawler.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

@Data
@TableName("galgame")
@Builder
public class GalGame {

    @TableId(value = "id", type = IdType.AUTO)
    Long id;

    @TableField("name")
    String name;

    @TableField("info")
    String info;

    @TableField("score")
    Double score;

    @TableField("`rank`")
    Long rank;

    @TableField("votes")
    String votes;

    @TableField("nick")
    String nick;

    @TableField("subject_id")
    Long subjectId;
}
