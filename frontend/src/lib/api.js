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

export const getRandomGroups = async () => {
  try {
    const response = await axiosClient.get('/group/random');
    return response.data;
  } catch (error) {
    const message =
      error.response?.data?.message ||
      (typeof error.response?.data === "string" && error.response.data) ||
      error.message;

    throw new Error(message);
  }
};

export const getGroupDetails = async (id) => {
  try {
    const response = await axiosClient.get(`/group/${id}`);
    return response.data;
  } catch (error) {
    const message =
      error.response?.data?.message ||
      (typeof error.response?.data === "string" && error.response.data) ||
      error.message;

    throw new Error(message);
  }
};

export const deleteGroup = async (id) => {
  try {
    const response = await axiosClient.delete(`/group/${id}`);
    return response.data;
  } catch (error) {
    const message =
      error.response?.data?.message ||
      (typeof error.response?.data === "string" && error.response.data) ||
      error.message;

    throw new Error(message);
  }
};

export const joinGroup = async (groupId) => {
  try {
    const response = await axiosClient.post(`/group-member/join/${groupId}`);
    return response.data;
  } catch (error) {
    const message =
      error.response?.data?.message ||
      (typeof error.response?.data === "string" && error.response.data) ||
      error.message;

    throw new Error(message);
  }
};

export const removeMember = async (groupId, userId) => {
  try {
    const response = await axiosClient.delete(`/group-member/${groupId}/user/${userId}`);
    return response.data;
  } catch (error) {
    const message =
      error.response?.data?.message ||
      (typeof error.response?.data === "string" && error.response.data) ||
      error.message;

    throw new Error(message);
  }
};

export const searchUsers = async (username) => {
  try {
    const response = await axiosClient.get(`/user/search`, {
      params: { username }
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

export const addMemberToGroup = async (groupId, userId) => {
  try {
    const response = await axiosClient.post(`/group-member/${groupId}/add/${userId}`);
    return response.data;
  } catch (error) {
    const message =
      error.response?.data?.message ||
      (typeof error.response?.data === "string" && error.response.data) ||
      error.message;

    throw new Error(message);
  }
};

export const searchGroups = async (name) => {
  try {
    const response = await axiosClient.get(`/group/search`, {
      params: { name }
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

export const createStudySession = async (startTime) => {
  try {
    const response = await axiosClient.post('/study-session/', { startTime });
    return response.data;
  } catch (error) {
    const message =
      error.response?.data?.message ||
      (typeof error.response?.data === "string" && error.response.data) ||
      error.message;

    throw new Error(message);
  }
};

export const completeStudySession = async (id, endTime) => {
  try {
    const response = await axiosClient.patch(`/study-session/${id}/complete`, { endTime });
    return response.data;
  } catch (error) {
    const message =
      error.response?.data?.message ||
      (typeof error.response?.data === "string" && error.response.data) ||
      error.message;

    throw new Error(message);
  }
};

export const addSubjectToSession = async (sessionId, subjectId) => {
  try {
    const response = await axiosClient.post('/session-subject/', { sessionId, subjectId });
    return response.data;
  } catch (error) {
    const message =
      error.response?.data?.message ||
      (typeof error.response?.data === "string" && error.response.data) ||
      error.message;

    throw new Error(message);
  }
};

export const getWeeklyStats = async () => {
  try {
    const response = await axiosClient.get('/stat/weekly');
    return response.data;
  } catch (error) {
    const message =
      error.response?.data?.message ||
      (typeof error.response?.data === "string" && error.response.data) ||
      error.message;

    throw new Error(message);
  }
};

export const syncDailyStat = async () => {
  try {
    const response = await axiosClient.post('/stat/sync');
    return response.data;
  } catch (error) {
    const message =
      error.response?.data?.message ||
      (typeof error.response?.data === "string" && error.response.data) ||
      error.message;

    throw new Error(message);
  }
};

export const getMonthlyStats = async () => {
  try {
    const response = await axiosClient.get('/stat/monthly');
    return response.data;
  } catch (error) {
    const message =
      error.response?.data?.message ||
      (typeof error.response?.data === "string" && error.response.data) ||
      error.message;

    throw new Error(message);
  }
};

