import React, { useState, useEffect, useCallback } from 'react';
import { 
  FaUser, FaIdCard, FaVoteYea, FaTools, FaBell, FaChartBar, 
  FaHome, FaUserEdit, FaImage, FaClipboardList, FaBullhorn, 
  FaFileAlt, FaCalendarAlt, FaDownload, FaEye, FaUpload, FaTrash, 
  FaCheckCircle, FaTimesCircle, FaClock, FaUsers, FaLock,
  FaCog, FaSync, FaUserTie
} from 'react-icons/fa';
import { getApiUrl } from '../config/apiConfig';
import secureStorage from '../utils/secureStorage';
import { sanitizeInput, validatePassword, validateRequired, validateFileSize, validateFileType } from '../utils/validation';
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

function PasswordChangeModal({
  isOpen,
  isLoading,
  error,
  success,
  form,
  onChange,
  onClose,
  onSubmit,
}) {
  if (!isOpen) {
    return null;
  }

  return (
    <div className="modal-overlay">
      <div className="modal-content password-modal">
        <div className="modal-header">
          <h4><FaLock /> Change Password</h4>
          <button type="button" className="modal-close" onClick={onClose}>×</button>
        </div>
        <div className="modal-body">
          {error && <div className="alert alert-error">{error}</div>}
          {success && <div className="alert alert-success">{success}</div>}
          <div className="form-grid">
            <div className="form-group">
              <label>Current Password</label>
              <input
                type="password"
                value={form.currentPassword}
                onChange={(event) => onChange('currentPassword', event.target.value)}
                placeholder="Enter your current password"
                autoComplete="current-password"
              />
            </div>
            <div className="form-group">
              <label>New Password</label>
              <input
                type="password"
                value={form.newPassword}
                onChange={(event) => onChange('newPassword', event.target.value)}
                placeholder="Enter a new password"
                autoComplete="new-password"
              />
            </div>
            <div className="form-group">
              <label>Confirm New Password</label>
              <input
                type="password"
                value={form.confirmPassword}
                onChange={(event) => onChange('confirmPassword', event.target.value)}
                placeholder="Repeat the new password"
                autoComplete="new-password"
              />
            </div>
          </div>
        </div>
        <div className="modal-actions">
          <button type="button" className="btn btn-secondary" onClick={onClose} disabled={isLoading}>
            Cancel
          </button>
          <button type="button" className="btn btn-warning" onClick={onSubmit} disabled={isLoading}>
            {isLoading ? 'Updating...' : 'Update Password'}
          </button>
        </div>
      </div>
    </div>
  );
}

function AuthenticationModuleStable({ onOpenPasswordModal, authSuccess }) {
  return (
    <div className="candidate-module">
      <div className="content-header">
        <h1 className="content-title">Authentication & Security</h1>
        <p className="content-subtitle">Manage your account security settings</p>
      </div>
      <div className="module-content">
        {authSuccess && <div className="alert alert-success">{authSuccess}</div>}
        <div className="auth-card">
          <h4><FaLock /> Account Security</h4>
          <div className="auth-settings">
            <div className="auth-setting-item">
              <div className="setting-label">
                <strong>Two-Factor Authentication</strong>
                <p>Add an extra layer of security to your account</p>
              </div>
              <div className="setting-toggle">
                <button type="button" className="btn btn-secondary" onClick={onOpenPasswordModal}>Change Password</button>
              </div>
            </div>

            <div className="auth-setting-item">
              <div className="setting-label">
                <strong>Password</strong>
                <p>Keep your account protected with a strong password</p>
              </div>
              <div className="setting-toggle">
                <button type="button" className="btn btn-secondary" onClick={onOpenPasswordModal}>Change Password</button>
              </div>
            </div>
          </div>

          <div className="auth-actions">
            <button type="button" className="btn btn-warning" onClick={onOpenPasswordModal}>Change Password</button>
          </div>
        </div>
      </div>
    </div>
  );
}

