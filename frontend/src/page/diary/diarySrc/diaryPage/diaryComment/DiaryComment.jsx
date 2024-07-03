import { Box, Center, Spinner } from "@chakra-ui/react";
import { useContext, useEffect, useState } from "react";
import axios from "axios";
import { useParams } from "react-router-dom";
import {
  extractUserIdFromDiaryId,
  generateDiaryId,
} from "../../../../../util/util.jsx";
import { LoginContext } from "../../../../../component/LoginProvider.jsx";
import { DiaryCommentWrite } from "./DiaryCommentWrite.jsx";
import { DiaryCommentList } from "./DiaryCommentList.jsx";
import DiaryPagination from "./DiaryPagination.jsx";

export function DiaryComment() {
  const { id } = useParams();
  const [diaryCommentList, setDiaryCommentList] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const { memberInfo } = useContext(LoginContext);
  const diaryId = useParams().diaryId;
  const isOwner =
    Number(memberInfo?.id) === Number(extractUserIdFromDiaryId(diaryId));
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);

  useEffect(() => {
    const diaryId = generateDiaryId(memberInfo.id);
    const fetchComments = async (page) => {
      try {
        const res = await axios.get(`/api/diaryComment/list`, {
          params: { id, page, pageSize: 5 },
        });
        setDiaryCommentList(res.data.comments);
        setTotalPages(res.data.totalPages);
        console.log("Comments fetched:", res.data);
      } catch (err) {
        console.error("Error fetching comments:", err);
      } finally {
        setIsLoading(false);
      }
    };
    fetchComments(currentPage);
  }, [id, memberInfo.id, currentPage]);

  const handleCommentAdded = (newComment) => {
    setDiaryCommentList((prevList) => [newComment, ...prevList]); // 새로운 댓글을 맨 위에 추가
  };

  const handlePageButtonClick = (pageNumber) => {
    setCurrentPage(pageNumber);
  };

  if (isLoading) {
    return (
      <Center>
        <Spinner />
      </Center>
    );
  }

  return (
    <Box>
      <DiaryCommentWrite
        onCommentAdded={handleCommentAdded}
        diaryCommentList={diaryCommentList}
      />
      <DiaryCommentList diaryCommentList={diaryCommentList} />
      <DiaryPagination
        pageInfo={{
          currentPageNumber: currentPage,
          nextPageNumber: currentPage < totalPages ? currentPage + 1 : null,
          prevPageNumber: currentPage > 1 ? currentPage - 1 : null,
          lastPageNumber: totalPages,
        }}
        pageNumbers={Array.from({ length: totalPages }, (_, i) => i + 1)}
        handlePageButtonClick={handlePageButtonClick}
        maxPageButtons={10} // 페이지 버튼을 10개씩 끊어서 표시합니다.
      />
    </Box>
  );
}
