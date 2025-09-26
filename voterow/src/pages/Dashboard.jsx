import React from 'react';
import AdminDashboard from '../components/AdminDashboard';
import VoterDashboard from '../components/VoterDashboard';
import ParticipantDashboard from '../components/ParticipantDashboard';

const Dashboard = ({ currentUser, onUpdateUser }) => {
  const renderDashboard = () => {
    switch (currentUser.userType) {
      case 'ADMIN':
        return <AdminDashboard user={currentUser} onUpdateUser={onUpdateUser} />;
      case 'VOTER':
        return <VoterDashboard user={currentUser} onUpdateUser={onUpdateUser} />;
      case 'PARTICIPANT':
      case 'CANDIDATE': // Handle both frontend terms
        return <ParticipantDashboard user={currentUser} onUpdateUser={onUpdateUser} />;
      default:
        return <p>Invalid user type: {currentUser.userType}</p>;
    }
  };

  // Display user-friendly type names
  const getUserTypeDisplay = () => {
    switch (currentUser.userType) {
      case 'ADMIN':
        return 'ADMIN';
      case 'VOTER':
        return 'VOTER';
      case 'PARTICIPANT':
        return 'CANDIDATE';
      default:
        return currentUser.userType;
    }
  };

  return (
    <div>
      <div className="dashboard-header">
        <h1>Welcome, {currentUser.fullName}!</h1>
        <p>Your {getUserTypeDisplay()} Dashboard</p>
      </div>
      {renderDashboard()}
    </div>
  );
};

export default Dashboard;