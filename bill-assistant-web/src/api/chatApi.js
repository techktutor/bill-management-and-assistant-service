import httpClient from "./httpClient";

export const sendChatMessage = (message) =>
  httpClient.post("/api/chat/process", message).then((res) => res.data);

export const getChatHistory = () =>
  httpClient.get("/api/chat/history").then((res) => res.data);
