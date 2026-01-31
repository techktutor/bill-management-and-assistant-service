import httpClient from "./httpClient";

export const getBills = (page = 0, size = 10) =>
  httpClient
    .get("/api/bills", { params: { page, size } })
    .then((res) => res.data);
