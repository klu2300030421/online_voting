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
import secureStorage from './utils/secureStorage';

function App() {
  const [currentUser, setCurrentUser] = useState(null);
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    // Initialize mock data on first load
    initializeData();

    // Check if there's a saved user session and restore it
    const savedUser = secureStorage.getUser();
    if (savedUser) {
      setCurrentUser(savedUser);
      console.log('Restored user session:', savedUser.email);
    } else {
      console.log('No saved user session found');
    }
  }, []);

  const handleLogin = (loginResponse) => {
    // Handle both old format (direct user) and new format (user + token)
    let user, token;
    
    if (loginResponse.user && loginResponse.token) {
      // New JWT format
      user = loginResponse.user;
      token = loginResponse.token;
      secureStorage.setToken(token);
    } else {
      // Old format - direct user object
      user = loginResponse;
    }
    
    // Store user securely
    secureStorage.setUser(user);
    
    setCurrentUser(user);
    navigate('/dashboard');
    console.log('User logged in:', user.email);
  };

  const handleLogout = () => {
    // Clear secure storage
    secureStorage.clear();
    setCurrentUser(null);
    navigate('/login');
    console.log('User logged out');
  };
  
  const handleUpdateUser = (updatedUser) => {
      setCurrentUser(updatedUser);
      // Persist updated user session securely
      secureStorage.setUser(updatedUser);
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