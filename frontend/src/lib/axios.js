import axios from "axios";

const axiosClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  withCredentials: true,
  timeout: 10000,
  headers: {
    "Content-Type": "application/json",
  }
});

// const axiosRaw = axios.create({
//   baseURL: import.meta.env.VITE_API_BASE_URL,
//   withCredentials: true,
// });

// let csrfToken = null;

// export const fetchCsrfToken = async () => {
//   const res = await axiosRaw.get("/csrf-token");
//   csrfToken = res.data.csrfToken;
//   return csrfToken;
// };

// axiosClient.interceptors.request.use(async (config) => {
//   if (!csrfToken) {
//     await fetchCsrfToken();
//   }

//   config.headers["X-CSRF-Token"] = csrfToken;

//   return config;
// });

export default axiosClient;