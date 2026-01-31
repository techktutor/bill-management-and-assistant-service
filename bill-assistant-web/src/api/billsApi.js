import httpClient from "./httpClient";

export const getBills = () =>
  httpClient.get("/api/bills").then((res) => res.data);

export const payBill = (billId) => httpClient.post(`/api/bills/${billId}/pay`);
