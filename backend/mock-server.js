const express = require('express');
const cors = require('cors');
const app = express();

// Enable CORS for all routes with explicit configuration
app.use(cors({
  origin: ['http://localhost:5173', 'http://localhost:5174'],
  methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'],
  allowedHeaders: ['Content-Type', 'Authorization', 'Cache-Control', 'Pragma'],
  credentials: true
}));
app.use(express.json());

// Mock admin dashboard data
app.get('/api/admin/dashboard-overview', (req, res) => {
  res.json({
    totalVoters: 1250,
    totalElections: 15,
    activeElections: 3,
    totalVotes: 8945,
    recentActivity: [
      { id: 1, action: "New voter registered", user: "John Doe", timestamp: new Date().toISOString() },
      { id: 2, action: "Election created", user: "Admin", timestamp: new Date(Date.now() - 300000).toISOString() },
      { id: 3, action: "Vote cast", user: "Jane Smith", timestamp: new Date(Date.now() - 600000).toISOString() }
    ]
  });
});

app.get('/api/admin/voters', (req, res) => {
  res.json([
    { id: 1, name: "John Doe", email: "john@example.com", status: "Active", registeredAt: "2024-01-15" },
    { id: 2, name: "Jane Smith", email: "jane@example.com", status: "Active", registeredAt: "2024-01-20" },
    { id: 3, name: "Bob Johnson", email: "bob@example.com", status: "Inactive", registeredAt: "2024-01-25" }
  ]);
});

// Mock election data with expanded structure
const mockElections = [
  { 
    id: 1, 
    title: "Presidential Election 2024", 
    status: "Active", 
    startDate: "2024-03-01", 
    endDate: "2024-03-15", 
    participants: 1200,
    enrollmentRequests: [],
    description: "National Presidential Election for 2024",
    eligibilityCriteria: "All registered voters",
    votingSystem: "Ranked Choice"
  },
  { 
    id: 2, 
    title: "Student Council Election", 
    status: "Completed", 
    startDate: "2024-02-01", 
    endDate: "2024-02-10", 
    participants: 500,
    enrollmentRequests: [],
    description: "Annual Student Council Election for University Student Body",
    eligibilityCriteria: "Enrolled students in good standing",
    votingSystem: "First Past the Post"
  },
  { 
    id: 3, 
    title: "Board Member Election", 
    status: "Upcoming", 
    startDate: "2024-04-01", 
    endDate: "2024-04-15", 
    participants: 0,
    enrollmentRequests: [],
    description: "Election for new board members of the organization",
    eligibilityCriteria: "Members with at least 2 years of membership",
    votingSystem: "Single Transferable Vote"
  }
];

app.get('/api/admin/elections', (req, res) => {
  res.json(mockElections);
});

app.get('/api/admin/votes', (req, res) => {
  res.json([
    { id: 1, electionTitle: "Presidential Election 2024", voterName: "John Doe", timestamp: "2024-03-02 10:30", status: "Verified" },
    { id: 2, electionTitle: "Presidential Election 2024", voterName: "Jane Smith", timestamp: "2024-03-02 11:45", status: "Verified" },
    { id: 3, electionTitle: "Student Council Election", voterName: "Bob Johnson", timestamp: "2024-02-05 14:20", status: "Verified" }
  ]);
});

app.get('/api/admin/results/:electionId', (req, res) => {
  res.json({
    electionTitle: "Presidential Election 2024",
    candidates: [
      { name: "Candidate A", votes: 450, percentage: 52.3 },
      { name: "Candidate B", votes: 320, percentage: 37.2 },
      { name: "Candidate C", votes: 90, percentage: 10.5 }
    ],
    totalVotes: 860,
    turnoutPercentage: 71.7
  });
});

app.get('/api/admin/security-audit', (req, res) => {
  res.json([
    { id: 1, event: "Successful login", user: "admin", timestamp: "2024-03-02 09:15", ip: "192.168.1.100" },
    { id: 2, event: "Failed login attempt", user: "unknown", timestamp: "2024-03-02 08:45", ip: "10.0.0.50" },
    { id: 3, event: "Vote cast", user: "voter123", timestamp: "2024-03-02 10:30", ip: "192.168.1.150" }
  ]);
});

