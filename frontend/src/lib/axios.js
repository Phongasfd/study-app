import axios from "axios";

const axiosClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  withCredentials: true,
  timeout: 10000,
  headers: {
    "Content-Type": "application/json",
  }
});

const getCookie = (name) => {
  const value = `; ${document.cookie}`; // Cookie: "JWT=xyz; XSRF-TOKEN=abc123; theme=dark"
  const parts = value.split(`; ${name}=`); // Value: value = "; JWT=xyz; XSRF-TOKEN=abc123"
  // // [
  //   "; JWT=xyz",
  //   "abc123"
  // ] --> parts
  
  if (parts.length === 2) { // find csrf cookie
    return parts.pop().split(';').shift(); // pop the last part and get token 
  } 
  
  return null;
}

axiosClient.interceptors.request.use((config) => {
  const csrfToken = getCookie("XSRF-TOKEN");

  if (csrfToken) {
    config.headers["X-XSRF-TOKEN"] = csrfToken;
  }

  return config;
});
export default axiosClient;