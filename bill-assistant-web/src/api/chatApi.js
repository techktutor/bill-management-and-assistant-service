import httpClient from "./httpClient";

export const sendChatMessage = (message) =>
  httpClient.post("/api/chat/process", message).then((res) => res.data);

export const getChatHistory = () =>
  httpClient.get("/api/chat/history").then((res) => res.data);

/**
 * Get current contextId (used to scope chat history)
 */
export const getChatContext = () =>
  httpClient.get("/api/context").then((res) => res.data.contextId);
