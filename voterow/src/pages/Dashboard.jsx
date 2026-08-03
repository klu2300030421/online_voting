import React from 'react';
import AdminDashboard from '../components/AdminDashboard';
import VoterDashboard from '../components/VoterDashboard';
import ParticipantDashboard from '../components/ParticipantDashboard';

const Dashboard = ({ currentUser, onUpdateUser }) => {
  const renderDashboard = () => {
    switch (currentUser.userType) {
      case 'ROLE_ADMIN':
      case 'ADMIN':
        return <AdminDashboard user={currentUser} onUpdateUser={onUpdateUser} />;
      case 'ROLE_VOTER':
      case 'VOTER':
        return <VoterDashboard user={currentUser} onUpdateUser={onUpdateUser} />;
      case 'ROLE_PARTICIPANT':
      case 'ROLE_CANDIDATE':
      case 'PARTICIPANT':
      case 'CANDIDATE':
        return <ParticipantDashboard user={currentUser} onUpdateUser={onUpdateUser} />;
      default:
        return <p>Invalid user type: {currentUser.userType}</p>;
    }
  };

  return <div className="dashboard-page">{renderDashboard()}</div>;
};

export default Dashboard;
