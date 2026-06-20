import './Groups.css';
import { useTranslation } from 'react-i18next';
import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { createGroup, getGroupsJoined, getRandomGroups, searchGroups, joinGroup, getGroupRanking, createGroupRanking } from '../lib/api';

const Groups = () => {
  const { user } = useAuth();
  const { t } = useTranslation();
  const [showModal, setShowModal] = useState(false); // Modal visibility state
  const [formData, setFormData] = useState({
    name: '',
    maxMembers: ''
  }); // Form data state
  const [isLoading, setIsLoading] = useState(false); // Loading state for form submission 
  const [groupsJoined, setGroupsJoined] = useState([]); // for my group section
  const [trendingGroups, setTrendingGroups] = useState([]); // for trending group section 
  const [groupSearchQuery, setGroupSearchQuery] = useState(''); // for group search query
  const [isSearchingGroups, setIsSearchingGroups] = useState(false); // for search loading
  const navigate = useNavigate();

  const handleEnterGroup = (id) => {
    navigate(`/groups/${id}`);
  };

  const handleJoinGroup = async (id) => {
    try {
      await joinGroup(id);
      try {
        await createGroupRanking(id, user.id);
      } catch (e) {
        console.error("Failed to init group ranking", e);
      }
      navigate(`/groups/${id}`);
    } catch (error) {
      console.error('Error joining group:', error);
      alert(t('groups.joinFailed') + ': ' + (error.message || ''));
    }
  }; // for user want to join group 

  const handleOpenModal = () => {
    if (!user) {
      window.location.href = "/login";
      return;
    }
    setShowModal(true);
  }; // Open create new group modal and check authentication

  const handleCloseModal = () => {
    setShowModal(false);
    setFormData({ name: '', maxMembers: '' });
  }; // Close create new group modal and reset form data

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  }; // handle change in create group form 

  const handleCreateGroup = async (e) => {
    e.preventDefault();
    if (!formData.name.trim()) {
      alert(t('groups.pleaseEnterGroupName'));
      return;
    }

    setIsLoading(true);
    try {
      const newGroup = await createGroup(formData.name, Number(formData.maxMembers));
      try {
        await createGroupRanking(newGroup.id, user.id);
      } catch (e) {
        console.error("Failed to init group ranking", e);
      }
      handleCloseModal();
      // Refresh groups list or navigate
      setGroupsJoined(prev => [...prev, { ...newGroup, userRank: 1 }]);
    } catch (error) {
      console.error('Error creating group:', error);
      alert(t('groups.failedCreate'));
    } finally {
      setIsLoading(false);
    }
  }; // handle create group form submission 

  const fetchTrendingGroups = async () => {
    try {
      const groups = await getRandomGroups();
      setTrendingGroups(groups);
    } catch (error) {
      console.error('Error fetching trending groups:', error);
    }
  }; // handle fetch trending group 

  useEffect(() => {
    const fetchGroups = async () => {
      try {
        const groups = await getGroupsJoined();
        const groupsWithRank = await Promise.all(
          groups.map(async (group) => {
            try {
              const rankings = await getGroupRanking(group.id);
              const myRank = rankings.find(r => r.userId === user.id);
              return { ...group, userRank: myRank ? myRank.rank : '-' };
            } catch (error) {
              console.error(`Error fetching rank for ${group.id}:`, error);
              return { ...group, userRank: '-' };
            }
          })
        );
        setGroupsJoined(groupsWithRank);
      } catch (error) {
        console.error('Error fetching joined groups:', error);
      }
    };

    fetchGroups(); // fetch groups joined
    fetchTrendingGroups(); // fetch groups trending  
  }, []);

  useEffect(() => {
    if (groupSearchQuery.trim() === '') {
      if (groupSearchQuery === '') fetchTrendingGroups();
      return;
    }
    // if query is empty, fetch trendinga groups 

    const delayDebounceFn = setTimeout(async () => {
      setIsSearchingGroups(true);
      try {
        const results = await searchGroups(groupSearchQuery);
        setTrendingGroups(results);
      } catch (error) {
        console.error('Error searching groups:', error);
      } finally {
        setIsSearchingGroups(false);
      }
    }, 1000);  // only call api when user stop typing for 1000ms 

    return () => clearTimeout(delayDebounceFn);
  }, [groupSearchQuery]);

  const groupImages = [
    "https://lh3.googleusercontent.com/aida-public/AB6AXuBquzG03V5HT1Uc3eqQkbeUKsiTtCUWt1cGIQu2Ci3LnFteI9GvrfBsHeNpRZ1OacHjrSHFHqrVGUSOnmc5_bpQglD0dF4t6ve1KsbIMo5r-uh8EwyBXQVYl-vN-QqG2-rQYCC94H-f00aoS-bRGeDe_LXvF-oAHRuU-XDu-xn7sEu37AA5oHT97O--WSNicbZF_IpO-5KVIYU6n3_Nt75H4ISOKauQ_SbOnmPFNBTdxOd9mgFL9WxEDlVgi-1DLSMdwI4x3o6vEYJY",
    "https://lh3.googleusercontent.com/aida-public/AB6AXuASucBs-xUAliFo3UUd-0EjlvfnEZVOzj_p-KhCBvK3DbYrmvDDNNUBbQlWD45ie5UZe0RDUqIzLxLLwP9ArjDc0lkAt3zsaWG67G3AIXNYNEMiau3R9FCG91vuhUEdB7YEJw9etckQSRRQLUp0Ga7eGYRlYqSo46O44lKbDZpnXBsbU63xjnI1erwhYdK3vhhKcDVAtxqbihxKx6JcqAx5SKu7sWv_QiJN-QmDehg1SgCVSohTYkntDxQDoUvON-41Lampu40TqioT",
    "https://lh3.googleusercontent.com/aida-public/AB6AXuBRnF6sRy_st2k9PQv0NDjGR-X0QZP__iYh1DhFU4gPYcxg0FufoW_dLqfIKySsu5-_liib3D02ckUN7_8oEYe_dNku0TkAKkMHPDFeJZbvcnfQ0edWxet1rEgO4bdU67wDQiejRXTWT2Le1sJi4D8NWhE-ByPqz-FZ1NjY2wsqJCuQcqPotAcQhutrN2tD_QMOr74rMSKoVdaB7lpA4YcZJSbtjUGpEVtOC-IrC_B9o2Gbh64OYmEK6KTJ9DsluUq_yTEiOSYbwm9c",
    "https://lh3.googleusercontent.com/aida-public/AB6AXuAy3I48OfwbMDmtr4gGtJPPWTcBn8_j9hu-xzuSauF_GXqdP_wTk0ArE29UwaU_ORcWNLYnPEoMJbD_Uf_4gE1Hsm1hf0tWHG0Yeuytbn--R5ixGDBEqpRxj08-wNkxT13KCYJ22WjZtUHTfJzBBrSEHPG1Eg3IDlU5OcvewphcTbqpV0pt-puSJgW1Pj6ux5SLiOVOh14HWKAHONIdSW4tCy0Nqy4T0C8Q6tFtywfQ8qnDUbSkbeba9oEzEgw5wfg3QwUyKcHgUq7n"
  ];

  return (
    <div className="groups-page">
      {/* My Groups Section */}
      <section className="groups-section">
        <div className="section-header">
          <h2 className="h3 text-on-background">{t('groups.myGroups')}</h2>
          <button className="create-group-btn" onClick={handleOpenModal}>
            <span className="material-symbols-outlined">add</span>
            {t('groups.createNew')}
          </button>
        </div>

        <div className="groups-grid">
          {groupsJoined.map((group) => (
            <div className="group-card" key={group.id}>
              <div className="group-card-header">
                <div className="icon-badge bg-primary-container text-white">
                  <span className="material-symbols-outlined">auto_stories</span>
                </div>
                  <span className="status-badge bg-secondary-container text-on-secondary-container">{t('groups.active')}</span>
              </div>
              <h3 className="body-lg font-h h3-margin">{group.name}</h3>
              <div className="group-stats">
                <div className="stat-item">
                  <span className="material-symbols-outlined icon-sm">group</span>
                  <span className="body-md text-sm">{group.maxMembers} {t('groups.members')}</span>
                </div>
                <div className="stat-item">
                  <span className="material-symbols-outlined icon-sm">leaderboard</span>
                  <span className="body-md text-sm">{t('groups.rank')}: {group.userRank || '-'}</span>
                </div>
              </div>
              <button className="btn-enter" onClick={() => handleEnterGroup(group.id)}>
                {t('groups.enterGroup')}
                <span className="material-symbols-outlined">arrow_forward</span>
              </button>
            </div>
          ))}

        </div>
      </section>

      {/* Trending Groups Section */}
      <section className="groups-section">
        <div className="section-header-wrap">
          <h2 className="h3 text-on-background">{t('groups.trendingGroups')}</h2>
          <div className="filter-controls">
            <div className="search-bar-wrap">
              <span className="material-symbols-outlined search-icon">search</span>
                <input
                type="text"
                className="search-input"
                placeholder={t('groups.searchPlaceholder')}
                value={groupSearchQuery}
                onChange={(e) => setGroupSearchQuery(e.target.value)}
              />
              {isSearchingGroups && <div className="search-loader-sm"></div>}
            </div>
          </div>
        </div>

        <div className="trending-grid">
          {trendingGroups.map((group, index) => (
            <div className="trending-card" key={group.id}>
              <div className="trending-img-wrap">
                <img src={groupImages[index % groupImages.length]} alt="Trending Group" className="trending-img" />
              </div>
              <div className="trending-content">
                <div>
                  <div className="trending-tags">
                    <span className="tag-badge bg-tertiary-fixed text-on-tertiary-fixed">Active</span>
                    <span className="tag-text">• {group.maxMembers} {t('groups.maxMembersText')}</span>
                  </div>
                  <h4 className="body-lg font-h trending-title">{group.name}</h4>
                  <p className="body-md text-sm text-on-surface-variant line-clamp-2">Owned by {group.ownerName}. Join this group to study together!</p>
                </div>
                <div className="trending-footer">
                  <div className="avatar-group">
                    <div className="avatar-sm bg-slate-200"></div>
                    <div className="avatar-sm bg-slate-300"></div>
                    <div className="avatar-sm bg-slate-400"></div>
                  </div>
                  <button className="btn-join" onClick={() => handleJoinGroup(group.id)}>{t('groups.join')}</button>
                </div>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* Create Group Modal */}
      {showModal && (
        <div className="modal-overlay" onClick={handleCloseModal}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2 className="h3 text-on-background">{t('groups.createGroupTitle')}</h2>
              <button
                className="modal-close-btn"
                onClick={handleCloseModal}
                aria-label="Close modal"
              >
                <span className="material-symbols-outlined">close</span>
              </button>
            </div>

            <form onSubmit={handleCreateGroup} className="modal-form">
              <div className="form-group">
                <label htmlFor="name" className="label-sm text-on-surface-variant">
                  {t('groups.groupName')} *
                </label>
                <input
                  type="text"
                  id="name"
                  name="name"
                  value={formData.name}
                  onChange={handleInputChange}
                  placeholder={t('groups.groupNamePlaceholder')}
                  className="form-input"
                  required
                />
              </div>

              <div className="form-group">
                <label htmlFor="subject" className="label-sm text-on-surface-variant">
                  {t('groups.maxMembers')}
                </label>
                <input
                  type="text"
                  id="maxMembers"
                  name="maxMembers"
                  value={formData.maxMembers}
                  onChange={handleInputChange}
                  placeholder={t('groups.maxMembersPlaceholder')}
                  className="form-input"
                />
              </div>

              <div className="modal-actions">
                <button
                  type="button"
                  className="btn-secondary"
                  onClick={handleCloseModal}
                >
                  {t('auth.cancel')}
                </button>
                <button
                  type="submit"
                  className="btn-primary"
                  disabled={isLoading}
                >
                  {isLoading ? t('groups.creating') : t('groups.createGroup')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default Groups;
