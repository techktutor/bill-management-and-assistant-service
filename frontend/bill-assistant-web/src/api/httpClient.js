import axios from "axios";

const httpClient = axios.create({
  baseURL: "http://localhost:8080/assistant",
  withCredentials: true,
});

export default httpClient;
