package com.backend.service.board;


import com.backend.domain.board.Board;
import com.backend.domain.board.BoardFile;
import com.backend.domain.board.BoardReport;
import com.backend.mapper.board.BoardCommentMapper;
import com.backend.mapper.board.BoardMapper;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
public class BoardService {
    private final BoardMapper mapper;
    private final S3Client s3Client;
    private final BoardCommentMapper boardCommentMapper;

    // session의
    private static String PAGE_INFO_SESSION_KEY = "pageInfo";
    // private static final String PAGE_INFO_SESSION_KEY = null;

    // aws.s3.bucket.name의 프로퍼티 값 주입
    @Value("${aws.s3.bucket.name}")
    private String bucketName;

    @Value("${image.src.prefix}")
    private String srcPrefix;

    public void add(Board board, MultipartFile[] files) throws Exception {


        mapper.insert(board);

        if (files != null && files.length > 0) {
            for (MultipartFile file : files) {
                mapper.insertFileName(board.getId(), file.getOriginalFilename());

                // 실제 파일 저장 (s3)
                // 부모 디렉토리 만들기
                String key = String.format("prj3/board/%d/%s", board.getId(), file.getOriginalFilename());
                s3Client.putObject(builder -> builder.bucket(bucketName).key(key).acl(ObjectCannedACL.PUBLIC_READ),
                        RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            }
        }
    }

    public boolean validate(Board board) throws Exception {
        return board.getTitle() != null && !board.getTitle().isBlank() &&
               board.getContent() != null && !board.getContent().isBlank();
    }

    public Map<String, Object> list(Integer page, Integer pageAmount, Boolean offsetReset, HttpSession session, String boardType,
                                    String searchType, String keyword)
            throws Exception {
        if (page <= 0) {
            throw new IllegalArgumentException("page must be greater than 0");
        }

        // 세션에서 값 가져오기
        Object sessionValue = session.getAttribute(PAGE_INFO_SESSION_KEY);
        Integer offset;

        // 세션 값이 없을 때 초기화
        if (sessionValue == null) {
            offset = 1;
            session.setAttribute(PAGE_INFO_SESSION_KEY, offset);
        } else if (sessionValue instanceof Integer) {
            offset = (Integer) sessionValue;
        } else if (sessionValue instanceof String) {
            try {
                offset = Integer.valueOf((String) sessionValue);
            } catch (NumberFormatException e) {
                throw new IllegalStateException("Invalid type for session attribute", e);
            }
        } else {
            throw new IllegalStateException("Invalid type for session attribute");
        }

        // 페이지에 따른 offset 계산

        // 세션에 새로운 offset 저장
        session.setAttribute(PAGE_INFO_SESSION_KEY, offset);


//        System.out.println("이것은 서비스의 boardType = " + boardType);
        // 페이지 정보 계산
        Map<String, Object> pageInfo = new HashMap<>();
        if (offsetReset) {
            offset = 0;
            page = 1;
            pageInfo.put("currentPageNumber", 1);
        } else {
            offset = (page - 1) * pageAmount;
            pageInfo.put("currentPageNumber", page);
        }

        Integer countByBoardType;
        if (boardType.equals("전체") && searchType.equals("전체") && keyword.equals("")) {

            countByBoardType = mapper.selectAllCount();
        } else {
//            System.out.println("이것은 서비스의 boardType = " + boardType);
            countByBoardType = mapper.selectByBoardType(boardType, searchType, keyword);
        }

        Integer lastPageNumber = (countByBoardType - 1) / pageAmount + 1;
        Integer leftPageNumber = (page - 1) / 10 * 10 + 1;
        Integer rightPageNumber = Math.min(leftPageNumber + 9, lastPageNumber);
        Integer prevPageNumber = (leftPageNumber > 1) ? leftPageNumber - 1 : null;
        Integer nextPageNumber = (rightPageNumber < lastPageNumber) ? rightPageNumber + 1 : null;

        if (prevPageNumber != null) {
            pageInfo.put("prevPageNumber", prevPageNumber);
        }
        if (nextPageNumber != null) {
            pageInfo.put("nextPageNumber", nextPageNumber);
        }

        pageInfo.put("lastPageNumber", lastPageNumber);
        pageInfo.put("leftPageNumber", leftPageNumber);
        pageInfo.put("rightPageNumber", rightPageNumber);
        pageInfo.put("offset", offset);

        List<Board> boardList = mapper.selectAllPaging(offset, pageAmount, boardType, searchType, keyword);

        // 각각의 Board 객체에 fileList 추가
        for (Board board : boardList) {
//            System.out.println("Board ID: " + board.getId());

            // getFileImageByboardId 메서드 호출
            String firstImageName = mapper.getFileImageByboardId(board.getId().toString());
//            System.out.println("File Name: " + firstImageName);

            if (firstImageName != null) {
                String thumbnailUrl = srcPrefix + "board/" + board.getId() + "/" + firstImageName;

//                System.out.println("First Image Name: " + firstImageName);
//                System.out.println("Thumbnail URL: " + thumbnailUrl);

                List<BoardFile> files = Collections.singletonList(new BoardFile(firstImageName, thumbnailUrl));
                board.setFileList(files);

//                System.out.println("Updated FileList: " + board.getFileList());
            }
        }

        return Map.of("pageInfo", pageInfo, "boardList", boardList);
    }

    public Map<String, Object> getByBoardIdAndMemberId(Integer id, Integer memberId) {
        int views = mapper.selectCountById(id);
        mapper.incrementViewsById(id, views);

        Map<String, Object> result = new HashMap<>();
        Board board = mapper.selectById(id);
        List<String> fileNames = mapper.selectFileNameByBoardId(id);
        List<BoardFile> files = fileNames.stream()
                .map(name -> new BoardFile(name, srcPrefix + "board/" + +id + "/" + name)).collect(Collectors.toList());
        board.setFileList(files);
        Map<String, Object> like = new HashMap<>();
//        System.out.println("files = " + files);
//        System.out.println("fileNames = " + fileNames);
        if (memberId == null) {
            like.put("like", false);
        } else {
            int c = mapper.selectLikeByBoardIdAndMemberId(id, memberId);
            like.put("like", c == 1);
        }
        like.put("count", mapper.selectCountLikeByBoardId(id));
        result.put("board", board);
        result.put("like", like);


        return result;
    }

    public void delete(Integer id) {
        //file명 조회
        List<String> fileNames = mapper.selectFileNameByBoardId(id);
        //s3에 있는 file
        for (String fileName : fileNames) {
            String key = STR."prj3/board/\{id}/\{fileName}";
            DeleteObjectRequest objectRequest = DeleteObjectRequest.builder().bucket(bucketName).key(key).build();
            s3Client.deleteObject(objectRequest);
        }
        //board_file
        mapper.deleteFileByBoardId(id);
        //board_like
        mapper.deleteLikeByBoardId(id);
        //board_comment
        boardCommentMapper.deleteByBoardId(id);
        //board
        mapper.deleteById(id);
    }

    public void edit(Board board, List<String> removeFileList, MultipartFile[] addFileList) throws IOException {
        if (removeFileList != null && removeFileList.size() > 0) {
            for (String fileName : removeFileList) {
                //s3파일 삭제
                String key = STR."prj3/board/\{board.getId()}/\{fileName}";
                DeleteObjectRequest objectRequest = DeleteObjectRequest.builder().bucket(bucketName).key(key).build();
                s3Client.deleteObject(objectRequest);

                //db 레코드 삭제
                mapper.deleteFileByBoardIdAndName(board.getId(), fileName);
            }
        }
        if (addFileList != null && addFileList.length > 0) {
            List<String> fileNameList = mapper.selectFileNameByBoardId(board.getId());
            for (MultipartFile file : addFileList) {
                String fileName = file.getOriginalFilename();
                if (!fileNameList.contains(fileName)) {
                    //새 파일이 기존에 없을때만 db에 추가
                    mapper.insertFileName(board.getId(), fileName);
                }
                //s3에 쓰기
                String key = STR."prj3/board/\{board.getId()}/\{fileName}";
                PutObjectRequest objectRequest = PutObjectRequest.builder().bucket(bucketName).key(key).acl(ObjectCannedACL.PUBLIC_READ).build();

                s3Client.putObject(objectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            }
        }
        mapper.update(board);
    }

    public boolean hasAccess(Integer id, Integer memberId) {
        Board board = mapper.selectById(id);

        if (board.getMemberId().equals(memberId) || memberId == 1) {
            return true;
        } else {
            return false;
        }

    }

    public Map<String, Object> like(Map<String, Object> req) {
        Map<String, Object> result = new HashMap<>();
        result.put("like", false);

        Integer boardId;
        Integer memberId;

        try {
            boardId = req.get("boardId") instanceof String
                    ? Integer.parseInt((String) req.get("boardId"))
                    : (Integer) req.get("boardId");
            memberId = req.get("memberId") instanceof String
                    ? Integer.parseInt((String) req.get("memberId"))
                    : (Integer) req.get("memberId");
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid boardId or memberId format");
        }

//        System.out.println("이것은 서비스의 req = " + req);

        // 이미 했으면
        int count = mapper.deleteLikeByBoardIdAndMemberId(boardId, memberId);
        // 안 했으면 (삭제된 행이 없으면)
        if (count == 0) {
            mapper.insertLikeByBoardIdAndMemberId(boardId, memberId);
            result.put("like", true);
        }
        result.put("count", mapper.selectCountLikeByBoardId(boardId));

        return result;
    }


    public boolean isLoggedIn(Integer memberId) {
        return memberId != null && memberId > 0;
    }


    public boolean addReport(Map<String, Object> req) {
        BoardReport boardReport = new BoardReport();
        boardReport.setBoardId((Integer) req.get("boardId"));
        boardReport.setMemberId((Integer) req.get("memberId"));
        boardReport.setContent((String) req.get("reason"));
        boardReport.setReportType((String) req.get("reportType")); // 새로운 필드 추가
        //신고 안 했으면
        int count = mapper.selectCountReportWithPrimaryKey(boardReport);
        if (count == 0) {
            mapper.insertReport(boardReport);
            return true;
        } else {
            return false;
        }
    }

    public Map<String, Object> reportList(Integer page, Integer pageAmount, Boolean offsetReset, HttpSession session, String boardType, String searchType, String keyword)
            throws Exception {

        if (page <= 0) {
            throw new IllegalArgumentException("page must be greater than 0");
        }

        // 세션에서 값 가져오기
        Object sessionValue = session.getAttribute(PAGE_INFO_SESSION_KEY);
        Integer offset;

        // 세션 값이 없을 때 초기화
        if (sessionValue == null) {
            offset = 1;
            session.setAttribute(PAGE_INFO_SESSION_KEY, offset);
        } else if (sessionValue instanceof Integer) {
            offset = (Integer) sessionValue;
        } else if (sessionValue instanceof String) {
            try {
                offset = Integer.valueOf((String) sessionValue);
            } catch (NumberFormatException e) {
                throw new IllegalStateException("Invalid type for session attribute", e);
            }
        } else {
            throw new IllegalStateException("Invalid type for session attribute");
        }

        // 페이지에 따른 offset 계산

        // 세션에 새로운 offset 저장
        session.setAttribute(PAGE_INFO_SESSION_KEY, offset);


//        System.out.println("이것은 서비스의 boardType = " + boardType);
        // 페이지 정보 계산
        Map<String, Object> pageInfo = new HashMap<>();
        if (offsetReset) {
            offset = 0;
            page = 1;
            pageInfo.put("currentPageNumber", 1);
        } else {
            offset = (page - 1) * pageAmount;
            pageInfo.put("currentPageNumber", page);
        }

        Integer countByBoardType;
        if (boardType.equals("전체") && searchType.equals("전체") && keyword.equals("")) {

            countByBoardType = mapper.selectAllCountWithReportBoard();
        } else {
//            System.out.println("이것은 서비스의 boardType = " + boardType);
            countByBoardType = mapper.selectByBoardTypeWithReportBoard(boardType, searchType, keyword);
        }

        Integer lastPageNumber = (countByBoardType - 1) / pageAmount + 1;
        Integer leftPageNumber = (page - 1) / 10 * 10 + 1;
        Integer rightPageNumber = Math.min(leftPageNumber + 9, lastPageNumber);
        Integer prevPageNumber = (leftPageNumber > 1) ? leftPageNumber - 1 : null;
        Integer nextPageNumber = (rightPageNumber < lastPageNumber) ? rightPageNumber + 1 : null;

        if (prevPageNumber != null) {
            pageInfo.put("prevPageNumber", prevPageNumber);
        }
        if (nextPageNumber != null) {
            pageInfo.put("nextPageNumber", nextPageNumber);
        }

        pageInfo.put("lastPageNumber", lastPageNumber);
        pageInfo.put("leftPageNumber", leftPageNumber);
        pageInfo.put("rightPageNumber", rightPageNumber);
        pageInfo.put("offset", offset);

        return Map.of("pageInfo", pageInfo, "boardList", mapper.selectAllPagingWithReportBoard(offset, pageAmount, boardType, searchType, keyword));
    }

    public Map<String, Object> reportContent(Integer boardId, Integer repoterMemberId) {
        Map<String, Object> response = new HashMap<>();

        // 게시글 정보 조회
        Board board = mapper.selectBoardById(boardId);

        // 신고 내용 조회
        List<BoardReport> reports = mapper.selectReportsByBoardId(boardId);

        // 응답 데이터 구성
        response.put("board", board);
        response.put("reports", reports);
        return response;
    }

    public List<Board> getLatestBoards() {
        return mapper.selectLatestBoards();
    }

    public List<Board> getPopularBoards() {
        return mapper.selectPopularBoards();
    }

    public List<Map<String, Object>> getTopLikedImages() {
        List<Map<String, Object>> topLikedImages = mapper.selectTopLikedImages();
        return topLikedImages.stream()
                .peek(image -> {
                    String imageUrl = (String) image.get("imageUrl");
                    Integer id = (Integer) image.get("id");
                    image.put("imageUrl", srcPrefix + "board/" + id + "/" + imageUrl);
                })
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getGuideBoards() {
        List<Map<String, Object>> GuideBoards = mapper.selectGuideBoards();
        return GuideBoards.stream()
                .peek(image -> {
                    String imageUrl = (String) image.get("imageUrl");
                    Integer id = (Integer) image.get("id");
                    image.put("imageUrl", srcPrefix + "board/" + id + "/" + imageUrl);
                })
                .collect(Collectors.toList());
    }
}
