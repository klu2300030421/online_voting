import React, { useState, useEffect } from 'react';
import { Routes, Route, useNavigate, useLocation } from 'react-router-dom';
import Navbar from './components/Navbar';
import Home from './pages/Home';
import Login from './pages/Login';
import Signup from './pages/Signup';
import Dashboard from './pages/Dashboard';
import Results from './pages/Results';
import Profile from './pages/Profile';
import { initializeData } from './data/mockData';

function App() {
  const [currentUser, setCurrentUser] = useState(null);
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    // Initialize mock data on first load
    initializeData();

    // Check if there's a saved user session and restore it
    const savedUser = localStorage.getItem('voterow_currentUser');
    if (savedUser) {
      try {
        const user = JSON.parse(savedUser);
        setCurrentUser(user);
        console.log('Restored user session:', user.email);
      } catch (error) {
        console.error('Error parsing saved user session:', error);
        localStorage.removeItem('voterow_currentUser');
      }
    } else {
      console.log('No saved user session found');
    }
  }, []);

  const handleLogin = (user) => {
    // Persist login to localStorage for session continuity
    localStorage.setItem('voterow_currentUser', JSON.stringify(user));
    setCurrentUser(user);
    navigate('/dashboard');
    console.log('User logged in:', user.email);
  };

  const handleLogout = () => {
    // Clear both state and any potential localStorage data
    localStorage.removeItem('voterow_currentUser');
    setCurrentUser(null);
    navigate('/login');
    console.log('User logged out');
  };
  
  const handleUpdateUser = (updatedUser) => {
      setCurrentUser(updatedUser);
      // Persist updated user session
      localStorage.setItem('voterow_currentUser', JSON.stringify(updatedUser));
      // Update in mock users data
      const users = JSON.parse(localStorage.getItem('voterow_users')) || [];
      const userIndex = users.findIndex(u => u.id === updatedUser.id);
      if (userIndex !== -1) {
          users[userIndex] = updatedUser;
          localStorage.setItem('voterow_users', JSON.stringify(users));
      }
      console.log('User profile updated:', updatedUser.email);
  }

  // Make updateUserProfile available globally for dashboard components
  useEffect(() => {
    window.updateUserProfile = handleUpdateUser;
    return () => {
      delete window.updateUserProfile;
    };
  }, []);

  return (
    <div className="app-container">
      <Navbar currentUser={currentUser} onLogout={handleLogout} />
      <main className="main-content">
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/login" element={<Login onLogin={handleLogin} />} />
          <Route path="/signup" element={<Signup onLogin={handleLogin} />} />
          
          {/* Protected Routes - Require Authentication */}
          <Route path="/dashboard" element={
            currentUser ? 
              <Dashboard currentUser={currentUser} onUpdateUser={handleUpdateUser} /> : 
              <Login onLogin={handleLogin} />
          } />
          <Route path="/results" element={
            currentUser ? 
              <Results /> : 
              <Login onLogin={handleLogin} />
          } />
          <Route path="/profile" element={
            currentUser ? 
              <Profile user={currentUser} onUpdateUser={handleUpdateUser} /> : 
              <Login onLogin={handleLogin} />
          } />
        </Routes>
      </main>
    </div>
  );
}

export default App;