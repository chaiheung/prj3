import { useContext, useState } from "react";
import { Box, Button, Flex, Textarea, Tooltip } from "@chakra-ui/react";
import axios from "axios";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faPencil } from "@fortawesome/free-solid-svg-icons";
import { LoginContext } from "../LoginProvider.jsx";

export function BoardCommentWrite({ boardId, isProcessing, setIsProcessing }) {
  const [boardComment, setBoardComment] = useState("");
  const { memberInfo, setMemberInfo } = useContext(LoginContext);
  const memberId = memberInfo && memberInfo.id ? parseInt(memberInfo.id) : null;
  const params = memberId ? { memberId } : {};

  function handleBoardCommentSubmitClick() {
    setIsProcessing(true);
    axios
      .postForm("/api/comment/add", {
        boardId,
        boardComment,
        memberId: params.memberId,
      })
      .then(() => {
        setBoardComment("");
      })
      .catch(() => {})
      .finally(() => {
        setIsProcessing(false);
      });
  }

  // console.log(account);

  if (!memberInfo) {
    return;
  }
  return (
    <Flex gap={2} mb={10}>
      <Box flex={1}>
        <Textarea
          // isDisabled={!account.isLoggedIn()}
          placeholder={"댓글을 작성해 보세요"}
          value={boardComment}
          onChange={(e) => setBoardComment(e.target.value)}
        />
      </Box>
      <Box>
        <Tooltip label="로그인 하세요" isDisabled={memberId} placement="bottom">
          <Button
            h={"100%"}
            w="80px"
            isLoading={isProcessing}
            onClick={handleBoardCommentSubmitClick}
            colorScheme={"blue"}
          >
            <FontAwesomeIcon icon={faPencil} />
          </Button>
        </Tooltip>
      </Box>
    </Flex>
  );
}