function CampaignToolsStable({
  campaignMaterials,
  campaignCandidateId,
  campaignElectionId,
  campaignLoading,
  campaignError,
  campaignSuccess,
  campaignForm,
  campaignElectionOptions,
  selectedCampaignElection,
  onElectionChange,
  onMaterialTypeChange,
  onTitleChange,
  onDescriptionChange,
  onFilePick,
  onRefreshMaterials,
  onUploadMaterial,
  onPostAnnouncement,
  onDeleteMaterial,
  onDownloadMaterial,
}) {
  const [previewUrls, setPreviewUrls] = useState({});

  useEffect(() => {
    let cancelled = false;
    const createdUrls = [];

    const loadPreviews = async () => {
      const imageMaterials = campaignMaterials.filter((material) => {
        return typeof material?.mimeType === 'string' && material.mimeType.startsWith('image/') && material.id;
      });

      const nextPreviewUrls = {};

      await Promise.all(imageMaterials.map(async (material) => {
        try {
          const token = secureStorage.getToken();
          const response = await fetch(getApiUrl(`/api/campaign-materials/download/${material.id}`), {
            headers: {
              ...(token ? { Authorization: `Bearer ${token}` } : {})
            }
          });

          if (!response.ok) {
            return;
          }

          const blob = await response.blob();
          const objectUrl = window.URL.createObjectURL(blob);
          nextPreviewUrls[material.id] = objectUrl;
          createdUrls.push(objectUrl);
        } catch {
          // Keep the textual card visible if preview generation fails.
        }
      }));

      if (!cancelled) {
        setPreviewUrls(nextPreviewUrls);
      }
    };

    loadPreviews();

    return () => {
      cancelled = true;
      createdUrls.forEach((url) => window.URL.revokeObjectURL(url));
    };
  }, [campaignMaterials]);

  return (
    <div className="candidate-module">
      <div className="content-header">
        <h1 className="content-title">Campaign Tools</h1>
        <p className="content-subtitle">Manage campaign materials for the selected election</p>
      </div>
      <div className="module-content">
        {campaignError && <div className="alert alert-error">{campaignError}</div>}
        {campaignSuccess && <div className="alert alert-success">{campaignSuccess}</div>}

        <div className="campaign-sections">
          <div className="campaign-section">
            <h4><FaVoteYea /> Election Context</h4>
            <div className="form-grid">
              <div className="form-group">
                <label>Election</label>
                <select
                  value={campaignElectionId}
                  onChange={(event) => onElectionChange(event.target.value)}
                >
                  {campaignElectionOptions.length === 0 && <option value="">No election available</option>}
                  {campaignElectionOptions.map((election) => (
                    <option key={election.id} value={election.id}>
                      {election.title}
                    </option>
                  ))}
                </select>
              </div>
              <div className="form-group">
                <label>Candidate Record</label>
                <input
                  type="text"
                  value={campaignCandidateId ? `Candidate #${campaignCandidateId}` : 'Not resolved yet'}
                  readOnly
                />
              </div>
            </div>
            <div className="form-actions">
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => onRefreshMaterials(campaignElectionId || selectedCampaignElection?.id)}
                disabled={campaignLoading || !campaignElectionId}
              >
                <FaSync /> Refresh Materials
              </button>
            </div>
          </div>

          <div className="campaign-section">
            <h4><FaUpload /> Upload Campaign Material</h4>
            <div className="campaign-upload-form">
              <div className="form-grid">
                <div className="form-group">
                  <label>Material Type</label>
                  <select
                    value={campaignForm.materialType}
                    onChange={(event) => onMaterialTypeChange(event.target.value)}
                  >
                    <option value="POSTER">Poster</option>
                    <option value="BROCHURE">Brochure</option>
                    <option value="MANIFESTO">Manifesto</option>
                    <option value="VOLUNTEER_FORM">Volunteer Form</option>
                    <option value="SYMBOL">Symbol</option>
                    <option value="OTHER">Other</option>
                  </select>
                </div>
                <div className="form-group">
                  <label>Title</label>
                  <input
                    type="text"
                    value={campaignForm.title}
                    onChange={(event) => onTitleChange(event.target.value)}
                    placeholder="Enter a title"
                  />
                </div>
                <div className="form-group">
                  <label>Description</label>
                  <textarea
                    rows="4"
                    value={campaignForm.description}
                    onChange={(event) => onDescriptionChange(event.target.value)}
                    placeholder="Optional details for the material"
                  />
                </div>
                <div className="form-group">
                  <label>File</label>
                  <input type="file" onChange={onFilePick} />
                </div>
              </div>
              <div className="form-actions">
                <button
                  type="button"
                  className="btn btn-primary"
                  onClick={onUploadMaterial}
                  disabled={campaignLoading || !campaignElectionId}
                >
                  {campaignLoading ? 'Uploading...' : 'Upload Material'}
                </button>
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={onPostAnnouncement}
                  disabled={campaignLoading || !campaignElectionId}
                >
                  Post Announcement
                </button>
              </div>
            </div>
          </div>

          <div className="campaign-section">
            <h4><FaClipboardList /> Published Materials</h4>
            <p className="campaign-summary">
              Showing submissions for {selectedCampaignElection?.title || 'the selected election'}.
            </p>
            {campaignLoading && campaignMaterials.length === 0 ? (
              <p>Loading campaign materials...</p>
            ) : campaignMaterials.length === 0 ? (
              <p>No campaign materials uploaded yet.</p>
            ) : (
              <div className="materials-grid">
                {campaignMaterials.map((material) => (
                  <div key={material.id} className="material-card">
                    <div className="material-preview">
                      {previewUrls[material.id] ? (
                        <img
                          src={previewUrls[material.id]}
                          alt={material.title || material.materialType}
                          className="material-preview-image"
                        />
                      ) : (
                        <div className="material-preview-placeholder">
                          <FaImage />
                          <span>{material.materialType || 'Material'}</span>
                        </div>
                      )}
                    </div>
                    <div className="material-card-header">
                      <h5>{material.title}</h5>
                      <span className={`status ${(material.isApproved ? 'approved' : 'pending')}`}>
                        {material.isApproved ? 'Approved' : 'Pending approval'}
                      </span>
                    </div>
                    <p><strong>Type:</strong> {material.materialType}</p>
                    <p><strong>Uploaded:</strong> {material.uploadDate ? new Date(material.uploadDate).toLocaleString() : 'Unknown'}</p>
                    {material.fileName && <p><strong>File:</strong> {material.fileName}</p>}
                    {material.description && <p>{material.description}</p>}
                    {material.rejectionReason && <p><strong>Rejection:</strong> {material.rejectionReason}</p>}
                    <div className="result-actions">
                      <button type="button" className="btn btn-info" onClick={() => onDownloadMaterial(material)}>
                        <FaDownload /> Download
                      </button>
                      <button type="button" className="btn btn-secondary" onClick={() => onDeleteMaterial(material.id)}>
                        <FaTrash /> Delete
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

const ParticipantDashboard = ({ user }) => {
  const [activeTab, setActiveTab] = useState('home');
  const [loading] = useState(false);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [allElections, setAllElections] = useState([]);
  const [enrollmentStatus, setEnrollmentStatus] = useState({});
  const [candidateApplications, setCandidateApplications] = useState([]);
  
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
  const [, setProfileImage] = useState(null);
  const [profileImagePreview, setProfileImagePreview] = useState('');
  const [, setPartySymbol] = useState(null);
  const [, setPartySymbolPreview] = useState('');

  // Campaign tools state
  const [campaignMaterials, setCampaignMaterials] = useState([]);
  const [campaignCandidateId, setCampaignCandidateId] = useState(null);
  const [campaignElectionId, setCampaignElectionId] = useState('');
  const [campaignLoading, setCampaignLoading] = useState(false);
  const [campaignError, setCampaignError] = useState('');
  const [campaignSuccess, setCampaignSuccess] = useState('');
  const [showPasswordModal, setShowPasswordModal] = useState(false);
  const [passwordLoading, setPasswordLoading] = useState(false);
  const [passwordError, setPasswordError] = useState('');
  const [passwordSuccess, setPasswordSuccess] = useState('');
  const [passwordForm, setPasswordForm] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: ''
  });
  const [campaignForm, setCampaignForm] = useState({
    materialType: 'POSTER',
    title: '',
    description: '',
    file: null
  });

  const getAuthHeaders = useCallback((extraHeaders = {}) => {
    const token = secureStorage.getToken();
    return {
      'Content-Type': 'application/json',
      ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
      ...extraHeaders
    };
  }, []);

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
    if (election.status) {
      switch (election.status.toUpperCase()) {
        case 'ACTIVE':
        case 'ONGOING':
          return 'Active';
        case 'SCHEDULED':
          return 'Upcoming';
        case 'COMPLETED':
        case 'RESULTS_PUBLISHED':
          return 'Completed';
        case 'DRAFT':
          return 'Draft';
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
    if (isRefreshing) return null; // Prevent multiple simultaneous calls
    
    try {
      setIsRefreshing(true);
      console.log('ParticipantDashboard: Fetching elections from participant endpoint...');
      const response = await fetch(getApiUrl('/api/participant/elections'), {
        method: 'GET',
        headers: getAuthHeaders({
          'Cache-Control': 'no-cache'
        })
      });
      
      if (response.ok) {
        const backendElections = await response.json();
        console.log('ParticipantDashboard: Successfully fetched elections from participant endpoint:', backendElections.length);
        
        const electionsWithParticipants = backendElections.map(election => ({
          ...election,
          participants: election.participants || [],
          participantUserIds: election.participantUserIds || [],
          enrollmentRequests: election.enrollmentRequests || []
        }));
        
        setAllElections(electionsWithParticipants);
        return electionsWithParticipants;
      } else {
        console.error('ParticipantDashboard: Failed to fetch from participant endpoint, status:', response.status);
        setAllElections([]);
        return [];
      }
    } catch (error) {
      console.error('ParticipantDashboard: Error connecting to participant endpoint:', error);
      setAllElections([]);
      return [];
    } finally {
      setIsRefreshing(false);
    }
  }, [getAuthHeaders, isRefreshing]);

  // Load elections on component mount - backend only
  useEffect(() => {
    fetchElectionsFromBackend();
  }, [user]);
  
  const handleEnroll = async (electionId) => {
    try {
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
        headers: getAuthHeaders(),
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
        
        // Refresh both elections and application status so admin approvals reflect immediately
        const latestElections = await fetchElectionsFromBackend();
        await checkCandidateApplicationStatus(latestElections || allElections);
      } else {
        const errorText = await response.text();
        console.error('ParticipantDashboard: Raw error response:', errorText);
        
        let errorMessage = 'Failed to submit application';
        
        try {
          const errorData = JSON.parse(errorText);
          errorMessage = errorData.error || errorData.message || errorMessage;
        } catch {
          errorMessage = errorText || errorMessage;
        }
        
        console.error('ParticipantDashboard: Parsed error message:', errorMessage);
        throw new Error(errorMessage);
      }
      
    } catch (error) {
      console.error('Error submitting enrollment:', error);
      alert(error.message || 'There was an error submitting your application. Please try again.');
      
      // Reset status on error
      setEnrollmentStatus(prev => ({
        ...prev,
        [electionId]: { status: 'ERROR', message: error.message || 'Application failed, please try again' }
      }));
    }
  };

  // Function to check candidate application status from database
  const checkCandidateApplicationStatus = useCallback(async (electionsOverride = allElections) => {
    if (!user) return;
    
    try {
      console.log('Fetching candidate applications from database for user:', user.id);
      const electionsToUse = Array.isArray(electionsOverride) ? electionsOverride : [];
      
      // Fetch candidate applications from participant endpoint
      const response = await fetch(
        getApiUrl(`/api/participant/candidate-applications?userId=${user.id}`),
        {
          headers: getAuthHeaders({
            'Cache-Control': 'no-cache'
          })
        }
      );
      if (!response.ok) {
        throw new Error('Failed to fetch candidate applications from database');
      }
      
      const applications = await response.json();
      console.log('Fetched candidate applications from database:', applications);
      
      const status = {};
      const applicationsByElection = new Map();
      
      applications.forEach(application => {
        const applicationStatus = application.status ? application.status.toUpperCase() : 'PENDING';
        if (application.electionId != null) {
          status[application.electionId] = {
            status: applicationStatus,
            message: applicationStatus === 'PENDING' ? 
              'Your application is pending approval by admin' :
              applicationStatus === 'APPROVED' ?
              'Your application has been approved. You can continue in the campaign portal.' :
              applicationStatus === 'REJECTED' ?
              'Your application was rejected' :
              `Application status: ${application.status || 'PENDING'}`,
            submittedAt: application.submittedAt || ''
          };
          applicationsByElection.set(application.electionId, {
            ...application,
            status: applicationStatus,
            electionTitle:
              application.electionTitle ||
              electionsToUse.find(election => election.id === application.electionId)?.title ||
              'Unknown Election'
          });
        } else {
          applicationsByElection.set(`application-${application.id}`, {
            ...application,
            status: applicationStatus
          });
        }
        
        console.log(`✅ Found application: User ${user.email} application status is ${applicationStatus}`);
      });
      
      electionsToUse.forEach(election => {
        const participantUserIds = Array.isArray(election.participantUserIds) ? election.participantUserIds : [];
        if (!participantUserIds.includes(user.id)) {
          return;
        }

        status[election.id] = {
          status: 'APPROVED',
          message: 'Your application has been approved. You can continue in the campaign portal.',
          submittedAt: status[election.id]?.submittedAt || election.createdAt || ''
        };

        const existingApplication = applicationsByElection.get(election.id);
        applicationsByElection.set(election.id, {
          id: existingApplication?.id || `approved-${election.id}`,
          userId: existingApplication?.userId || user.id,
          electionId: election.id,
          electionTitle: existingApplication?.electionTitle || election.title,
          partyName: existingApplication?.partyName || profile.partyName || 'Independent',
          status: 'APPROVED',
          submittedAt: existingApplication?.submittedAt || election.createdAt || ''
        });
      });

      const mergedApplications = Array.from(applicationsByElection.values()).sort((first, second) => {
        const firstDate = first.submittedAt ? new Date(first.submittedAt).getTime() : 0;
        const secondDate = second.submittedAt ? new Date(second.submittedAt).getTime() : 0;
        return secondDate - firstDate;
      });

      setCandidateApplications(mergedApplications);
      setEnrollmentStatus(status);
      
    } catch (error) {
      console.error('Error fetching candidate applications from database:', error);
      
      // If database fails, clear enrollment status
      setCandidateApplications([]);
      setEnrollmentStatus({});
    }
  }, [allElections, getAuthHeaders, profile.partyName, user]);

  // Check candidate application status when elections or user changes
  useEffect(() => {
    if (allElections.length > 0 && user) {
      checkCandidateApplicationStatus(allElections);
    }
  }, [allElections, user, checkCandidateApplicationStatus]); // Check when elections or user changes

  useEffect(() => {
    if (activeTab !== 'elections' || !user) {
      return;
    }

    const interval = setInterval(() => {
      fetchElectionsFromBackend().then((latestElections) => {
        checkCandidateApplicationStatus(latestElections || allElections);
      });
    }, 5000);

    return () => clearInterval(interval);
  }, [activeTab, allElections, checkCandidateApplicationStatus, fetchElectionsFromBackend, user]);

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

  const handlePasswordFieldChange = useCallback((field, value) => {
    setPasswordForm(prev => ({ ...prev, [field]: value }));
  }, []);

  const handleChangePassword = async () => {
    setPasswordError('');
    setPasswordSuccess('');

    if (!passwordForm.currentPassword.trim()) {
      setPasswordError('Current password is required');
      return;
    }

    if (!validatePassword(passwordForm.newPassword)) {
      setPasswordError('New password must be at least 8 characters long');
      return;
    }

    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      setPasswordError('New password and confirmation do not match');
      return;
    }

    try {
      setPasswordLoading(true);
      const response = await fetch(getApiUrl('/api/auth/change-password'), {
        method: 'PUT',
        headers: getAuthHeaders(),
        body: JSON.stringify(passwordForm)
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || 'Failed to update password');
      }

      setPasswordSuccess('Password updated successfully');
      setShowPasswordModal(false);
      setPasswordForm({
        currentPassword: '',
        newPassword: '',
        confirmPassword: ''
      });
    } catch (error) {
      setPasswordError(error.message || 'Failed to update password');
    } finally {
      setPasswordLoading(false);
    }
  };

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
      const response = await fetch(getApiUrl('/api/auth/profile'), {
        method: 'PUT',
        headers: getAuthHeaders(),
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
        if (window.__voterowUpdateUser) {
          window.__voterowUpdateUser(updatedUser);
        }
      } else {
        const errorText = await response.text();
        setProfileError(errorText || 'Failed to update profile');
      }
    } catch {
      setProfileError('Network error. Please try again.');
    } finally {
      setProfileLoading(false);
    }
  };

  const campaignElectionOptions = Array.from(
    new Map(
      [...candidateApplications, ...allElections]
        .map((item) => {
          const electionId = item?.electionId ?? item?.id;
          if (electionId == null) {
            return null;
          }

          return [Number(electionId), {
            id: Number(electionId),
            title: item?.electionTitle || item?.title || `Election ${electionId}`,
            status: item?.status || item?.electionStatus || item?.applicationStatus || 'UNKNOWN'
          }];
        })
        .filter(Boolean)
    ).values()
  );

  const selectedCampaignElection = campaignElectionOptions.find(
    (election) => election.id === Number(campaignElectionId)
  ) || campaignElectionOptions[0] || null;

  useEffect(() => {
    if (!campaignElectionId && campaignElectionOptions.length > 0) {
      setCampaignElectionId(String(campaignElectionOptions[0].id));
    }
  }, [campaignElectionId, campaignElectionOptions]);

  const loadCampaignMaterials = useCallback(async (electionId) => {
    if (!user?.id || !electionId) {
      setCampaignCandidateId(null);
      setCampaignMaterials([]);
      return;
    }

    setCampaignLoading(true);
    setCampaignError('');

    try {
      const token = secureStorage.getToken();
      const candidateResponse = await fetch(
        getApiUrl(`/api/participant/candidate-id?userId=${user.id}&electionId=${electionId}`),
        {
          headers: {
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
            'Cache-Control': 'no-cache'
          }
        }
      );

      if (!candidateResponse.ok) {
        if (candidateResponse.status === 404) {
          setCampaignCandidateId(null);
          setCampaignMaterials([]);
          setCampaignError('No candidate record was found for the selected election yet.');
          return;
        }
        throw new Error('Failed to resolve campaign candidate');
      }

      const candidateId = await candidateResponse.json();
      setCampaignCandidateId(candidateId);

      const materialsResponse = await fetch(
        getApiUrl(`/api/campaign-materials/candidate/${candidateId}`),
        {
          headers: {
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
            'Cache-Control': 'no-cache'
          }
        }
      );

      if (!materialsResponse.ok) {
        throw new Error('Failed to load campaign materials');
      }

      const materials = await materialsResponse.json();
      setCampaignMaterials(Array.isArray(materials) ? materials : []);
    } catch (error) {
      setCampaignMaterials([]);
      setCampaignError(error.message || 'Failed to load campaign materials');
    } finally {
      setCampaignLoading(false);
    }
  }, [user?.id]);

  useEffect(() => {
    if (activeTab === 'campaign') {
      loadCampaignMaterials(campaignElectionId || selectedCampaignElection?.id);
    }
  }, [activeTab, campaignElectionId, loadCampaignMaterials, selectedCampaignElection?.id]);

  const submitCampaignMaterial = useCallback(async ({ type, title, description, file }) => {
    if (!user?.id) {
      throw new Error('User session is missing');
    }

    const electionId = campaignElectionId || selectedCampaignElection?.id;
    if (!electionId) {
      throw new Error('Select an election before uploading campaign materials');
    }

    if (!campaignCandidateId) {
      throw new Error('Candidate record not resolved for the selected election');
    }

    const uploadFile = file || new File(
      [description || title || 'Campaign material'],
      `${(title || type || 'campaign-material').replace(/\s+/g, '-').toLowerCase()}.txt`,
      { type: 'text/plain' }
    );

    const formData = new FormData();
    formData.append('file', uploadFile);
    formData.append('candidateId', String(campaignCandidateId));
    formData.append('electionId', String(electionId));
    formData.append('materialType', type);
    formData.append('title', title);
    if (description) {
      formData.append('description', description);
    }

    const token = secureStorage.getToken();
    const response = await fetch(getApiUrl('/api/campaign-materials/upload'), {
      method: 'POST',
      headers: {
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      },
      body: formData,
    });

    const responseText = await response.text();
    let responseBody = {};
    try {
      responseBody = responseText ? JSON.parse(responseText) : {};
    } catch {
      responseBody = { message: responseText };
    }

    if (!response.ok) {
      throw new Error(responseBody.error || responseBody.message || 'Failed to upload campaign material');
    }

    await loadCampaignMaterials(electionId);
    return responseBody;
  }, [campaignCandidateId, campaignElectionId, loadCampaignMaterials, selectedCampaignElection?.id, user?.id]);

  const handleCampaignFilePick = (event) => {
    const file = event.target.files?.[0] || null;
    setCampaignForm(prev => ({ ...prev, file }));
  };

  const handleCampaignUpload = async () => {
    setCampaignError('');
    setCampaignSuccess('');

    if (!campaignForm.title.trim()) {
      setCampaignError('Campaign material title is required');
      return;
    }

    try {
      setCampaignLoading(true);
      await submitCampaignMaterial({
        type: campaignForm.materialType,
        title: campaignForm.title.trim(),
        description: campaignForm.description.trim(),
        file: campaignForm.file
      });
      setCampaignSuccess('Campaign material uploaded successfully');
      setCampaignForm({
        materialType: 'POSTER',
        title: '',
        description: '',
        file: null
      });
    } catch (error) {
      setCampaignError(error.message || 'Failed to upload campaign material');
    } finally {
      setCampaignLoading(false);
    }
  };

  const handleCampaignAnnouncement = async () => {
    setCampaignError('');
    setCampaignSuccess('');

    if (!campaignForm.title.trim() && !campaignForm.description.trim()) {
      setCampaignError('Enter an announcement title or message');
      return;
    }

    try {
      setCampaignLoading(true);
      await submitCampaignMaterial({
        type: 'ANNOUNCEMENT',
        title: campaignForm.title.trim() || 'Announcement',
        description: campaignForm.description.trim() || campaignForm.title.trim(),
      });
      setCampaignSuccess('Announcement published successfully');
      setCampaignForm({
        materialType: 'POSTER',
        title: '',
        description: '',
        file: null
      });
    } catch (error) {
      setCampaignError(error.message || 'Failed to publish announcement');
    } finally {
      setCampaignLoading(false);
    }
  };

  const handleCampaignDelete = async (materialId) => {
    const confirmed = window.confirm('Delete this campaign material?');
    if (!confirmed) {
      return;
    }

    setCampaignError('');
    setCampaignSuccess('');

    try {
      const token = secureStorage.getToken();
      const response = await fetch(getApiUrl(`/api/campaign-materials/${materialId}`), {
        method: 'DELETE',
        headers: {
          ...(token ? { Authorization: `Bearer ${token}` } : {})
        }
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || 'Failed to delete campaign material');
      }

      await loadCampaignMaterials(campaignElectionId || selectedCampaignElection?.id);
      setCampaignSuccess('Campaign material deleted successfully');
    } catch (error) {
      setCampaignError(error.message || 'Failed to delete campaign material');
    }
  };

  const handleCampaignDownload = async (material) => {
    try {
      const token = secureStorage.getToken();
      const response = await fetch(getApiUrl(`/api/campaign-materials/download/${material.id}`), {
        headers: {
          ...(token ? { Authorization: `Bearer ${token}` } : {})
        }
      });

      if (!response.ok) {
        throw new Error('Failed to download campaign material');
      }

      const blob = await response.blob();
      const objectUrl = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = objectUrl;
      link.download = material.fileName || `${material.title || 'campaign-material'}.bin`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(objectUrl);
    } catch (error) {
      setCampaignError(error.message || 'Failed to download campaign material');
    }
  };
  
  // Filter for elections this user is part of and upcoming ones they can join
  const enrolledElections = allElections.filter(e => enrollmentStatus[e.id]?.status === 'APPROVED');
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
            onClick={async () => {
              const latestElections = await fetchElectionsFromBackend();
              await checkCandidateApplicationStatus(latestElections || allElections);
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
                  {candidateApplications.map(application => (
                    <tr key={application.id}>
                      <td>{application.electionTitle || 'Unknown Election'}</td>
                      <td>{application.submittedAt ? new Date(application.submittedAt).toLocaleDateString() : 'N/A'}</td>
                      <td>
                        <span className={`status ${(application.status || 'PENDING').toLowerCase()}`}>
                          {application.status || 'PENDING'}
                        </span>
                      </td>
                      <td>
                        <button className="btn btn-sm btn-info" type="button">View Details</button>
                      </td>
                    </tr>
                  ))}
                  {candidateApplications.length === 0 && (
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
        <p className="content-subtitle">Manage campaign materials against the live backend</p>
      </div>
      <div className="module-content">
        {campaignError && <div className="alert alert-error">{campaignError}</div>}
        {campaignSuccess && <div className="alert alert-success">{campaignSuccess}</div>}

        <div className="campaign-sections">
          <div className="campaign-section">
            <h4><FaVoteYea /> Election Context</h4>
            <div className="form-grid">
              <div className="form-group">
                <label>Election</label>
                <select
                  value={campaignElectionId}
                  onChange={(event) => setCampaignElectionId(event.target.value)}
                >
                  {campaignElectionOptions.length === 0 && <option value="">No election available</option>}
                  {campaignElectionOptions.map((election) => (
                    <option key={election.id} value={election.id}>
                      {election.title}
                    </option>
                  ))}
                </select>
              </div>
              <div className="form-group">
                <label>Candidate Record</label>
                <input
                  type="text"
                  value={campaignCandidateId ? `Candidate #${campaignCandidateId}` : 'Not resolved yet'}
                  readOnly
                />
              </div>
            </div>
            <div className="form-actions">
              <button
                className="btn btn-secondary"
                onClick={() => loadCampaignMaterials(campaignElectionId || selectedCampaignElection?.id)}
                disabled={campaignLoading || !campaignElectionId}
              >
                <FaSync /> Refresh Materials
              </button>
            </div>
          </div>

          <div className="campaign-section">
            <h4><FaUpload /> Upload Campaign Material</h4>
            <div className="campaign-upload-form">
              <div className="form-grid">
                <div className="form-group">
                  <label>Material Type</label>
                  <select
                    value={campaignForm.materialType}
                    onChange={(event) => setCampaignForm(prev => ({ ...prev, materialType: event.target.value }))}
                  >
                    <option value="POSTER">Poster</option>
                    <option value="BROCHURE">Brochure</option>
                    <option value="MANIFESTO">Manifesto</option>
                    <option value="VOLUNTEER_FORM">Volunteer Form</option>
                    <option value="SYMBOL">Symbol</option>
                    <option value="OTHER">Other</option>
                  </select>
                </div>
                <div className="form-group">
                  <label>Title</label>
                  <input
                    type="text"
                    value={campaignForm.title}
                    onChange={(event) => setCampaignForm(prev => ({ ...prev, title: event.target.value }))}
                    placeholder="Enter a title"
                  />
                </div>
                <div className="form-group">
                  <label>Description</label>
                  <textarea
                    rows="4"
                    value={campaignForm.description}
                    onChange={(event) => setCampaignForm(prev => ({ ...prev, description: event.target.value }))}
                    placeholder="Optional details for the material"
                  />
                </div>
                <div className="form-group">
                  <label>File</label>
                  <input type="file" onChange={handleCampaignFilePick} />
                </div>
              </div>
              <div className="form-actions">
                <button
                  className="btn btn-primary"
                  onClick={handleCampaignUpload}
                  disabled={campaignLoading || !campaignElectionId}
                >
                  {campaignLoading ? 'Uploading...' : 'Upload Material'}
                </button>
                <button
                  className="btn btn-secondary"
                  onClick={handleCampaignAnnouncement}
                  disabled={campaignLoading || !campaignElectionId}
                >
                  Post Announcement
                </button>
              </div>
            </div>
          </div>

          <div className="campaign-section">
            <h4><FaClipboardList /> Published Materials</h4>
            {campaignLoading && campaignMaterials.length === 0 ? (
              <p>Loading campaign materials...</p>
            ) : campaignMaterials.length === 0 ? (
              <p>No campaign materials uploaded yet.</p>
            ) : (
              <div className="materials-grid">
                {campaignMaterials.map((material) => (
                  <div key={material.id} className="material-card">
                    <div className="material-card-header">
                      <h5>{material.title}</h5>
                      <span className={`status ${(material.isApproved ? 'approved' : 'pending')}`}>
                        {material.isApproved ? 'Approved' : 'Pending approval'}
                      </span>
                    </div>
                    <p><strong>Type:</strong> {material.materialType}</p>
                    <p><strong>Uploaded:</strong> {material.uploadDate ? new Date(material.uploadDate).toLocaleString() : 'Unknown'}</p>
                    {material.description && <p>{material.description}</p>}
                    {material.rejectionReason && <p><strong>Rejection:</strong> {material.rejectionReason}</p>}
                    <div className="result-actions">
                      <button className="btn btn-info" onClick={() => handleCampaignDownload(material)}>
                        <FaDownload /> Download
                      </button>
                      <button className="btn btn-secondary" onClick={() => handleCampaignDelete(material.id)}>
                        <FaTrash /> Delete
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
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
  const ResultsModule = () => {
    const [results, setResults] = useState([]);
    const [loadingResults, setLoadingResults] = useState(true);

    useEffect(() => {
      const token = secureStorage.getToken();
      fetch(getApiUrl('/api/voter/results'), {
        headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) }
      })
        .then(r => r.ok ? r.json() : [])
        .then(data => setResults(Array.isArray(data) ? data : []))
        .catch(() => setResults([]))
        .finally(() => setLoadingResults(false));
    }, []);

    // Find this candidate's user id to highlight their row
    const myUserId = user?.id;

    return (
      <div className="candidate-module">
        <div className="content-header">
          <h1 className="content-title">Results & Reports</h1>
          <p className="content-subtitle">View election results and your performance</p>
        </div>
        <div className="module-content">
          {loadingResults ? (
            <p>Loading results...</p>
          ) : results.length === 0 ? (
            <p>No published results yet. Results will appear here once the admin publishes them.</p>
          ) : (
            <div className="results-sections">
              {results.map(election => (
                <div key={election.electionId} className="result-card">
                  <h5>{election.electionTitle}</h5>
                  <p>Total Votes: <strong>{election.totalVotes}</strong></p>
                  <div className="results-cards">
                    {election.candidateResults && election.candidateResults.map(cr => {
                      const isMe = cr.candidateId && allElections.some(e =>
                        e.id === election.electionId &&
                        (e.participantUserIds || []).includes(myUserId)
                      ) && candidateApplications.some(a =>
                        a.electionId === election.electionId && a.status === 'APPROVED'
                      );
                      return (
                        <div key={cr.candidateId} className="result-card" style={cr.isWinner ? {border:'2px solid #28a745'} : {}}>
                          <h5>{cr.rank === 1 ? '🏆 ' : `#${cr.rank} `}{cr.candidateName}</h5>
                          {cr.party && cr.party !== 'Independent' && <p>{cr.party}</p>}
                          <div className="result-stats">
                            <div className="stat-item"><label>Rank:</label><span>#{cr.rank}</span></div>
                            <div className="stat-item"><label>Votes:</label><span>{cr.votes}</span></div>
                            <div className="stat-item"><label>Vote %:</label><span>{cr.percentage}%</span></div>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    );
  };

  if (!user) {
    return <div className="participant-dashboard"><div className="loading-message"><p>Loading user data...</p></div></div>;
  }

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
        {activeTab === 'profile' && (
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
        )}
        {activeTab === 'authentication' && (
          <AuthenticationModuleStable
            onOpenPasswordModal={() => setShowPasswordModal(true)}
            authSuccess={passwordSuccess}
          />
        )}
        {activeTab === 'elections' && <ElectionParticipationModule />}
        {activeTab === 'campaign' && (
          <CampaignToolsStable
            user={user}
            campaignMaterials={campaignMaterials}
            campaignCandidateId={campaignCandidateId}
            campaignElectionId={campaignElectionId}
            campaignLoading={campaignLoading}
            campaignError={campaignError}
            campaignSuccess={campaignSuccess}
            campaignForm={campaignForm}
            campaignElectionOptions={campaignElectionOptions}
            selectedCampaignElection={selectedCampaignElection}
            onElectionChange={setCampaignElectionId}
            onMaterialTypeChange={(value) => setCampaignForm(prev => ({ ...prev, materialType: value }))}
            onTitleChange={(value) => setCampaignForm(prev => ({ ...prev, title: value }))}
            onDescriptionChange={(value) => setCampaignForm(prev => ({ ...prev, description: value }))}
            onFilePick={handleCampaignFilePick}
            onRefreshMaterials={loadCampaignMaterials}
            onUploadMaterial={handleCampaignUpload}
            onPostAnnouncement={handleCampaignAnnouncement}
            onDeleteMaterial={handleCampaignDelete}
            onDownloadMaterial={handleCampaignDownload}
          />
        )}
        {activeTab === 'notifications' && <NotificationsModule />}
        {activeTab === 'results' && <ResultsModule />}
      </main>
      <PasswordChangeModal
        isOpen={showPasswordModal}
        isLoading={passwordLoading}
        error={passwordError}
        success={passwordSuccess}
        form={passwordForm}
        onChange={handlePasswordFieldChange}
        onClose={() => setShowPasswordModal(false)}
        onSubmit={handleChangePassword}
      />
    </div>
  );
};

export default ParticipantDashboard;
