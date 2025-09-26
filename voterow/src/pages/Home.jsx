// src/pages/Home.jsx
import React from 'react';
import { Link } from 'react-router-dom';

const Home = () => {
  return (
    <div style={{ textAlign: 'center', paddingTop: '4rem' }}>
      <h1>Welcome to vote.row</h1>
      <p style={{ fontSize: '1.2rem', margin: '1rem 0 2rem' }}>
        The secure and transparent platform for online voting.
      </p>
      <Link to="/signup" className="btn btn-primary">Get Started</Link>
    </div>
  );
};

export default Home;

