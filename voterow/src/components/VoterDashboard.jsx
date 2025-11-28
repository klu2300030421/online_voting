import React, { useState, useEffect } from 'react';
import { FaHome, FaUser, FaLock, FaVoteYea, FaBell, FaQuestionCircle, FaChartBar, FaIdCard, FaPhone, FaEye, FaTimes, FaCheck } from 'react-icons/fa';
import { getApiUrl } from '../config/apiConfig';
import secureStorage from '../utils/secureStorage';
import './VoterDashboard.css';

const VoterDashboard = ({ user }) => {
  // Guard clause for undefined user
  if (!user) {
    return (
      <div className="voter-dashboard">
        <div className="loading-message">
          <p>Loading user data...</p>
        </div>
      </div>
    );
  }

  const [activeTab, setActiveTab] = useState('home');
  const [elections, setElections] = useState([]);
  const [allUsers, setAllUsers] = useState([]);
  const [allCandidates, setAllCandidates] = useState([]);
  const [showModal, setShowModal] = useState(false);
  const [selectedElection, setSelectedElection] = useState(null);
  const [notifications, setNotifications] = useState([]);
  const [profile, setProfile] = useState({
    fullName: user?.fullName || '',
    email: user?.email || '',
    age: user?.age || '',
    phoneNumber: user?.phoneNumber || user?.phone || '',
    idProofNumber: user?.idProofNumber || user?.idNumber || '',
    address: user?.address || ''
  });
  const [isProfileEditing, setIsProfileEditing] = useState(false);
  const [profileLoading, setProfileLoading] = useState(false);
  const [profileError, setProfileError] = useState('');
  const [profileSuccess, setProfileSuccess] = useState('');
  const [supportTickets, setSupportTickets] = useState([]);
  const [newTicket, setNewTicket] = useState({ subject: '', message: '' });

  // Update profile when user prop changes - memoized to prevent unnecessary updates
  useEffect(() => {
    if (user && user.id) {
      setProfile(prevProfile => {
        const newProfile = {
          fullName: user.fullName || '',
          email: user.email || '',
          age: user.age || '',
          phoneNumber: user.phoneNumber || user.phone || '',
          idProofNumber: user.idProofNumber || user.idNumber || '',
          address: user.address || ''
        };
        
        // Only update if values actually changed
        if (JSON.stringify(prevProfile) !== JSON.stringify(newProfile)) {
          return newProfile;
        }
        return prevProfile;
      });
    }
  }, [user?.id, user?.fullName, user?.email, user?.age, user?.phoneNumber, user?.phone, user?.idProofNumber, user?.idNumber, user?.address]);

  // Navigation tabs for voter modules
  const voterTabs = [
    { id: 'home', label: 'Home', icon: FaHome },
    { id: 'authentication', label: 'Authentication', icon: FaLock },
    { id: 'profile', label: 'Profile Management', icon: FaUser },
    { id: 'voting', label: 'Voting', icon: FaVoteYea },
    { id: 'notifications', label: 'Notifications', icon: FaBell },
    { id: 'support', label: 'Support', icon: FaQuestionCircle },
    { id: 'results', label: 'Results', icon: FaChartBar }
  ];

  // A single function to determine the status of an election
  const getElectionStatus = (election) => {
    // Handle admin-created elections with status field
    if (election.status) {
      switch (election.status.toUpperCase()) {
        case 'ONGOING':
          return 'Active';
        case 'SCHEDULED':
          return 'Upcoming';
        case 'COMPLETED':
          return 'Completed';
        default:
          return election.status;
      }
    }
    
    // Handle old format elections with date-based status
    const now = new Date();
    const startDate = new Date(election.startDate);
    const endDate = new Date(election.endDate);

    if (now < startDate) return 'Upcoming';
    if (now >= startDate && now <= endDate) return 'Active';
    return 'Completed';
  };

  useEffect(() => {
    const currentUserId = user?.id;
    if (!currentUserId) return;
    
    const fetchElectionsFromBackend = async () => {
      try {
        console.log('VoterDashboard: Fetching elections from backend...');
        // Get JWT token
        const token = secureStorage.getToken();
        
        // Try voter endpoint first (requires auth), then fall back to participant endpoint (public)
        let response = await fetch(getApiUrl('/api/voter/elections'), {
          method: 'GET',
          headers: {
            'Content-Type': 'application/json',
            'Cache-Control': 'no-cache',
            ...(token ? { 'Authorization': `Bearer ${token}` } : {})
          }
        });
        
        // If voter endpoint fails, try participant endpoint
        if (!response.ok) {
          console.log('VoterDashboard: Voter endpoint not available, trying participant endpoint...');
          response = await fetch(getApiUrl('/api/participant/elections'), {
            method: 'GET',
            headers: {
              'Content-Type': 'application/json',
              'Cache-Control': 'no-cache',
              ...(token ? { 'Authorization': `Bearer ${token}` } : {})
            }
          });
        }
        
        if (response.ok) {
          const backendElections = await response.json();
          console.log('VoterDashboard: Successfully fetched elections from admin database:', backendElections);
          // Enrich with vote-status per election
          try {
            const enriched = await Promise.all(
              backendElections.map(async (e) => {
                try {
                  const r = await fetch(getApiUrl(`/api/voting/check/${currentUserId}/${e.id}`));
                  if (r.ok) {
                    const j = await r.json();
                    return { ...e, hasVoted: Boolean(j.hasVoted) };
                  }
                } catch (_) {}
                return { ...e, hasVoted: false };
              })
            );
            setElections(enriched);
          } catch (enrichErr) {
            console.warn('VoterDashboard: Failed enriching elections with vote status', enrichErr);
            setElections(backendElections);
          }
        } else {
          console.error('VoterDashboard: Failed to fetch from admin database, status:', response.status);
          setElections([]);
        }
      } catch (error) {
        console.error('VoterDashboard: Error connecting to admin database:', error);
        setElections([]);
      }
    };

    // Backend-only mode - no localStorage operations
    
    // Load elections from database only
    fetchElectionsFromBackend();
    
    // Refresh elections every 5 seconds from database
    const interval = setInterval(fetchElectionsFromBackend, 5000);

    // Users will be fetched from backend when needed
    setAllUsers([]);

    // Load candidates from database - fetch from proper Candidate table
    const fetchCandidatesFromDatabase = async () => {
      try {
        console.log('VoterDashboard: Fetching approved candidates from database');
        
        // Fetch from both User table (old format) and Candidate table (new format)
        const [usersResponse, candidatesResponse] = await Promise.all([
          fetch(getApiUrl('/api/admin/candidates')),
          fetch(getApiUrl('/api/participant/candidate-applications?userId=0')).catch(() => ({ ok: false }))
        ]);
        
        let allApprovedCandidates = [];
        
        // Get approved candidates from User table (old format)
        if (usersResponse.ok) {
          const userCandidates = await usersResponse.json();
          const approvedUserCandidates = userCandidates.filter(candidate => 
            candidate.status === 'APPROVED' || candidate.isVerified === true
          );
          allApprovedCandidates = [...allApprovedCandidates, ...approvedUserCandidates];
        }
        
        console.log('VoterDashboard: Found approved candidates:', allApprovedCandidates.length);
        setAllCandidates(allApprovedCandidates);
        
      } catch (error) {
        console.error('VoterDashboard: Error fetching candidates from database:', error);
        setAllCandidates([]);
      }
    };
    
    fetchCandidatesFromDatabase();

    // Notifications and tickets will be fetched from backend
    setNotifications([]);
    setSupportTickets([]);

    return () => clearInterval(interval);
  }, [user?.id]);

  // Separate useEffect for election notifications to run when elections change
  useEffect(() => {
    if (elections.length > 0) {
      generateElectionNotifications(elections);
    }
  }, [elections]);

  const generateElectionNotifications = (allElections) => {
    const now = new Date();
    const newNotifications = [];

    allElections.forEach(election => {
      const startDate = new Date(election.startDate);
      const endDate = new Date(election.endDate);
      const timeToStart = startDate.getTime() - now.getTime();
      const timeToEnd = endDate.getTime() - now.getTime();

      // Poll opening soon notification
      if (timeToStart > 0 && timeToStart <= 24 * 60 * 60 * 1000) {
        newNotifications.push({
          id: `poll-opening-${election.id}`,
          type: 'info',
          message: `Election "${election.title}" opens tomorrow!`,
          timestamp: new Date().toISOString(),
          read: false
        });
      }

      // Poll closing soon notification
      if (timeToEnd > 0 && timeToEnd <= 2 * 60 * 60 * 1000) {
        newNotifications.push({
          id: `poll-closing-${election.id}`,
          type: 'warning',
          message: `Election "${election.title}" closes in 2 hours!`,
          timestamp: new Date().toISOString(),
          read: false
        });
      }

      // Vote confirmation
      if (hasVoted(election)) {
        newNotifications.push({
          id: `vote-confirmed-${election.id}`,
          type: 'success',
          message: `Your vote in "${election.title}" has been confirmed!`,
          timestamp: new Date().toISOString(),
          read: false
        });
      }
    });

    if (newNotifications.length > 0) {
      setNotifications(prev => [...prev, ...newNotifications]);
      // TODO: Save notifications to backend
    }
  };

  const openVoteModal = (election) => {
    setSelectedElection(election);
    setShowModal(true);
  };

  const handleVote = async (candidateId) => {
    if (!selectedElection || !user?.id) {
      console.error('Missing selectedElection or user.id');
      return;
    }

    try {
      const response = await fetch(getApiUrl('/api/voting/cast'), {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          voterId: user.id,
          candidateId: candidateId,
          electionId: selectedElection.id
        })
      });

      const result = await response.json();
      
      if (response.ok && result.success) {
        alert('Your vote has been cast successfully!');
        setShowModal(false);
        setSelectedElection(null);
        // Refresh elections to update vote status
        fetchElectionsFromBackend();
      } else {
        alert(result.error || 'Failed to cast vote');
      }
    } catch (error) {
      console.error('Error casting vote:', error);
      alert('Error casting vote. Please try again.');
    }
  };

  const hasVoted = (election) => {
    // Defer to server-verified flag placed on election by fetchElectionsFromBackend
    // Fallback to false if not present
    return Boolean(election?.hasVoted);
  };
  
  const getParticipantName = (id) => {
    // First try to find in candidates list
    const candidate = allCandidates.find(c => c.id === id);
    if (candidate) {
      return candidate.name;
    }
    
    // Fallback to users list
    const participant = allUsers.find(u => u.id === id);
    return participant ? participant.fullName : 'Unknown Candidate';
  };

  const handleProfileUpdate = (field, value) => {
    const updatedProfile = { ...profile, [field]: value };
    setProfile(updatedProfile);
  };

  const saveProfile = async () => {
    setProfileLoading(true);
    setProfileError('');
    setProfileSuccess('');
    
    try {
      const response = await fetch(getApiUrl(`/api/auth/profile?email=${encodeURIComponent(user.email)}`), {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          fullName: profile.fullName,
          age: profile.age ? parseInt(profile.age) : null,
          phoneNumber: profile.phoneNumber || null,
          idProofNumber: profile.idProofNumber || null,
          address: profile.address || null
        }),
      });

      if (response.ok) {
        const updatedUser = await response.json();
        setProfile({
          fullName: updatedUser.fullName,
          email: updatedUser.email,
          age: updatedUser.age || '',
          phoneNumber: updatedUser.phoneNumber || '',
          idProofNumber: updatedUser.idProofNumber || '',
          address: updatedUser.address || ''
        });
        setProfileSuccess('Profile updated successfully!');
        setIsProfileEditing(false);
        
        // Update parent component user data if possible
        if (window.updateUserProfile) {
          window.updateUserProfile(updatedUser);
        }
      } else {
        const errorText = await response.text();
        setProfileError(errorText || 'Failed to update profile');
      }
    } catch (err) {
      setProfileError('Network error. Please try again.');
    } finally {
      setProfileLoading(false);
    }
  };

  const handleSupportTicket = () => {
    if (!newTicket.subject || !newTicket.message) return;

    const ticket = {
      id: Date.now(),
      subject: newTicket.subject,
      message: newTicket.message,
      status: 'Open',
      timestamp: new Date().toISOString(),
      userId: user.id
    };

    setSupportTickets(prev => [...prev, ticket]);
    setNewTicket({ subject: '', message: '' });
    // TODO: Submit ticket to backend
    alert('Support ticket submitted successfully!');
  };

  const markNotificationAsRead = (notificationId) => {
    setNotifications(prev => prev.map(n => 
      n.id === notificationId ? { ...n, read: true } : n
    ));
    // TODO: Update notification status in backend
  };

  const isEligibleVoter = () => {
    const age = user.dateOfBirth ? new Date().getFullYear() - new Date(user.dateOfBirth).getFullYear() : 0;
    return age >= 18 && user.idNumber;
  };

  const renderVoteButton = (election) => {
    const status = getElectionStatus(election);
    
    // Check if user has already voted (one vote per voter)
    if (hasVoted(election)) {
      return <button className="btn btn-secondary" disabled>Already Voted</button>;
    }
    
    // CRITICAL: Must have approved candidates assigned before allowing voting
    const approvedCandidatesForElection = allCandidates.filter(candidate => 
      candidate.electionId === election.id || 
      (election.participants && election.participants.includes(candidate.id))
    );
    
    if (approvedCandidatesForElection.length === 0) {
      return <button className="btn btn-secondary" disabled>No Candidates Available</button>;
    }
    
    // Election status check
    if (election.status === 'ONGOING' || election.status === 'ACTIVE' || status === 'Active') {
      return <button className="btn btn-primary" onClick={() => openVoteModal(election)}>Vote Now</button>;
    } else if (status === 'Upcoming' || election.status === 'SCHEDULED') {
      return <button className="btn btn-secondary" disabled>Election Not Started</button>;
    } else if (status === 'Completed' || election.status === 'COMPLETED') {
      return <button className="btn btn-secondary" disabled>Election Ended</button>;
    } else {
      return <button className="btn btn-secondary" disabled>Voting Unavailable</button>;
    }
  };

  // Render different modules based on active tab
  const renderModule = () => {
    switch (activeTab) {
      case 'home':
        return (
          <div className="module-content">
            <h2>Welcome, {user.fullName}!</h2>
            <div className="dashboard-overview">
              <div className="stats-grid">
                <div className="stat-card">
                  <h3>{elections.length}</h3>
                  <p>Eligible Elections</p>
                </div>
                <div className="stat-card">
                  <h3>{elections.filter(e => hasVoted(e)).length}</h3>
                  <p>Votes Cast</p>
                </div>
                <div className="stat-card">
                  <h3>{notifications.filter(n => !n.read).length}</h3>
                  <p>Unread Notifications</p>
                </div>
                <div className="stat-card">
                  <h3>{supportTickets.length}</h3>
                  <p>Support Tickets</p>
                </div>
              </div>
              <div className="recent-activity">
                <h3>Recent Elections</h3>
                {elections.slice(0, 3).map(election => (
                  <div key={election.id} className="activity-item">
                    <h4>{election.title}</h4>
                    <p>Status: <span className={`status-${getElectionStatus(election).toLowerCase()}`}>
                      {getElectionStatus(election)}
                    </span></p>
                  </div>
                ))}
              </div>
            </div>
          </div>
        );

      case 'authentication':
        return (
          <div className="module-content">
            <h2>Authentication & Eligibility</h2>
            <div className="auth-section">
              <div className="eligibility-check">
                <h3><FaIdCard className="module-icon" /> Eligibility Status</h3>
                <div className={`eligibility-status ${isEligibleVoter() ? 'eligible' : 'not-eligible'}`}>
                  <FaCheck className="status-icon" />
                  <p>{isEligibleVoter() ? 'You are eligible to vote!' : 'Eligibility verification required'}</p>
                </div>
                <div className="verification-details">
                  <p><strong>Age Requirement:</strong> {user.dateOfBirth ? 
                    `${new Date().getFullYear() - new Date(user.dateOfBirth).getFullYear()} years old` : 
                    'Date of birth not provided'}</p>
                  <p><strong>ID Verification:</strong> {user.idNumber ? 'Verified' : 'Not provided'}</p>
                  <p><strong>Email Verification:</strong> Verified</p>
                </div>
              </div>
              
              <div className="security-settings">
                <h3><FaLock className="module-icon" /> Security Settings</h3>
                <div className="security-options">
                  <div className="security-item">
                    <label>Two-Factor Authentication</label>
                    <button className="btn btn-primary">Enable 2FA</button>
                  </div>
                  <div className="security-item">
                    <label>Email Notifications</label>
                    <button className="btn btn-secondary">Configure</button>
                  </div>
                  <div className="security-item">
                    <label>Login History</label>
                    <button className="btn btn-info">View History</button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        );

      case 'profile':
        return (
          <div className="module-content">
            <h2>Profile Management</h2>
            {profileError && <div className="alert alert-error">{profileError}</div>}
            {profileSuccess && <div className="alert alert-success">{profileSuccess}</div>}
            
            <div className="profile-section">
              <div className="personal-details">
                <div className="profile-header">
                  <h3><FaUser className="module-icon" /> Personal Details</h3>
                  <button 
                    className="btn btn-secondary"
                    onClick={() => setIsProfileEditing(!isProfileEditing)}
                    disabled={profileLoading}
                  >
                    {isProfileEditing ? 'Cancel' : 'Edit Profile'}
                  </button>
                </div>
                
                <div className="form-grid">
                  <div className="form-group">
                    <label>Full Name *</label>
                    <input 
                      type="text" 
                      value={profile.fullName} 
                      onChange={(e) => handleProfileUpdate('fullName', e.target.value)}
                      disabled={!isProfileEditing}
                      required
                    />
                  </div>
                  <div className="form-group">
                    <label>Email</label>
                    <input 
                      type="email" 
                      value={profile.email} 
                      disabled 
                      title="Email cannot be changed"
                    />
                  </div>
                  <div className="form-group">
                    <label>Age</label>
                    <input 
                      type="number" 
                      value={profile.age} 
                      onChange={(e) => handleProfileUpdate('age', e.target.value)}
                      disabled={!isProfileEditing}
                      min="18"
                      max="100"
                    />
                  </div>
                  <div className="form-group">
                    <label>Phone Number</label>
                    <input 
                      type="tel" 
                      value={profile.phoneNumber} 
                      onChange={(e) => handleProfileUpdate('phoneNumber', e.target.value)}
                      disabled={!isProfileEditing}
                      maxLength="15"
                    />
                  </div>
                  <div className="form-group">
                    <label>ID Proof Number</label>
                    <input 
                      type="text" 
                      value={profile.idProofNumber} 
                      onChange={(e) => handleProfileUpdate('idProofNumber', e.target.value)}
                      disabled={!isProfileEditing}
                      maxLength="20"
                    />
                  </div>
                  <div className="form-group">
                    <label>Address</label>
                    <textarea 
                      value={profile.address} 
                      onChange={(e) => handleProfileUpdate('address', e.target.value)}
                      disabled={!isProfileEditing}
                      maxLength="500"
                      rows="3"
                    />
                  </div>
                </div>
                
                {isProfileEditing && (
                  <div className="profile-actions">
                    <button 
                      className="btn btn-primary" 
                      onClick={saveProfile}
                      disabled={profileLoading || !profile.fullName.trim()}
                    >
                      {profileLoading ? 'Saving...' : 'Save Changes'}
                    </button>
                    <button 
                      className="btn btn-secondary" 
                      onClick={() => {
                        setIsProfileEditing(false);
                        setProfileError('');
                        setProfileSuccess('');
                        // Reset to original values
                        setProfile({
                          fullName: user?.fullName || '',
                          email: user?.email || '',
                          age: user?.age || '',
                          phoneNumber: user?.phoneNumber || user?.phone || '',
                          idProofNumber: user?.idProofNumber || user?.idNumber || '',
                          address: user?.address || ''
                        });
                      }}
                      disabled={profileLoading}
                    >
                      Cancel
                    </button>
                  </div>
                )}
              </div>
              
              <div className="assigned-elections">
                <h3><FaVoteYea className="module-icon" /> Assigned Elections</h3>
                <div className="elections-list">
                  {elections.map(election => (
                    <div key={election.id} className="election-card">
                      <h4>{election.title}</h4>
                      <p>Start: {new Date(election.startDate).toLocaleDateString()}</p>
                      <p>End: {new Date(election.endDate).toLocaleDateString()}</p>
                      <span className={`status-badge ${getElectionStatus(election).toLowerCase()}`}>
                        {getElectionStatus(election)}
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </div>
        );

      case 'voting':
        return (
          <div className="module-content">
            <h2>Voting Interface</h2>

            <div className="voting-section">
              <div className="elections-grid">
                {elections.length > 0 ? (
                  elections.map(election => (
                    <div key={election.id} className="election-card large">
                      <div className="card-header">
                        <h3>{election.title}</h3>
                        <span className={`status-badge ${getElectionStatus(election).toLowerCase()}`}>
                          {getElectionStatus(election)}
                        </span>
                      </div>
                      <div className="card-content">
                        <p><strong>Starts:</strong> {new Date(election.startDate).toLocaleString()}</p>
                        <p><strong>Ends:</strong> {new Date(election.endDate).toLocaleString()}</p>
                        <p><strong>Candidates:</strong> {(election.participants || []).length}</p>
                        <p><strong>Debug Info:</strong> Election ID: {election.id}, Status: {election.status}, Participants: {JSON.stringify(election.participants)}</p>
                        
                        <div className="candidate-preview">
                          <h4>Candidates:</h4>
                          {allCandidates.filter(c => c.electionId === election.id || (election.participants && election.participants.includes(c.id))).length > 0 ? (
                            allCandidates.filter(c => c.electionId === election.id || (election.participants && election.participants.includes(c.id))).slice(0, 3).map(candidate => (
                              <span key={candidate.id} className="candidate-tag">
                                {candidate.name}
                              </span>
                            ))
                          ) : (
                            <p className="no-candidates">Candidates will be announced soon</p>
                          )}
                        </div>
                        
                        <div className="voting-actions">
                          {hasVoted(election) ? (
                            <div className="vote-status">
                              <FaCheck className="voted-icon" />
                              <span>Vote Cast Successfully</span>
                            </div>
                          ) : (
                            renderVoteButton(election)
                          )}
                        </div>
                      </div>
                    </div>
                  ))
                ) : (
                  <div className="no-elections">
                    <FaVoteYea className="no-data-icon" />
                    {allCandidates.length > 0 ? (
                      <div>
                        <p>Elections are available but candidates haven't been assigned yet.</p>
                        <p>There are {allCandidates.length} approved candidates waiting to be assigned to elections.</p>
                        <p>Please contact the administrator to assign candidates to elections.</p>
                      </div>
                    ) : (
                      <p>You are not eligible for any elections at this time.</p>
                    )}
                  </div>
                )}
              </div>
            </div>
          </div>
        );

      case 'notifications':
        return (
          <div className="module-content">
            <h2>Notifications</h2>
            <div className="notifications-section">
              <div className="notification-stats">
                <div className="stat-item">
                  <span className="count">{notifications.length}</span>
                  <span className="label">Total</span>
                </div>
                <div className="stat-item">
                  <span className="count">{notifications.filter(n => !n.read).length}</span>
                  <span className="label">Unread</span>
                </div>
              </div>
              
              <div className="notifications-list">
                {notifications.length > 0 ? (
                  notifications.map(notification => (
                    <div key={notification.id} 
                         className={`notification-item ${notification.read ? 'read' : 'unread'} ${notification.type}`}
                         onClick={() => markNotificationAsRead(notification.id)}>
                      <div className="notification-icon">
                        <FaBell />
                      </div>
                      <div className="notification-content">
                        <p>{notification.message}</p>
                        <small>{new Date(notification.timestamp).toLocaleString()}</small>
                      </div>
                      {!notification.read && <div className="unread-indicator"></div>}
                    </div>
                  ))
                ) : (
                  <div className="no-notifications">
                    <FaBell className="no-data-icon" />
                    <p>No notifications yet.</p>
                  </div>
                )}
              </div>
            </div>
          </div>
        );

      case 'support':
        return (
          <div className="module-content">
            <h2>Support Center</h2>
            <div className="support-section">
              <div className="support-tabs">
                <div className="tab-content active">
                  <div className="create-ticket">
                    <h3><FaQuestionCircle className="module-icon" /> Report an Issue</h3>
                    <div className="ticket-form">
                      <div className="form-group">
                        <label>Subject</label>
                        <input type="text" value={newTicket.subject} 
                               placeholder="Brief description of the issue"
                               onChange={(e) => setNewTicket({...newTicket, subject: e.target.value})} />
                      </div>
                      <div className="form-group">
                        <label>Message</label>
                        <textarea value={newTicket.message} 
                                  placeholder="Detailed description of the issue"
                                  onChange={(e) => setNewTicket({...newTicket, message: e.target.value})} />
                      </div>
                      <button className="btn btn-primary" onClick={handleSupportTicket}>
                        Submit Ticket
                      </button>
                    </div>
                  </div>

                  <div className="my-tickets">
                    <h3>My Support Tickets</h3>
                    {supportTickets.length > 0 ? (
                      supportTickets.map(ticket => (
                        <div key={ticket.id} className="ticket-item">
                          <div className="ticket-header">
                            <h4>{ticket.subject}</h4>
                            <span className={`status-badge ${ticket.status.toLowerCase()}`}>
                              {ticket.status}
                            </span>
                          </div>
                          <p>{ticket.message}</p>
                          <small>Submitted: {new Date(ticket.timestamp).toLocaleString()}</small>
                        </div>
                      ))
                    ) : (
                      <p>No support tickets submitted.</p>
                    )}
                  </div>

                  <div className="faq-section">
                    <h3>Frequently Asked Questions</h3>
                    <div className="faq-list">
                      <div className="faq-item">
                        <h4>How do I vote in an election?</h4>
                        <p>Navigate to the Voting section, select an active election, and click "Vote Now" to see candidates and cast your vote.</p>
                      </div>
                      <div className="faq-item">
                        <h4>Can I change my vote after casting it?</h4>
                        <p>No, votes are final once submitted. Please review your choice carefully before confirming.</p>
                      </div>
                      <div className="faq-item">
                        <h4>When will election results be available?</h4>
                        <p>Results are published in the Results section after the election ends and votes are tallied.</p>
                      </div>
                      <div className="faq-item">
                        <h4>What if I'm having technical issues?</h4>
                        <p>Use the support ticket system above to report any technical problems. Our team will assist you promptly.</p>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        );

      case 'results':
        return (
          <div className="module-content">
            <h2>Election Results</h2>
            <div className="results-section">
              <div className="completed-elections">
                {elections.filter(e => getElectionStatus(e) === 'Completed').length > 0 ? (
                  elections.filter(e => getElectionStatus(e) === 'Completed').map(election => (
                    <div key={election.id} className="result-card">
                      <div className="result-header">
                        <h3>{election.title}</h3>
                        <span className="completion-date">
                          Completed: {new Date(election.endDate).toLocaleDateString()}
                        </span>
                      </div>
                      <div className="result-content">
                        <div className="participation-stats">
                          <h4>Participation Statistics</h4>
                          <div className="stats-grid">
                            <div className="stat">
                              <span className="value">{(election.voters || []).length}</span>
                              <span className="label">Eligible Voters</span>
                            </div>
                            <div className="stat">
                              <span className="value">{Object.keys(election.votes || {}).length}</span>
                              <span className="label">Votes Cast</span>
                            </div>
                            <div className="stat">
                              <span className="value">
                                {election.voters && election.voters.length > 0 ? 
                                  ((Object.keys(election.votes || {}).length / election.voters.length) * 100).toFixed(1) : 
                                  '0.0'}%
                              </span>
                              <span className="label">Turnout</span>
                            </div>
                          </div>
                        </div>
                        
                        <div className="candidate-results">
                          <h4>Results</h4>
                          {(election.participants || []).map(participantId => {
                            const votes = Object.values(election.votes || {}).filter(vote => vote === participantId).length;
                            const totalVotes = Object.keys(election.votes || {}).length;
                            const percentage = totalVotes > 0 ? (votes / totalVotes) * 100 : 0;
                            return (
                              <div key={participantId} className="candidate-result">
                                <div className="candidate-info">
                                  <span className="name">{getParticipantName(participantId)}</span>
                                  <span className="votes">{votes} votes ({percentage.toFixed(1)}%)</span>
                                </div>
                                <div className="vote-bar">
                                  <div className="vote-fill" style={{width: `${percentage}%`}}></div>
                                </div>
                              </div>
                            );
                          })}
                        </div>
                        
                        {hasVoted(election) && (
                          <div className="my-vote">
                            <p><FaCheck className="voted-icon" /> You participated in this election</p>
                          </div>
                        )}
                      </div>
                    </div>
                  ))
                ) : (
                  <div className="no-results">
                    <FaChartBar className="no-data-icon" />
                    <p>No completed elections to display results for.</p>
                  </div>
                )}
              </div>
            </div>
          </div>
        );

      default:
        return <div>Module not found</div>;
    }
  };

  return (
    <div className="voter-dashboard">
      {/* Sidebar Navigation */}
      <div className="voter-sidebar">
        <div className="sidebar-header">
          <h2 className="sidebar-title">Voter Portal</h2>
          <p className="sidebar-subtitle">{user.fullName}</p>
        </div>
        
        <div className="sidebar-nav">
          {voterTabs.map(tab => (
            <button
              key={tab.id}
              className={`nav-tab ${activeTab === tab.id ? 'active' : ''}`}
              onClick={() => setActiveTab(tab.id)}
            >
              <tab.icon className="tab-icon" />
              <span>{tab.label}</span>
            </button>
          ))}
        </div>
      </div>

      {/* Main Content Area */}
      <div className="voter-main-content">
        {renderModule()}
      </div>

      {/* Voting Modal */}
      {showModal && selectedElection && (
        <div className="modal-overlay">
          <div className="modal-content">
            <div className="modal-header">
              <h3>Vote in: {selectedElection.title}</h3>
              <button className="close-btn" onClick={() => setShowModal(false)}>
                <FaTimes />
              </button>
            </div>
            <div className="modal-body">
              <div className="voting-instructions">
                <p>Please select one candidate to cast your vote. This action cannot be undone.</p>
                <div className="security-notice">
                  <FaLock className="security-icon" />
                  <span>Your vote is anonymous and secure</span>
                </div>
              </div>
              <div className="candidate-voting-list">
                {allCandidates.filter(c => c.electionId === selectedElection.id || (selectedElection.participants && selectedElection.participants.includes(c.id))).length > 0 ? (
                  allCandidates.filter(c => c.electionId === selectedElection.id || (selectedElection.participants && selectedElection.participants.includes(c.id))).map(candidate => (
                    <div key={candidate.id} className="candidate-voting-card">
                      <div className="candidate-details">
                        <h4>{candidate.name}</h4>
                        <p>{candidate.party ? `Party: ${candidate.party}` : 'Independent'}</p>
                        <p>Click to vote for this candidate</p>
                      </div>
                      <button
                        className="vote-btn"
                        onClick={() => handleVote(candidate.id)}
                      >
                        <FaVoteYea /> Cast Vote
                      </button>
                    </div>
                  ))
                ) : (
                  <div className="no-candidates-modal">
                    <p>No candidates are assigned to this election yet.</p>
                  </div>
                )
                }
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default VoterDashboard;