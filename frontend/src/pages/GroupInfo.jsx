import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import './GroupInfo.css';
import { deleteGroup, removeMember, searchUsers, addMemberToGroup, getGroupDetails, getChatMessages, sendChatMessage, getGroupRanking } from '../lib/api';
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

  useEffect(() => {
    fetchGroupInfo();
    fetchMessages();
    fetchRanking();
  }, [id]);

  useEffect(() => {
    if (!currentUser || !id) return;
    
    // Connect to WebSocket dynamically based on API base URL
    const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || 'https://13.214.163.45.nip.io/api';
    const wsProtocol = apiBaseUrl.startsWith('https') ? 'wss:' : 'ws:';
    let host;
    try {
      host = new URL(apiBaseUrl).host;
    } catch (e) {
      host = apiBaseUrl.replace(/^(https?:\/\/)?/, '').split('/')[0];
    }
    const wsUrl = `${wsProtocol}//${host}/ws?groupId=${id}&userId=${currentUser.id}`;
    const websocket = new WebSocket(wsUrl);
    setWs(websocket);

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

    websocket.onclose = () => {
      console.log('WebSocket connection closed');
    };

    return () => {
      websocket.close();
    };
  }, [id, currentUser]);

  const isOwner = group?.ownerName === currentUser?.username;

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
    </div>
  );
};

export default GroupInfo;