app.get('/api/admin/notifications', (req, res) => {
  res.json([
    { id: 1, title: "System Maintenance", message: "Scheduled maintenance on Sunday", type: "warning", sent: false },
    { id: 2, title: "Election Reminder", message: "Presidential election ends tomorrow", type: "info", sent: true },
    { id: 3, title: "Security Alert", message: "Multiple failed login attempts detected", type: "error", sent: true }
  ]);
});

app.get('/api/admin/reports', (req, res) => {
  res.json([
    { id: 1, name: "Voter Registration Report", generatedAt: "2024-03-01 14:30", size: "2.3 MB", type: "PDF" },
    { id: 2, name: "Election Results Summary", generatedAt: "2024-02-28 16:45", size: "1.8 MB", type: "Excel" },
    { id: 3, name: "Security Audit Log", generatedAt: "2024-03-02 09:00", size: "890 KB", type: "PDF" }
  ]);
});

app.get('/api/admin/settings', (req, res) => {
  res.json({
    systemName: "VoteRow System",
    maintenanceMode: false,
    registrationEnabled: true,
    votingEnabled: true,
    emailNotifications: true,
    smsNotifications: false,
    auditLogging: true,
    sessionTimeout: 30
  });
});

// Mock auth endpoints
app.post('/api/auth/login', (req, res) => {
  const { username, password } = req.body;
  if (username === 'admin' && password === 'admin123') {
    res.json({
      token: 'mock-jwt-token',
      user: {
        id: 1,
        username: 'admin',
        email: 'admin@voterow.com',
        userType: 'ADMIN',
        fullName: 'System Administrator'
      }
    });
  } else {
    res.status(401).json({ error: 'Invalid credentials' });
  }
});

const PORT = 8081;
app.listen(PORT, () => {
  console.log(`Mock backend server running on http://localhost:${PORT}`);
  console.log('Available endpoints:');
  console.log('- GET /api/admin/dashboard-overview');
  console.log('- GET /api/admin/voters');
  console.log('- GET /api/admin/elections');
  console.log('- GET /api/admin/votes');
  console.log('- GET /api/admin/results/:electionId');
  console.log('- GET /api/admin/security-audit');
  console.log('- GET /api/admin/notifications');
  console.log('- GET /api/admin/reports');
  console.log('- GET /api/admin/settings');
  console.log('- POST /api/auth/login');
  console.log('- POST /api/elections/:id/enroll');
});

// Enrollment endpoint for candidates to apply for elections
app.post('/api/elections/:id/enroll', (req, res) => {
  const electionId = parseInt(req.params.id);
  const enrollmentRequest = req.body;
  
  // Find the election
  const electionIndex = mockElections.findIndex(e => e.id === electionId);
  
  if (electionIndex === -1) {
    return res.status(404).json({ 
      error: 'Election not found',
      message: 'The election you are trying to enroll in does not exist.'
    });
  }

  // Check if election status allows enrollment
  if (mockElections[electionIndex].status !== 'Active' && mockElections[electionIndex].status !== 'Upcoming') {
    return res.status(400).json({ 
      error: 'Invalid enrollment',
      message: 'You can only enroll in active or upcoming elections.'
    });
  }

  // Add enrollment request to the election
  const newEnrollmentRequest = {
    id: Date.now(), // Use timestamp as unique ID
    participantId: enrollmentRequest.participantId,
    userId: enrollmentRequest.userId,
    email: enrollmentRequest.email,
    fullName: enrollmentRequest.fullName,
    timestamp: enrollmentRequest.timestamp || new Date().toISOString(),
    status: 'PENDING',
    reviewTimestamp: null,
    reviewedBy: null
  };

  mockElections[electionIndex].enrollmentRequests.push(newEnrollmentRequest);
  
  // Return success response
  res.status(200).json({ 
    success: true,
    message: 'Enrollment request submitted successfully',
    enrollment: newEnrollmentRequest
  });
});