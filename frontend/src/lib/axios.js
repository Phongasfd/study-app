import axios from "axios";
import i18next from 'i18next';

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

  // set Accept-Language header from current i18n language
  try {
    config.headers['Accept-Language'] = i18next.language || navigator.language || 'vi';
  } catch (e) {
    // ignore
  }

  return config;
});

// when access-token expires, automatically call /refresh, not logout user 
// Implement refresh-on-401 with single-flight refresh
let isRefreshing = false; // only let 1 request refresh at a time 
let refreshSubscribers = []; // // Queue các request đang bị 401 trong lúc refresh đang diễn ra
 
// Call all subscribers once refresh is done 
function onRefreshed() {
  refreshSubscribers.forEach(cb => cb()); // callback để retry các request đang bị 401
  refreshSubscribers = []; // clear quêu 
}

/**
* Thêm request vào hàng đợi chờ refresh xong
* callback sẽ chạy lại request
*/
function addRefreshSubscriber(cb) {
  refreshSubscribers.push(cb);
}

// Interceptor để bắt lỗi 401 và thực hiện refresh token 
axiosClient.interceptors.response.use(
  response => response,
  async error => {
    const originalRequest = error.config; 

    const isRefreshRequest =
    originalRequest?.url?.includes('/auth/refresh');



    // If 401 and not retrying, try refresh
    if (error.response && error.response.status === 401 && !originalRequest._retry && !isRefreshRequest) {
      /**
       * Đã có request khác đang refresh rồi
       * không refresh lại lần nữa
       * chỉ chờ kết quả refresh
       */
      if (isRefreshing) {
        // queue and wait
        // không refresh nữa
        // chỉ đợi refresh xong rồi chạy lại request này
        return new Promise((resolve, reject) => {
          addRefreshSubscriber(() => {
            originalRequest._retry = true; // đánh dấu đã retry rồi để tránh loop 
            resolve(axiosClient(originalRequest)); // retry request sau khi refresh xong
          });
        });
      }

      // request hiện tại đứng ra refresh token
      isRefreshing = true;

      try {
        await axiosClient.post('/auth/refresh');
        isRefreshing = false;
        onRefreshed();
        originalRequest._retry = true;
        return axiosClient(originalRequest);
      } catch (refreshError) {
        isRefreshing = false;
        refreshSubscribers = [];
        // if refresh fails, propagate original error
        return Promise.reject(refreshError);
      }
    }
    // không phải 401 hoặc đã retry rồi
    return Promise.reject(error);
  }
);
export default axiosClient;