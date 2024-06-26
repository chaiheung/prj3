package com.backend.domain.board;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Board {
    private Integer id;
    private String title;
    private String content;
    private String writer;
    private Integer memberId;
    private LocalDateTime inserted;
    private Integer views;
    private String boardType;
    private String repoterId;
    private String repoterNickname;
    private String thumbnailUrl;
    private BoardFile fileName;

    private Integer numberOfReports;
    private Integer numberOfImages;
    private Integer numberOfComments;
    private Integer numberOfLikes;
    private List<BoardFile> fileList;

}
