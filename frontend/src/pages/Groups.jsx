import './Groups.css';
import { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { createGroup, getGroupsJoined } from '../lib/api';

const Groups = () => {
  const { user } = useAuth();
  const [showModal, setShowModal] = useState(false); // Modal visibility state
  const [formData, setFormData] = useState({ 
    name: '',
    maxMembers: ''
  }); // Form data state
  const [isLoading, setIsLoading] = useState(false); // Loading state for form submission 

  const handleOpenModal = () => {
    if (!user) {
      window.location.href = "/login";
      return;
    }
    setShowModal(true);
  }; // Open modal and check authentication

  const handleCloseModal = () => {
    setShowModal(false);
    setFormData({ name: '', maxMembers: '' });
  }; // Close modal and reset form data

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  }; // 

  const handleCreateGroup = async (e) => {
    e.preventDefault();
    if (!formData.name.trim()) {
      alert('Please enter a group name');
      return;
    }

    setIsLoading(true);
    try {
      await createGroup(formData.name, formData.maxMembers);
      handleCloseModal();
      // Refresh groups list or navigate
      window.location.reload();
    } catch (error) {
      console.error('Error creating group:', error);
      alert('Failed to create group. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  const [groupsJoined, setGroupsJoined] = useState([]);

  useEffect(() => {
      // Fetch groups joined by the user on component mount
      const fetchGroups = async () => {
        try {
          const groups = await getGroupsJoined();
          setGroupsJoined(groups);
        } catch (error) {
          console.error('Error fetching groups:', error);
        }
      };

      fetchGroups();
  }, []);

  return (
    <div className="groups-page">
      {/* My Groups Section */}
      <section className="groups-section">
        <div className="section-header">
          <h2 className="h3 text-on-background">My Groups</h2>
          <button className="create-group-btn" onClick={handleOpenModal}>
            <span className="material-symbols-outlined">add</span>
            Create New
          </button>
        </div>
        
        <div className="groups-grid">
          {groupsJoined.map((group) => (
            <div className="group-card" key={group.id}>
              <div className="group-card-header">
                <div className="icon-badge bg-primary-container text-white">
                  <span className="material-symbols-outlined">auto_stories</span>
                </div>
              <span className="status-badge bg-secondary-container text-on-secondary-container">Active</span>
            </div>
            <h3 className="body-lg font-h h3-margin">{group.name}</h3>
            <div className="group-stats">
              <div className="stat-item">
                <span className="material-symbols-outlined icon-sm">group</span>
                <span className="body-md text-sm">{group.maxMembers} Members</span>
              </div>
            </div>
            <button className="btn-enter">
              Enter Group
              <span className="material-symbols-outlined">arrow_forward</span>
            </button>
          </div>
          ))}

        </div>
      </section>

      {/* Trending Groups Section */}
      <section className="groups-section">
        <div className="section-header-wrap">
          <h2 className="h3 text-on-background">Trending Groups</h2>
          <div className="filter-controls">
            <div className="search-bar-wrap">
              <span className="material-symbols-outlined search-icon">search</span>
              <input type="text" className="search-input" placeholder="Find by subject or interest..." />
            </div>
            <button className="filter-btn">
              <span className="material-symbols-outlined">tune</span>
              Filter
            </button>
          </div>
        </div>

        <div className="trending-grid">
          {/* Trending Card 1 */}
          <div className="trending-card">
            <div className="trending-img-wrap">
              <img src="https://lh3.googleusercontent.com/aida-public/AB6AXuBquzG03V5HT1Uc3eqQkbeUKsiTtCUWt1cGIQu2Ci3LnFteI9GvrfBsHeNpRZ1OacHjrSHFHqrVGUSOnmc5_bpQglD0dF4t6ve1KsbIMo5r-uh8EwyBXQVYl-vN-QqG2-rQYCC94H-f00aoS-bRGeDe_LXvF-oAHRuU-XDu-xn7sEu37AA5oHT97O--WSNicbZF_IpO-5KVIYU6n3_Nt75H4ISOKauQ_SbOnmPFNBTdxOd9mgFL9WxEDlVgi-1DLSMdwI4x3o6vEYJY" alt="Trending Group" className="trending-img" />
            </div>
            <div className="trending-content">
              <div>
                <div className="trending-tags">
                  <span className="tag-badge bg-tertiary-fixed text-on-tertiary-fixed">Science</span>
                  <span className="tag-text">• 1.2k members</span>
                </div>
                <h4 className="body-lg font-h trending-title">The Feynman Method Hive</h4>
                <p className="body-md text-sm text-on-surface-variant line-clamp-2">Explaining complex concepts simply. Join daily sprint sessions.</p>
              </div>
              <div className="trending-footer">
                <div className="avatar-group">
                  <div className="avatar-sm bg-slate-200"></div>
                  <div className="avatar-sm bg-slate-300"></div>
                  <div className="avatar-sm bg-slate-400"></div>
                  <div className="avatar-sm bg-slate-500 avatar-count">+1k</div>
                </div>
                <button className="btn-join">Join</button>
              </div>
            </div>
          </div>

          {/* Trending Card 2 */}
          <div className="trending-card">
            <div className="trending-img-wrap">
              <img src="https://lh3.googleusercontent.com/aida-public/AB6AXuASucBs-xUAliFo3UUd-0EjlvfnEZVOzj_p-KhCBvK3DbYrmvDDNNUBbQlWD45ie5UZe0RDUqIzLxLLwP9ArjDc0lkAt3zsaWG67G3AIXNYNEMiau3R9FCG91vuhUEdB7YEJw9etckQSRRQLUp0Ga7eGYRlYqSo46O44lKbDZpnXBsbU63xjnI1erwhYdK3vhhKcDVAtxqbihxKx6JcqAx5SKu7sWv_QiJN-QmDehg1SgCVSohTYkntDxQDoUvON-41Lampu40TqioT" alt="Trending Group" className="trending-img" />
            </div>
            <div className="trending-content">
              <div>
                <div className="trending-tags">
                  <span className="tag-badge bg-primary-fixed text-on-primary-fixed">Code</span>
                  <span className="tag-text">• 840 members</span>
                </div>
                <h4 className="body-lg font-h trending-title">Algorithm Architects</h4>
                <p className="body-md text-sm text-on-surface-variant line-clamp-2">Daily LeetCode grind and system design discussions.</p>
              </div>
              <div className="trending-footer">
                <div className="avatar-group">
                  <div className="avatar-sm bg-slate-200"></div>
                  <div className="avatar-sm bg-slate-300"></div>
                  <div className="avatar-sm bg-slate-400 avatar-count">+2</div>
                </div>
                <button className="btn-join">Join</button>
              </div>
            </div>
          </div>

          {/* Trending Card 3 */}
          <div className="trending-card">
            <div className="trending-img-wrap">
              <img src="https://lh3.googleusercontent.com/aida-public/AB6AXuBRnF6sRy_st2k9PQv0NDjGR-X0QZP__iYh1DhFU4gPYcxg0FufoW_dLqfIKySsu5-_liib3D02ckUN7_8oEYe_dNku0TkAKkMHPDFeJZbvcnfQ0edWxet1rEgO4bdU67wDQiejRXTWT2Le1sJi4D8NWhE-ByPqz-FZ1NjY2wsqJCuQcqPotAcQhutrN2tD_QMOr74rMSKoVdaB7lpA4YcZJSbtjUGpEVtOC-IrC_B9o2Gbh64OYmEK6KTJ9DsluUq_yTEiOSYbwm9c" alt="Trending Group" className="trending-img" />
            </div>
            <div className="trending-content">
              <div>
                <div className="trending-tags">
                  <span className="tag-badge bg-secondary-fixed text-on-secondary-fixed">Philosophy</span>
                  <span className="tag-text">• 256 members</span>
                </div>
                <h4 className="body-lg font-h trending-title">Stoic Study Hall</h4>
                <p className="body-md text-sm text-on-surface-variant line-clamp-2">Reading through the meditations and practicing daily reflection.</p>
              </div>
              <div className="trending-footer">
                <div className="avatar-group">
                  <div className="avatar-sm bg-slate-200"></div>
                  <div className="avatar-sm bg-slate-300"></div>
                </div>
                <button className="btn-join">Join</button>
              </div>
            </div>
          </div>

          {/* Trending Card 4 */}
          <div className="trending-card">
            <div className="trending-img-wrap">
              <img src="https://lh3.googleusercontent.com/aida-public/AB6AXuAy3I48OfwbMDmtr4gGtJPPWTcBn8_j9hu-xzuSauF_GXqdP_wTk0ArE29UwaU_ORcWNLYnPEoMJbD_Uf_4gE1Hsm1hf0tWHG0Yeuytbn--R5ixGDBEqpRxj08-wNkxT13KCYJ22WjZtUHTfJzBBrSEHPG1Eg3IDlU5OcvewphcTbqpV0pt-puSJgW1Pj6ux5SLiOVOh14HWKAHONIdSW4tCy0Nqy4T0C8Q6tFtywfQ8qnDUbSkbeba9oEzEgw5wfg3QwUyKcHgUq7n" alt="Trending Group" className="trending-img" />
            </div>
            <div className="trending-content">
              <div>
                <div className="trending-tags">
                  <span className="tag-badge bg-surface-variant text-on-surface-variant">Focus</span>
                  <span className="tag-text">• 4.5k members</span>
                </div>
                <h4 className="body-lg font-h trending-title">Pomodoro Global</h4>
                <p className="body-md text-sm text-on-surface-variant line-clamp-2">Synchronized pomodoro sessions with ambient soundscapes.</p>
              </div>
              <div className="trending-footer">
                <div className="avatar-group">
                  <div className="avatar-sm bg-slate-200"></div>
                  <div className="avatar-sm bg-slate-300"></div>
                  <div className="avatar-sm bg-slate-400"></div>
                  <div className="avatar-sm bg-slate-500 avatar-count">+4k</div>
                </div>
                <button className="btn-join">Join</button>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Create Group Modal */}
      {showModal && (
        <div className="modal-overlay" onClick={handleCloseModal}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2 className="h3 text-on-background">Create New Group</h2>
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
                  Group Name *
                </label>
                <input
                  type="text"
                  id="name"
                  name="name"
                  value={formData.name}
                  onChange={handleInputChange}
                  placeholder="e.g., Advanced Calculus II"
                  className="form-input"
                  required
                />
              </div>

              <div className="form-group">
                <label htmlFor="subject" className="label-sm text-on-surface-variant">
                  Max Members 
                </label>
                <input
                  type="text"
                  id="maxMembers"
                  name="maxMembers"
                  value={formData.maxMembers}
                  onChange={handleInputChange}
                  placeholder="e.g., 20"
                  className="form-input"
                />
              </div>

              <div className="modal-actions">
                <button
                  type="button"
                  className="btn-secondary"
                  onClick={handleCloseModal}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="btn-primary"
                  disabled={isLoading}
                >
                  {isLoading ? 'Creating...' : 'Create Group'}
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
