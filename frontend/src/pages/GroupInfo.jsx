import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import './GroupInfo.css';
import {
  deleteGroup,
  removeMember,
  searchUsers,
  addMemberToGroup,
  getGroupDetails,
  getChatMessages,
  sendChatMessage,
  getGroupRanking,
  getGroupJoinRequests,
  approveJoinRequest,
  rejectJoinRequest,
  getGroupChallenges,
  getChallengeDetail,
  createGroupChallenge,
  deleteGroupChallenge,
} from '../lib/api';
import { useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { useTranslation } from 'react-i18next';


const GroupInfo = () => {
  const { id } = useParams(); // group id from url 
  const navigate = useNavigate();
  const { user: currentUser } = useAuth(); // get user from userAuth and assign it to currentUser variable  
  const [message, setMessage] = useState(''); // message state for the chat box 
  const [showSearch, setShowSearch] = useState(false); // state to control the user search overlay visibility 
  const [searchQuery, setSearchQuery] = useState(''); // state for the search query 
  const [searchResults, setSearchResults] = useState([]); // state for the search results 
  const [isSearching, setIsSearching] = useState(false); // state to control the search loader visibility 
  
  const [group, setGroup] = useState(null); // state for the group data 
  const [members, setMembers] = useState([]); // state for group members
  const [isLoading, setIsLoading] = useState(true); // state for loading 

  const [ranking, setRanking] = useState([]); // state for group rankings
  const [chatMessages, setChatMessages] = useState([]); // state for chat messages
  const [ws, setWs] = useState(null); // websocket state for real-time updates
  const [joinRequests, setJoinRequests] = useState([]);
  const [challenges, setChallenges] = useState([]);
  const [selectedChallenge, setSelectedChallenge] = useState(null);
  const [showChallengeModal, setShowChallengeModal] = useState(false);
  const [challengeForm, setChallengeForm] = useState({
    title: '',
    description: '',
    targetHours: '10',
    startDate: new Date().toISOString().slice(0, 10),
    endDate: new Date(Date.now() + 6 * 86400000).toISOString().slice(0, 10),
  });
  
  const [secondsTick, setSecondsTick] = useState(0); // used to force re-renders every second so active timers count up

  const { t } = useTranslation();

  // Tick the timer every second to refresh active timers 
  useEffect(() => {
    const timer = setInterval(() => {
      setSecondsTick(prev => prev + 1);
    }, 1000);
    return () => clearInterval(timer);
  }, []);

  // secondsTick is used to force re-renders every second so active timers count up
  const formatTotalActiveDuration = (baseDuration, startTimeStr, isStudying, _tick) => {
    let totalSeconds = baseDuration || 0;

    if (isStudying && startTimeStr) {
      try {
        let start;
        if (Array.isArray(startTimeStr)) {
          start = new Date(Date.UTC(startTimeStr[0], startTimeStr[1] - 1, startTimeStr[2], startTimeStr[3] || 0, startTimeStr[4] || 0, startTimeStr[5] || 0));
        } else {
          let isoStr = startTimeStr;
          if (!isoStr.endsWith('Z') && !/[+-]\d{2}:\d{2}$/.test(isoStr)) {
            isoStr = isoStr + 'Z';
          }
          start = new Date(isoStr);
        }
        const diffMs = Math.max(0, Date.now() - start.getTime());
        totalSeconds += Math.floor(diffMs / 1000);
      } catch (e) {
        // ignore
      }
    }

    const hours = Math.floor(totalSeconds / 3600);
    const mins = Math.floor((totalSeconds % 3600) / 60);
    const secs = totalSeconds % 60;
    return `${hours.toString().padStart(2, '0')}:${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  };

  const fetchGroupInfo = async () => {
    try {
      const data = await getGroupDetails(id);
      setGroup(data);
      setMembers(data.members);
    } catch (error) {
      console.error('Error fetching group details:', error);
      alert(t('groupInfo.failedLoad'));
    } finally {
      setIsLoading(false);
    }
  };

  const fetchMessages = async () => {
    try {
      const msgs = await getChatMessages(id);
      setChatMessages(msgs);
    } catch (error) {
      console.error('Error fetching messages:', error);
    }
  };

  const fetchRanking = async () => {
    try {
      const data = await getGroupRanking(id);
      setRanking(data);
    } catch (error) {
      console.error('Error fetching rankings:', error);
    }
  };

  const fetchChallenges = async () => {
    try {
      const list = await getGroupChallenges(id);
      setChallenges(list || []);
    } catch {
      // non-member or offline
    }
  };

  const fetchJoinRequests = async () => {
    try {
      const reqs = await getGroupJoinRequests(id);
      setJoinRequests(reqs || []);
    } catch {
      setJoinRequests([]);
    }
  };

  useEffect(() => {
    fetchGroupInfo();
    fetchMessages();
    fetchRanking();
    fetchChallenges();
  }, [id]);

  useEffect(() => {
    if (!currentUser || !id) return;
    
    // Connect to WebSocket dynamically based on API base URL
    // same fallback as lib/api.js (the old one pointed at the retired EC2 host)
    const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';
    const wsProtocol = apiBaseUrl.startsWith('https') ? 'wss:' : 'ws:';
    let host;
    try {
      host = new URL(apiBaseUrl).host;
    } catch (e) {
      host = apiBaseUrl.replace(/^(https?:\/\/)?/, '').split('/')[0];
    }
    const wsUrl = `${wsProtocol}//${host}/ws?groupId=${id}`;

    // The backend runs as several instances; when the one holding this socket restarts
    // (deploy, crash), reconnect and nginx routes the new socket to a healthy instance.
    let websocket;
    let attempt = 0;
    let retryTimer;
    let closedByUs = false;

    const connect = () => {
      websocket = new WebSocket(wsUrl);
      setWs(websocket);

      websocket.onopen = () => {
        if (attempt > 0) {
          // events sent while we were disconnected are not replayed: reload them
          fetchMessages();
          fetchRanking();
        }
        attempt = 0;
      };

      websocket.onmessage = (event) => {
        try {
          const messageData = JSON.parse(event.data);
          if (messageData.type === 'chat') {
            setChatMessages((prev) => [...prev, messageData.data]);
          } else if (messageData.type === 'ranking') {
            setRanking(messageData.data);
          }
        } catch (error) {
          console.error('Failed to parse websocket message:', error);
        }
      };

      websocket.onclose = (event) => {
        // 1007 bad groupId, 1008 not a member: retrying cannot help
        if (closedByUs || event.code === 1007 || event.code === 1008) return;
        // exponential backoff (1s, 2s, 4s ... 30s) with jitter, so clients don't reconnect in lockstep
        const delay = Math.min(30000, 1000 * 2 ** attempt) + Math.random() * 500;
        attempt += 1;
        console.log(`WebSocket closed (${event.code}), reconnecting in ${Math.round(delay)} ms`);
        retryTimer = setTimeout(connect, delay);
      };
    };

    connect();

    return () => {
      closedByUs = true;
      clearTimeout(retryTimer);
      websocket.close();
    };
  }, [id, currentUser]);

  const isOwner = group?.ownerName === currentUser?.username;

  useEffect(() => {
    if (isOwner && id) {
      fetchJoinRequests();
    }
  }, [isOwner, id]);

  const handleApproveRequest = async (reqId) => {
    try {
      await approveJoinRequest(id, reqId);
      await Promise.all([fetchJoinRequests(), fetchGroupInfo(), fetchRanking()]);
    } catch (err) {
      alert(err.message || 'Failed to approve');
    }
  };

  const handleRejectRequest = async (reqId) => {
    try {
      await rejectJoinRequest(id, reqId);
      await fetchJoinRequests();
    } catch (err) {
      alert(err.message || 'Failed to reject');
    }
  };

  const handleCreateChallenge = async (e) => {
    e.preventDefault();
    try {
      await createGroupChallenge(id, {
        title: challengeForm.title.trim(),
        description: challengeForm.description.trim() || null,
        targetHours: Number(challengeForm.targetHours),
        startDate: challengeForm.startDate,
        endDate: challengeForm.endDate,
      });
      setShowChallengeModal(false);
      setChallengeForm((prev) => ({ ...prev, title: '', description: '' }));
      await fetchChallenges();
    } catch (err) {
      alert(err.message || 'Failed to create challenge');
    }
  };

  const handleViewChallenge = async (challengeId) => {
    if (selectedChallenge?.challenge?.id === challengeId) {
      setSelectedChallenge(null);
      return;
    }
    try {
      const detail = await getChallengeDetail(id, challengeId);
      setSelectedChallenge(detail);
    } catch (err) {
      console.error(err);
    }
  };

  const handleDeleteChallenge = async (challengeId) => {
    try {
      await deleteGroupChallenge(id, challengeId);
      if (selectedChallenge?.challenge?.id === challengeId) {
        setSelectedChallenge(null);
      }
      await fetchChallenges();
    } catch (err) {
      console.error(err);
    }
  };

  useEffect(() => {
    const delayDebounceFn = setTimeout(async () => {
      if (searchQuery.length >= 2) {
        setIsSearching(true);
        try {
          const results = await searchUsers(searchQuery);
          setSearchResults(results);
        } catch (error) {
          console.error(error);
        } finally {
          setIsSearching(false);
        }
      } else {
        setSearchResults([]);
      }
    }, 1000); // only call api after user stop typing for 1000ms 

    return () => clearTimeout(delayDebounceFn);
  }, [searchQuery]);

  const handleAddMember = async (userId) => {
    try {
      await addMemberToGroup(group.id, userId);
      alert(t('groupInfo.memberAddedSuccess'));
      handleCloseSearch();
      fetchGroupInfo(); // Refresh member list
    } catch (error) {
      console.error(error.message);
    }
  }; 

  const handleRemoveMember = async (userId) => {
    if (!window.confirm(t('groupInfo.removeMemberConfirm'))) return;
    try {
      await removeMember(group.id, userId);
      alert(t('groupInfo.memberRemovedSuccess'));
      fetchGroupInfo(); // Refresh member list
    } catch (error) {
      console.error(error.message);
    }
  };

  const handleLeaveGroup = async () => {
    if (!window.confirm(t('groupInfo.leaveConfirm'))) return;
    try {
      await removeMember(group.id, currentUser.id);
      navigate('/groups');
    } catch (error) {
      console.error(error.message);
    }
  };

  const handleDeleteGroup = async () => {
    if (!window.confirm(t('groupInfo.deleteConfirm'))) return;
    try {
      await deleteGroup(group.id);
      navigate('/groups');
    } catch (error) {
      console.error(error.message);
    }
  };

  const handleSendMessage = async (e) => {
    e?.preventDefault();
    if (!message.trim()) return;
    
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify({ content: message }));
      setMessage('');
    } else {
      try {
        await sendChatMessage(group.id, message);
        setMessage('');
        fetchMessages();
      } catch (error) {
        console.error(error.message);
      }
    }
  };

  const handleCloseSearch = () => {
    setShowSearch(false);
    setSearchQuery('');
    setSearchResults([]);
  };

  useEffect(() => {
    const handleEsc = (event) => {
      if (event.key === 'Escape') {
        handleCloseSearch();
      }
    };
    window.addEventListener('keydown', handleEsc);
    return () => window.removeEventListener('keydown', handleEsc);
  }, []);

  if (isLoading) {
    return <div className="loading-state">{t('groupInfo.loading')}</div>;
  }

  if (!group) {
    return <div className="error-state">{t('groupInfo.notFound')}</div>;
  }

  return (
    <div className="group-info-page">
      {/* Search Overlay */}
      {showSearch && (
        <div className="search-overlay" onClick={handleCloseSearch}>
          <div className="search-box-container info-card" onClick={(e) => e.stopPropagation()}>
            <div className="search-header">
              <h3 className="h3 text-primary">{t('groupInfo.addNewMember')}</h3>
              <button className="close-search-btn" onClick={handleCloseSearch}>
                <span className="material-symbols-outlined">close</span>
              </button>
            </div>
            
            <div className="search-input-group">
  
              <input
                type="text"
                placeholder={t('groupInfo.searchPlaceholder')}
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                autoFocus
                className="user-search-input"
              />
              {isSearching && <div className="search-loader"></div>}
            </div>

            <div className="search-results-list">
              {searchResults.length > 0 ? (
                searchResults.map(user => (
                  <div className="search-result-item" key={user.id}>
                    <img src={user.avatar || "https://ui-avatars.com/api/?name=" + user.username} alt={user.username} className="result-avatar" />
                    <div className="result-info">
                      <p className="label-sm text-primary">{user.username}</p>
                      <p className="text-xs text-on-surface-variant">{user.email}</p>
                    </div>
                    <button className="btn-primary-sm" onClick={() => handleAddMember(user.id)}>
                      {t('groupInfo.invite')}
                    </button>
                  </div>
                ))
              ) : searchQuery.length >= 2 && !isSearching ? (
                <p className="no-results body-md">{t('groupInfo.noUsersFound', { q: searchQuery })}</p>
              ) : (
                <p className="search-hint body-md">{t('groupInfo.type2chars')}</p>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Header */}
      <div className="group-info-header">
        <button className="back-btn" onClick={() => navigate('/groups')}>
          <span className="material-symbols-outlined">arrow_back</span>
        </button>
        <div className="header-content">
          <h1 className="h2 text-primary">{group.name}</h1>
          <p className="body-md text-on-surface-variant">{t('groupInfo.ownedBy', { owner: group.ownerName, count: members.length, max: group.maxMembers })}</p>
        </div>
        <div className="header-actions">
          {isOwner ? (
            <>
              <button 
                className={`btn-secondary add-member-btn ${showSearch ? 'active' : ''}`}
                onClick={() => setShowSearch(!showSearch)}
              >
                <span className="material-symbols-outlined">person_add</span>
                {showSearch ? t('auth.cancel') : t('groupInfo.addMember')}
              </button>
                <button className="btn-danger delete-group-btn" onClick={handleDeleteGroup}>
                <span className="material-symbols-outlined">delete_forever</span>
                {t('groupInfo.deleteGroup')}
              </button>
            </>
          ) : (
            <button className="btn-danger leave-group-btn" onClick={handleLeaveGroup}>
              <span className="material-symbols-outlined">logout</span>
              {t('groupInfo.leaveGroup')}
            </button>
          )}
        </div>
      </div>

      <div className="group-info-grid">
        {/* Left Column: Member List */}
        <div className="info-card member-list-card">
          <h3 className="h3 text-primary card-title">{t('groupInfo.members')}</h3>
          <div className="members-scroll">
            {members.map(member => {
              const rankingMember = ranking.find(r => r.userId === member.id);
              const isStudying = rankingMember ? rankingMember.isStudying : member.isStudying;
              const activeSessionStartTime = rankingMember ? rankingMember.activeSessionStartTime : member.activeSessionStartTime;

              return (
                <div className={`member-item ${isStudying ? 'studying-active' : ''}`} key={member.id}>
                  <div className="avatar-wrap">
                    <img src={member.avatar || `https://ui-avatars.com/api/?name=${member.username}`} alt={member.username} className="member-avatar" />
                    {isStudying && <span className="status-dot pulsing-green"></span>}
                  </div>
                  <div className="member-info">
                    <div className="member-name-row">
                      <p className="label-sm text-primary">{member.username}</p>
                      {isStudying && <span className="studying-tag">Studying</span>}
                    </div>
                    <p className="text-xs text-on-surface-variant" style={{ display: 'flex', gap: '8px' }}>
                      {isStudying ? (
                        <span className="member-active-timer">Focusing: {formatTotalActiveDuration(rankingMember?.baseDuration, activeSessionStartTime, isStudying, secondsTick)}</span>
                      ) : (
                        <>
                          <span>{member.role}</span>
                          <span className="member-active-timer">• Time: {formatTotalActiveDuration(rankingMember?.baseDuration, null, false, secondsTick)}</span>
                        </>
                      )}
                    </p>
                  </div>
                  {isOwner && member.username !== currentUser.username && (
                    <button className="remove-member-btn" title="Remove Member" onClick={() => handleRemoveMember(member.id)}>
                      <span className="material-symbols-outlined">person_remove</span>
                    </button>
                  )}
                </div>
              );
            })}
          </div>
        </div>

        {/* Middle Column: Ranking */}
        <div className="info-card ranking-card">
          <h3 className="h3 text-primary card-title">{t('groupInfo.leaderboard')}</h3>
          <div className="ranking-list">
            {ranking.map((rank, index) => (
              <div className={`rank-item ${index === 0 ? 'top-rank' : ''} ${rank.isStudying ? 'studying-active' : ''}`} key={rank.id || rank.userId}>
                <span className="rank-number">{rank.rank}</span>
                <div className="avatar-wrap">
                  <img src={`https://ui-avatars.com/api/?name=${rank.username}`} alt={rank.username} className="rank-avatar" />
                  {rank.isStudying && <span className="status-dot pulsing-green"></span>}
                </div>
                <div className="rank-details">
                  <div className="rank-user-info">
                    <p className="label-sm text-primary">{rank.username}</p>
                    {rank.isStudying && <span className="studying-tag">Studying</span>}
                  </div>
                  <div className="progress-bar-wrap">
                    <div className="progress-bar-fill" style={{ width: `${Math.max(10, 100 - index * 15)}%` }}></div>
                  </div>
                </div>
                <div className="rank-time-box">
                  <span className="focus-time">{(rank.totalDuration / 3600).toFixed(1)}h</span>
                  <span className="active-timer">{formatTotalActiveDuration(rank.baseDuration, rank.activeSessionStartTime, rank.isStudying, secondsTick)}</span>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Right Column: Chat Box */}
        <div className="info-card chat-card">
          <h3 className="h3 text-primary card-title">{t('groupInfo.groupChat')}</h3>
          <div className="chat-messages">
            {chatMessages.map(msg => {
              const isMe = msg.senderId === currentUser.id;
              let date = new Date();
              if (msg.timestamp) {
                if (Array.isArray(msg.timestamp)) {
                  date = new Date(Date.UTC(msg.timestamp[0], msg.timestamp[1] - 1, msg.timestamp[2], msg.timestamp[3] || 0, msg.timestamp[4] || 0, msg.timestamp[5] || 0));
                } else {
                  let ts = msg.timestamp;
                  if (typeof ts === 'string' && !ts.endsWith('Z') && !/[+-]\d{2}:\d{2}$/.test(ts)) {
                    ts += 'Z';
                  }
                  date = new Date(ts);
                }
              }
              if (isNaN(date.getTime())) date = new Date();
              const timeString = date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
              return (
                <div className={`chat-bubble-wrap ${isMe ? 'me' : ''}`} key={msg.id}>
                  {!isMe && <p className="chat-user">{msg.senderName}</p>}
                  <div className="chat-bubble">
                    <p className="body-md">{msg.content}</p>
                    <span className="chat-time">{timeString}</span>
                  </div>
                </div>
              );
            })}
          </div>
            <form className="chat-input-wrap" onSubmit={handleSendMessage} style={{width: '100%', display: 'flex', gap: '8px'}}>
              <input
                type="text"
                placeholder={t('groupInfo.chatPlaceholder')}
                value={message}
                onChange={(e) => setMessage(e.target.value)}
                className="chat-input"
              />
              <button type="submit" className="send-btn">
                <span className="material-symbols-outlined">send</span>
              </button>
            </form>
        </div>
      </div>

      {/* Pending Join Requests (Owner of Private Group) */}
      {isOwner && joinRequests.length > 0 && (
        <div className="info-card" style={{ marginTop: 20 }}>
          <h3 className="h3 text-primary card-title">{t('groupInfo.joinRequests')} ({joinRequests.length})</h3>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            {joinRequests.map((req) => (
              <div
                key={req.id}
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  padding: '10px 14px',
                  borderRadius: 12,
                  background: 'var(--color-surface-container-low)',
                }}
              >
                <div>
                  <p className="label-sm text-primary">{req.username}</p>
                  {req.message && <p className="body-md text-on-surface-variant" style={{ fontSize: 13 }}>{req.message}</p>}
                </div>
                <div style={{ display: 'flex', gap: 8 }}>
                  <button className="btn-primary-sm" onClick={() => handleApproveRequest(req.id)}>
                    {t('groupInfo.approve')}
                  </button>
                  <button className="btn-secondary" style={{ padding: '6px 12px' }} onClick={() => handleRejectRequest(req.id)}>
                    {t('groupInfo.reject')}
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Group Challenges Section */}
      <div className="info-card" style={{ marginTop: 20 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 14, flexWrap: 'wrap', gap: 10 }}>
          <h3 className="h3 text-primary card-title" style={{ margin: 0 }}>{t('groupInfo.challenges')}</h3>
          {isOwner && (
            <button className="btn-primary-sm" onClick={() => setShowChallengeModal(!showChallengeModal)}>
              {showChallengeModal ? t('auth.cancel') : t('groupInfo.newChallenge')}
            </button>
          )}
        </div>

        {showChallengeModal && (
          <form
            onSubmit={handleCreateChallenge}
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))',
              gap: 10,
              padding: 14,
              marginBottom: 16,
              borderRadius: 12,
              background: 'var(--color-surface-container-low)',
            }}
          >
            <input
              type="text"
              className="user-search-input"
              placeholder={t('groupInfo.challengeTitlePlaceholder')}
              value={challengeForm.title}
              onChange={(e) => setChallengeForm({ ...challengeForm, title: e.target.value })}
              required
            />
            <input
              type="number"
              step="0.5"
              min="0.5"
              max="1000"
              className="user-search-input"
              placeholder={t('groupInfo.targetHours')}
              value={challengeForm.targetHours}
              onChange={(e) => setChallengeForm({ ...challengeForm, targetHours: e.target.value })}
              required
            />
            <input
              type="date"
              className="user-search-input"
              value={challengeForm.startDate}
              onChange={(e) => setChallengeForm({ ...challengeForm, startDate: e.target.value })}
              required
            />
            <input
              type="date"
              className="user-search-input"
              value={challengeForm.endDate}
              onChange={(e) => setChallengeForm({ ...challengeForm, endDate: e.target.value })}
              required
            />
            <button type="submit" className="btn-primary-sm">
              {t('groupInfo.createChallenge')}
            </button>
          </form>
        )}

        {challenges.length === 0 ? (
          <p className="body-md text-on-surface-variant">{t('groupInfo.noChallenges')}</p>
        ) : (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: 14 }}>
            {challenges.map((ch) => (
              <div
                key={ch.id}
                style={{
                  padding: 16,
                  borderRadius: 14,
                  background: 'var(--color-surface-container-low)',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: 8,
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <strong className="text-primary">{ch.title}</strong>
                  <span className="studying-tag">{ch.status}</span>
                </div>
                <span style={{ fontSize: 12, color: 'var(--color-outline)' }}>
                  {ch.startDate} → {ch.endDate} • {(ch.targetSeconds / 3600).toFixed(1)}h
                </span>
                <div className="progress-bar-wrap">
                  <div className="progress-bar-fill" style={{ width: `${ch.myPercent}%` }}></div>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: 12 }}>
                  <span>{(ch.myProgressSeconds / 3600).toFixed(1)}h ({ch.myPercent}%)</span>
                  <div style={{ display: 'flex', gap: 8 }}>
                    <button
                      type="button"
                      className="btn-primary-sm"
                      onClick={() => handleViewChallenge(ch.id)}
                    >
                      {t('groupInfo.viewLeaderboard')}
                    </button>
                    {isOwner && (
                      <button
                        type="button"
                        className="remove-member-btn"
                        onClick={() => handleDeleteChallenge(ch.id)}
                      >
                        <span className="material-symbols-outlined">delete</span>
                      </button>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}

        {selectedChallenge && (
          <div style={{ marginTop: 16, padding: 14, borderRadius: 12, background: 'var(--color-surface-container)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
              <strong>{selectedChallenge.challenge?.title}</strong>
              <span style={{ fontSize: 12 }}>
                {t('groupInfo.completedCount', { count: selectedChallenge.completedCount })}
              </span>
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
              {(selectedChallenge.participants || []).map((p) => (
                <div key={p.userId} style={{ display: 'flex', justifyContent: 'space-between', fontSize: 13 }}>
                  <span>#{p.rank} {p.username}</span>
                  <span>{(p.progressSeconds / 3600).toFixed(1)}h ({p.percent}%)</span>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default GroupInfo;
