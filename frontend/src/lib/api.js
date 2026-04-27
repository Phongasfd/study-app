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
    const message =
      error.response?.data?.message ||
      (typeof error.response?.data === "string" && error.response.data) ||
      error.message;

    throw new Error(message);
  }
};
 
export const googleAuth = () => {
  window.location.href = 'http://localhost:8080/oauth2/authorization/google';
}

export const login = async (email, password) => {
  try {
    const response = await axiosClient.post('/auth/login', {
      email,
      password
    });
    return response.data;
  } catch (error) {
    const message =
      error.response?.data?.message ||
      (typeof error.response?.data === "string" && error.response.data) ||
      error.message;

    throw new Error(message);
  }
}

export const getSubjects = async () => {
  try {
    const response = await axiosClient.get('/subject/');
    return response.data;
  } catch (error) {
    const message =
      error.response?.data?.message ||
      (typeof error.response?.data === "string" && error.response.data) ||
      error.message;

    throw new Error(message);
  }
};

export const createSubject = async (name) => {
  try {
    const response = await axiosClient.post('/subject/', { name });
    return response.data;
  } catch (error) {
    const message =
    error.response?.data?.message ||
    (typeof error.response?.data === "string" && error.response.data) ||
    error.message;

    throw new Error(message);
  }
};

export const deleteSubject = async (id) => {
  try {
    const response = await axiosClient.delete(`/subject/${id}`);
    return response.data;
  } catch (error) {
    const message =
    error.response?.data?.message ||
    (typeof error.response?.data === "string" && error.response.data) ||
    error.message;

    throw new Error(message);
  }
};

export const createGroup = async (name, maxMembers) => {
  try {
    const response = await axiosClient.post('/group/', { name, maxMembers });
    return response.data;
  } catch (error) {

    const message =
    error.response?.data?.message ||
    (typeof error.response?.data === "string" && error.response.data) ||
    error.message;

    throw new Error(message);
  }
};

export const getGroupsJoined = async () => {
  try {
    const response = await axiosClient.get('/group/joined');
    return response.data;
  } catch (error) {
    const message =
      error.response?.data?.message ||
      (typeof error.response?.data === "string" && error.response.data) ||
      error.message;

    throw new Error(message);
  }
};
