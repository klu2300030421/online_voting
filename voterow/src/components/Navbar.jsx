import React from 'react';
import { Link } from 'react-router-dom';

const Navbar = ({ currentUser, onLogout }) => {
  return (
    <nav className="navbar">
      <Link to="/" className="nav-logo">vote.row</Link>
      <div className="nav-links">
        {currentUser ? (
          <>
            <span className="welcome-message">Welcome, {currentUser.fullName || currentUser.username || 'Admin'}</span>
            <button onClick={onLogout} className="btn btn-secondary">Logout</button>
          </>
        ) : (
          <>
            <Link to="/login">Login</Link>
            <Link to="/signup" className="btn btn-primary">Sign Up</Link>
          </>
        )}
      </div>
    </nav>
  );
};

export default Navbar;