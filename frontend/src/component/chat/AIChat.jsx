import React, { useContext, useEffect, useRef, useState } from "react";
import {
  Box,
  Button,
  Flex,
  IconButton,
  Input,
  InputGroup,
  InputRightElement,
  Text,
  VStack,
} from "@chakra-ui/react";
import { ChatIcon, MinusIcon } from "@chakra-ui/icons";
import SockJS from "sockjs-client";
import { Client } from "@stomp/stompjs";
import axios from "axios";
import { LoginContext } from "../LoginProvider.jsx";

export function AIChat() {
  const { memberInfo } = useContext(LoginContext);
  const name = memberInfo?.nickname || "Anonymous"; // 사용자 nickname
  const [message, setMessage] = useState("");
  const [messages, setMessages] = useState([
    {
      role: "assistant",
      content: "안녕하세요, 무엇을 도와드릴까요?",
      timestamp: new Date().toISOString(),
    },
  ]);
  const [stompClient, setStompClient] = useState(null); // stompClient 상태 정의 및 초기화
  const [isConnected, setIsConnected] = useState(false);
  const [isMinimized, setIsMinimized] = useState(false); // 최소화 상태 관리
  const messagesEndRef = useRef(null);

  useEffect(() => {
    const socket = new SockJS("http://localhost:8080/ws");
    const client = new Client({
      webSocketFactory: () => socket,
      reconnectDelay: 5000,
      onConnect: () => {
        console.log("Connected to WebSocket");
        client.subscribe("/topic/ai-messages", (message) => {
          const receivedMessage = JSON.parse(message.body);
          console.log("Received message:", receivedMessage);
          if (
            !messages.find((msg) => msg.timestamp === receivedMessage.timestamp)
          ) {
            setMessages((prevMessages) => [...prevMessages, receivedMessage]);
          }
        });
        setIsConnected(true);
        setStompClient(client); // stompClient 상태 설정
        console.log("STOMP Client Connected");
      },
      onStompError: (frame) => {
        console.error("Broker error: ", frame.headers["message"], frame.body);
        setIsConnected(false);
      },
      onWebSocketError: (error) => {
        console.error("WebSocket error: ", error);
        setIsConnected(false);
      },
      onDisconnect: () => {
        console.log("Disconnected from WebSocket");
        setIsConnected(false);
      },
    });
    client.activate();

    return () => {
      if (client) {
        client.deactivate();
      }
    };
  }, [messages]);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  const fetchGPTResponse = async (userMessage) => {
    try {
      const response = await axios.post("http://localhost:5000/api/chat", {
        messages: [...messages, userMessage],
      });
      return {
        role: "assistant",
        content: response.data.content,
        roleDescription: "Veterinarian",
        timestamp: new Date().toISOString(),
      };
    } catch (error) {
      console.error("Error fetching completion: ", error);
      return {
        role: "assistant",
        content: "Error occurred while fetching response",
        roleDescription: "Veterinarian",
        timestamp: new Date().toISOString(),
      };
    }
  };

  const sendMessage = async () => {
    if (isConnected && message) {
      const userMessage = {
        role: "user",
        content: message,
        timestamp: new Date().toISOString(),
      };

      setMessages([...messages, userMessage]);
      setMessage("");

      const assistantMessage = await fetchGPTResponse(userMessage);
      setMessages((prevMessages) => [...prevMessages, assistantMessage]);

      const chatMessage = {
        sender: name,
        content: userMessage.content,
        timestamp: new Date().toISOString(),
      };
      console.log("Sending message:", chatMessage);
      stompClient.publish({
        destination: "/app/ai-chat",
        body: JSON.stringify(chatMessage),
      });
    } else {
      console.error(
        "Cannot send message: WebSocket is not connected or message is empty.",
      );
    }
  };

  const handleKeyDown = (e) => {
    if (e.key === "Enter") {
      sendMessage();
    }
  };

  const toggleMinimize = () => {
    setIsMinimized(!isMinimized);
  };

  return (
    <Box
      mt={10}
      p={4}
      w="80%"
      maxW="800px"
      mx="auto"
      borderWidth="1px"
      borderRadius="lg"
      overflow="hidden"
      bg="white"
      boxShadow="lg"
    >
      <Box
        display="flex"
        justifyContent="space-between"
        alignItems="center"
        borderBottomWidth="1px"
        p={2}
      >
        <Text fontWeight="bold" fontSize="lg">
          AI 수의사와 상담
        </Text>
        <IconButton
          icon={isMinimized ? <ChatIcon /> : <MinusIcon />}
          size="sm"
          onClick={toggleMinimize}
          aria-label={isMinimized ? "Expand Chat" : "Minimize Chat"}
        />
      </Box>
      {!isMinimized && (
        <VStack spacing={4} p={2}>
          <Box
            width="100%"
            h="500px"
            overflowY="scroll"
            p={2}
            borderWidth="1px"
            borderRadius="lg"
            bg="gray.50"
          >
            {messages.map((msg, index) => (
              <Flex
                key={index}
                justifyContent={msg.role === "user" ? "flex-end" : "flex-start"}
                mb={2}
              >
                <Box
                  bg={msg.role === "user" ? "blue.100" : "gray.100"}
                  p={3}
                  borderRadius="md"
                  maxWidth="70%"
                >
                  <Text fontSize="md" fontWeight="bold">
                    {msg.role === "user" ? name : "AI 수의사"}
                  </Text>
                  <Text fontSize="md">{msg.content}</Text>
                  <Text fontSize="sm" color="gray.500">
                    {new Date(msg.timestamp).toLocaleString("ko-KR", {
                      timeZone: "Asia/Seoul",
                    })}
                  </Text>
                </Box>
              </Flex>
            ))}
            <div ref={messagesEndRef} />
          </Box>
          <InputGroup>
            <Input
              type="text"
              value={message}
              onChange={(e) => setMessage(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="메시지를 입력하세요"
              fontSize="md"
            />
            <InputRightElement width="4.5rem">
              <Button
                h="1.75rem"
                size="md"
                onClick={sendMessage}
                disabled={!isConnected}
              >
                보내기
              </Button>
            </InputRightElement>
          </InputGroup>
        </VStack>
      )}
    </Box>
  );
}
