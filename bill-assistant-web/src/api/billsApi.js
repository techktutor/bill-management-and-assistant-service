import httpClient from "./httpClient";

export const getBills = (page = 0, size = 10) =>
  httpClient
    .get("/api/bills", { params: { page, size } })
    .then((res) => res.data);


export const payBill = (billId) =>
  httpClient
    .post(`/api/bills/${billId}/pay`)
    .then((res) => res.data);

export const getBillById = (billId) =>
  httpClient
  .get(`/api/bills/${billId}`)
  .then((res) => res.data);