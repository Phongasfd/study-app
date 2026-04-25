import axiosClient from "./axios";

export const register = async (username, email, password) => {
  try {
    const response = await axiosClient.post('/auth/register', {
      username,
      email,
      password
    });
    return response.data;
  } catch (error) {
    throw new Error(error.response?.data || error.message);
  }
};
 
export const googleAuth = () => {
  window.location.href = 'http://localhost:8080/oauth2/authorization/google';
}

export const login =  async (email, password) => {
  try {
    const response = await axiosClient.post('/auth/login', {
      email,
      password
    });
    return response.data;
  } catch (error) {
    throw new Error(error.response?.data || error.message); 
  }

}
