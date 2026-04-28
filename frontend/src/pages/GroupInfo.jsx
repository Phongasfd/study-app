import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import './GroupInfo.css';
import { deleteGroup, removeMember, searchUsers, addMemberToGroup, getGroupDetails } from '../lib/api';
import { useEffect } from 'react';
import { useAuth } from '../context/AuthContext';


const GroupInfo = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user: currentUser } = useAuth();
  const [message, setMessage] = useState('');
  const [showSearch, setShowSearch] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState([]);
  const [isSearching, setIsSearching] = useState(false);
  
  const [group, setGroup] = useState(null);
  const [members, setMembers] = useState([]);
  const [isLoading, setIsLoading] = useState(true);

  // Mock Data for ranking and chat (as requested to not touch ranking/stats)
  const ranking = [
    { id: 1, name: "Alex Rivers", focusTime: "41.2h", rank: 1, avatar: "https://lh3.googleusercontent.com/aida-public/AB6AXuA6JQrxIjN4tJ-h-d4nhh0a-YI9aWpJOJkVLvWzJ8uIMvEdABgWgqz26HHayJVxqDjn-LzPV5sBRLq3G5OWeq9XXuUGKQ4pnlFL9H3mNBkKoTTTUCsro2hsX7ByTFJrjBB9LE6C6Vf61V_grvCIpX6EcKf-oD-wtssT7VZVq_PvmCphgTXBOC9qSpSqwHfpcJyNlYDvS5CJHhW4wX5VCYMdR2xTs1Uf1I0BKPM14ujdFwov2dZbKiOHyV035xda72tZjjmacgB7ZNBS" },
    { id: 2, name: "Sarah L.", focusTime: "32.5h", rank: 2, avatar: "https://lh3.googleusercontent.com/aida-public/AB6AXuBPOvCOETwtzsvhEEEWYWNxFbzCcTEeNEzZrhsTS2OI5fFV1mGGode6b0wLtby_HPjhgrkNSncf0X6qCbq_hqQiyjCvIPf9_kS09n4c6d2MXl4QSklFFGogpJNpBJCywseakPIBJmGmBMjYjCtyPEWwoXyCaV4TlIjU_Lxv8ggcSgNydj5h5QE3ZQjGdltiQ8ApDytiMAEb9CiUqxM-RDlm1sX0yc9DJcYPaXjrfgNeljYgfueI5NtJPsRrcmZvFyULmQf5x_CV6byP" },
    { id: 3, name: "Maya J.", focusTime: "30.8h", rank: 3, avatar: "https://lh3.googleusercontent.com/aida-public/AB6AXuAN3xJ4y9Uld08a_ONU7xrLTqrL6nnEnwJH_LEgn_Hrkp_MMS_K89BeUAK30-YB4TncmCPiedqqS_fXyO0bxsVeMhUHOQ11OkB-TDTQSC6h2BClAtx1cSiF2GauKR1bmtu0FD9g9dgrskKI4iljTLDpmab4ZWLS68rJbuz0mNJP_earR1K1R5BkbAVqtbRfOJkNBTMArt5czGtiSvqkBs1YrmJct6EEjQXjgOqAuEarD_YzPgbhv8B-NnYpDr-DEv7fhkBlh1s_GzXw" },
  ];

  const chatMessages = [
    { id: 1, user: "Alex Rivers", text: "Welcome everyone! Let's hit our goals this week.", time: "10:30 AM", isMe: false },
    { id: 2, user: "You", text: "Thanks Alex! Ready to focus.", time: "10:32 AM", isMe: true },
    { id: 3, user: "Sarah L.", text: "Anyone studying for the midterm tonight?", time: "11:15 AM", isMe: false },
  ];

  const fetchGroupInfo = async () => {
    try {
      const data = await getGroupDetails(id);
      setGroup(data);
      setMembers(data.members);
    } catch (error) {
      console.error('Error fetching group details:', error);
      alert('Failed to load group details');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchGroupInfo();
  }, [id]);

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
    }, 500);

    return () => clearTimeout(delayDebounceFn);
  }, [searchQuery]);

  const handleAddMember = async (userId) => {
    try {
      await addMemberToGroup(group.id, userId);
      alert('Member added successfully');
      handleCloseSearch();
      fetchGroupInfo(); // Refresh member list
    } catch (error) {
      alert(error.message);
    }
  };

  const handleRemoveMember = async (userId) => {
    if (!window.confirm('Are you sure you want to remove this member?')) return;
    try {
      await removeMember(group.id, userId);
      alert('Member removed successfully');
      fetchGroupInfo(); // Refresh member list
    } catch (error) {
      alert(error.message);
    }
  };

  const handleLeaveGroup = async () => {
    if (!window.confirm('Are you sure you want to leave this group?')) return;
    try {
      await removeMember(group.id, currentUser.id);
      navigate('/groups');
    } catch (error) {
      alert(error.message);
    }
  };

  const handleDeleteGroup = async () => {
    if (!window.confirm('Are you sure you want to delete this group? This action cannot be undone.')) return;
    try {
      await deleteGroup(group.id);
      navigate('/groups');
    } catch (error) {
      alert(error.message);
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
    return <div className="loading-state">Loading group details...</div>;
  }

  if (!group) {
    return <div className="error-state">Group not found</div>;
  }

  return (
    <div className="group-info-page">
      {/* Search Overlay */}
      {showSearch && (
        <div className="search-overlay" onClick={handleCloseSearch}>
          <div className="search-box-container info-card" onClick={(e) => e.stopPropagation()}>
            <div className="search-header">
              <h3 className="h3 text-primary">Add New Member</h3>
              <button className="close-search-btn" onClick={handleCloseSearch}>
                <span className="material-symbols-outlined">close</span>
              </button>
            </div>
            
            <div className="search-input-group">
  
              <input
                type="text"
                placeholder="Search users by username..."
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
                      Invite
                    </button>
                  </div>
                ))
              ) : searchQuery.length >= 2 && !isSearching ? (
                <p className="no-results body-md">No users found for "{searchQuery}"</p>
              ) : (
                <p className="search-hint body-md">Type at least 2 characters to search...</p>
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
          <p className="body-md text-on-surface-variant">Owned by {group.ownerName} • {members.length}/{group.maxMembers} Members</p>
        </div>
        <div className="header-actions">
          {isOwner ? (
            <>
              <button 
                className={`btn-secondary add-member-btn ${showSearch ? 'active' : ''}`}
                onClick={() => setShowSearch(!showSearch)}
              >
                <span className="material-symbols-outlined">person_add</span>
                {showSearch ? 'Cancel' : 'Add Member'}
              </button>
              <button className="btn-danger delete-group-btn" onClick={handleDeleteGroup}>
                <span className="material-symbols-outlined">delete_forever</span>
                Delete Group
              </button>
            </>
          ) : (
            <button className="btn-danger leave-group-btn" onClick={handleLeaveGroup}>
              <span className="material-symbols-outlined">logout</span>
              Leave Group
            </button>
          )}
        </div>
      </div>

      <div className="group-info-grid">
        {/* Left Column: Member List */}
        <div className="info-card member-list-card">
          <h3 className="h3 text-primary card-title">Members</h3>
          <div className="members-scroll">
            {members.map(member => (
              <div className="member-item" key={member.id}>
                <div className="avatar-wrap">
                  <img src={member.avatar || `https://ui-avatars.com/api/?name=${member.username}`} alt={member.username} className="member-avatar" />
                  <span className={`status-dot ${member.status}`}></span>
                </div>
                <div className="member-info">
                  <p className="label-sm text-primary">{member.username}</p>
                  <p className="text-xs text-on-surface-variant">{member.role}</p>
                </div>
                {isOwner && member.username !== currentUser.username && (
                  <button className="remove-member-btn" title="Remove Member" onClick={() => handleRemoveMember(member.id)}>
                    <span className="material-symbols-outlined">person_remove</span>
                  </button>
                )}
              </div>
            ))}
          </div>
        </div>

        {/* Middle Column: Ranking */}
        <div className="info-card ranking-card">
          <h3 className="h3 text-primary card-title">Leaderboard</h3>
          <div className="ranking-list">
            {ranking.map((rank, index) => (
              <div className={`rank-item ${index === 0 ? 'top-rank' : ''}`} key={rank.id}>
                <span className="rank-number">{rank.rank}</span>
                <img src={rank.avatar} alt={rank.name} className="rank-avatar" />
                <div className="rank-details">
                  <p className="label-sm text-primary">{rank.name}</p>
                  <div className="progress-bar-wrap">
                    <div className="progress-bar-fill" style={{ width: `${100 - index * 15}%` }}></div>
                  </div>
                </div>
                <span className="focus-time">{rank.focusTime}</span>
              </div>
            ))}
          </div>
        </div>

        {/* Right Column: Chat Box */}
        <div className="info-card chat-card">
          <h3 className="h3 text-primary card-title">Group Chat</h3>
          <div className="chat-messages">
            {chatMessages.map(msg => (
              <div className={`chat-bubble-wrap ${msg.isMe ? 'me' : ''}`} key={msg.id}>
                {!msg.isMe && <p className="chat-user">{msg.user}</p>}
                <div className="chat-bubble">
                  <p className="body-md">{msg.text}</p>
                  <span className="chat-time">{msg.time}</span>
                </div>
              </div>
            ))}
          </div>
          <div className="chat-input-wrap">
            <input
              type="text"
              placeholder="Type a message..."
              value={message}
              onChange={(e) => setMessage(e.target.value)}
              className="chat-input"
            />
            <button className="send-btn">
              <span className="material-symbols-outlined">send</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default GroupInfo;
