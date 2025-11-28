import React, { useState, useEffect, useCallback } from 'react';
import { 
  FaPlus, FaEdit, FaToggleOn, FaToggleOff, FaTrash, FaUsers, FaUserTie, 
  FaLock, FaVoteYea, FaCog, FaChartBar, FaBell, FaShieldAlt, FaFileExport,
  FaUserCheck, FaCalendarAlt, FaPlay, FaStop, FaSearch, FaUpload, FaDownload,
  FaEye, FaEnvelope, FaSms, FaDatabase, FaKey, FaUserShield, FaHome, FaUser, FaTimes, FaSyncAlt
} from 'react-icons/fa';
import './AdminDashboard.css';
import UserModal from './UserModal';
import ErrorBoundary from './ErrorBoundary';
import secureStorage from '../utils/secureStorage';

// Centralized API base URL. Configure via VITE_API_BASE_URL, fallback to Spring Boot port 8083
const BASE_API_URL = import.meta.env?.VITE_API_BASE_URL || 'http://localhost:8083';
const INCLUDE_CREDENTIALS = (import.meta.env?.VITE_INCLUDE_CREDENTIALS || 'false') === 'true';

// URL validation function
const isValidUrl = (url) => {
  try {
    const urlObj = new URL(url);
    return ['http:', 'https:'].includes(urlObj.protocol);
  } catch {
    return false;
  }
};

