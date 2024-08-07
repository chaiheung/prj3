import { Box, Button, Textarea, Tooltip, useToast } from "@chakra-ui/react";
import { useContext, useState } from "react";
import axios from "axios";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faPaperPlane } from "@fortawesome/free-solid-svg-icons";
import { LoginContext } from "../LoginProvider.jsx";
import { StarRating } from "./StarRating.jsx";

export function CommentWrite({ hospitalId, isProcessing, setIsProcessing }) {
  const [comment, setComment] = useState("");
  const [ratingIndex, setRatingIndex] = useState(1);
  const toast = useToast();
  const { memberInfo, setMemberInfo } = useContext(LoginContext);
  const access = memberInfo ? memberInfo.access : null; // access가 없는 경우를 처리
  const isLoggedIn = Boolean(access);

  if (!memberInfo) {
    return null; // 또는 로딩 스피너를 표시할 수 있습니다.
  }

  async function handleCommentSubmitClick() {
    setIsProcessing(true);

    try {
      await axios.post("/api/hospitalComment/add", {
        hospitalId,
        comment,
        memberId: memberInfo.id,
        nickname: memberInfo.nickname,
        rate: ratingIndex,
      });

      setComment("");
      setRatingIndex(1); // 별점 초기화
      toast({
        description: "댓글이 등록되었습니다.",
        position: "top",
        status: "success",
      });
    } catch (err) {
      toast({
        description: "댓글 등록에 실패했습니다.",
        position: "top",
        status: "error",
      });
    } finally {
      setIsProcessing(false);
      setComment("");
      setRatingIndex(1); // 별점 초기화
    }
  }

  return (
    <Box>
      <StarRating ratingIndex={ratingIndex} setRatingIndex={setRatingIndex} />
      <Textarea
        isDisabled={!isLoggedIn}
        placeholder={
          isLoggedIn
            ? "리뷰를 작성해 보세요."
            : "리뷰를 작성하시려면 로그인하세요."
        }
        value={comment}
        onChange={(e) => setComment(e.target.value)}
      />
      <Tooltip label="로그인 하세요" isDisabled={isLoggedIn} placement="top">
        <Button
          isDisabled={comment.trim().length === 0}
          isLoading={isProcessing}
          onClick={handleCommentSubmitClick}
          colorScheme="blue"
        >
          <FontAwesomeIcon icon={faPaperPlane} />
        </Button>
      </Tooltip>
    </Box>
  );
}
