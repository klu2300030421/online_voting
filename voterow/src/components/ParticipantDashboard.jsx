import React, { useState, useEffect, useCallback } from 'react';
import { 
  FaUser, FaIdCard, FaVoteYea, FaTools, FaBell, FaChartBar, 
  FaHome, FaUserEdit, FaImage, FaClipboardList, FaBullhorn, 
  FaFileAlt, FaCalendarAlt, FaDownload, FaEye, FaUpload, 
  FaCheckCircle, FaTimesCircle, FaClock, FaUsers, FaLock,
  FaCog, FaSync, FaUserTie
} from 'react-icons/fa';
import { getApiUrl } from '../config/apiConfig';
import secureStorage from '../utils/secureStorage';
import { sanitizeInput, validateRequired, validateFileSize, validateFileType } from '../utils/validation';
import './ParticipantDashboard.css';

// Stable, top-level Profile Management component to avoid remounts on each parent re-render
function ProfileManagementStable({
  profile,
  isProfileEditing,
  profileLoading,
  profileError,
  profileSuccess,
  onToggleEdit,
  onFieldChange,
  onSave,
  onCancel,
}) {
  return (
    <div className="candidate-module">
      <div className="content-header">
        <h1 className="content-title">Profile Management</h1>
        <p className="content-subtitle">Update your personal details and candidate information</p>
      </div>
      <div className="module-content">
        {profileError && <div className="alert alert-error">{profileError}</div>}
        {profileSuccess && <div className="alert alert-success">{profileSuccess}</div>}
        <div className="profile-sections">
          <div className="profile-section">
            <div className="section-header">
              <h4><FaUser /> Personal Details</h4>
              <button 
                className="btn btn-secondary"
                onClick={onToggleEdit}
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
                  onChange={(e) => onFieldChange('fullName', e.target.value)}
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
                <label>Phone Number</label>
                <input 
                  type="tel" 
                  value={profile.phoneNumber}
                  onChange={(e) => onFieldChange('phoneNumber', e.target.value)}
                  disabled={!isProfileEditing}
                  maxLength="15"
                  placeholder="Enter phone number" 
                />
              </div>
              <div className="form-group">
                <label>Age</label>
                <input 
                  type="number" 
                  value={profile.age}
                  onChange={(e) => onFieldChange('age', e.target.value)}
                  disabled={!isProfileEditing}
                  min="18" 
                  max="100"
                />
              </div>
              <div className="form-group">
                <label>ID Proof Number</label>
                <input 
                  type="text" 
                  value={profile.idProofNumber}
                  onChange={(e) => onFieldChange('idProofNumber', e.target.value)}
                  disabled={!isProfileEditing}
                  maxLength="20"
                />
              </div>
              <div className="form-group">
                <label>Address</label>
                <textarea 
                  value={profile.address}
                  onChange={(e) => onFieldChange('address', e.target.value)}
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
                  onClick={onSave}
                  disabled={profileLoading || !profile.fullName.trim()}
                >
                  {profileLoading ? 'Saving...' : 'Save Changes'}
                </button>
                <button 
                  className="btn btn-secondary" 
                  onClick={onCancel}
                  disabled={profileLoading}
                >
                  Cancel
                </button>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

const ParticipantDashboard = ({ user }) => {
  // Guard clause for undefined user
  if (!user) {
    return (
      <div className="participant-dashboard">
        <div className="loading-message">
          <p>Loading user data...</p>
        </div>
      </div>
    );
  }

  const [activeTab, setActiveTab] = useState('home');
  const [loading, setLoading] = useState(false);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [allElections, setAllElections] = useState([]);
  const [enrollmentStatus, setEnrollmentStatus] = useState({});
  
  // Profile management state
  const [profile, setProfile] = useState({
    fullName: user?.fullName || '',
    email: user?.email || '',
    age: user?.age || '',
    phoneNumber: user?.phoneNumber || '',
    idProofNumber: user?.idProofNumber || '',
    address: user?.address || '',
    biography: '',
    partyName: '',
    partySymbol: ''
  });
  const [isProfileEditing, setIsProfileEditing] = useState(false);
  const [profileLoading, setProfileLoading] = useState(false);
  const [profileError, setProfileError] = useState('');
  const [profileSuccess, setProfileSuccess] = useState('');
  
  // Image handling states
  const [profileImage, setProfileImage] = useState(null);
  const [profileImagePreview, setProfileImagePreview] = useState('');
  const [partySymbol, setPartySymbol] = useState(null);
  const [partySymbolPreview, setPartySymbolPreview] = useState('');

  // Navigation tabs for candidate modules
  const tabs = [
    { id: 'home', label: 'Home', icon: FaHome },
    { id: 'profile', label: 'Profile Management', icon: FaUserEdit },
    { id: 'authentication', label: 'Authentication', icon: FaIdCard },
    { id: 'elections', label: 'Election Participation', icon: FaVoteYea },
    { id: 'campaign', label: 'Campaign Tools', icon: FaTools },
    { id: 'notifications', label: 'Notifications', icon: FaBell },
    { id: 'results', label: 'Results & Reports', icon: FaChartBar }
  ];

  // Helper function to check election status based on current time and admin status
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

  // Define fetchElectionsFromBackend function with useCallback to prevent unnecessary re-renders
  const fetchElectionsFromBackend = useCallback(async () => {
    if (isRefreshing) return false; // Prevent multiple simultaneous calls
    
    try {
      setIsRefreshing(true);
      console.log('ParticipantDashboard: Fetching elections from participant endpoint...');
      const token = secureStorage.getToken();
      const response = await fetch(getApiUrl('/api/participant/elections'), {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
          'Cache-Control': 'no-cache',
          ...(token ? { 'Authorization': `Bearer ${token}` } : {})
        }
      });
      
      if (response.ok) {
        const backendElections = await response.json();
        console.log('ParticipantDashboard: Successfully fetched elections from participant endpoint:', backendElections.length);
        
        const electionsWithParticipants = backendElections.map(election => ({
          ...election,
          participants: election.participants || [],
          enrollmentRequests: election.enrollmentRequests || []
        }));
        
        setAllElections(electionsWithParticipants);
        return true;
      } else {
        console.error('ParticipantDashboard: Failed to fetch from participant endpoint, status:', response.status);
        setAllElections([]);
        return false;
      }
    } catch (error) {
      console.error('ParticipantDashboard: Error connecting to participant endpoint:', error);
      setAllElections([]);
      return false;
    } finally {
      setIsRefreshing(false);
    }
  }, [isRefreshing]);

  // Load elections on component mount - backend only
  useEffect(() => {
    fetchElectionsFromBackend();
  }, [user]);
  
  // Setup enrollment status for the current user
  useEffect(() => {
    if (allElections.length > 0 && user) {
      const status = {};
      
      allElections.forEach(election => {
        // Check if user is already a participant (approved)
        if (election.participants && election.participants.includes(user.id)) {
          status[election.id] = { status: 'APPROVED', message: 'Your application has been approved' };
        }
        // Check if user has a pending enrollment request
        else if (election.enrollmentRequests) {
          const request = election.enrollmentRequests.find(req => req.userId === user.id);
          if (request) {
            status[election.id] = { 
              status: request.status, 
              message: request.status === 'PENDING' ? 
                'Your application is pending approval' : 
                request.status === 'REJECTED' ? 
                'Your application was rejected' : 
                'Application status: ' + request.status
            };
          }
        }
      });
      
      setEnrollmentStatus(status);
    }
  }, [allElections, user]);

  const handleEnroll = async (electionId) => {
    try {
      // Update enrollment status for UI feedback first
      setEnrollmentStatus(prev => ({
        ...prev,
        [electionId]: { status: 'PENDING', message: 'Application submitted, waiting for approval' }
      }));
      
      // Create candidate application data for the proper endpoint
      const candidateApplication = {
        userId: user.id,
        electionId: electionId,
        party: profile.partyName || 'Independent'
      };
      
      // Submit candidate application to participant endpoint
      console.log('ParticipantDashboard: Submitting candidate application:', candidateApplication);
      const response = await fetch(getApiUrl('/api/participant/candidate-application'), {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(candidateApplication),
      });
      
      console.log('Response status:', response.status);
      console.log('Response headers:', response.headers);
      
      if (response.ok) {
        const result = await response.json();
        console.log('ParticipantDashboard: Successfully submitted candidate application:', result);
        
        // Update enrollment status to reflect successful submission
        setEnrollmentStatus(prev => ({
          ...prev,
          [electionId]: { status: 'PENDING', message: 'Application submitted successfully, awaiting admin approval' }
        }));
        
        alert('Your application has been submitted and is pending approval by an administrator.');
        
        // Refresh application status
        checkCandidateApplicationStatus();
      } else {
        const errorText = await response.text();
        console.error('ParticipantDashboard: Raw error response:', errorText);
        
        let errorMessage = 'Failed to submit application';
        
        try {
          const errorData = JSON.parse(errorText);
          errorMessage = errorData.error || errorData.message || errorMessage;
        } catch (e) {
          errorMessage = errorText || errorMessage;
        }
        
        console.error('ParticipantDashboard: Parsed error message:', errorMessage);
        throw new Error(errorMessage);
      }
      
    } catch (error) {
      console.error('Error submitting enrollment:', error);
      alert('There was an error submitting your application. Please try again.');
      
      // Reset status on error
      setEnrollmentStatus(prev => ({
        ...prev,
        [electionId]: { status: 'ERROR', message: 'Application failed, please try again' }
      }));
    }
  };

  // Function to check candidate application status from database
  const checkCandidateApplicationStatus = useCallback(async () => {
    if (!user || !allElections) return;
    
    try {
      console.log('Fetching candidate applications from database for user:', user.id);
      
      // Fetch candidate applications from participant endpoint
      const response = await fetch(getApiUrl(`/api/participant/candidate-applications?userId=${user.id}`));
      if (!response.ok) {
        throw new Error('Failed to fetch candidate applications from database');
      }
      
      const applications = await response.json();
      console.log('Fetched candidate applications from database:', applications);
      
      const status = {};
      
      applications.forEach(application => {
        const applicationStatus = application.status ? application.status.toUpperCase() : 'PENDING';
        // Map to all elections since candidate applications are global
        allElections.forEach(election => {
          status[election.id] = {
            status: applicationStatus,
            message: applicationStatus === 'PENDING' ? 
              'Your candidate application is pending approval by admin' :
              applicationStatus === 'APPROVED' ?
              'Your candidate application has been approved! You can now participate in elections.' :
              applicationStatus === 'REJECTED' ?
              'Your candidate application was rejected' :
              `Application status: ${application.status || 'PENDING'}`
          };
        });
        
        console.log(`✅ Found application: User ${user.email} application status is ${applicationStatus}`);
      });
      
      // Update enrollment status with candidate application status
      setEnrollmentStatus(prevStatus => ({ ...prevStatus, ...status }));
      
    } catch (error) {
      console.error('Error fetching candidate applications from database:', error);
      
      // If database fails, clear enrollment status
      setEnrollmentStatus({});
    }
  }, [allElections, user]);

  // Check candidate application status when elections or user changes
  useEffect(() => {
    if (allElections.length > 0 && user) {
      checkCandidateApplicationStatus();
    }
  }, [allElections, user, checkCandidateApplicationStatus]); // Check when elections or user changes

  // Profile management functions
  useEffect(() => {
    if (user) {
      setProfile({
        fullName: user.fullName || '',
        email: user.email || '',
        age: user.age || '',
        phoneNumber: user.phoneNumber || '',
        idProofNumber: user.idProofNumber || '',
        address: user.address || '',
        biography: '',
        partyName: '',
        partySymbol: ''
      });
    }
  }, [user]);

  const handleProfileUpdate = useCallback((field, value) => {
    setProfile(prev => ({ ...prev, [field]: value }));
  }, []);

  const handleImageUpload = useCallback((type, file) => {
    if (!file) return;

    const reader = new FileReader();
    reader.onloadend = () => {
      if (type === 'profile') {
        setProfileImage(file);
        setProfileImagePreview(reader.result);
      } else if (type === 'party') {
        setPartySymbol(file);
        setPartySymbolPreview(reader.result);
      }
    };
    reader.readAsDataURL(file);
  }, []);

  const saveProfile = async () => {
    if (!profile.fullName.trim()) {
      setProfileError('Full name is required');
      return;
    }

    setProfileLoading(true);
    setProfileError('');
    setProfileSuccess('');

    try {
      // Create profile update request
      const updateRequest = {
        fullName: profile.fullName,
        age: profile.age ? parseInt(profile.age) : null,
        phoneNumber: profile.phoneNumber,
        idProofNumber: profile.idProofNumber,
        address: profile.address
      };

      // Update profile via correct API endpoint
      const response = await fetch(getApiUrl(`/api/auth/profile?email=${encodeURIComponent(user.email)}`), {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(updateRequest)
      });

      if (response.ok) {
        const updatedUser = await response.json();
        setProfile(prev => ({
          ...prev,
          fullName: updatedUser.fullName,
          email: updatedUser.email,
          age: updatedUser.age || '',
          phoneNumber: updatedUser.phoneNumber || '',
          idProofNumber: updatedUser.idProofNumber || '',
          address: updatedUser.address || ''
        }));
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
  
  // Filter for elections this user is part of and upcoming ones they can join
  const enrolledElections = allElections.filter(e => e.participants && e.participants.includes(user.id));
  const upcomingElections = allElections.filter(e => getElectionStatus(e) === 'Upcoming');

  // Home Module Component
  const HomeModule = () => (
    <div className="candidate-module">
      <div className="content-header">
        <h1 className="content-title">Welcome, {user?.fullName || user?.username || 'Candidate'}!</h1>
        <p className="content-subtitle">Your Candidate Portal Dashboard</p>
      </div>
      <div className="module-content">
        <div className="candidate-welcome-card">
          <div className="welcome-icon">
            <FaUser />
          </div>
          <div className="welcome-text">
            <h3>Candidate Dashboard</h3>
            <p>Welcome to your candidate portal. Here you can manage your profile, participate in elections, run campaigns, and track your performance.</p>
          </div>
        </div>
        
        <div className="dashboard-overview">
          <div className="overview-card">
            <div className="overview-icon">
              <FaVoteYea />
            </div>
            <div className="overview-text">
              <h3>{enrolledElections.length}</h3>
              <p>Enrolled Elections</p>
            </div>
          </div>
          
          <div className="overview-card">
            <div className="overview-icon">
              <FaCalendarAlt />
            </div>
            <div className="overview-text">
              <h3>{upcomingElections.length}</h3>
              <p>Upcoming Elections</p>
            </div>
          </div>
        </div>

        <div className="dashboard-actions">
          <button 
            className="btn btn-primary" 
            onClick={() => setActiveTab('elections')}
          >
            <FaVoteYea /> Apply for Elections
          </button>
          <button 
            className="btn btn-secondary" 
            onClick={() => setActiveTab('profile')}
          >
            <FaUserEdit /> Update Profile
          </button>
          <button 
            className="btn btn-info"
            onClick={() => setActiveTab('campaign')}
          >
            <FaTools /> Campaign Tools
          </button>
        </div>
      </div>
    </div>
  );

  // Authentication Module Component
  const AuthenticationModule = () => (
    <div className="candidate-module">
      <div className="content-header">
        <h1 className="content-title">Authentication & Security</h1>
        <p className="content-subtitle">Manage your account security settings</p>
      </div>
      <div className="module-content">
        <div className="auth-card">
          <h4><FaLock /> Account Security</h4>
          <div className="auth-settings">
            <div className="auth-setting-item">
              <div className="setting-label">
                <strong>Two-Factor Authentication</strong>
                <p>Add an extra layer of security to your account</p>
              </div>
              <div className="setting-toggle">
                <button className="btn btn-secondary">Enable 2FA</button>
              </div>
            </div>
            
            <div className="auth-setting-item">
              <div className="setting-label">
                <strong>Password</strong>
                <p>Last changed 30 days ago</p>
              </div>
              <div className="setting-toggle">
                <button className="btn btn-secondary">Change Password</button>
              </div>
            </div>
          </div>
          
          <div className="auth-actions">
            <button className="btn btn-warning">Change Password</button>
            <button className="btn btn-info">Enable 2FA</button>
          </div>
        </div>
      </div>
    </div>
  );

  // Profile Management Module Component
  const ProfileManagementModule = React.memo(() => {
    return (
      <ProfileManagementStable
        profile={profile}
        isProfileEditing={isProfileEditing}
        profileLoading={profileLoading}
        profileError={profileError}
        profileSuccess={profileSuccess}
        onToggleEdit={() => setIsProfileEditing(!isProfileEditing)}
        onFieldChange={handleProfileUpdate}
        onSave={saveProfile}
        onCancel={() => {
          setIsProfileEditing(false);
          setProfileError('');
          setProfileSuccess('');
          setProfile({
            fullName: user?.fullName || '',
            email: user?.email || '',
            age: user?.age || '',
            phoneNumber: user?.phoneNumber || '',
            idProofNumber: user?.idProofNumber || '',
            address: user?.address || '',
            biography: '',
            partyName: '',
            partySymbol: ''
          });
        }}
      />
    );
  });

  // Election Participation Module Component
  const ElectionParticipationModule = () => {
    return (
    <div className="candidate-module">
      <div className="content-header">
        <h1 className="content-title">Election Participation</h1>
        <p className="content-subtitle">Apply for elections and manage your candidacies</p>
        <div className="header-actions">
          <button 
            className="btn btn-secondary" 
            onClick={() => {
              fetchElectionsFromBackend();
              checkCandidateApplicationStatus();
            }}
            disabled={isRefreshing}
            title="Refresh elections and application status"
          >
            <FaSync className={isRefreshing ? 'spinning' : ''} /> 
            {isRefreshing ? 'Refreshing...' : 'Refresh All'}
          </button>
        </div>
      </div>
      <div className="module-content">
        <div className="election-sections">
          <div className="election-section">
            <h4><FaVoteYea /> Available Elections</h4>

            <div className="elections-grid">
              {allElections && allElections.length > 0 ? (
                  allElections.map(election => {
                    // Get enrollment status for this election
                    const status = enrollmentStatus[election.id];
                    
                    // Check different statuses
                    const isApproved = status && status.status === 'APPROVED';
                    const isPending = status && status.status === 'PENDING';
                    const isRejected = status && status.status === 'REJECTED';
                    
                    return (
                      <div key={election.id} className="election-card">
                        <div className="election-info">
                          <h5>{election.title}</h5>
                          <p>Starts: {new Date(election.startDate).toLocaleString()}</p>
                          <p>Ends: {new Date(election.endDate).toLocaleString()}</p>
                          <p>Status: {getElectionStatus(election)}</p>
                        </div>
                        <div className="election-status">
                          <span className={`status ${
                            isApproved ? 'approved' : 
                            isPending ? 'pending' : 
                            isRejected ? 'rejected' : 'open'
                          }`}>
                            {isApproved ? 'Enrolled' : 
                             isPending ? 'Pending Approval' : 
                             isRejected ? 'Application Rejected' : 'Open for Registration'}
                          </span>
                          
                          {status && <p className="status-message">{status.message}</p>}
                          
                          <button 
                            className={`btn ${
                              isApproved || isPending ? 'btn-secondary' : 
                              isRejected ? 'btn-warning' : 'btn-primary'
                            }`}
                            onClick={() => handleEnroll(election.id)}
                            disabled={isApproved || isPending}
                          >
                            {isApproved ? '✓ Enrolled' : 
                             isPending ? '⏳ Pending' : 
                             isRejected ? 'Re-Apply' : 'Apply Now'}
                          </button>
                        </div>
                      </div>
                    );
                  })
              ) : (
                <p>No elections available for registration.</p>
              )}
            </div>
          </div>

          <div className="election-section">
            <h4><FaClipboardList /> My Applications</h4>
            <div className="applications-table">
              <table className="table">
                <thead>
                  <tr>
                    <th>Election</th>
                    <th>Applied Date</th>
                    <th>Status</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {enrolledElections.map(election => (
                    <tr key={election.id}>
                      <td>{election.title}</td>
                      <td>{new Date().toLocaleDateString()}</td>
                      <td><span className="status approved">Approved</span></td>
                      <td>
                        <button className="btn btn-sm btn-info">View Details</button>
                      </td>
                    </tr>
                  ))}
                  {enrolledElections.length === 0 && (
                    <tr>
                      <td colSpan="4" style={{ textAlign: 'center' }}>No applications yet</td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>

          <div className="election-section">
            <h4><FaFileAlt /> Eligibility Requirements</h4>
            <div className="requirements-checklist">
              <div className="requirement-item completed">
                <FaCheckCircle />
                <span>Age requirement (18+ years)</span>
              </div>
              <div className="requirement-item completed">
                <FaCheckCircle />
                <span>Identity verification</span>
              </div>
              <div className="requirement-item pending">
                <FaClock />
                <span>Criminal background check</span>
              </div>
              <div className="requirement-item pending">
                <FaClock />
                <span>Financial disclosure</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
  };

  // Campaign Tools Module Component
  const CampaignToolsModule = () => (
    <div className="candidate-module">
      <div className="content-header">
        <h1 className="content-title">Campaign Tools</h1>
        <p className="content-subtitle">Manage your campaign materials and communications</p>
      </div>
      <div className="module-content">
        <div className="campaign-sections">
          <div className="campaign-section">
            <h4><FaUserTie /> Party Details</h4>
            <div className="party-details-form">
              <div className="form-row">
                <div className="form-group">
                  <label>Party Name</label>
                  <input 
                    type="text" 
                    id="partyName"
                    placeholder="Enter your party name"
                    defaultValue={profile.partyName || ''}
                    className="form-input"
                  />
                </div>
                <div className="form-group">
                  <label>Party Symbol</label>
                  <div className="symbol-upload">
                    <input 
                      type="file" 
                      id="partySymbol"
                      accept="image/*"
                      style={{display: 'none'}}
                      onChange={(e) => {
                        const file = e.target.files[0];
                        if (file) {
                          if (!validateFileSize(file, 5)) {
                            alert('File size must be less than 5MB');
                            return;
                          }
                          if (!validateFileType(file, ['image/jpeg', 'image/png', 'image/gif'])) {
                            alert('Please select a valid image file (JPEG, PNG, GIF)');
                            return;
                          }
                          
                          alert('Party symbol uploaded successfully!');
                          handleImageUpload('party', file);
                        }
                      }}
                    />
                    <button 
                      className="btn btn-secondary"
                      onClick={() => document.getElementById('partySymbol').click()}
                    >
                      Upload Symbol
                    </button>
                  </div>
                </div>
              </div>
              <div className="form-actions">
                <button 
                  className="btn btn-primary"
                  onClick={async () => {
                    const partyName = sanitizeInput(document.getElementById('partyName').value);
                    if (!validateRequired(partyName)) {
                      alert('Please enter a party name');
                      return;
                    }
                    if (partyName.length > 100) {
                      alert('Party name must be less than 100 characters');
                      return;
                    }
                    
                    try {
                      alert('Party details saved successfully!');
                      setProfile(prev => ({ ...prev, partyName: partyName }));
                    } catch (error) {
                      alert('Error saving party details: ' + error.message);
                    }
                  }}
                >
                  Save Party Details
                </button>
              </div>
            </div>
          </div>

          <div className="campaign-section">
            <h4><FaFileAlt /> Manifesto</h4>
            <div className="manifesto-upload">
              <div className="upload-area large">
                <FaUpload />
                <h5>Upload Your Manifesto</h5>
                <p>Share your vision and promises with voters</p>
                <button className="btn btn-primary" onClick={() => {
                  const input = document.createElement('input');
                  input.type = 'file';
                  input.accept = '.pdf,.doc,.docx';
                  input.onchange = (e) => {
                    const file = e.target.files[0];
                    if (file) {
                      // Validate file
                      if (file.size > 10 * 1024 * 1024) {
                        alert('File size must be less than 10MB');
                        return;
                      }
                      
                      alert(`Manifesto "${file.name}" uploaded successfully!`);
                    }
                  };
                  input.click();
                }}>Upload Manifesto</button>
              </div>
            </div>
          </div>

          <div className="campaign-section">
            <h4><FaBullhorn /> Announcements</h4>
            <div className="announcement-creator">
              <textarea 
                id="announcementText"
                rows="4" 
                placeholder="Create an announcement for your supporters..."
                className="announcement-textarea"
              ></textarea>
              <div className="announcement-actions">
                <button 
                  className="btn btn-primary" 
                  onClick={() => {
                    const text = document.getElementById('announcementText').value;
                    if (!text.trim()) {
                      alert('Please enter an announcement message');
                      return;
                    }
                    if (text.length > 500) {
                      alert('Announcement must be less than 500 characters');
                      return;
                    }
                    
                    alert('Announcement posted successfully!');
                    document.getElementById('announcementText').value = '';
                  }}
                >
                  Post Announcement
                </button>
              </div>
            </div>
          </div>

          <div className="campaign-section">
            <h4><FaBullhorn /> Campaign Materials</h4>
            <div className="materials-grid">
              <div className="material-upload" onClick={() => {
                const input = document.createElement('input');
                input.type = 'file';
                input.accept = 'image/*';
                input.multiple = true;
                input.onchange = (e) => {
                  let successCount = 0;
                  let errorCount = 0;
                  
                  for (const file of e.target.files) {
                    if (file.size > 5 * 1024 * 1024) {
                      alert(`${file.name} is too large. Maximum size is 5MB.`);
                      errorCount++;
                      continue;
                    }
                    if (!file.type.startsWith('image/')) {
                      alert(`${file.name} is not a valid image file.`);
                      errorCount++;
                      continue;
                    }
                    successCount++;
                  }
                  
                  if (successCount > 0) {
                    alert(`${successCount} poster(s) uploaded successfully!`);
                  }
                };
                input.click();
              }}>
                <FaImage />
                <p>Upload Posters</p>
              </div>
              
              <div className="material-upload" onClick={() => {
                const input = document.createElement('input');
                input.type = 'file';
                input.accept = '.pdf';
                input.multiple = true;
                input.onchange = (e) => {
                  let successCount = 0;
                  let errorCount = 0;
                  
                  for (const file of e.target.files) {
                    if (file.size > 10 * 1024 * 1024) {
                      alert(`${file.name} is too large. Maximum size is 10MB.`);
                      errorCount++;
                      continue;
                    }
                    if (file.type !== 'application/pdf') {
                      alert(`${file.name} is not a PDF file.`);
                      errorCount++;
                      continue;
                    }
                    successCount++;
                  }
                  
                  if (successCount > 0) {
                    alert(`${successCount} brochure(s) uploaded successfully!`);
                  }
                };
                input.click();
              }}>
                <FaFileAlt />
                <p>Upload Brochures</p>
              </div>
              
              <div className="material-upload" onClick={() => {
                const input = document.createElement('input');
                input.type = 'file';
                input.accept = '.pdf,.doc,.docx';
                input.multiple = true;
                input.onchange = (e) => {
                  let successCount = 0;
                  let errorCount = 0;
                  
                  for (const file of e.target.files) {
                    if (file.size > 5 * 1024 * 1024) {
                      alert(`${file.name} is too large. Maximum size is 5MB.`);
                      errorCount++;
                      continue;
                    }
                    const validTypes = ['application/pdf', 'application/msword', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'];
                    if (!validTypes.includes(file.type)) {
                      alert(`${file.name} is not a valid document file.`);
                      errorCount++;
                      continue;
                    }
                    successCount++;
                  }
                  
                  if (successCount > 0) {
                    alert(`${successCount} volunteer form(s) uploaded successfully!`);
                  }
                };
                input.click();
              }}>
                <FaUsers />
                <p>Volunteer Forms</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );

  // Notifications Module Component
  const NotificationsModule = () => (
    <div className="candidate-module">
      <div className="content-header">
        <h1 className="content-title">Notifications</h1>
        <p className="content-subtitle">Stay updated with important information</p>
      </div>
      <div className="module-content">
        <div className="notifications-sections">
          <div className="notification-filters">
            <button className="filter-btn active">All</button>
            <button className="filter-btn">Applications</button>
            <button className="filter-btn">Elections</button>
            <button className="filter-btn">Campaign</button>
          </div>

          <div className="notifications-list">
            <div className="notification-item unread">
              <div className="notification-icon election">
                <FaVoteYea />
              </div>
              <div className="notification-content">
                <h5>Application Approved</h5>
                <p>Your application for {enrolledElections[0]?.title || 'Presidential Election 2024'} has been approved.</p>
                <span className="notification-time">2 hours ago</span>
              </div>
              <div className="notification-actions">
                <button className="btn btn-sm btn-secondary">Mark as Read</button>
              </div>
            </div>
            
            <div className="notification-item">
              <div className="notification-icon system">
                <FaCog />
              </div>
              <div className="notification-content">
                <h5>Profile Update Required</h5>
                <p>Please complete your profile by adding your party information.</p>
                <span className="notification-time">1 day ago</span>
              </div>
              <div className="notification-actions">
                <button className="btn btn-sm btn-primary">Update Profile</button>
              </div>
            </div>
            
            <div className="notification-item">
              <div className="notification-icon campaign">
                <FaBullhorn />
              </div>
              <div className="notification-content">
                <h5>New Election Announced</h5>
                <p>A new election has been announced. Check eligibility and apply now.</p>
                <span className="notification-time">3 days ago</span>
              </div>
              <div className="notification-actions">
                <button className="btn btn-sm btn-primary">View Details</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );

  // Results Module Component
  const ResultsModule = () => (
    <div className="candidate-module">
      <div className="content-header">
        <h1 className="content-title">Results & Reports</h1>
        <p className="content-subtitle">View election results and download reports</p>
      </div>
      <div className="module-content">
        <div className="results-sections">
          <div className="results-section">
            <h4><FaChartBar /> Election Results</h4>
            <div className="results-cards">
              {enrolledElections.length > 0 ? (
                enrolledElections.map(election => (
                  <div key={election.id} className="result-card">
                    <h5>{election.title}</h5>
                    <div className="result-status">
                      <span className={`status ${getElectionStatus(election).toLowerCase()}`}>
                        {getElectionStatus(election)}
                      </span>
                    </div>
                    <div className="result-stats">
                      <div className="stat-item">
                        <label>Position:</label>
                        <span>2nd</span>
                      </div>
                      <div className="stat-item">
                        <label>Votes:</label>
                        <span>1,245</span>
                      </div>
                      <div className="stat-item">
                        <label>Vote %:</label>
                        <span>32.5%</span>
                      </div>
                    </div>
                    <div className="result-actions">
                      <button className="btn btn-info">
                        <FaEye /> View Details
                      </button>
                      <button className="btn btn-secondary">
                        <FaDownload /> Download Report
                      </button>
                    </div>
                  </div>
                ))
              ) : (
                <p>No election results available yet.</p>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );

  // Sidebar navigation
  return (
    <div className="participant-dashboard">
      <aside className="sidebar">
        <div className="sidebar-header">
          <h1>VoteRow Candidate</h1>
          <div className="candidate-portal-text">Campaign Portal</div>
        </div>
        
        <div className="user-info">
          <div className="user-avatar">
            {profileImagePreview ? (
              <img src={profileImagePreview} alt={user.fullName} />
            ) : (
              <span>{user?.fullName?.charAt(0) || user?.email?.charAt(0) || 'C'}</span>
            )}
          </div>
          <div className="user-details">
            <h3>{user?.fullName || user?.email}</h3>
            <span className="user-role">Candidate</span>
          </div>
        </div>
        
        <nav className="sidebar-nav">
          <ul>
            {tabs.map(tab => (
              <li 
                key={tab.id} 
                className={activeTab === tab.id ? 'active' : ''}
                onClick={() => setActiveTab(tab.id)}
              >
                <tab.icon className="nav-icon" />
                <span>{tab.label}</span>
              </li>
            ))}
          </ul>
        </nav>
      </aside>
      
      <main className="content">
        {loading && (
          <div className="loading-overlay">
            <div className="spinner"></div>
            <p>Loading...</p>
          </div>
        )}
        
        {activeTab === 'home' && <HomeModule />}
        {activeTab === 'profile' && <ProfileManagementModule />}
        {activeTab === 'authentication' && <AuthenticationModule />}
        {activeTab === 'elections' && <ElectionParticipationModule />}
        {activeTab === 'campaign' && <CampaignToolsModule />}
        {activeTab === 'notifications' && <NotificationsModule />}
        {activeTab === 'results' && <ResultsModule />}
      </main>
    </div>
  );
};

export default ParticipantDashboard;