const AdminDashboard = ({ user, onUpdateUser }) => {
  // Guard clause for undefined user
  if (!user) {
    return (
      <div className="admin-dashboard">
        <div className="loading-message">
          <p>Loading admin data...</p>
        </div>
      </div>
    );
  }

  const [activeTab, setActiveTab] = useState('home');
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState({
    overview: {},
    users: [],
    voters: [],
    candidates: [],
    elections: [],
    results: [],
    auditLogs: [],
    notifications: [],
    reports: [],
    settings: {}
  });

  // Election management state
  const [showElectionModal, setShowElectionModal] = useState(false);
  const [showEnrollmentRequestsModal, setShowEnrollmentRequestsModal] = useState(false);
  const [currentElectionId, setCurrentElectionId] = useState(null);
  const [enrollmentRequests, setEnrollmentRequests] = useState([]);
  const [editingElection, setEditingElection] = useState(null);
  const [electionForm, setElectionForm] = useState({
    title: '',
    description: '',
    startDate: '',
    endDate: '',
    status: 'DRAFT'
  });

  // Navigation tabs for all admin modules
  const tabs = [
    { id: 'home', label: 'Home', icon: FaHome },
    { id: 'profile', label: 'Profile', icon: FaUser },
    { id: 'overview', label: 'Overview', icon: FaChartBar },
    { id: 'auth', label: 'Authentication & Security', icon: FaLock },
    { id: 'voters', label: 'Voter Management', icon: FaUsers },
    { id: 'candidates', label: 'Candidate Management', icon: FaUserTie },
    { id: 'elections', label: 'Election Management', icon: FaVoteYea },
    { id: 'voting', label: 'Voting Process Control', icon: FaPlay },
    { id: 'results', label: 'Results & Analytics', icon: FaChartBar },
    { id: 'security', label: 'Security & Audit', icon: FaShieldAlt },
    { id: 'notifications', label: 'Communications', icon: FaBell },
    { id: 'reports', label: 'Reports & Export', icon: FaFileExport },
    { id: 'settings', label: 'System Settings', icon: FaCog }
  ];

  // Helper function to get headers with JWT token
  const getAuthHeaders = (includeContentType = true, additionalHeaders = {}) => {
    const token = secureStorage.getToken();
    return {
      ...(includeContentType ? { 'Content-Type': 'application/json' } : {}),
      'Cache-Control': 'no-cache, no-store',
      'Pragma': 'no-cache',
      ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
      ...additionalHeaders
    };
  };

  // API call functions - wrapped in useCallback to ensure stable reference
  const apiCall = useCallback(async (endpoint, options = {}) => {
    try {
      const fullUrl = `${BASE_API_URL}/api/admin${endpoint}`;
      
      // Validate URL to prevent SSRF
      if (!isValidUrl(fullUrl)) {
        throw new Error('Invalid URL detected');
      }
      
      // Reduced console logging to prevent fluctuations
      // console.log(`AdminDashboard: Making API call to ${endpoint}`, options);
      
      // Add cache-busting parameter to avoid cached responses
      const cacheBuster = `?_cb=${Date.now()}`;
      const finalUrl = `${fullUrl}${cacheBuster}`;
      
      // Get JWT token from secure storage
      const token = secureStorage.getToken();
      
      const fetchOptions = {
        ...options,
        // Only include credentials if explicitly enabled; avoids CORS issues with "*" origins
        ...(INCLUDE_CREDENTIALS ? { credentials: 'include' } : {}),
        headers: {
          // Don't set Content-Type for FormData - let browser set it automatically
          ...(options.body instanceof FormData ? {} : { 'Content-Type': 'application/json' }),
          'Cache-Control': 'no-cache, no-store',
          'Pragma': 'no-cache',
          // Add Authorization header with JWT token if available
          ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
          ...options.headers
        }
      };
      
      // console.log(`AdminDashboard: Fetch options:`, fetchOptions);
      
      let response;
      try {
        response = await fetch(finalUrl, fetchOptions);
      } catch (fetchError) {
        // Network error - server not reachable
        console.error(`AdminDashboard: Network error for ${endpoint}:`, fetchError);
        // Check if it's a CORS or network error
        if (fetchError.message && fetchError.message.includes('Failed to fetch')) {
          throw new Error('Cannot connect to server. Please check if the backend server (http://localhost:8083) is running.');
        }
        throw new Error(`Failed to connect to server: ${fetchError.message || 'Network error'}`);
      }
      
      // console.log(`AdminDashboard: API response status:`, response.status);
      
      if (response.ok) {
        // Handle empty response for DELETE requests
        const contentType = response.headers.get('content-type');
        if (contentType && contentType.includes('application/json')) {
          try {
            const data = await response.json();
            // Only log on errors, not every successful call
            // console.log(`AdminDashboard: API call to ${endpoint} successful:`, data);
            return data;
          } catch (jsonError) {
            // If JSON parsing fails, return success for DELETE operations
            if (options.method === 'DELETE') {
              return { success: true, message: 'Operation completed successfully' };
            }
            throw new Error('Invalid JSON response from server');
          }
        } else {
          // For DELETE requests that return 200 OK but no body
          if (options.method === 'DELETE') {
            return { success: true, message: 'Operation completed successfully' };
          }
          return { success: true };
        }
      } else {
        let errorText = '';
        try {
          errorText = await response.text();
        } catch (e) {
          errorText = `HTTP ${response.status}`;
        }
        
        // Provide better error messages for specific HTTP status codes
        let message = '';
        if (response.status === 403) {
          message = `Access Denied (403): You don't have permission to perform this action. Please ensure you are logged in as an ADMIN user.`;
        } else if (response.status === 401) {
          message = `Unauthorized (401): Your session has expired. Please log in again.`;
        } else {
          message = `HTTP ${response.status} on ${endpoint}${errorText ? `: ${errorText}` : ''}`;
        }
        
        console.error(`AdminDashboard: ${message}`);
        throw new Error(message);
      }
    } catch (error) {
      console.error(`AdminDashboard: API Error for ${endpoint}:`, error.message);
      // Re-throw with more context if it's not already our formatted error
      if (error.message && !error.message.includes('HTTP') && !error.message.includes('Failed to connect')) {
        throw new Error(`API call failed: ${error.message}`);
      }
      throw error;
    }
  }, []); // Empty dependencies - apiCall doesn't depend on any component state/props

  // Load elections from API, only fallback to localStorage if needed
  const loadElections = useCallback(async () => {
    try {
      // First try to get elections from the API (primary source of truth)
      const apiElections = await apiCall('/elections');
      
      // If API call was successful, use that data exclusively
      if (apiElections) {
        // console.log('AdminDashboard: Successfully loaded elections from API:', apiElections);
        
        // Process the elections data
        const processedElections = apiElections.map(election => ({
          ...election,
          candidateCount: election.participants ? election.participants.length : 0,
          // Ensure votes object exists
          votes: election.votes || {}
        }));
        
        // Update the component state
        setData(prev => ({ ...prev, elections: processedElections }));
        
        // Cache the data in localStorage for offline functionality
        localStorage.setItem('voterow_elections', JSON.stringify(processedElections));
        
        return processedElections;
      } 
      
      // If API call failed, fall back to localStorage
      // console.log('AdminDashboard: API call failed, falling back to localStorage');
      let localElections = [];
      try {
        localElections = JSON.parse(localStorage.getItem('voterow_elections')) || [];
      } catch (error) {
        console.error('Failed to parse elections from localStorage:', error);
        localElections = [];
      }
      
      const processedLocalElections = localElections.map(election => ({
        ...election,
        candidateCount: election.participants ? election.participants.length : 0,
        // Ensure votes object exists
        votes: election.votes || {}
      }));
      
      // console.log('AdminDashboard: Loading elections from localStorage:', processedLocalElections);
      setData(prev => ({ ...prev, elections: processedLocalElections }));
      
      return processedLocalElections;
    } catch (error) {
      console.error('Error loading elections:', error);
      return [];
    }
  }, []);
  
  const loadTabData = useCallback(async () => {
    try {
      switch (activeTab) {
        case 'overview':
          // Try to load overview, fall back to mock data
          try {
            const overview = await apiCall('/dashboard/overview');
            setData(prev => ({ ...prev, overview }));
          } catch (error) {
            // Fallback data
            const fallbackOverview = {
              totalElections: 5,
              totalVoters: 1247,
              totalCandidates: 15,
              activeElections: 2,
              recentActivity: [
                { id: 1, action: "New voter registered", user: "John Doe", timestamp: new Date().toISOString() },
                { id: 2, action: "Election created", user: "Admin", timestamp: new Date(Date.now() - 300000).toISOString() }
              ]
            };
            setData(prev => ({ ...prev, overview: fallbackOverview }));
          }
          break;
        case 'voters':
          // Load voters from API
          try {
            const voters = await apiCall('/voters');
            if (voters && Array.isArray(voters)) {
              setData(prev => ({ ...prev, voters }));
              localStorage.setItem('voterow_voters', JSON.stringify(voters));
            }
          } catch (error) {
            console.log('AdminDashboard: Failed to load voters from API, using cached data');
          }
          break;
        case 'security':
          try {
            const auditLogs = await apiCall('/security-audit');
            setData(prev => ({ ...prev, auditLogs }));
          } catch (error) {
            const fallbackLogs = [
              { id: 1, event: "Successful login", user: "admin", timestamp: "2024-03-02 09:15", ip: "192.168.1.100" },
              { id: 2, event: "Failed login attempt", user: "unknown", timestamp: "2024-03-02 08:45", ip: "10.0.0.50" }
            ];
            setData(prev => ({ ...prev, auditLogs: fallbackLogs }));
          }
          break;
        case 'settings':
          try {
            const settings = await apiCall('/settings/system-config');
            setData(prev => ({ ...prev, settings }));
          } catch (error) {
            setData(prev => ({ ...prev, settings: {} }));
          }
          break;
        default:
          // For other tabs, data is loaded in their specific useEffect
          break;
      }
    } catch (error) {
      console.log('Using fallback data due to API unavailability');
    }
  }, [activeTab]);

  // Load data only when explicitly requested
  const loadDataForTab = useCallback(async (tabName) => {
    if (tabName === 'candidates') {
      try {
        const candidates = await apiCall('/candidates');
        if (candidates && Array.isArray(candidates)) {
          setData(prev => ({ ...prev, candidates }));
        }
      } catch (error) {
        console.log('API failed, using empty array');
        setData(prev => ({ ...prev, candidates: [] }));
      }
    }
  }, []);

  // Initialize component and load data for the current tab on mount
  useEffect(() => {
    // console.log('AdminDashboard: Component mounted, initial tab:', activeTab);
    
    // Load cached data immediately for better UX
    const cachedVoters = localStorage.getItem('voterow_voters');
    if (cachedVoters) {
      try {
        let parsedVoters = [];
        try {
          parsedVoters = JSON.parse(cachedVoters);
        } catch (parseError) {
          console.error('Failed to parse cached voters:', parseError);
          parsedVoters = [];
        }
        setData(prev => ({ ...prev, voters: parsedVoters }));
        // console.log('AdminDashboard: Restored voters from cache on mount:', parsedVoters.length);
      } catch (error) {
        console.error('AdminDashboard: Failed to parse cached voters on mount:', error);
      }
    }

    // Load cached candidates data
    const cachedCandidates = localStorage.getItem('voterow_candidates');
    if (cachedCandidates) {
      try {
        let parsedCandidates = [];
        try {
          parsedCandidates = JSON.parse(cachedCandidates);
        } catch (parseError) {
          console.error('Failed to parse cached candidates:', parseError);
          parsedCandidates = [];
        }
        setData(prev => ({ ...prev, candidates: parsedCandidates }));
        // console.log('AdminDashboard: Restored candidates from cache on mount:', parsedCandidates.length);
      } catch (error) {
        console.error('AdminDashboard: Failed to parse cached candidates on mount:', error);
      }
    }

    // Load cached elections data
    const cachedElections = localStorage.getItem('voterow_elections');
    if (cachedElections) {
      try {
        const parsedElections = JSON.parse(cachedElections);
        setData(prev => ({ ...prev, elections: parsedElections }));
        // console.log('AdminDashboard: Restored elections from cache on mount:', parsedElections.length);
      } catch (error) {
        console.error('AdminDashboard: Failed to parse cached elections on mount:', error);
      }
    }

    // Note: Data will be loaded manually via refresh buttons
  }, []); // Empty dependency array means this runs once on mount

  // Load elections when the elections tab is opened
  useEffect(() => {
    if (activeTab === 'elections') {
      // console.log('AdminDashboard: Elections tab opened, loading from database');
      loadElections().catch(error => {
        console.error('AdminDashboard: Failed to load elections:', error);
      });
    }
  }, [activeTab, loadElections]);

  // Election form handlers - memoized to prevent re-renders
  const handleFormChange = useCallback((field, value) => {
    setElectionForm(prev => ({
      ...prev,
      [field]: value
    }));
  }, []);
  
  // Memoized loadData function to prevent re-renders
  const loadData = useCallback((type) => {
    // console.log('AdminDashboard: loadData called for type:', type);
    switch(type) {
      case 'elections':
        // console.log('AdminDashboard: Force loading elections data');
        setLoading(true);
        loadElections().finally(() => setLoading(false));
        break;
      case 'voters':
        // console.log('AdminDashboard: Force loading voters data');
        setLoading(true);
        loadTabData().finally(() => setLoading(false));
        break;
      case 'candidates':
        // console.log('AdminDashboard: Force loading candidates data');
        setLoading(true);
        loadTabData().finally(() => setLoading(false));
        break;
      default:
        loadTabData();
    }
  }, [loadElections, loadTabData]);

  // Election management functions - memoized to prevent re-renders
  const handleCreateElection = useCallback(() => {
    // console.log('AdminDashboard: Create Election button clicked');
    setEditingElection(null);
    setElectionForm({
      title: '',
      description: '',
      startDate: '',
      endDate: '',
      status: 'DRAFT'
    });
    setShowElectionModal(true);
    // console.log('AdminDashboard: Election modal set to visible');
  }, []);

  const handleEditElection = useCallback((election) => {
    setEditingElection(election);
    setElectionForm({
      title: election.title,
      description: election.description || '',
      startDate: election.startDate ? election.startDate.split('T')[0] : '',
      endDate: election.endDate ? election.endDate.split('T')[0] : '',
      status: election.status
    });
    setShowElectionModal(true);
  }, []);

  const handleSaveElection = useCallback(async () => {
    // console.log('AdminDashboard: handleSaveElection called with form:', electionForm);
    
    try {
      // Validate form
      if (!electionForm.title || !electionForm.startDate || !electionForm.endDate) {
        console.log('AdminDashboard: Validation failed - missing required fields');
        alert('Please fill in all required fields');
        return;
      }
      
      // Validate dates
      if (new Date(electionForm.endDate) <= new Date(electionForm.startDate)) {
        console.log('AdminDashboard: Validation failed - invalid date range');
        alert('End date must be after start date');
        return;
      }
      
      // console.log('AdminDashboard: Form validation passed, proceeding with save');
      let apiSuccess = false;
      
      if (editingElection) {
        // Always try to update via API first
        try {
          // Format dates properly for backend (ISO format)
          const formattedElectionData = {
            title: electionForm.title,
            description: electionForm.description,
            startDate: electionForm.startDate ? new Date(electionForm.startDate).toISOString() : null,
            endDate: electionForm.endDate ? new Date(electionForm.endDate).toISOString() : null,
            status: electionForm.status || 'DRAFT'
          };
          
          console.log('AdminDashboard: Updating election via API with data:', formattedElectionData);
          
          const apiResponse = await apiCall(`/elections/${editingElection.id}`, {
            method: 'PUT',
            body: JSON.stringify(formattedElectionData)
          });
          
          if (apiResponse && apiResponse.id) {
            console.log('AdminDashboard: Successfully updated election via API');
            apiSuccess = true;
            // Update local state with API response
            setData(prev => ({
              ...prev,
              elections: prev.elections.map(e => 
                e.id === editingElection.id ? apiResponse : e
              )
            }));
            // Refresh elections from database
            await loadElections();
          } else {
            throw new Error('Invalid response from server');
          }
        } catch (apiError) {
          console.error('AdminDashboard: API update failed:', apiError);
          alert('Failed to update election in database. Changes saved locally only.');
        }
        
        // Only update localStorage if API call failed
        if (!apiSuccess) {
          console.log('AdminDashboard: Falling back to localStorage for election update');
          // Get existing elections from localStorage
          const existingElections = JSON.parse(localStorage.getItem('voterow_elections')) || [];
          
          // Update existing election in localStorage
          const updatedElections = existingElections.map(e => 
            e.id === editingElection.id 
              ? { 
                  ...e, 
                  title: electionForm.title,
                  description: electionForm.description,
                  startDate: electionForm.startDate + 'T09:00:00+05:30',
                  endDate: electionForm.endDate + 'T17:00:00+05:30',
                  status: electionForm.status
                }
              : e
          );
          localStorage.setItem('voterow_elections', JSON.stringify(updatedElections));
          
          // Update local state
          setData(prev => ({
            ...prev,
            elections: prev.elections.map(e => 
              e.id === editingElection.id 
                ? { 
                    ...e, 
                    title: electionForm.title,
                    description: electionForm.description,
                    startDate: electionForm.startDate,
                    endDate: electionForm.endDate,
                    status: electionForm.status
                  }
                : e
            )
          }));
        }
      } else {
        // Always try to create via API first
        try {
          // Format dates properly for backend (ISO format)
          const formattedElectionData = {
            title: electionForm.title,
            description: electionForm.description,
            startDate: electionForm.startDate ? new Date(electionForm.startDate).toISOString() : null,
            endDate: electionForm.endDate ? new Date(electionForm.endDate).toISOString() : null,
            status: electionForm.status || 'DRAFT'
          };
          
          console.log('AdminDashboard: Creating election via API with data:', formattedElectionData);
          
          const apiResponse = await apiCall('/elections', {
            method: 'POST',
            body: JSON.stringify(formattedElectionData)
          });
          
          if (apiResponse && apiResponse.id) {
            console.log('AdminDashboard: Successfully created election via API with ID:', apiResponse.id);
            apiSuccess = true;
            // Update local state with API response
            setData(prev => ({
              ...prev,
              elections: [...prev.elections, apiResponse]
            }));
            // Refresh elections from database
            await loadElections();
          } else {
            throw new Error('Invalid response from server');
          }
        } catch (apiError) {
          console.error('AdminDashboard: API creation failed:', apiError);
          // Show user that we're falling back to localStorage
          alert('Backend server is not accessible. Election will be saved locally only.');
        }
        
        // Only use localStorage if API call failed
        if (!apiSuccess) {
          console.log('AdminDashboard: Falling back to localStorage for election creation');
          // Get existing elections from localStorage
          const existingElections = JSON.parse(localStorage.getItem('voterow_elections')) || [];
          
          // Create new election
          const newElection = {
            id: Math.max(...existingElections.map(e => e.id || 0), 0) + 1,
            title: electionForm.title,
            description: electionForm.description,
            startDate: electionForm.startDate + 'T09:00:00+05:30',
            endDate: electionForm.endDate + 'T17:00:00+05:30',
            status: electionForm.status,
            participants: [],
            enrollmentRequests: [],
            voters: [], // Empty voters array means all voters can participate
            votes: {},
            candidateCount: 0,
            voteCount: 0 // Track total votes cast
          };
          
          const updatedElections = [...existingElections, newElection];
          localStorage.setItem('voterow_elections', JSON.stringify(updatedElections));
          
          // Update local state
          setData(prev => ({
            ...prev,
            elections: [...prev.elections, newElection]
          }));
        }
      }
      
      // console.log('AdminDashboard: Election save completed, closing modal');
      setShowElectionModal(false);
      alert(editingElection ? 'Election updated successfully!' : 'Election created successfully!');
      
      // Reload elections data and ensure localStorage is updated
      // console.log('AdminDashboard: Reloading elections data');
      await loadElections();
      console.log('AdminDashboard: Elections reloaded successfully');
    } catch (error) {
      console.error('AdminDashboard: Error saving election:', error);
      alert('Error saving election. Please try again.');
    }
  }, [electionForm, editingElection, loadElections]);

  const handleStartElection = useCallback(async (election) => {
    try {
      // Try to start via API first
      try {
        await apiCall(`/elections/${election.id}/start`, {
          method: 'POST'
        });
      } catch (apiError) {
        console.log('API start failed, using localStorage');
      }
      
      const existingElections = JSON.parse(localStorage.getItem('voterow_elections')) || [];
      const updatedElections = existingElections.map(e => 
        e.id === election.id ? { ...e, status: 'ONGOING' } : e
      );
      localStorage.setItem('voterow_elections', JSON.stringify(updatedElections));
      
      // Update local state
      setData(prev => ({
        ...prev,
        elections: prev.elections.map(e => 
          e.id === election.id ? { ...e, status: 'ONGOING' } : e
        )
      }));
      
      alert('Election started successfully!');
    } catch (error) {
      console.error('Error starting election:', error);
      alert('Error starting election. Please try again.');
    }
  }, []);

  const handleEndElection = useCallback(async (election) => {
    try {
      // Try to end via API first
      try {
        await apiCall(`/elections/${election.id}/end`, {
          method: 'POST'
        });
      } catch (apiError) {
        console.log('API end failed, using localStorage');
      }
      
      const existingElections = JSON.parse(localStorage.getItem('voterow_elections')) || [];
      const updatedElections = existingElections.map(e => 
        e.id === election.id ? { ...e, status: 'COMPLETED' } : e
      );
      localStorage.setItem('voterow_elections', JSON.stringify(updatedElections));
      
      // Update local state
      setData(prev => ({
        ...prev,
        elections: prev.elections.map(e => 
          e.id === election.id ? { ...e, status: 'COMPLETED' } : e
        )
      }));
      
      alert('Election ended successfully!');
    } catch (error) {
      console.error('Error ending election:', error);
      alert('Error ending election. Please try again.');
    }
  }, []);
  
  // View and manage enrollment requests
  const viewEnrollmentRequests = (electionId) => {
    const election = data.elections.find(e => e.id === electionId);
    if (election) {
      setCurrentElectionId(electionId);
      setEnrollmentRequests(election.enrollmentRequests || []);
      setShowEnrollmentRequestsModal(true);
    }
  };
  
  const handleEnrollmentRequest = (electionId, participantId, action) => {
    const existingElections = JSON.parse(localStorage.getItem('voterow_elections')) || [];
    const election = existingElections.find(e => e.id === electionId);
    
    if (!election) return;
    
    let updatedElections = existingElections;
    
    if (action === 'approve') {
      // Update enrollment request status
      updatedElections = existingElections.map(election => {
        if (election.id === electionId) {
          const updatedRequests = (election.enrollmentRequests || []).map(req => {
            if (req.userId === participantId || req.participantId === participantId) {
              return { ...req, status: 'APPROVED', actionDate: new Date().toISOString() };
            }
            return req;
          });
          
          // Add to participants list if not already included
          const participants = election.participants || [];
          if (!participants.includes(participantId)) {
            participants.push(participantId);
          }
          
          return { 
            ...election, 
            enrollmentRequests: updatedRequests,
            participants: participants,
            candidateCount: participants.length
          };
        }
        return election;
      });
    } else if (action === 'reject') {
      // Update enrollment request status to rejected
      updatedElections = existingElections.map(election => {
        if (election.id === electionId) {
          const updatedRequests = (election.enrollmentRequests || []).map(req => {
            if (req.userId === participantId || req.participantId === participantId) {
              return { ...req, status: 'REJECTED', actionDate: new Date().toISOString() };
            }
            return req;
          });
          
          return { 
            ...election, 
            enrollmentRequests: updatedRequests 
          };
        }
        return election;
      });
    }
    
    // Save to localStorage
    localStorage.setItem('voterow_elections', JSON.stringify(updatedElections));
    
    // Update state
    setData(prev => ({
      ...prev,
      elections: updatedElections
    }));
    
    // Update enrollment requests list
    const updatedElection = updatedElections.find(e => e.id === electionId);
    setEnrollmentRequests(updatedElection?.enrollmentRequests || []);
    
    alert(`Candidate application ${action === 'approve' ? 'approved' : 'rejected'} successfully!`);
  };

  // Module 1: Authentication & Authorization Component
  const AuthenticationModule = () => (
    <div className="admin-module">
      <h3 className="module-title"><FaLock /> Authentication & Authorization</h3>
      <div className="module-content">
        <div className="action-buttons">
          <button className="btn btn-primary"><FaKey /> Enable 2FA</button>
          <button className="btn btn-secondary"><FaUserShield /> Manage Admin Roles</button>
          <button className="btn btn-info"><FaLock /> Security Settings</button>
        </div>
        <div className="info-cards">
          <div className="info-card">
            <h4>Super Admins</h4>
            <p className="stat-number">2</p>
          </div>
          <div className="info-card">
            <h4>Sub Admins</h4>
            <p className="stat-number">5</p>
          </div>
          <div className="info-card">
            <h4>2FA Enabled</h4>
            <p className="stat-number">85%</p>
          </div>
        </div>
      </div>
    </div>
  );

  // Module 2: Voter Management Component
  const VoterManagementModule = ({ voters = [], setData, data, loadData }) => {
    // State for voter management
    const [showAddVoterModal, setShowAddVoterModal] = useState(false);
    const [showImportModal, setShowImportModal] = useState(false);
    const [showVoterModal, setShowVoterModal] = useState(false);
    const [selectedVoter, setSelectedVoter] = useState(null);
    const [isEditingVoter, setIsEditingVoter] = useState(false);
    const [voterForm, setVoterForm] = useState({
      fullName: '',
      email: '',
      age: '',
      phone: ''
    });
    const [importFile, setImportFile] = useState(null);
    
    // Event handlers for voter management
    const handleAddVoter = useCallback(() => {
      console.log("Opening Add Voter modal");
      setSelectedVoter(null);
      setIsEditingVoter(false);
      setShowAddVoterModal(true);  // Fixed: was using setShowVoterModal
    }, []);
    
    const handleImportCSV = useCallback(() => {
      console.log("Opening Import CSV modal");
      setShowImportModal(true);
    }, []);
    
    const handleVerifyVoters = useCallback(async () => {
      console.log("Verifying voters");
      setLoading(true);
      try {
        const response = await apiCall('/voters/verify', {
          method: 'POST'
        });
        loadData('voters');
        // Only show success alert if API call was successful
        if (response) {
          alert("Voter verification process initiated successfully");
        }
      } catch (error) {
        console.error("Error verifying voters:", error);
        // Only show alert for actual errors, not for expected API unavailability
        if (error.message && !error.message.includes('Failed to fetch')) {
          alert("Failed to verify voters. Please try again.");
        }
      } finally {
        setLoading(false);
      }
    }, []);
    
    const handleExportList = useCallback(async () => {
      console.log("Exporting voter list");
      try {
        // First refresh the voters data to ensure we have the latest
        const token = secureStorage.getToken();
        const votersResponse = await fetch(`${BASE_API_URL}/api/admin/voters`, {
          credentials: 'include',
          headers: {
            'Content-Type': 'application/json',
            ...(token ? { 'Authorization': `Bearer ${token}` } : {})
          }
        });
        
        if (votersResponse.ok) {
          const latestVoters = await votersResponse.json();
          // Update the UI with latest data
          setData(prevData => ({
            ...prevData,
            voters: latestVoters
          }));
          localStorage.setItem('voterow_voters', JSON.stringify(latestVoters));
        }
        
        // Now export the voters
        const response = await fetch(`${BASE_API_URL}/api/admin/voters/export`, {
          credentials: 'include',
          headers: getAuthHeaders()
        });
        
        if (response.ok) {
          const blob = await response.blob();
          const url = window.URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.style.display = 'none';
          a.href = url;
          a.download = 'voters.csv';
          document.body.appendChild(a);
          a.click();
          window.URL.revokeObjectURL(url);
          alert("Voters list exported successfully");
        } else {
          alert("Failed to export voters list");
        }
      } catch (error) {
        console.error("Error exporting voters:", error);
        alert("Failed to export voters list. Please try again.");
      }
    }, [setData]);

    // Voter action handlers
    const handleEditVoter = useCallback((voter) => {
      console.log("Editing voter:", voter);
      // Set selected voter for editing
      setSelectedVoter(voter);
      setShowAddVoterModal(true);  // Fixed: was using setShowVoterModal
      setIsEditingVoter(true);
    }, []);

    const handleToggleVoterStatus = useCallback(async (voter) => {
      console.log("Toggling voter status:", voter);
      try {
        const response = await fetch(`${BASE_API_URL}/api/admin/voters/${voter.id}/toggle-status`, {
          method: 'PUT',
          credentials: 'include',
          headers: getAuthHeaders(),
          body: JSON.stringify({ isActive: !voter.isActive })
        });

        if (response.ok) {
          // Update voters list locally
          const updatedVoters = voters.map(v => 
            v.id === voter.id ? { ...v, isActive: !v.isActive } : v
          );
          setData(prevData => ({
            ...prevData,
            voters: updatedVoters
          }));
          // Update localStorage cache
          localStorage.setItem('votersData', JSON.stringify(updatedVoters));
          alert(`Voter ${voter.isActive ? 'deactivated' : 'activated'} successfully`);
        } else {
          alert("Failed to update voter status");
        }
      } catch (error) {
        console.error("Error toggling voter status:", error);
        alert("Failed to update voter status. Please try again.");
      }
    }, [voters, setData]);

    const handleToggleVoterVerification = useCallback(async (voter) => {
      console.log("Toggling voter verification:", voter);
      try {
        const response = await fetch(`${BASE_API_URL}/api/admin/voters/${voter.id}/toggle-verification`, {
          method: 'PUT',
          credentials: 'include',
          headers: getAuthHeaders(),
          body: JSON.stringify({ isVerified: !voter.isVerified })
        });

        if (response.ok) {
          // Update voters list locally
          const updatedVoters = voters.map(v => 
            v.id === voter.id ? { ...v, isVerified: !v.isVerified } : v
          );
          setData(prevData => ({
            ...prevData,
            voters: updatedVoters
          }));
          // Update localStorage cache
          localStorage.setItem('votersData', JSON.stringify(updatedVoters));
          alert(`Voter ${voter.isVerified ? 'unverified' : 'verified'} successfully`);
        } else {
          alert("Failed to update voter verification status");
        }
      } catch (error) {
        console.error("Error toggling voter verification:", error);
        alert("Failed to update voter verification. Please try again.");
      }
    }, [voters, setData]);

    const handleDeleteVoter = useCallback(async (voter) => {
      if (!voter.id) {
        alert("Error: Voter ID is missing");
        return;
      }
      
      if (window.confirm(`Are you sure you want to delete voter "${voter.fullName || voter.name}"? This action cannot be undone.`)) {
        try {
          console.log('AdminDashboard: Deleting voter with ID:', voter.id);
          
          // Check if backend is accessible
          const token = secureStorage.getToken();
          if (!token) {
            throw new Error('Not authenticated. Please log in again.');
          }
          
          // Use DELETE endpoint for hard delete
          console.log('AdminDashboard: Calling DELETE endpoint for voter:', voter.id);
          const response = await apiCall(`/voters/${voter.id}`, { method: 'DELETE' });
          console.log('AdminDashboard: Delete response:', response);
          
          // DELETE endpoint returns {success: true, message: "..."} 
          // Check for success in various formats
          if (response && (response.success === true || (response.success !== false && response.message) || response.id)) {
            console.log('AdminDashboard: Voter deleted successfully:', response);
            
            // Remove from local state
            const updatedVoters = voters.filter(v => v.id !== voter.id);
            setData(prevData => ({
              ...prevData,
              voters: updatedVoters
            }));
            localStorage.setItem('voterow_voters', JSON.stringify(updatedVoters));
            
            // Refresh from database
            await loadData('voters');
            
            alert('Voter deleted successfully');
          } else {
            throw new Error(response?.message || response?.error || 'Delete failed');
          }
        } catch (error) {
          console.error('AdminDashboard: Error deleting voter:', error);
          
          // Provide more specific error messages
          let errorMessage = 'Failed to delete voter';
          if (error.message) {
            if (error.message.includes('fetch')) {
              errorMessage = 'Cannot connect to server. Please check if the backend is running.';
            } else if (error.message.includes('401') || error.message.includes('Unauthorized')) {
              errorMessage = 'Authentication failed. Please log in again.';
            } else if (error.message.includes('404')) {
              errorMessage = 'Voter not found. It may have already been deleted.';
            } else {
              errorMessage = `Failed to delete voter: ${error.message}`;
            }
          }
          
          alert(errorMessage);
        }
      }
    }, [voters, setData, loadData]);
    
    // Add debugging and error handling (reduced logging)
    // console.log("VoterManagementModule render - voters:", voters, "length:", voters?.length);
    
    if (!voters) {
      return (
        <div className="admin-module">
          <h3 className="module-title"><FaUsers /> Voter Management</h3>
          <div className="module-content">
            <p>Loading voters data...</p>
          </div>
        </div>
      );
    }
    
    return (
    <div className="admin-module">
      <h3 className="module-title"><FaUsers /> Voter Management</h3>
      <div className="module-content">
        <div className="action-buttons">
          <button className="btn btn-primary" onClick={handleAddVoter}><FaPlus /> Add Voter</button>
          <button className="btn btn-secondary" onClick={handleImportCSV}><FaUpload /> Import CSV</button>
          <button className="btn btn-info" onClick={handleVerifyVoters}><FaUserCheck /> Verify Voters</button>
          <button className="btn btn-success" onClick={handleExportList}><FaDownload /> Export List</button>
          <button className="btn btn-warning" onClick={async () => {
            try {
              // Clear cache first to ensure fresh data
              localStorage.removeItem('voterow_voters');
              localStorage.removeItem('votersData');
              
              const response = await fetch(`${BASE_API_URL}/api/admin/voters`, {
                credentials: 'include',
                headers: {
                  'Content-Type': 'application/json',
                  'Cache-Control': 'no-cache, no-store, must-revalidate',
                  'Pragma': 'no-cache'
                }
              });
              if (response.ok) {
                const freshVoters = await response.json();
                setData(prevData => ({
                  ...prevData,
                  voters: freshVoters
                }));
                localStorage.setItem('voterow_voters', JSON.stringify(freshVoters));
                alert(`Refreshed ${freshVoters.length} voters from database`);
              } else {
                alert('Failed to refresh voters data');
              }
            } catch (error) {
              console.error('Error refreshing voters:', error);
              alert('Error refreshing voters data');
            }
          }}><FaSyncAlt /> Refresh Data</button>
        </div>
        
        {/* Add Voter Modal */}
        {showAddVoterModal && (
          <div className="modal-overlay">
            <div className="modal-content">
              <div className="modal-header">
                <h3>Add New Voter</h3>
                <button className="close-btn" onClick={() => setShowAddVoterModal(false)}><FaTimes /></button>
              </div>
              <div className="modal-body">
                <form onSubmit={async (e) => {
                  e.preventDefault();
                  console.log("Submitting voter form", voterForm);
                  console.log("BASE_API_URL:", BASE_API_URL);
                  console.log("Full URL will be:", `${BASE_API_URL}/api/admin/voters`);
                  
                  try {
                    // Add API call here
                    const data = await apiCall('/voters', {
                      method: 'POST',
                      body: JSON.stringify(voterForm)
                    });
                    
                    console.log("API call successful, received data:", data);
                    
                    if (data) {
                      setShowAddVoterModal(false);
                      loadData('voters');
                      setVoterForm({ fullName: '', email: '', age: '', phone: '' });
                      alert("Voter added successfully!");
                    } else {
                      console.error("API call returned no data");
                      alert("Failed to add voter. No data returned from server.");
                    }
                  } catch (err) {
                    console.error("Error adding voter:", err);
                    console.error("Error details:", {
                      message: err.message,
                      stack: err.stack,
                      name: err.name
                    });
                    alert(`Failed to add voter. ${err?.message || 'Unknown error occurred'}`);
                  }
                }}>
                  <div className="form-group">
                    <label>Full Name</label>
                    <input 
                      type="text" 
                      value={voterForm.fullName} 
                      onChange={(e) => setVoterForm({...voterForm, fullName: e.target.value})} 
                      required 
                    />
                  </div>
                  <div className="form-group">
                    <label>Email</label>
                    <input 
                      type="email" 
                      value={voterForm.email} 
                      onChange={(e) => setVoterForm({...voterForm, email: e.target.value})} 
                      required 
                    />
                  </div>
                  <div className="form-group">
                    <label>Age</label>
                    <input 
                      type="number" 
                      value={voterForm.age} 
                      onChange={(e) => setVoterForm({...voterForm, age: e.target.value})} 
                      required 
                    />
                  </div>
                  <div className="form-group">
                    <label>Phone</label>
                    <input 
                      type="tel" 
                      value={voterForm.phone} 
                      onChange={(e) => setVoterForm({...voterForm, phone: e.target.value})} 
                    />
                  </div>
                  <div className="form-actions">
                    <button type="button" className="btn btn-secondary" onClick={() => setShowAddVoterModal(false)}>Cancel</button>
                    <button type="submit" className="btn btn-primary">Add Voter</button>
                  </div>
                </form>
              </div>
            </div>
          </div>
        )}
        
        {/* Import CSV Modal */}
        {showImportModal && (
          <div className="modal-overlay">
            <div className="modal-content">
              <div className="modal-header">
                <h3>Import Voters CSV</h3>
                <button className="close-btn" onClick={() => setShowImportModal(false)}><FaTimes /></button>
              </div>
              <div className="modal-body">
                <form onSubmit={(e) => {
                  e.preventDefault();
                  if (!importFile) {
                    alert("Please select a CSV file to import");
                    return;
                  }
                  
                  const formData = new FormData();
                  formData.append('file', importFile);
                  
                  fetch(`${BASE_API_URL}/api/admin/voters/import`, {
                    method: 'POST',
                    body: formData,
                    // Don't set Content-Type header - let browser set it automatically for FormData
                    // This ensures proper multipart/form-data boundary is set
                    credentials: 'include'
                  })
                  .then(response => {
                    if (response.ok) {
                      setShowImportModal(false);
                      loadData('voters');
                      setImportFile(null);
                      alert("Voters imported successfully");
                    } else {
                      alert("Failed to import voters");
                    }
                  })
                  .catch(err => {
                    console.error("Error importing voters:", err);
                    alert("Failed to import voters. Please try again.");
                  });
                }}>
                  <div className="form-group">
                    <label>CSV File</label>
                    <input 
                      type="file" 
                      accept=".csv" 
                      onChange={(e) => setImportFile(e.target.files[0])} 
                      required 
                    />
                  </div>
                  <div className="form-group">
                    <small>The CSV file should contain the following columns: fullName, email, age, phone</small>
                  </div>
                  <div className="form-actions">
                    <button type="button" className="btn btn-secondary" onClick={() => setShowImportModal(false)}>Cancel</button>
                    <button type="submit" className="btn btn-primary">Import</button>
                  </div>
                </form>
              </div>
            </div>
          </div>
        )}
        <div className="data-table">
          <h4>All Voters ({voters?.length || 0})</h4>
          {voters && voters.length > 0 ? (
            <table className="table">
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Email</th>
                  <th>Age</th>
                  <th>Status</th>
                  <th>Verified</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {voters.map(voter => (
                  <tr key={voter.id}>
                    <td>{voter.fullName}</td>
                    <td>{voter.email}</td>
                    <td>{voter.age}</td>
                    <td>
                      <span className={`status ${voter.isActive ? 'active' : 'inactive'}`}>
                        {voter.isActive ? 'Active' : 'Inactive'}
                      </span>
                    </td>
                    <td>
                      <span className={`status ${voter.isVerified ? 'verified' : 'pending'}`}>
                        {voter.isVerified ? 'Verified' : 'Pending'}
                      </span>
                    </td>
                    <td>
                      <button 
                        className="btn btn-sm btn-primary" 
                        onClick={() => handleEditVoter(voter)}
                        title="Edit Voter"
                      >
                        <FaEdit />
                      </button>
                      <button 
                        className="btn btn-sm btn-warning" 
                        onClick={() => handleToggleVoterVerification(voter)}
                        title={voter.isVerified ? "Mark as Unverified" : "Mark as Verified"}
                      >
                        <FaToggleOn />
                      </button>
                      <button 
                        className="btn btn-sm btn-danger" 
                        onClick={() => handleDeleteVoter(voter)}
                        title="Delete Voter"
                      >
                        <FaTrash />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          ) : (
            <p>No voters found.</p>
          )}
        </div>
      </div>

      {/* Edit Voter Modal */}
      {showVoterModal && (
        <UserModal 
          isOpen={showVoterModal}
          onClose={() => {
            setShowVoterModal(false);
            setSelectedVoter(null);
            setIsEditingVoter(false);
          }}
          user={selectedVoter}
          title={isEditingVoter ? "Edit Voter" : "Add Voter"}
          onSave={async (updatedUser, userId) => {
            try {
              console.log('AdminDashboard: Saving voter:', userId ? 'Update' : 'Create', updatedUser);
              
              let result;
              if (userId) {
                // Update existing voter
                result = await apiCall(`/voters/${userId}`, {
                  method: 'PUT',
                  body: JSON.stringify(updatedUser)
                });
              } else {
                // Create new voter - map form fields to API format
                // Backend expects: fullName, email, age, phone (not phoneNumber)
                // UserModal sends: fullName, email, password, age, userType
                const voterData = {
                  fullName: updatedUser.fullName || updatedUser.name || '',
                  email: updatedUser.email || '',
                  age: updatedUser.age ? (typeof updatedUser.age === 'string' ? parseInt(updatedUser.age) : updatedUser.age) : null,
                  phone: updatedUser.phoneNumber || updatedUser.phone || ''
                };
                
                // Validate required fields
                if (!voterData.fullName || !voterData.email) {
                  throw new Error('Full name and email are required');
                }
                
                // Validate age
                if (!voterData.age || voterData.age < 18) {
                  throw new Error('Age must be at least 18');
                }
                
                console.log('AdminDashboard: Creating voter with data:', voterData);
                console.log('AdminDashboard: API URL will be:', `${BASE_API_URL}/api/admin/voters`);
                
                try {
                  result = await apiCall('/voters', {
                    method: 'POST',
                    body: JSON.stringify(voterData)
                  });
                  console.log('AdminDashboard: Voter creation response:', result);
                } catch (apiError) {
                  console.error('AdminDashboard: API call failed:', apiError);
                  // Re-throw with more context
                  throw apiError;
                }
              }

              if (result && (result.id || result.success !== false)) {
                console.log('AdminDashboard: Voter saved successfully:', result);
                
                if (userId) {
                  // Update voters list locally for edit
                  const updatedVoters = voters.map(v => 
                    v.id === userId ? { ...v, ...result } : v
                  );
                  setData(prevData => ({
                    ...prevData,
                    voters: updatedVoters
                  }));
                  localStorage.setItem('voterow_voters', JSON.stringify(updatedVoters));
                  
                  // Refresh from database
                  await loadData('voters');
                  
                  alert("Voter updated successfully");
                } else {
                  // Add new voter to list for create
                  const newVoters = [...voters, result];
                  setData(prevData => ({
                    ...prevData,
                    voters: newVoters
                  }));
                  localStorage.setItem('voterow_voters', JSON.stringify(newVoters));
                  
                  // Refresh from database
                  await loadData('voters');
                  
                  alert("Voter created successfully");
                }
                
                // Close modal
                setShowVoterModal(false);
                setSelectedVoter(null);
                setIsEditingVoter(false);
              } else {
                throw new Error(result?.message || result?.error || 'Invalid response from server');
              }
            } catch (error) {
              console.error(`Error ${userId ? 'updating' : 'creating'} voter:`, error);
              
              let errorMessage = `Failed to ${userId ? 'update' : 'create'} voter`;
              if (error.message) {
                if (error.message.includes('Email already exists') || error.message.includes('409')) {
                  errorMessage = 'Email already exists. Please use a different email address.';
                } else if (error.message.includes('fetch') || error.message.includes('connect to server') || error.message.includes('Network')) {
                  errorMessage = 'Cannot connect to server. Please check if the backend server (http://localhost:8083) is running.';
                } else if (error.message.includes('401') || error.message.includes('Unauthorized')) {
                  errorMessage = 'Authentication failed. Please log out and log in again.';
                } else {
                  errorMessage = `Failed to ${userId ? 'update' : 'create'} voter: ${error.message}`;
                }
              }
              
              alert(errorMessage);
              // Don't close modal on error so user can retry
            }
          }}
        />
      )}
    </div>
  );
  };

  // Module 3: Candidate Management Component
  const CandidateManagementModule = () => {
    // State for candidate management
    const [showAddCandidateModal, setShowAddCandidateModal] = useState(false);
    const [showSymbolUploadModal, setShowSymbolUploadModal] = useState(false);
    const [showApproveModal, setShowApproveModal] = useState(false);
    const [selectedCandidate, setSelectedCandidate] = useState(null);
    const [candidateForm, setCandidateForm] = useState({
      name: '',
      party: '',
      email: '',
      phone: ''
    });
    const [symbolFile, setSymbolFile] = useState(null);
    const [pendingCandidates, setPendingCandidates] = useState([]);
    
    // Function to load candidates data - wrapped in useCallback to prevent unnecessary re-renders
    // MUST be defined BEFORE the useEffect that calls it
    const loadData2 = useCallback(async (type) => {
      try {
        // Try to load from API first
        const apiCandidates = await apiCall('/candidates');
        
        if (apiCandidates && Array.isArray(apiCandidates)) {
          setData(prev => ({ ...prev, candidates: apiCandidates }));
          
          const pending = apiCandidates.filter(c => 
            c.status === 'PENDING' || (!c.isVerified && c.status !== 'APPROVED')
          );
          setPendingCandidates(pending);
          return;
        }
        
        // Fallback to localStorage if API fails
        console.log('AdminDashboard: API failed, using localStorage fallback');
        const localCandidates = JSON.parse(localStorage.getItem('voterow_candidates')) || [];
        setData(prev => ({ ...prev, candidates: localCandidates }));
        const pending = localCandidates.filter(c => c.status?.toLowerCase() === 'pending');
        setPendingCandidates(pending);
        
      } catch (error) {
        console.error(`Error loading ${type} data:`, error);
        // Final fallback to localStorage
        const localCandidates = JSON.parse(localStorage.getItem('voterow_candidates')) || [];
        setData(prev => ({ ...prev, candidates: localCandidates }));
        const pending = localCandidates.filter(c => c.status?.toLowerCase() === 'pending');
        setPendingCandidates(pending);
      }
    }, []); // No dependencies to prevent infinite loops
    
    // Refresh candidates from database when candidates tab is opened - load ONCE
    useEffect(() => {
      if (activeTab === 'candidates') {
        console.log('AdminDashboard: Candidates tab opened, loading from database');
        loadData2('candidates');
      }
    }, [activeTab]); // ONLY depend on activeTab
    
    // Handle add candidate button
    const handleAddCandidate = () => {
      setShowAddCandidateModal(true);
    };
    
    // Handle upload symbol button
    const handleUploadSymbol = () => {
      setShowSymbolUploadModal(true);
    };
    
    // Handle approve candidates button
    const handleApproveCandidate = async () => {
      console.log('AdminDashboard: Loading candidates for approval...');
      
      // First try to load fresh data from API
      try {
        const candidates = await apiCall('/candidates');
        if (candidates && Array.isArray(candidates)) {
          console.log('AdminDashboard: Fresh candidates loaded from API:', candidates.length);
          setData(prev => ({ ...prev, candidates }));
          
          // Filter for pending candidates (both PENDING and unverified)
          const pending = candidates.filter(c => {
            const status = c.status?.toLowerCase();
            const isVerified = c.isVerified;
            return status === 'pending' || (!isVerified && status !== 'approved' && status !== 'rejected');
          });
          
          console.log('AdminDashboard: Found pending candidates:', pending.length);
          console.log('AdminDashboard: Pending candidates details:', pending);
          setPendingCandidates(pending);
        } else {
          // Fallback to existing data
          const currentCandidates = data.candidates || [];
          const pending = currentCandidates.filter(c => {
            const status = c.status?.toLowerCase();
            const isVerified = c.isVerified;
            return status === 'pending' || (!isVerified && status !== 'approved' && status !== 'rejected');
          });
          
          console.log('AdminDashboard: Using existing candidates, found pending:', pending.length);
          setPendingCandidates(pending);
        }
      } catch (error) {
        console.error('AdminDashboard: Error loading candidates for approval:', error);
        
        // Fallback to existing data
        const currentCandidates = data.candidates || [];
        const pending = currentCandidates.filter(c => {
          const status = c.status?.toLowerCase();
          const isVerified = c.isVerified;
          return status === 'pending' || (!isVerified && status !== 'approved' && status !== 'rejected');
        });
        
        console.log('AdminDashboard: Error fallback, found pending candidates:', pending.length);
        setPendingCandidates(pending);
      }
      
      setShowApproveModal(true);
    };
    
    // Handle edit button for a candidate
    const handleEditCandidate = (candidate) => {
      setSelectedCandidate(candidate);
      setCandidateForm({
        name: candidate.name || '',
        party: candidate.party || '',
        email: candidate.email || '',
        phone: candidate.phone || ''
      });
      setShowAddCandidateModal(true);
    };
    
    // Handle status change for a candidate
    const handleCandidateStatus = async (candidate, status) => {
      try {
        console.log('AdminDashboard: Updating candidate status:', candidate.name, 'to', status);
        
        // Try API call first
        try {
          await apiCall(`/candidates/${candidate.id}/status`, {
            method: 'POST',
            body: JSON.stringify({ 
              status: status.toUpperCase() 
            })
          });
          console.log('AdminDashboard: Successfully updated candidate status in database');
        } catch (apiError) {
          console.log('AdminDashboard: API call failed, updating locally only:', apiError);
        }
        
        // Update local UI state immediately regardless of API success
        setData(prev => ({
          ...prev,
          candidates: prev.candidates.map(c => 
            c.id === candidate.id ? { ...c, status: status.toUpperCase() } : c
          )
        }));
        
        // Auto-assign approved candidates to all active elections
        if (status.toUpperCase() === 'APPROVED') {
          const elections = JSON.parse(localStorage.getItem('voterow_elections')) || [];
          let assignedCount = 0;
          
          const updatedElections = elections.map(election => {
            if (election.status !== 'COMPLETED') {
              const participants = election.participants || [];
              if (!participants.includes(candidate.id)) {
                participants.push(candidate.id);
                assignedCount++;
                console.log(`Auto-assigned candidate ${candidate.name} to election ${election.title}`);
                return { ...election, participants, candidateCount: participants.length };
              }
            }
            return election;
          });
          
          if (assignedCount > 0) {
            localStorage.setItem('voterow_elections', JSON.stringify(updatedElections));
            setData(prev => ({ ...prev, elections: updatedElections }));
          }
        }
        
        // Update pending candidates list
        setPendingCandidates(prev => prev.filter(c => c.id !== candidate.id));
        
        console.log(`AdminDashboard: Candidate ${candidate.name} ${status.toLowerCase()} successfully`);
        alert(`Candidate ${status.toLowerCase() === 'approved' ? 'approved and assigned to election' : 'rejected'} successfully`);
        
      } catch (error) {
        console.error(`Error updating candidate status:`, error);
        alert(`Error updating candidate status: ${error.message}`);
      }
    };
    
    // Handle symbol upload for a candidate
    const handleSymbolUpload = async (candidate) => {
      if (!symbolFile) {
        alert('Please select a file to upload');
        return;
      }
      
      try {
        console.log('Uploading symbol file:', symbolFile.name, 'for candidate:', candidate.id);
        
        // Use FormData for proper file upload handling
        const formData = new FormData();
        formData.append('file', symbolFile);
        
        // Use direct fetch instead of apiCall to avoid content-type issues
        const token = secureStorage.getToken();
        const response = await fetch(`${BASE_API_URL}/api/admin/candidates/${candidate.id}/symbol`, {
          method: 'POST',
          body: formData,
          headers: {
            ...(token ? { 'Authorization': `Bearer ${token}` } : {})
            // Don't set Content-Type - let browser handle it for FormData
          },
          credentials: 'include'
        });
        
        const result = await response.json();
        
        if (response.ok && result) {
          alert('Symbol uploaded successfully');
          setSymbolFile(null);
          setShowSymbolUploadModal(false);
          loadData('candidates');
        } else {
          console.error('Symbol upload failed:', response.status, result);
          alert('Failed to upload symbol. Please try again.');
        }
      } catch (error) {
        console.error('Symbol upload error:', error);
        console.error('Error details:', {
          message: error.message,
          stack: error.stack,
          name: error.name
        });
        console.error(`Error uploading symbol:`, error);
        alert(`Error uploading symbol: ${error.message}`);
      }
    };
    
    // Handle form submission for adding/editing a candidate
    const handleSubmitCandidateForm = async (e) => {
      e.preventDefault();
      
      // Basic form validation
      if (!candidateForm.name || !candidateForm.email) {
        alert('Please fill in required fields (Name, Email)');
        return;
      }
      
      // Email format validation
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (!emailRegex.test(candidateForm.email)) {
        alert('Please enter a valid email address');
        return;
      }
      
      try {
        console.log('AdminDashboard: Submitting candidate form:', selectedCandidate ? 'Update' : 'Create', candidateForm);
        
        if (selectedCandidate) {
          // Edit existing candidate
          const result = await apiCall(`/candidates/${selectedCandidate.id}`, {
            method: 'PUT',
            body: JSON.stringify(candidateForm)
          });
          
          if (result && result.id) {
            console.log('AdminDashboard: Candidate updated successfully:', result);
            
            setData(prev => ({
              ...prev,
              candidates: prev.candidates.map(c => 
                c.id === selectedCandidate.id ? { ...c, ...result } : c
              )
            }));
            
            // Refresh from database
            await loadData2('candidates');
            
            alert('Candidate updated successfully');
            setShowAddCandidateModal(false);
            setSelectedCandidate(null);
            setCandidateForm({ name: '', party: '', email: '', phone: '' });
          } else {
            throw new Error('Invalid response from server');
          }
        } else {
          // Add new candidate
          const result = await apiCall('/candidates', {
            method: 'POST',
            body: JSON.stringify(candidateForm)
          });
          
          if (result && result.id) {
            console.log('AdminDashboard: Candidate created successfully in database:', result);
            
            // Refresh from database immediately
            await loadData2('candidates');
            
            alert(`Candidate "${result.name}" created successfully!`);
            
            setShowAddCandidateModal(false);
            setCandidateForm({ name: '', party: '', email: '', phone: '' });
          } else {
            throw new Error('Invalid response from server');
          }
        }
      } catch (error) {
        console.error('Error submitting candidate form:', error);
        
        // Handle specific error types
        let errorMessage = `Failed to ${selectedCandidate ? 'update' : 'add'} candidate`;
        if (error.message) {
          if (error.message.includes('409') || error.message.includes('Email already exists')) {
            errorMessage = 'Email already exists! Please use a different email address.';
          } else if (error.message.includes('500')) {
            errorMessage = 'Server error occurred. Please check if the backend is running and try again.';
          } else if (error.message.includes('Failed to fetch') || error.message.includes('Failed to connect')) {
            errorMessage = `Cannot connect to server. Please check if the backend server (${BASE_API_URL}) is running.`;
          } else {
            errorMessage = `${errorMessage}: ${error.message}`;
          }
        }
        
        alert(errorMessage);
      }
    };
    
    return (
      <div className="admin-module">
        <h3 className="module-title"><FaUserTie /> Candidate/Party Management</h3>
        <div className="module-content">
          <div className="action-buttons">
            <button className="btn btn-primary" onClick={handleAddCandidate}><FaPlus /> Add Candidate</button>
            <button className="btn btn-success" onClick={async () => {
              try {
                const token = secureStorage.getToken();
                const response = await fetch(`${BASE_API_URL}/api/admin/status`, {
                  headers: {
                    ...(token ? { 'Authorization': `Bearer ${token}` } : {})
                  }
                });
                if (response.ok) {
                  const status = await response.json();
                  alert(`✅ System Health: ${status.status}\nDatabase: ${status.database}\nUsers: ${status.userCount}\nElections: ${status.electionCount}\nCandidates: ${status.candidateCount}`);
                } else {
                  alert('⚠️ Backend responding but may have issues');
                }
              } catch (error) {
                alert(`❌ Backend server not running at ${BASE_API_URL}`);
              }
            }}>System Health</button>
            <button className="btn btn-secondary" onClick={handleUploadSymbol}><FaUpload /> Upload Symbols</button>
            <button className="btn btn-info" onClick={handleApproveCandidate}><FaUserCheck /> Approve Candidates</button>
            <button className="btn btn-secondary" onClick={async () => {
              try {
                console.log('Refreshing candidates from database...');
                const candidates = await apiCall('/candidates');
                console.log('API response:', candidates);
                
                if (candidates && Array.isArray(candidates)) {
                  setData(prev => ({ ...prev, candidates }));
                  const pending = candidates.filter(c => 
                    c.status === 'PENDING' || (!c.isVerified && c.status !== 'APPROVED')
                  );
                  setPendingCandidates(pending);
                  alert(`✅ Loaded ${candidates.length} candidates from database (${pending.length} pending)`);
                } else {
                  console.error('Invalid candidates response:', candidates);
                  alert('❌ Invalid response from database');
                }
              } catch (error) {
                console.error('Error loading candidates:', error);
                alert(`❌ Failed to load candidates: ${error.message}`);
              }
            }}><FaSyncAlt /> Refresh</button>
            <button className="btn btn-warning" onClick={async () => {
              try {
                // Get fresh data
                const [candidatesResponse, electionsResponse] = await Promise.all([
                  apiCall('/candidates'),
                  apiCall('/elections')
                ]);
                
                const approvedCandidates = (candidatesResponse || []).filter(c => c.status === 'APPROVED');
                const elections = electionsResponse || [];
                
                if (approvedCandidates.length === 0) {
                  alert('No approved candidates found. Please approve some candidates first.');
                  return;
                }
                
                if (elections.length === 0) {
                  alert('No elections found. Please create an election first.');
                  return;
                }
                
                let totalAssigned = 0;
                
                // Assign to each active election
                for (const election of elections) {
                  if (election.status !== 'COMPLETED') {
                    try {
                      const response = await fetch(`${BASE_API_URL}/api/voting/assign-candidates/${election.id}`, {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' }
                      });
                      if (response.ok) {
                        const result = await response.json();
                        totalAssigned += result.assignedCount || 0;
                      }
                    } catch (err) {
                      console.warn(`Failed to assign to election ${election.id}:`, err);
                    }
                  }
                }
                
                alert(`✅ Auto-Assignment Complete!\n${approvedCandidates.length} approved candidates\n${totalAssigned} assignments made`);
                loadElections(); // Refresh elections
              } catch (error) {
                alert('❌ Auto-assignment failed: ' + error.message);
              }
            }}>🚀 Fix & Assign All</button>
          </div>
          <div className="data-table">
            <h4>All Candidates ({data.candidates?.length || 0})</h4>
            {data.candidates && data.candidates.length > 0 ? (
              <table className="table">
                <thead>
                  <tr>
                    <th>Name</th>
                    <th>Party</th>
                    <th>Election</th>
                    <th>Status</th>
                    <th>Symbol</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {data.candidates.map(candidate => (
                    <tr key={candidate.id}>
                      <td>{candidate.name}</td>
                      <td>{candidate.party || 'Independent'}</td>
                      <td>{candidate.election?.title || 'Not Assigned'}</td>
                      <td>
                        <span className={`status ${candidate.status?.toLowerCase()}`}>
                          {candidate.status}
                        </span>
                      </td>
                      <td>
                        {candidate.symbolUrl ? 
                          <img src={candidate.symbolUrl} alt="Symbol" className="symbol-image" style={{width: "30px", height: "30px"}} /> : 
                          'No Symbol'
                        }
                      </td>
                      <td>
                        <button className="btn btn-sm btn-primary" onClick={() => handleEditCandidate(candidate)}><FaEdit /></button>
                        <button 
                          className="btn btn-sm btn-success" 
                          onClick={() => handleCandidateStatus(candidate, 'APPROVED')}
                          disabled={candidate.status === 'APPROVED'}
                        >
                          <FaUserCheck />
                        </button>
                        <button 
                          className="btn btn-sm btn-danger"
                          onClick={() => handleCandidateStatus(candidate, 'REJECTED')}
                          disabled={candidate.status === 'REJECTED'}
                        >
                          Reject
                        </button>
                        <button 
                          className="btn btn-sm btn-danger"
                          onClick={async () => {
                            if (window.confirm(`Delete candidate "${candidate.name}"? This action cannot be undone.`)) {
                              try {
                                console.log('AdminDashboard: Deleting candidate with ID:', candidate.id);
                                
                                // Check if backend is accessible
                                const token = secureStorage.getToken();
                                if (!token) {
                                  throw new Error('Not authenticated. Please log in again.');
                                }
                                
                                const response = await apiCall(`/candidates/${candidate.id}`, { method: 'DELETE' });
                                
                                if (response && (response.success !== false)) {
                                  console.log('AdminDashboard: Candidate deleted successfully:', response);
                                  
                                  // Remove from local state
                                  const updatedCandidates = data.candidates.filter(c => c.id !== candidate.id);
                                  setData(prev => ({ ...prev, candidates: updatedCandidates }));
                                  localStorage.setItem('voterow_candidates', JSON.stringify(updatedCandidates));
                                  
                                  // Refresh from database
                                  await loadData2('candidates');
                                  
                                  alert('Candidate deleted successfully');
                                } else {
                                  throw new Error(response?.message || 'Delete failed');
                                }
                              } catch (err) {
                                console.error('AdminDashboard: Error deleting candidate:', err);
                                
                                // Provide more specific error messages
                                let errorMessage = 'Failed to delete candidate';
                                if (err.message) {
                                  if (err.message.includes('fetch')) {
                                    errorMessage = 'Cannot connect to server. Please check if the backend is running.';
                                  } else if (err.message.includes('401') || err.message.includes('Unauthorized')) {
                                    errorMessage = 'Authentication failed. Please log in again.';
                                  } else if (err.message.includes('404')) {
                                    errorMessage = 'Candidate not found. It may have already been deleted.';
                                  } else {
                                    errorMessage = `Failed to delete candidate: ${err.message}`;
                                  }
                                }
                                
                                alert(errorMessage);
                              }
                            }
                          }}
                        >
                          <FaTrash />
                        </button>
                        {candidate.status === 'APPROVED' && (
                          <button 
                            className="btn btn-sm btn-info"
                            onClick={() => {
                              const elections = JSON.parse(localStorage.getItem('voterow_elections')) || [];
                              const election = elections.find(e => e.id === candidate.electionId);
                              if (election) {
                                const participants = election.participants || [];
                                if (!participants.includes(candidate.id)) {
                                  participants.push(candidate.id);
                                  const updatedElections = elections.map(e => 
                                    e.id === election.id ? { ...e, participants, candidateCount: participants.length } : e
                                  );
                                  localStorage.setItem('voterow_elections', JSON.stringify(updatedElections));
                                  setData(prev => ({ ...prev, elections: updatedElections }));
                                  alert(`${candidate.name} assigned to election participants`);
                                } else {
                                  alert(`${candidate.name} already assigned to election`);
                                }
                              }
                            }}
                            title="Assign to Election"
                          >
                            Assign
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            ) : (
              <p>No candidates found.</p>
            )}
          </div>
        </div>
        
        {/* Add/Edit Candidate Modal */}
        {showAddCandidateModal && (
          <div className="modal-overlay">
            <div className="modal-content">
              <div className="modal-header">
                <h3>{selectedCandidate ? 'Edit' : 'Add'} Candidate</h3>
                <button className="close-btn" onClick={() => {
                  setShowAddCandidateModal(false);
                  setSelectedCandidate(null);
                  setCandidateForm({ name: '', party: '', email: '', phone: '' });
                }}><FaTimes /></button>
              </div>
              <div className="modal-body">
                <form onSubmit={handleSubmitCandidateForm}>
                  <div className="form-group">
                    <label>Full Name</label>
                    <input 
                      type="text" 
                      value={candidateForm.name} 
                      onChange={(e) => setCandidateForm({...candidateForm, name: e.target.value})} 
                      required 
                    />
                  </div>
                  <div className="form-group">
                    <label>Party Name</label>
                    <input 
                      type="text" 
                      value={candidateForm.party} 
                      onChange={(e) => setCandidateForm({...candidateForm, party: e.target.value})} 
                      placeholder="Leave blank for Independent"
                    />
                  </div>
                  <div className="form-group">
                    <label>Email</label>
                    <input 
                      type="email" 
                      value={candidateForm.email} 
                      onChange={(e) => setCandidateForm({...candidateForm, email: e.target.value})} 
                      required 
                    />
                  </div>
                  <div className="form-group">
                    <label>Phone</label>
                    <input 
                      type="tel" 
                      value={candidateForm.phone} 
                      onChange={(e) => setCandidateForm({...candidateForm, phone: e.target.value})} 
                    />
                  </div>
                  <div className="form-actions">
                    <button type="button" className="btn btn-secondary" onClick={() => {
                      setShowAddCandidateModal(false);
                      setSelectedCandidate(null);
                      setCandidateForm({ name: '', party: '', email: '', phone: '' });
                    }}>Cancel</button>
                    <button type="submit" className="btn btn-primary">
                      {selectedCandidate ? 'Update' : 'Add'} Candidate
                    </button>
                  </div>
                </form>
              </div>
            </div>
          </div>
        )}
        
        {/* Symbol Upload Modal */}
        {showSymbolUploadModal && (
          <div className="modal-overlay">
            <div className="modal-content">
              <div className="modal-header">
                <h3>Upload Candidate Symbol</h3>
                <button className="close-btn" onClick={() => {
                  setShowSymbolUploadModal(false);
                  setSymbolFile(null);
                }}><FaTimes /></button>
              </div>
              <div className="modal-body">
                <div className="form-group">
                  <label>Select Candidate</label>
                  <select 
                    className="form-control" 
                    onChange={(e) => {
                      const candidate = data.candidates.find(c => c.id === parseInt(e.target.value));
                      setSelectedCandidate(candidate);
                    }}
                    required
                  >
                    <option value="">-- Select Candidate --</option>
                    {data.candidates.map(candidate => (
                      <option key={candidate.id} value={candidate.id}>{candidate.name} ({candidate.party || 'Independent'})</option>
                    ))}
                  </select>
                </div>
                <div className="form-group">
                  <label>Upload Symbol</label>
                  <input 
                    type="file" 
                    className="form-control"
                    accept="image/*"
                    onChange={(e) => setSymbolFile(e.target.files[0])}
                    required
                  />
                </div>
                <div className="form-actions">
                  <button className="btn btn-secondary" onClick={() => {
                    setShowSymbolUploadModal(false);
                    setSymbolFile(null);
                    setSelectedCandidate(null);
                  }}>Cancel</button>
                  <button 
                    className="btn btn-primary" 
                    onClick={() => handleSymbolUpload(selectedCandidate)}
                    disabled={!selectedCandidate || !symbolFile}
                  >
                    Upload Symbol
                  </button>
                </div>
              </div>
            </div>
          </div>
        )}
        
        {/* Approve Candidates Modal */}
        {showApproveModal && (
          <div className="modal-overlay">
            <div className="modal-content">
              <div className="modal-header">
                <h3>Approve Candidates ({pendingCandidates.length} pending)</h3>
                <button className="close-btn" onClick={() => setShowApproveModal(false)}><FaTimes /></button>
              </div>
              <div className="modal-body">
                {pendingCandidates.length > 0 ? (
                  <>
                    <div className="approval-info">
                      <p><strong>Found {pendingCandidates.length} candidate(s) awaiting approval:</strong></p>
                    </div>
                    <table className="table">
                      <thead>
                        <tr>
                          <th>Name</th>
                          <th>Email</th>
                          <th>Party</th>
                          <th>Applied Date</th>
                          <th>Status</th>
                          <th>Actions</th>
                        </tr>
                      </thead>
                      <tbody>
                        {pendingCandidates.map(candidate => (
                          <tr key={candidate.id}>
                            <td>{candidate.name}</td>
                            <td>{candidate.email}</td>
                            <td>{candidate.party || 'Independent'}</td>
                            <td>{candidate.appliedDate ? new Date(candidate.appliedDate).toLocaleDateString() : 'N/A'}</td>
                            <td>
                              <span className="status pending">
                                {candidate.status || 'PENDING'}
                              </span>
                            </td>
                            <td>
                              <button 
                                className="btn btn-sm btn-success"
                                onClick={() => handleCandidateStatus(candidate, 'APPROVED')}
                                title="Approve this candidate"
                              >
                                ✓ Approve
                              </button>
                              <button 
                                className="btn btn-sm btn-danger"
                                onClick={() => handleCandidateStatus(candidate, 'REJECTED')}
                                title="Reject this candidate"
                              >
                                ✗ Reject
                              </button>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </>
                ) : (
                  <div className="empty-state">
                    <p><strong>No pending candidates to approve.</strong></p>
                    <p>Candidates will appear here when they submit applications for elections.</p>
                    <div style={{ marginTop: '15px' }}>
                      <button 
                        className="btn btn-primary"
                        onClick={async () => {
                          // Refresh candidates data
                          try {
                            const candidates = await apiCall('/candidates');
                            if (candidates && Array.isArray(candidates)) {
                              setData(prev => ({ ...prev, candidates }));
                              const pending = candidates.filter(c => {
                                const status = c.status?.toLowerCase();
                                const isVerified = c.isVerified;
                                return status === 'pending' || (!isVerified && status !== 'approved' && status !== 'rejected');
                              });
                              setPendingCandidates(pending);
                              if (pending.length > 0) {
                                alert(`Found ${pending.length} pending candidates after refresh!`);
                              }
                            } else {
                              // Check localStorage
                              const localCandidates = JSON.parse(localStorage.getItem('voterow_candidates')) || [];
                              if (localCandidates.length > 0) {
                                setData(prev => ({ ...prev, candidates: localCandidates }));
                                const pending = localCandidates.filter(c => {
                                  const status = c.status?.toLowerCase();
                                  const isVerified = c.isVerified;
                                  return status === 'pending' || (!isVerified && status !== 'approved' && status !== 'rejected');
                                });
                                setPendingCandidates(pending);
                                if (pending.length > 0) {
                                  alert(`Found ${pending.length} pending candidates in local storage!`);
                                } else {
                                  alert('No pending candidates found. Make sure candidates have submitted applications.');
                                }
                              } else {
                                alert('No candidate data found. Candidates need to submit applications first.');
                              }
                            }
                          } catch (error) {
                            console.error('Error refreshing candidates:', error);
                            alert('Error refreshing data. Check console for details.');
                          }
                        }}
                      >
                        🔄 Refresh Candidates
                      </button>
                    </div>
                  </div>
                )}
                <div className="form-actions">
                  <button className="btn btn-secondary" onClick={() => setShowApproveModal(false)}>Close</button>
                </div>
              </div>
            </div>
          </div>
        )}
      </div>
    );
  };

  // Module 4: Election Management Component
  const ElectionManagementModule = ({ 
    data, 
    setData, 
    loadData, 
    handleCreateElection
  }) => {
    const [showScheduleModal, setShowScheduleModal] = useState(false);
    const [showAssignCandidatesModal, setShowAssignCandidatesModal] = useState(false);
    const [selectedElection, setSelectedElection] = useState(null);
    const [scheduleForm, setScheduleForm] = useState({
      startDate: '',
      endDate: '',
    });
    const [availableCandidates, setAvailableCandidates] = useState([]);
    const [selectedCandidates, setSelectedCandidates] = useState([]);
    
    // Handlers for election management
    const handleScheduleElection = (election) => {
      setSelectedElection(election);
      setScheduleForm({
        startDate: election.startDate ? election.startDate.split('T')[0] : '',
        endDate: election.endDate ? election.endDate.split('T')[0] : ''
      });
      setShowScheduleModal(true);
    };
    
    const handleAssignCandidates = (election) => {
      setSelectedElection(election);
      
      // Load approved candidates from localStorage (since API returns 403)
      const localCandidates = JSON.parse(localStorage.getItem('voterow_candidates')) || [];
      const approvedCandidates = localCandidates.filter(c => 
        c.status === 'Approved' || c.status === 'ACTIVE'
      );
      console.log('All local candidates:', localCandidates);
      console.log('Approved candidates for assignment:', approvedCandidates);
      setAvailableCandidates(approvedCandidates);
      
      // Get currently assigned candidates
      setSelectedCandidates(election.participants || []);
      
      setShowAssignCandidatesModal(true);
    };
    
    // Save functions
    const handleSaveSchedule = async () => {
      try {
        if (!scheduleForm.startDate || !scheduleForm.endDate) {
          alert("Please set both start and end dates");
          return;
        }
        
        await apiCall(`/elections/${selectedElection.id}/schedule`, {
          method: 'POST',
          body: JSON.stringify(scheduleForm)
        });
        
        alert("Election schedule updated successfully!");
        setShowScheduleModal(false);
        loadData('elections');
      } catch (err) {
        console.error("Error saving schedule:", err);
        alert("Failed to update election schedule. Please try again.");
      }
    };
    
    const handleSaveAssignments = async () => {
      try {
        console.log('Saving candidate assignments:', selectedCandidates);
        
        try {
          // Try database first - use different endpoint
          await apiCall(`/elections/${selectedElection.id}`, {
            method: 'PUT',
            body: JSON.stringify({
              ...selectedElection,
              participants: selectedCandidates
            })
          });
          console.log('Database assignment successful');
          // Update local state immediately
          const updatedElections = data.elections.map(election => {
            if (election.id === selectedElection.id) {
              return {
                ...election,
                participants: selectedCandidates,
                candidateCount: selectedCandidates.length
              };
            }
            return election;
          });
          setData(prev => ({ ...prev, elections: updatedElections }));
        } catch (apiError) {
          console.log('Database failed, using localStorage fallback');
          // Fallback to localStorage
          const allElections = JSON.parse(localStorage.getItem('voterow_elections')) || [];
          const updatedElections = allElections.map(election => {
            if (election.id === selectedElection.id) {
              return {
                ...election,
                participants: selectedCandidates,
                candidateCount: selectedCandidates.length,
                votes: election.votes || {}
              };
            }
            return election;
          });
          localStorage.setItem('voterow_elections', JSON.stringify(updatedElections));
          setData(prev => ({ ...prev, elections: updatedElections }));
        }
        
        alert("Candidate assignments updated successfully!");
        setShowAssignCandidatesModal(false);
        
        // Force immediate UI update from localStorage
        const refreshedElections = JSON.parse(localStorage.getItem('voterow_elections')) || [];
        const electionsWithCount = refreshedElections.map(election => ({
          ...election,
          candidateCount: election.participants ? election.participants.length : 0
        }));
        setData(prev => ({ ...prev, elections: electionsWithCount }));
      } catch (err) {
        console.error("Error saving candidate assignments:", err);
        alert("Failed to update candidate assignments. Please try again.");
      }
    };
    
    const toggleCandidateSelection = (candidateId) => {
      if (selectedCandidates.includes(candidateId)) {
        setSelectedCandidates(selectedCandidates.filter(id => id !== candidateId));
      } else {
        setSelectedCandidates([...selectedCandidates, candidateId]);
      }
    };
    
    return (
      <div className="admin-module">
        <h3 className="module-title"><FaVoteYea /> Election Management</h3>
        <div className="module-content">
          <div className="action-buttons">
            <button className="btn btn-primary" onClick={handleCreateElection}>
              <FaPlus /> Create Election
            </button>
            <button className="btn btn-secondary" onClick={() => {
              if (data.elections && data.elections.length > 0) {
                // Auto-select first election if available
                const firstElection = data.elections[0];
                handleScheduleElection(firstElection);
              } else {
                alert("Please create an election first.");
              }
            }}>
              <FaCalendarAlt /> Schedule Elections
            </button>
            <button className="btn btn-info" onClick={() => {
              if (data.elections && data.elections.length > 0) {
                // Auto-select first election if available
                const firstElection = data.elections[0];
                handleAssignCandidates(firstElection);
              } else {
                alert("Please create an election first.");
              }
            }}>
              <FaUserTie /> Assign Candidates
            </button>
            <button className="btn btn-secondary" onClick={() => loadData('elections')}>
              <FaSearch /> Refresh Data
            </button>
          </div>
          <div className="data-table">
            <h4>All Elections ({data.elections.length || 0})</h4>
            {data.elections && data.elections.length > 0 ? (
              <table className="table">
                <thead>
                  <tr>
                    <th>Title</th>
                    <th>Start Date</th>
                    <th>End Date</th>
                    <th>Status</th>
                    <th>Candidates</th>
                    <th>Votes Cast</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {data.elections.map(election => (
                  <tr key={election.id}>
                    <td>{election.title}</td>
                    <td>{election.startDate ? new Date(election.startDate).toLocaleDateString() : 'Not Set'}</td>
                    <td>{election.endDate ? new Date(election.endDate).toLocaleDateString() : 'Not Set'}</td>
                    <td>
                      <span className={`status ${election.status?.toLowerCase()}`}>
                        {election.status}
                      </span>
                    </td>
                    <td>{election.participants ? election.participants.length : 0}</td>
                    <td>{election.votes ? Object.keys(election.votes).length : 0}</td>
                    <td>
                      <button 
                        className="btn btn-sm btn-primary" 
                        onClick={() => handleEditElection(election)}
                        title="Edit Election"
                      >
                        <FaEdit />
                      </button>

                      <button 
                        className="btn btn-sm btn-success" 
                        onClick={() => handleStartElection(election)}
                        disabled={election.status === 'ONGOING' || election.status === 'COMPLETED'}
                        title="Start Election"
                      >
                        <FaPlay />
                      </button>
                      <button 
                        className="btn btn-sm btn-danger" 
                        onClick={() => handleEndElection(election)}
                        disabled={election.status === 'COMPLETED' || election.status === 'SCHEDULED'}
                        title="End Election"
                      >
                        <FaStop />
                      </button>
                      <button 
                        className="btn btn-sm btn-danger" 
                        onClick={async () => {
                          if (window.confirm(`Delete election "${election.title}"? This action cannot be undone.`)) {
                            try {
                              console.log('AdminDashboard: Deleting election with ID:', election.id);
                              
                              // Check if backend is accessible
                              const token = secureStorage.getToken();
                              if (!token) {
                                throw new Error('Not authenticated. Please log in again.');
                              }
                              
                              const response = await apiCall(`/elections/${election.id}`, { method: 'DELETE' });
                              
                              if (response) {
                                console.log('AdminDashboard: Election deleted successfully:', response);
                                
                                // Remove from local state
                                const updatedElections = data.elections.filter(e => e.id !== election.id);
                                setData(prev => ({ ...prev, elections: updatedElections }));
                                localStorage.setItem('voterow_elections', JSON.stringify(updatedElections));
                                
                                // Refresh from database to ensure consistency
                                await loadElections();
                                
                                alert('Election deleted successfully');
                              } else {
                                throw new Error('No response from server');
                              }
                            } catch (err) {
                              console.error('AdminDashboard: Error deleting election:', err);
                              
                              // Provide more specific error messages
                              let errorMessage = 'Failed to delete election';
                              if (err.message) {
                                if (err.message.includes('fetch')) {
                                  errorMessage = 'Cannot connect to server. Please check if the backend is running.';
                                } else if (err.message.includes('401') || err.message.includes('Unauthorized')) {
                                  errorMessage = 'Authentication failed. Please log in again.';
                                } else if (err.message.includes('404')) {
                                  errorMessage = 'Election not found. It may have already been deleted.';
                                } else {
                                  errorMessage = `Failed to delete election: ${err.message}`;
                                }
                              }
                              
                              alert(errorMessage);
                            }
                          }
                        }}
                        title="Delete Election"
                      >
                        <FaTrash />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          ) : (
            <p>No elections found. <button className="btn btn-primary btn-sm" onClick={handleCreateElection}>Create your first election</button></p>
          )}
        </div>
        
        {/* Schedule Election Modal */}
        {showScheduleModal && (
          <div className="modal-overlay">
            <div className="modal-content">
              <div className="modal-header">
                <h3>Schedule Election</h3>
                <button className="close-btn" onClick={() => setShowScheduleModal(false)}>×</button>
              </div>
              <div className="modal-body">
                {selectedElection && (
                  <>
                    <h4>{selectedElection.title}</h4>
                    <div className="form-group">
                      <label>Start Date</label>
                      <input 
                        type="date" 
                        className="form-control" 
                        value={scheduleForm.startDate} 
                        onChange={(e) => setScheduleForm({...scheduleForm, startDate: e.target.value})}
                      />
                    </div>
                    <div className="form-group">
                      <label>End Date</label>
                      <input 
                        type="date" 
                        className="form-control" 
                        value={scheduleForm.endDate} 
                        onChange={(e) => setScheduleForm({...scheduleForm, endDate: e.target.value})}
                      />
                    </div>
                  </>
                )}
              </div>
              <div className="modal-footer">
                <button className="btn btn-secondary" onClick={() => setShowScheduleModal(false)}>
                  Cancel
                </button>
                <button className="btn btn-primary" onClick={handleSaveSchedule}>
                  Update Schedule
                </button>
              </div>
            </div>
          </div>
        )}
        
        {/* Assign Candidates Modal */}
        {showAssignCandidatesModal && (
          <div className="modal-overlay">
            <div className="modal-content">
              <div className="modal-header">
                <h3>Assign Candidates</h3>
                <button className="close-btn" onClick={() => setShowAssignCandidatesModal(false)}>×</button>
              </div>
              <div className="modal-body">
                {selectedElection && (
                  <>
                    <h4>Election: {selectedElection.title}</h4>
                    {availableCandidates.length === 0 ? (
                      <p>No candidates available. Please add candidates first.</p>
                    ) : (
                      <div className="candidate-list">
                        {availableCandidates.map(candidate => (
                          <div className="candidate-item" key={candidate.id}>
                            <label className="checkbox-label">
                              <input 
                                type="checkbox"
                                checked={selectedCandidates.includes(candidate.id)}
                                onChange={() => toggleCandidateSelection(candidate.id)}
                              />
                              <span>{candidate.name}</span> 
                              <span className="candidate-party">({candidate.party || 'Independent'})</span>
                            </label>
                          </div>
                        ))}
                      </div>
                    )}
                  </>
                )}
              </div> 
              <div className="modal-footer">
                <button className="btn btn-secondary" onClick={() => setShowAssignCandidatesModal(false)}>
                  Cancel
                </button>
                <button 
                  className="btn btn-primary" 
                  onClick={handleSaveAssignments}
                  disabled={availableCandidates.length === 0}
                >
                  Save Assignments
                </button>
              </div>
            </div>
          </div>
        )}


      </div>
    </div>
    );
  };

  // Module 5: Voting Process Control Component
  const VotingControlModule = () => (
    <div className="admin-module">
      <h3 className="module-title"><FaPlay /> Voting Process Control</h3>
      <div className="module-content">
        <div className="control-panel">
          <div className="control-card">
            <h4>Election Control</h4>
            <div className="control-buttons">
              <button className="btn btn-success btn-lg"><FaPlay /> Start Election</button>
              <button className="btn btn-danger btn-lg"><FaStop /> Stop Election</button>
            </div>
          </div>
          <div className="control-card">
            <h4>Live Monitoring</h4>
            <div className="live-stats">
              <div className="stat-item">
                <span className="stat-label">Active Voters:</span>
                <span className="stat-value">156</span>
              </div>
              <div className="stat-item">
                <span className="stat-label">Total Votes:</span>
                <span className="stat-value">1,247</span>
              </div>
              <div className="stat-item">
                <span className="stat-label">Turnout:</span>
                <span className="stat-value">62.3%</span>
              </div>
            </div>
          </div>
        </div>
        <div className="activity-feed">
          <h4>Recent Activity</h4>
          <div className="activity-list">
            <div className="activity-item">Voter #1247 cast vote - 2 minutes ago</div>
            <div className="activity-item">Voter #1246 cast vote - 3 minutes ago</div>
            <div className="activity-item">Voter #1245 cast vote - 4 minutes ago</div>
          </div>
        </div>
      </div>
    </div>
  );

  // Module 6: Results & Analytics Component
  const ResultsModule = () => (
    <div className="admin-module">
      <h3 className="module-title"><FaChartBar /> Results Management & Analytics</h3>
      <div className="module-content">
        <div className="action-buttons">
          <button className="btn btn-primary"><FaPlay /> Count Votes</button>
          <button className="btn btn-success"><FaEye /> View Results</button>
          <button className="btn btn-info"><FaFileExport /> Publish Results</button>
          <button className="btn btn-secondary"><FaDownload /> Download Report</button>
        </div>
        <div className="results-overview">
          <div className="result-card">
            <h4>Election Results</h4>
            <div className="result-stats">
              <div className="stat">Total Votes: <strong>1,247</strong></div>
              <div className="stat">Valid Votes: <strong>1,235</strong></div>
              <div className="stat">Invalid Votes: <strong>12</strong></div>
              <div className="stat">Turnout: <strong>62.3%</strong></div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );

  // Module 7: Security & Audit Component
  const SecurityModule = () => (
    <div className="admin-module">
      <h3 className="module-title"><FaShieldAlt /> Security & Audit System</h3>
      <div className="module-content">
        <div className="action-buttons">
          <button className="btn btn-primary"><FaShieldAlt /> Encrypt Votes</button>
          <button className="btn btn-warning"><FaSearch /> Fraud Detection</button>
          <button className="btn btn-info"><FaDownload /> Export Audit Log</button>
        </div>
        <div className="security-stats">
          <div className="security-card">
            <h4>Security Status</h4>
            <div className="security-item">
              <span className="label">Vote Encryption:</span>
              <span className="status active">Enabled</span>
            </div>
            <div className="security-item">
              <span className="label">Audit Logging:</span>
              <span className="status active">Active</span>
            </div>
            <div className="security-item">
              <span className="label">Fraud Detection:</span>
              <span className="status active">Monitoring</span>
            </div>
          </div>
        </div>
        <div className="audit-log">
          <h4>Recent Audit Entries</h4>
          {data.auditLogs && data.auditLogs.length > 0 ? (
            <div className="log-entries">
              {data.auditLogs.slice(0, 10).map(log => (
                <div key={log.id} className="log-entry">
                  <span className="log-time">{new Date(log.timestamp).toLocaleString()}</span>
                  <span className="log-action">{log.action}</span>
                  <span className="log-user">{log.user}</span>
                </div>
              ))}
            </div>
          ) : (
            <p>No audit logs available.</p>
          )}
        </div>
      </div>
    </div>
  );

  // Module 8: Communication & Notifications Component
  const CommunicationModule = () => (
    <div className="admin-module">
      <h3 className="module-title"><FaBell /> Communication & Notifications</h3>
      <div className="module-content">
        <div className="action-buttons">
          <button className="btn btn-primary"><FaSms /> Send SMS</button>
          <button className="btn btn-secondary"><FaEnvelope /> Send Email</button>
          <button className="btn btn-info"><FaBell /> Broadcast Notification</button>
        </div>
        <div className="communication-tabs">
          <div className="comm-tab active">
            <h4>Send Notifications</h4>
            <form className="notification-form">
              <div className="form-group">
                <label>Message Type:</label>
                <select className="form-control">
                  <option>SMS</option>
                  <option>Email</option>
                  <option>Both</option>
                </select>
              </div>
              <div className="form-group">
                <label>Recipients:</label>
                <select className="form-control">
                  <option>All Voters</option>
                  <option>All Candidates</option>
                  <option>Custom List</option>
                </select>
              </div>
              <div className="form-group">
                <label>Message:</label>
                <textarea className="form-control" rows="4" placeholder="Enter your message..."></textarea>
              </div>
              <button type="submit" className="btn btn-primary">Send Notification</button>
            </form>
          </div>
        </div>
      </div>
    </div>
  );

  // Home Module Component
  const HomeModule = () => (
    <div className="admin-module">
      <div className="content-header">
        <h1 className="content-title">Welcome to VoteRow Admin Panel</h1>
        <p className="content-subtitle">Comprehensive Election Management System</p>
      </div>
      <div className="module-content">
        <div className="home-content">
          <div className="admin-welcome-card">
            <div className="welcome-icon">
              <FaHome />
            </div>
            <div className="welcome-text">
              <h3>Admin Dashboard</h3>
              <p>Welcome to the VoteRow administration panel. Here you can manage all aspects of your election system with powerful tools and comprehensive controls.</p>
            </div>
          </div>
          
          <div className="feature-grid">
            <div className="feature-card">
              <FaUsers className="feature-icon" />
              <h4>User Management</h4>
              <p>Manage voters, candidates, and administrators with complete control over user permissions and verification processes.</p>
            </div>
            
            <div className="feature-card">
              <FaVoteYea className="feature-icon" />
              <h4>Election Control</h4>
              <p>Create, schedule, and monitor elections with real-time tracking and comprehensive management tools.</p>
            </div>
            
            <div className="feature-card">
              <FaChartBar className="feature-icon" />
              <h4>Analytics & Reports</h4>
              <p>Access detailed analytics, generate reports, and export data for comprehensive election analysis.</p>
            </div>
            
            <div className="feature-card">
              <FaShieldAlt className="feature-icon" />
              <h4>Security & Audit</h4>
              <p>Monitor system security, track user activities, and maintain comprehensive audit logs for transparency.</p>
            </div>
          </div>
          
          <div className="quick-actions">
            <h4>Quick Actions</h4>
            <div className="action-buttons">
              <button className="btn btn-primary" onClick={() => setActiveTab('elections')}>
                <FaPlus /> Create New Election
              </button>
              <button className="btn btn-success" onClick={() => setActiveTab('voters')}>
                <FaUsers /> Manage Voters
              </button>
              <button className="btn btn-info" onClick={() => setActiveTab('overview')}>
                <FaChartBar /> View Analytics
              </button>
              <button className="btn btn-warning" onClick={() => setActiveTab('security')}>
                <FaShieldAlt /> Security Audit
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );

  // Profile Module Component
  const ProfileModule = () => {
    const [profile, setProfile] = useState({
      fullName: user?.fullName || user?.username || 'Admin User',
      email: user?.email || 'admin@voterow.com',
      age: user?.age || '',
      phoneNumber: user?.phoneNumber || '',
      idProofNumber: user?.idProofNumber || '',
      address: user?.address || '',
      role: user?.userType === 'ADMIN' ? 'Super Administrator' : user?.userType || 'Administrator',
      lastLogin: new Date().toLocaleString(),
      accountCreated: user?.createdAt || '2024-01-15',
      permissions: user?.userType === 'ADMIN' ? 
        ['Full Access', 'User Management', 'Election Control', 'System Settings'] : 
        ['Limited Access']
    });

    const [isEditing, setIsEditing] = useState(false);
    const [editData, setEditData] = useState({ ...profile });
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');

    // Update profile when user prop changes
    useEffect(() => {
      if (user) {
        const updatedProfile = {
          fullName: user.fullName || user.username || 'Admin User',
          email: user.email || 'admin@voterow.com',
          age: user.age || '',
          phoneNumber: user.phoneNumber || '',
          idProofNumber: user.idProofNumber || '',
          address: user.address || '',
          role: user.userType === 'ADMIN' ? 'Super Administrator' : user.userType || 'Administrator',
          lastLogin: new Date().toLocaleString(),
          accountCreated: user.createdAt || '2024-01-15',
          permissions: user.userType === 'ADMIN' ? 
            ['Full Access', 'User Management', 'Election Control', 'System Settings'] : 
            ['Limited Access']
        };
        setProfile(updatedProfile);
        setEditData(updatedProfile);
      }
    }, [user]);

    const handleSave = async () => {
      setIsLoading(true);
      setError('');
      setSuccess('');
      
      try {
  const response = await fetch(`${BASE_API_URL}/api/auth/profile?email=${encodeURIComponent(user.email)}`, {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({
            fullName: editData.fullName,
            age: editData.age ? parseInt(editData.age) : null,
            phoneNumber: editData.phoneNumber || null,
            idProofNumber: editData.idProofNumber || null,
            address: editData.address || null
          }),
        });

        if (response.ok) {
          const updatedUser = await response.json();
          
          // Create the complete updated user object
          const completeUpdatedUser = {
            ...user,
            fullName: updatedUser.fullName,
            age: updatedUser.age || '',
            phoneNumber: updatedUser.phoneNumber || '',
            idProofNumber: updatedUser.idProofNumber || '',
            address: updatedUser.address || ''
          };
          
          // Update the profile state
          setProfile({
            ...profile,
            fullName: completeUpdatedUser.fullName,
            age: completeUpdatedUser.age,
            phoneNumber: completeUpdatedUser.phoneNumber,
            idProofNumber: completeUpdatedUser.idProofNumber,
            address: completeUpdatedUser.address
          });
          
          setSuccess('Profile updated successfully!');
          setIsEditing(false);
          
          // Update parent component user data through the proper prop
          if (onUpdateUser) {
            onUpdateUser(completeUpdatedUser);
          }
          
          // Also update the global function for backwards compatibility
          if (window.updateUserProfile) {
            window.updateUserProfile(completeUpdatedUser);
          }
        } else {
          const errorText = await response.text();
          setError(errorText || 'Failed to update profile');
        }
      } catch (err) {
        // If API fails, still update locally for a better user experience
        console.warn('API update failed, updating locally:', err);
        
        const localUpdatedUser = {
          ...user,
          fullName: editData.fullName,
          age: editData.age || '',
          phoneNumber: editData.phoneNumber || '',
          idProofNumber: editData.idProofNumber || '',
          address: editData.address || ''
        };
        
        // Update the profile state locally
        setProfile({
          ...profile,
          fullName: localUpdatedUser.fullName,
          age: localUpdatedUser.age,
          phoneNumber: localUpdatedUser.phoneNumber,
          idProofNumber: localUpdatedUser.idProofNumber,
          address: localUpdatedUser.address
        });
        
        setSuccess('Profile updated locally (backend unavailable)');
        setIsEditing(false);
        
        // Update parent component
        if (onUpdateUser) {
          onUpdateUser(localUpdatedUser);
        }
        
        if (window.updateUserProfile) {
          window.updateUserProfile(localUpdatedUser);
        }
        
        setError('Network error, but changes saved locally. They will sync when the server is available.');
      } finally {
        setIsLoading(false);
      }
    };

    const handleCancel = () => {
      setEditData({ ...profile });
      setIsEditing(false);
      setError('');
      setSuccess('');
    };

    return (
      <div className="admin-module">
        <div className="content-header">
          <h1 className="content-title">Administrator Profile</h1>
          <p className="content-subtitle">Manage your account settings and information</p>
        </div>
        <div className="module-content">
          {error && <div className="alert alert-error">{error}</div>}
          {success && <div className="alert alert-success">{success}</div>}
          
          <div className="profile-container">
            <div className="profile-card">
              <div className="profile-header">
                <div className="profile-avatar">
                  <FaUser />
                </div>
                <div className="profile-info">
                  <h3>{profile.fullName}</h3>
                  <p className="role-badge">{profile.role}</p>
                </div>
                <button 
                  className="btn btn-secondary"
                  onClick={() => setIsEditing(!isEditing)}
                  disabled={isLoading}
                >
                  <FaEdit /> {isEditing ? 'Cancel' : 'Edit Profile'}
                </button>
              </div>
              
              <div className="profile-body">
                <div className="profile-section">
                  <h4>Personal Information</h4>
                  <div className="info-grid">
                    <div className="info-item">
                      <label>Full Name *</label>
                      {isEditing ? (
                        <input 
                          type="text" 
                          value={editData.fullName}
                          onChange={(e) => setEditData({...editData, fullName: e.target.value})}
                          required
                        />
                      ) : (
                        <span>{profile.fullName}</span>
                      )}
                    </div>
                    <div className="info-item">
                      <label>Email</label>
                      <span>{profile.email}</span>
                    </div>
                    <div className="info-item">
                      <label>Age</label>
                      {isEditing ? (
                        <input 
                          type="number" 
                          value={editData.age}
                          onChange={(e) => setEditData({...editData, age: e.target.value})}
                          min="18"
                          max="100"
                        />
                      ) : (
                        <span>{profile.age || 'Not specified'}</span>
                      )}
                    </div>
                    <div className="info-item">
                      <label>Phone Number</label>
                      {isEditing ? (
                        <input 
                          type="tel" 
                          value={editData.phoneNumber}
                          onChange={(e) => setEditData({...editData, phoneNumber: e.target.value})}
                          maxLength="15"
                        />
                      ) : (
                        <span>{profile.phoneNumber || 'Not specified'}</span>
                      )}
                    </div>
                    <div className="info-item">
                      <label>ID Proof Number</label>
                      {isEditing ? (
                        <input 
                          type="text" 
                          value={editData.idProofNumber}
                          onChange={(e) => setEditData({...editData, idProofNumber: e.target.value})}
                          maxLength="20"
                        />
                      ) : (
                        <span>{profile.idProofNumber || 'Not specified'}</span>
                      )}
                    </div>
                    <div className="info-item">
                      <label>Address</label>
                      {isEditing ? (
                        <textarea 
                          value={editData.address}
                          onChange={(e) => setEditData({...editData, address: e.target.value})}
                          maxLength="500"
                          rows="3"
                        />
                      ) : (
                        <span>{profile.address || 'Not specified'}</span>
                      )}
                    </div>
                    <div className="info-item">
                      <label>Role</label>
                      <span>{profile.role}</span>
                    </div>
                    <div className="info-item">
                      <label>Last Login</label>
                      <span>{profile.lastLogin}</span>
                    </div>
                  </div>
                </div>
                
                <div className="profile-section">
                  <h4>Account Details</h4>
                  <div className="info-grid">
                    <div className="info-item">
                      <label>Account Created</label>
                      <span>{profile.accountCreated}</span>
                    </div>
                    <div className="info-item">
                      <label>Permissions</label>
                      <div className="permission-tags">
                        {profile.permissions.map((permission, index) => (
                          <span key={index} className="permission-tag">{permission}</span>
                        ))}
                      </div>
                    </div>
                  </div>
                </div>
                
                {isEditing && (
                  <div className="profile-actions">
                    <button 
                      className="btn btn-primary" 
                      onClick={handleSave}
                      disabled={isLoading || !editData.fullName.trim()}
                    >
                      {isLoading ? 'Saving...' : 'Save Changes'}
                    </button>
                    <button 
                      className="btn btn-secondary" 
                      onClick={handleCancel}
                      disabled={isLoading}
                    >
                      Cancel
                    </button>
                  </div>
                )}
              </div>
            </div>
            
            <div className="security-card">
              <h4><FaShieldAlt /> Security Settings</h4>
              <div className="security-options">
                <div className="security-item">
                  <div className="security-info">
                    <h5>Two-Factor Authentication</h5>
                    <p>Add an extra layer of security to your account</p>
                  </div>
                  <button className="btn btn-primary">Enable 2FA</button>
                </div>
                <div className="security-item">
                  <div className="security-info">
                    <h5>Change Password</h5>
                    <p>Update your account password</p>
                  </div>
                  <button className="btn btn-warning">Change Password</button>
                </div>
                <div className="security-item">
                  <div className="security-info">
                    <h5>Login Sessions</h5>
                    <p>Manage active login sessions</p>
                  </div>
                  <button className="btn btn-info">View Sessions</button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  };

  // Module 9: Dashboard & Reports Component
  const DashboardOverviewModule = () => (
    <div className="admin-module">
      <div className="content-header">
        <h1 className="content-title">Welcome, {user?.fullName || user?.username || 'Admin'}!</h1>
        <p className="content-subtitle">Your VoteRow Administration Dashboard</p>
      </div>
      <div className="module-content">
        <div className="overview-stats">
          <div className="stat-card primary">
            <div className="stat-icon-wrapper">
              <FaVoteYea className="stat-icon" />
            </div>
            <div className="stat-info">
              <h3>{data.overview.totalElections || 5}</h3>
              <p>Total Elections</p>
            </div>
          </div>
          <div className="stat-card success">
            <div className="stat-icon-wrapper">
              <FaUsers className="stat-icon" />
            </div>
            <div className="stat-info">
              <h3>{data.overview.totalVoters || 1247}</h3>
              <p>Registered Voters</p>
            </div>
          </div>
          <div className="stat-card warning">
            <div className="stat-icon-wrapper">
              <FaUserTie className="stat-icon" />
            </div>
            <div className="stat-info">
              <h3>{data.overview.totalCandidates || 15}</h3>
              <p>Candidates</p>
            </div>
          </div>
          <div className="stat-card info">
            <div className="stat-icon-wrapper">
              <FaPlay className="stat-icon" />
            </div>
            <div className="stat-info">
              <h3>{data.overview.activeElections || 2}</h3>
              <p>Active Elections</p>
            </div>
          </div>
        </div>
        
        <div className="dashboard-grid">
          <div className="chart-card">
            <div className="card-header">
              <h4><FaChartBar /> Recent Activity</h4>
            </div>
            <div className="activity-list">
              {(data.overview.recentActivity || [
                { id: 1, action: "New voter registered", user: "John Doe", timestamp: new Date().toISOString() },
                { id: 2, action: "Election created", user: "Admin", timestamp: new Date(Date.now() - 300000).toISOString() },
                { id: 3, action: "Vote cast", user: "Jane Smith", timestamp: new Date(Date.now() - 600000).toISOString() }
              ]).map(activity => (
                <div key={activity.id} className="activity-item">
                  <div className="activity-icon">
                    <FaBell />
                  </div>
                  <div className="activity-content">
                    <p className="activity-action">{activity.action}</p>
                    <p className="activity-meta">{activity.user} - {new Date(activity.timestamp).toLocaleString()}</p>
                  </div>
                </div>
              ))}
            </div>
          </div>
          
          <div className="chart-card">
            <div className="card-header">
              <h4><FaChartBar /> Quick Actions</h4>
            </div>
            <div className="quick-actions">
              <button className="btn btn-primary" onClick={() => setActiveTab('elections')}>
                <FaPlus /> Create Election
              </button>
              <button className="btn btn-success" onClick={() => setActiveTab('voters')}>
                <FaUsers /> Manage Voters
              </button>
              <button className="btn btn-info" onClick={() => setActiveTab('candidates')}>
                <FaUserTie /> Review Candidates
              </button>
              <button className="btn btn-warning" onClick={() => setActiveTab('reports')}>
                <FaFileExport /> Generate Reports
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );

  // Module 10: System Settings Component
  const SystemSettingsModule = () => (
    <div className="admin-module">
      <h3 className="module-title"><FaCog /> System Settings & Configuration</h3>
      <div className="module-content">
        <div className="settings-tabs">
          <div className="settings-section">
            <h4>Admin Users</h4>
            <div className="action-buttons">
              <button className="btn btn-primary"><FaPlus /> Add Admin</button>
              <button className="btn btn-secondary"><FaUserShield /> Manage Permissions</button>
            </div>
          </div>
          <div className="settings-section">
            <h4>System Configuration</h4>
            <form className="config-form">
              <div className="form-group">
                <label>Timezone:</label>
                <select className="form-control">
                  <option>UTC</option>
                  <option>Asia/Kolkata</option>
                  <option>America/New_York</option>
                </select>
              </div>
              <div className="form-group">
                <label>Max Voting Hours:</label>
                <input type="number" className="form-control" defaultValue="24" />
              </div>
              <div className="form-group">
                <label>
                  <input type="checkbox" /> Enable Voter Verification
                </label>
              </div>
              <div className="form-group">
                <label>
                  <input type="checkbox" /> Enable Audit Logging
                </label>
              </div>
              <button type="submit" className="btn btn-primary">Save Configuration</button>
            </form>
          </div>
          <div className="settings-section">
            <h4>Backup & Restore</h4>
            <div className="backup-controls">
              <button className="btn btn-success"><FaDatabase /> Create Backup</button>
              <button className="btn btn-warning"><FaUpload /> Restore Backup</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );

  // Render the appropriate module based on active tab - Memoized to prevent unnecessary re-renders
  const renderActiveModule = useCallback(() => {
    try {
      switch (activeTab) {
        case 'home': 
          return (
            <ErrorBoundary componentName="Home Module">
              <HomeModule key="home" />
            </ErrorBoundary>
          );
        case 'profile': 
          return (
            <ErrorBoundary componentName="Profile Module">
              <ProfileModule key="profile" />
            </ErrorBoundary>
          );
        case 'overview': 
          return (
            <ErrorBoundary componentName="Overview Module">
              <DashboardOverviewModule key="overview" />
            </ErrorBoundary>
          );
        case 'auth': 
          return (
            <ErrorBoundary componentName="Authentication Module">
              <AuthenticationModule key="auth" />
            </ErrorBoundary>
          );
        case 'voters': 
          return (
            <ErrorBoundary componentName="Voter Management Module">
              <VoterManagementModule key="voters" voters={data.voters || []} setData={setData} data={data} loadData={loadData} />
            </ErrorBoundary>
          );
        case 'candidates': 
          return (
            <ErrorBoundary componentName="Candidate Management Module">
              <CandidateManagementModule key="candidates" />
            </ErrorBoundary>
          );
        case 'elections': 
          return (
            <ErrorBoundary componentName="Election Management Module">
              <ElectionManagementModule key="elections" 
                data={data} 
                setData={setData} 
                loadData={loadData}
                handleCreateElection={handleCreateElection}
              />
            </ErrorBoundary>
          );
        case 'voting': 
          return (
            <ErrorBoundary componentName="Voting Control Module">
              <VotingControlModule key="voting" />
            </ErrorBoundary>
          );
        case 'results': 
          return (
            <ErrorBoundary componentName="Results Module">
              <ResultsModule key="results" />
            </ErrorBoundary>
          );
        case 'security': 
          return (
            <ErrorBoundary componentName="Security Module">
              <SecurityModule key="security" />
            </ErrorBoundary>
          );
        case 'notifications': 
          return (
            <ErrorBoundary componentName="Communication Module">
              <CommunicationModule key="notifications" />
            </ErrorBoundary>
          );
        case 'settings': 
          return (
            <ErrorBoundary componentName="System Settings Module">
              <SystemSettingsModule key="settings" />
            </ErrorBoundary>
          );
        default: 
          return (
            <ErrorBoundary componentName="Default Home Module">
              <HomeModule key="default" />
            </ErrorBoundary>
          );
      }
    } catch (error) {
      console.error('Error in renderActiveModule:', error);
      return (
        <div style={{ padding: '20px', color: 'red' }}>
          <h3>Error loading module: {activeTab}</h3>
          <p>Please try refreshing the page or selecting a different tab.</p>
          <button onClick={() => setActiveTab('home')}>Go to Home</button>
        </div>
      );
    }
  }, [activeTab, data, loadData, handleCreateElection]);

  // Removed duplicate ElectionModal component - using inline modal in ElectionManagementModule instead

  return (
    <div className="admin-dashboard">
      {/* Sidebar Navigation */}
      <div className="admin-sidebar">
        <div className="sidebar-header">
          <h1 className="sidebar-title">VoteRow Admin</h1>
          <p className="sidebar-subtitle">System Management</p>
        </div>
        <nav className="sidebar-nav">
          {tabs.map(tab => {
            const IconComponent = tab.icon;
            return (
              <button
                key={tab.id}
                className={`nav-tab ${activeTab === tab.id ? 'active' : ''}`}
                onClick={() => setActiveTab(tab.id)}
              >
                <IconComponent className="tab-icon" />
                <span>{tab.label}</span>
              </button>
            );
          })}
        </nav>
      </div>

      {/* Main Content */}
      <div className="admin-content">
        <div className="admin-content-area">
          {loading && <div className="loading-spinner">Loading...</div>}
          {renderActiveModule()}
        </div>
      </div>
      
      {/* Enrollment Requests Modal */}
      {showEnrollmentRequestsModal && (
        <div className="modal-overlay">
          <div className="modal-container">
            <div className="modal-header">
              <h3>Enrollment Requests</h3>
              <button 
                className="close-btn"
                onClick={() => setShowEnrollmentRequestsModal(false)}
              >×</button>
            </div>
            <div className="modal-body">
              {enrollmentRequests.length > 0 ? (
                <table className="table">
                  <thead>
                    <tr>
                      <th>Participant Name</th>
                      <th>Email</th>
                      <th>Request Date</th>
                      <th>Status</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {enrollmentRequests.map((request, index) => (
                      <tr key={index}>
                        <td>{request.fullName || 'Unnamed Participant'}</td>
                        <td>{request.email || 'No Email'}</td>
                        <td>{new Date(request.requestDate || Date.now()).toLocaleString()}</td>
                        <td>
                          <span className={`status ${(request.status || 'PENDING').toLowerCase()}`}>
                            {request.status || 'PENDING'}
                          </span>
                        </td>
                        <td>
                          {(!request.status || request.status === 'PENDING') && (
                            <>
                              <button 
                                className="btn btn-sm btn-success"
                                onClick={() => handleEnrollmentRequest(currentElectionId, request.userId || request.participantId, 'approve')}
                              >
                                Approve
                              </button>
                              <button 
                                className="btn btn-sm btn-danger"
                                onClick={() => handleEnrollmentRequest(currentElectionId, request.userId || request.participantId, 'reject')}
                              >
                                Reject
                              </button>
                            </>
                          )}
                          {request.status && request.status !== 'PENDING' && (
                            <span className="status-text">No actions available</span>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              ) : (
                <p className="empty-state">No enrollment requests for this election.</p>
              )}
            </div>
            <div className="modal-footer">
              <button 
                className="btn btn-secondary"
                onClick={() => setShowEnrollmentRequestsModal(false)}
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Election Modal - Moved outside to prevent re-rendering issues */}
      {showElectionModal && (
        <div className="modal-overlay">
          <div className="modal-content">
            <div className="modal-header">
              <h3>{editingElection ? 'Edit Election' : 'Create New Election'}</h3>
              <button className="close-btn" onClick={() => setShowElectionModal(false)}>
                <FaTimes />
              </button>
            </div>
            <div className="modal-body">
              <form onSubmit={(e) => { e.preventDefault(); handleSaveElection(); }}>
                <div className="form-group">
                  <label>Election Title *</label>
                  <input
                    type="text"
                    className="form-control"
                    value={electionForm.title}
                    onChange={(e) => setElectionForm({...electionForm, title: e.target.value})}
                    placeholder="Enter election title"
                    required
                    autoFocus
                  />
                </div>
                <div className="form-group">
                  <label>Description</label>
                  <textarea
                    className="form-control"
                    value={electionForm.description}
                    onChange={(e) => setElectionForm({...electionForm, description: e.target.value})}
                    placeholder="Enter election description"
                    rows="3"
                  />
                </div>
                <div className="form-row">
                  <div className="form-group">
                    <label>Start Date *</label>
                    <input
                      type="date"
                      className="form-control"
                      value={electionForm.startDate}
                      onChange={(e) => setElectionForm({...electionForm, startDate: e.target.value})}
                      required
                    />
                  </div>
                  <div className="form-group">
                    <label>End Date *</label>
                    <input
                      type="date"
                      className="form-control"
                      value={electionForm.endDate}
                      onChange={(e) => setElectionForm({...electionForm, endDate: e.target.value})}
                      required
                    />
                  </div>
                </div>
                <div className="form-group">
                  <label>Status</label>
                  <select
                    className="form-control"
                    value={electionForm.status}
                    onChange={(e) => setElectionForm({...electionForm, status: e.target.value})}
                  >
                    <option value="DRAFT">Draft</option>
                    <option value="SCHEDULED">Scheduled</option>
                    <option value="ACTIVE">Active</option>
                    <option value="COMPLETED">Completed</option>
                    <option value="CANCELLED">Cancelled</option>
                  </select>
                </div>
                <div className="form-actions">
                  <button type="button" className="btn btn-secondary" onClick={() => setShowElectionModal(false)}>
                    Cancel
                  </button>
                  <button type="submit" className="btn btn-primary">
                    {editingElection ? 'Update Election' : 'Create Election'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default AdminDashboard;