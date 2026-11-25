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
  const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';
  window.location.href = `${apiBaseUrl}/oauth2/authorization/google`;
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

export const createGroup = async (name, maxMembers, visibility = 'PUBLIC', description = '') => {
  try {
    const response = await axiosClient.post('/group/', { name, maxMembers, visibility, description });
    return response.data;
  } catch (error) {

    const message =
      error.response?.data?.message ||
      (typeof error.response?.data === "string" && error.response.data) ||
      error.message;

    throw new Error(message);
  }
};

export const updateGroupSettings = async (id, settings) => {
  try {
    const response = await axiosClient.patch(`/group/${id}/settings`, settings);
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

export const joinGroup = async (groupId, message = null) => {
  try {
    const response = await axiosClient.post(`/group-member/join/${groupId}`, message ? { message } : {});
    return response.data;
  } catch (error) {
    const errMessage =
      error.response?.data?.message ||
      (typeof error.response?.data === "string" && error.response.data) ||
      error.message;

    throw new Error(errMessage);
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

export const getWeeklySubjectStats = async () => {
  try {
    const response = await axiosClient.get('/session-subject/weekly-stats');
    return response.data;
  } catch (error) {
    const message =
      error.response?.data?.message ||
      (typeof error.response?.data === "string" && error.response.data) ||
      error.message;

    throw new Error(message);
  }
};

export const getMonthlySubjectStats = async () => {
  try {
    const response = await axiosClient.get('/session-subject/monthly-stats');
    return response.data;
  } catch (error) {
    const message =
      error.response?.data?.message ||
      (typeof error.response?.data === "string" && error.response.data) ||
      error.message;

    throw new Error(message);
  }
};

export const getGroupRanking = async (groupId) => {
  try {
    const response = await axiosClient.get(`/group-ranking/${groupId}`);
    return response.data;
  } catch (error) {
    const message =
      error.response?.data?.message ||
      (typeof error.response?.data === "string" && error.response.data) ||
      error.message;

    throw new Error(message);
  }
};

export const createGroupRanking = async (groupId, userId) => {
  try {
    const response = await axiosClient.post('/group-ranking', { groupId, userId });
    return response.data;
  } catch (error) {
    const message =
      error.response?.data?.message ||
      (typeof error.response?.data === "string" && error.response.data) ||
      error.message;

    throw new Error(message);
  }
};

export const getChatMessages = async (groupId) => {
  try {
    const response = await axiosClient.get(`/chat/${groupId}`);
    return response.data;
  } catch (error) {
    const message =
      error.response?.data?.message ||
      (typeof error.response?.data === "string" && error.response.data) ||
      error.message;

    throw new Error(message);
  }
};

export const sendChatMessage = async (groupId, content) => {
  try {
    const response = await axiosClient.post('/chat', { groupId, content });
    return response.data;
  } catch (error) {
    const message =
      error.response?.data?.message ||
      (typeof error.response?.data === "string" && error.response.data) ||
      error.message;

    throw new Error(message);
  }
};

const unwrapError = (error) => {
  const message =
    error.response?.data?.message ||
    (typeof error.response?.data === "string" && error.response.data) ||
    error.message;
  throw new Error(message);
};

// ---- Study Goals ----

export const getGoals = async () => {
  try {
    const response = await axiosClient.get('/goals');
    return response.data;
  } catch (error) {
    unwrapError(error);
  }
};

export const getDailyGoal = async () => {
  try {
    const response = await axiosClient.get('/goals/daily');
    return response.status === 204 ? null : response.data;
  } catch (error) {
    unwrapError(error);
  }
};

export const createGoal = async ({ subjectId = null, period, targetMinutes }) => {
  try {
    const response = await axiosClient.post('/goals', { subjectId, period, targetMinutes });
    return response.data;
  } catch (error) {
    unwrapError(error);
  }
};

export const updateGoal = async (id, targetMinutes) => {
  try {
    const response = await axiosClient.put(`/goals/${id}`, { targetMinutes });
    return response.data;
  } catch (error) {
    unwrapError(error);
  }
};

export const deleteGoal = async (id) => {
  try {
    await axiosClient.delete(`/goals/${id}`);
  } catch (error) {
    unwrapError(error);
  }
};

// ---- Study Tasks ----

export const getTasks = async (params = {}) => {
  try {
    const response = await axiosClient.get('/tasks', { params });
    return response.data;
  } catch (error) {
    unwrapError(error);
  }
};

export const getTaskSummary = async () => {
  try {
    const response = await axiosClient.get('/tasks/summary');
    return response.data;
  } catch (error) {
    unwrapError(error);
  }
};

export const createTask = async (task) => {
  try {
    const response = await axiosClient.post('/tasks', task);
    return response.data;
  } catch (error) {
    unwrapError(error);
  }
};

export const updateTask = async (id, task) => {
  try {
    const response = await axiosClient.put(`/tasks/${id}`, task);
    return response.data;
  } catch (error) {
    unwrapError(error);
  }
};

export const changeTaskStatus = async (id, status) => {
  try {
    const response = await axiosClient.patch(`/tasks/${id}/status`, { status });
    return response.data;
  } catch (error) {
    unwrapError(error);
  }
};

export const deleteTask = async (id) => {
  try {
    await axiosClient.delete(`/tasks/${id}`);
  } catch (error) {
    unwrapError(error);
  }
};

// ---- Profile & Achievements ----

export const getProfileSummary = async () => {
  try {
    const response = await axiosClient.get('/profile/summary');
    return response.data;
  } catch (error) {
    unwrapError(error);
  }
};

export const getAchievements = async () => {
  try {
    const response = await axiosClient.get('/profile/achievements');
    return response.data;
  } catch (error) {
    unwrapError(error);
  }
};

// ---- Notifications ----

export const getNotifications = async (page = 0, size = 15) => {
  try {
    const response = await axiosClient.get('/notifications', { params: { page, size } });
    return response.data;
  } catch (error) {
    unwrapError(error);
  }
};

export const getUnreadNotificationCount = async () => {
  try {
    const response = await axiosClient.get('/notifications/unread-count');
    return response.data?.count ?? 0;
  } catch (error) {
    unwrapError(error);
  }
};

export const markNotificationRead = async (id) => {
  try {
    await axiosClient.patch(`/notifications/${id}/read`);
  } catch (error) {
    unwrapError(error);
  }
};

export const markAllNotificationsRead = async () => {
  try {
    const response = await axiosClient.patch('/notifications/read-all');
    return response.data;
  } catch (error) {
    unwrapError(error);
  }
};

export const deleteNotification = async (id) => {
  try {
    await axiosClient.delete(`/notifications/${id}`);
  } catch (error) {
    unwrapError(error);
  }
};

// ---- Group Join Requests & Challenges ----

export const getGroupJoinRequests = async (groupId) => {
  try {
    const response = await axiosClient.get(`/group/${groupId}/join-requests`);
    return response.data;
  } catch (error) {
    unwrapError(error);
  }
};

export const getMyPendingJoinRequests = async () => {
  try {
    const response = await axiosClient.get('/group/join-requests/mine');
    return response.data;
  } catch (error) {
    unwrapError(error);
  }
};

export const approveJoinRequest = async (groupId, requestId) => {
  try {
    await axiosClient.post(`/group/${groupId}/join-requests/${requestId}/approve`);
  } catch (error) {
    unwrapError(error);
  }
};

export const rejectJoinRequest = async (groupId, requestId) => {
  try {
    await axiosClient.post(`/group/${groupId}/join-requests/${requestId}/reject`);
  } catch (error) {
    unwrapError(error);
  }
};

export const cancelMyJoinRequest = async (groupId) => {
  try {
    await axiosClient.delete(`/group/${groupId}/join-requests/mine`);
  } catch (error) {
    unwrapError(error);
  }
};

export const getGroupChallenges = async (groupId) => {
  try {
    const response = await axiosClient.get(`/group/${groupId}/challenges`);
    return response.data;
  } catch (error) {
    unwrapError(error);
  }
};

export const getChallengeDetail = async (groupId, challengeId) => {
  try {
    const response = await axiosClient.get(`/group/${groupId}/challenges/${challengeId}`);
    return response.data;
  } catch (error) {
    unwrapError(error);
  }
};

export const createGroupChallenge = async (groupId, payload) => {
  try {
    const response = await axiosClient.post(`/group/${groupId}/challenges`, payload);
    return response.data;
  } catch (error) {
    unwrapError(error);
  }
};

export const deleteGroupChallenge = async (groupId, challengeId) => {
  try {
    await axiosClient.delete(`/group/${groupId}/challenges/${challengeId}`);
  } catch (error) {
    unwrapError(error);
  }
};

