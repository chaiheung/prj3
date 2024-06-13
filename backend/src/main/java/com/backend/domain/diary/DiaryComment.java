package com.backend.domain.diary;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DiaryComment {
    private Integer id;
    private Integer boardId;
    private Integer memberId;
    private String comment;
    private LocalDateTime inserted;

    private String nick_name;
}
