import httpClient from "./httpClient";

export const uploadBills = (files) => {
  const formData = new FormData();
  files.forEach((file) => formData.append("files", file));

  return httpClient.post("/api/ingest/files", formData, {
    headers: {
      "Content-Type": "multipart/form-data",
    },
  });
};
