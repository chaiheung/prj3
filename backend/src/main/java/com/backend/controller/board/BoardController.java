package com.backend.controller.board;

import com.backend.domain.board.Board;
import com.backend.domain.board.DeleteRequest;
import com.backend.service.board.BoardService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/board")
public class BoardController {
    final BoardService service;
    private final DefaultAuthenticationEventPublisher authenticationEventPublisher;

    @PostMapping("add")
    public ResponseEntity add(Board board,
                              @RequestParam(value = "files[]", required = false)
                              MultipartFile[] files

    ) throws Exception {
//        System.out.println("이것은 Post요청 board = " + board);


//        System.out.println("files = " + files);
        if (service.validate(board)) {

            service.add(board, files);
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("list")
    public Map<String, Object> list(@RequestParam(defaultValue = "1") Integer page,
                                    @RequestParam(defaultValue = "30") Integer pageAmount,
                                    @RequestParam(defaultValue = "false") Boolean offsetReset,
                                    HttpSession session,
                                    @RequestParam(defaultValue = "전체") String boardType,
                                    @RequestParam(defaultValue = "전체") String searchType,
                                    @RequestParam(defaultValue = "") String keyword) throws Exception {
//        System.out.println("page = " + page);
//        System.out.println("이것은 서비스의 searchType = " + searchType);
        return service.list(page, pageAmount, offsetReset, session, boardType, searchType, keyword);
    }

    @GetMapping("{id}")
    public Map<String, Object> get(@PathVariable Integer id, @RequestParam(required = false) Integer memberId) {

        System.out.println("컨트롤러의 get요청 memberId = " + memberId);

        return service.getByBoardIdAndMemberId(id, memberId);

    }

    @DeleteMapping("{id}")
    public ResponseEntity delete(@PathVariable Integer id, @RequestParam Integer memberId) {

        //서버에서 이중 교차검증
        if (service.hasAccess(id, memberId)) {

            service.delete(id);
            return ResponseEntity.ok().build();

        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @PutMapping("edit")
    public ResponseEntity edit(Board board,
                               @RequestParam(value = "removeFileList[]", required = false)
                               List<String> removeFileList,
                               @RequestParam(value = "addFileList[]", required = false)
                               MultipartFile[] addFileList,
                               @RequestParam(required = false) Integer memberId
    ) throws Exception {
//        System.out.println("이것은 PUT요청 board= " + board);

        if (service.validate(board) && service.hasAccess(board.getId(), memberId)) {
            service.edit(board, removeFileList, addFileList);
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @PutMapping("/like")
    public ResponseEntity<Map<String, Object>> like(@RequestBody Map<String, Object> req) {
        System.out.println("컨트롤러의 like 메서드 req = " + req);

        // memberId가 null이거나 비어있는 경우
        if (!req.containsKey("memberId") || req.get("memberId") == null) {
            return ResponseEntity.badRequest().build();
        }

//         memberId가 Integer가 아닌 경우 처리
//        if (!(req.get("memberId") instanceof Integer)) {
//            return ResponseEntity.badRequest().build();
//        }

        return ResponseEntity.ok(service.like(req));
    }

    @PostMapping("/report")
    public ResponseEntity report(@RequestBody Map<String, Object> req) {
        System.out.println("req = " + req);
        if (service.isLoggedIn((Integer) req.get("memberId"))) {
            if (service.addReport(req)) {
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        } else {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/report/list")
    public Map<String, Object> reportList(@RequestParam(defaultValue = "1") Integer page,
                                          @RequestParam(defaultValue = "30") Integer pageAmount,
                                          @RequestParam(defaultValue = "false") Boolean offsetReset,
                                          HttpSession session,
                                          @RequestParam(defaultValue = "전체") String boardType,
                                          @RequestParam(defaultValue = "전체") String searchType,
                                          @RequestParam(defaultValue = "") String keyword) throws Exception {
//        System.out.println("page = " + page);
        return service.reportList(page, pageAmount, offsetReset, session, boardType, searchType, keyword);
    }

    @GetMapping("/report/list/content")
    public ResponseEntity<Map<String, Object>> reportContent(@RequestParam Integer boardId, @RequestParam Integer repoterMemberId) {
        Map<String, Object> response = service.reportContent(boardId, repoterMemberId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("delete")
    public ResponseEntity deleteMultiple(@RequestBody DeleteRequest deleteRequest) {
        List<Integer> ids = deleteRequest.getIds();
        Integer memberId = deleteRequest.getMemberId();
        System.out.println("memberId = " + memberId);

        for (Integer id : ids) {
            if (!service.hasAccess(id, memberId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }

        for (Integer id : ids) {
            service.delete(id);
        }

        return ResponseEntity.ok().build();
    }

    @GetMapping("/latest")
    public ResponseEntity<List<Board>> getLatestBoards() {
        List<Board> latestBoards = service.getLatestBoards();
        return ResponseEntity.ok(latestBoards);
    }

    @GetMapping("/popular")
    public ResponseEntity<List<Board>> getPopularBoards() {
        List<Board> popularBoards = service.getPopularBoards();
        return ResponseEntity.ok(popularBoards);
    }

    @GetMapping("/topLikedImages")
    public ResponseEntity<List<Map<String, Object>>> getTopLikedImages() {
        List<Map<String, Object>> topLikedImages = service.getTopLikedImages();
        return ResponseEntity.ok(topLikedImages);
    }

    @GetMapping("/guide")
    public ResponseEntity<List<Map<String, Object>>> getGuideBoards() {
        List<Map<String, Object>> guideBoards = service.getGuideBoards();
        return ResponseEntity.ok(guideBoards);
    }
}
