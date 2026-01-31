import httpClient from "./httpClient";

export const getPayments = () =>
  httpClient.get("/api/payments").then(res => res.data);
