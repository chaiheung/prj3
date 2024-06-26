import {
  Badge,
  Box,
  Button,
  Center,
  Flex,
  Heading,
  Input,
  Select,
  Table,
  Tbody,
  Td,
  Th,
  Thead,
  Tr,
  useColorModeValue,
} from "@chakra-ui/react";
import React, { useContext, useEffect, useState } from "react";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faImages, faMagnifyingGlass } from "@fortawesome/free-solid-svg-icons";
import axios from "axios";
import { useNavigate, useSearchParams } from "react-router-dom";
import { LoginContext } from "../../../../../component/LoginProvider.jsx";
import { generateDiaryId } from "../../../../../util/util.jsx";
import Pagination from "../../../../../component/Pagination.jsx";
import { format } from "date-fns";

export function DiaryBoardList() {
  const { memberInfo, setMemberInfo } = useContext(LoginContext);
  const [diaryBoardList, setDiaryBoardList] = useState([]);
  const [pageInfo, setPageInfo] = useState({});
  const [searchType, setSearchType] = useState("all");
  const [searchKeyword, setSearchKeyword] = useState("");
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  useEffect(() => {
    axios.get(`/api/diaryBoard/list?${searchParams}`).then((res) => {
      // 변경된 부분
      setDiaryBoardList(res.data.diaryBoardList);
      setPageInfo(res.data.pageInfo);
    });

    setSearchType("all");
    setSearchKeyword("");

    const typeParam = searchParams.get("type");
    const keywordParam = searchParams.get("keyword");
    if (typeParam) {
      setSearchType(typeParam);
    }
    if (keywordParam) {
      setSearchKeyword(keywordParam);
    }
  }, [searchParams]);

  const pageNumbers = [];
  for (let i = pageInfo.leftPageNumber; i <= pageInfo.rightPageNumber; i++) {
    pageNumbers.push(i);
  }

  function handleSearchClick() {
    const params = new URLSearchParams(searchParams);
    params.set("type", searchType);
    params.set("keyword", searchKeyword);
    navigate(`/api/diaryBoard/list?${params.toString()}`); // 변경된 부분
  }

  function handlePageButtonClick(pageNumber) {
    const params = new URLSearchParams(searchParams);
    params.set("page", pageNumber);
    navigate(`/api/diaryBoard/list?${params.toString()}`); // 변경된 부분
  }

  function handleSelectedDiaryBoard(id) {
    const diaryId = generateDiaryId(memberInfo.id);
    return () => navigate(`/diary/${diaryId}/view/${id}`);
  }

  function handleWriteClick() {
    const diaryId = generateDiaryId(memberInfo.id);
    navigate(`/diary/${diaryId}/write/${diaryBoardList.id}`);
  }

  // const bg = useColorModeValue("white", "gray.800");
  const hoverBg = useColorModeValue("gray.100", "gray.700");

  return (
    <>
      <Box mb={5}></Box>
      <Center>
        <Heading>다이어리 목록</Heading>
      </Center>
      <Box>
        {memberInfo && <Button onClick={handleWriteClick}>글쓰기</Button>}
      </Box>
      <Box>
        {diaryBoardList.length === 0 && <Center>조회 결과가 없습니다.</Center>}
        {diaryBoardList.length > 0 && (
          <Table>
            <Thead>
              <Tr>
                <Th>N번째 일기</Th>
                <Th>내용</Th>
                <Th>who?</Th>
                <Th>작성일자</Th>
              </Tr>
            </Thead>
            <Tbody>
              {diaryBoardList.map((diaryBoard) => (
                <Tr
                  key={diaryBoard.id}
                  _hover={{ bg: hoverBg }}
                  cursor="pointer"
                  onClick={handleSelectedDiaryBoard(diaryBoard.id)}
                >
                  <Td textAlign="center">{diaryBoard.id}</Td>
                  <Td textAlign="center">
                    {diaryBoard.title}
                    {diaryBoard.numberOfImages > 0 && (
                      <Badge ml={2} colorScheme="teal">
                        <FontAwesomeIcon icon={faImages} />
                        {diaryBoard.numberOfImages}
                      </Badge>
                    )}
                  </Td>
                  <Td textAlign="center">{diaryBoard.writer}</Td>
                  <Td>
                    {" "}
                    <span style={{ color: "red" }} />
                    {format(new Date(diaryBoard.inserted), "yyyy.MM.dd")}
                  </Td>
                </Tr>
              ))}
            </Tbody>
          </Table>
        )}
      </Box>
      <Pagination
        pageInfo={pageInfo}
        pageNumbers={pageNumbers}
        handlePageButtonClick={handlePageButtonClick}
      />

      <Center mb={10}>
        <Flex gap={2}>
          <Box>
            <Select
              value={searchType}
              onChange={(e) => setSearchType(e.target.value)}
              boxShadow="md"
              _hover={{ boxShadow: "lg" }}
            >
              <option value="all">전체</option>
              <option value="text">글</option>
              <option value="nickname">작성자</option>
            </Select>
          </Box>
          <Box>
            <Input
              value={searchKeyword}
              onChange={(e) => setSearchKeyword(e.target.value)}
              placeholder="검색어"
              boxShadow="md"
              _hover={{ boxShadow: "lg" }}
            />
          </Box>
          <Box>
            <Button
              onClick={handleSearchClick}
              colorScheme="teal"
              boxShadow="md"
              _hover={{ boxShadow: "lg" }}
            >
              <FontAwesomeIcon icon={faMagnifyingGlass} />
            </Button>
          </Box>
        </Flex>
      </Center>
    </>
  );
}
