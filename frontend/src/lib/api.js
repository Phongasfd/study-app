import axiosClient from "./axios";

export const register = async (username, email, password) => {
  const response = await axiosClient.post('/auth/register', {
    username,
    email,
    password
  });
  return response.data;
};
 
export const googleAuth = () => {
  window.location.href = 'http://localhost:8080/oauth2/authorization/google';
}
