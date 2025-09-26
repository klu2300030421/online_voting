import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';

const Login = ({ onLogin }) => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setIsLoading(true);

    try {
      const response = await fetch('http://localhost:8081/api/auth/login', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          email,
          password,
        }),
      });

      if (response.ok) {
        // Login successful, parse user info directly from login response
        const userInfo = await response.json();
        
        // Map backend user types to frontend display types
        let frontendUserType = userInfo.userType;
        switch (userInfo.userType) {
          case 'ROLE_ADMIN':
            frontendUserType = 'ADMIN';
            break;
          case 'ROLE_VOTER':
            frontendUserType = 'VOTER';
            break;
          case 'ROLE_PARTICIPANT':
            frontendUserType = 'PARTICIPANT';
            break;
          default:
            frontendUserType = userInfo.userType;
        }

        const user = {
          id: userInfo.id,
          email: userInfo.email,
          fullName: userInfo.fullName,
          age: userInfo.age,
          phoneNumber: userInfo.phoneNumber,
          idProofNumber: userInfo.idProofNumber,
          address: userInfo.address,
          userType: frontendUserType,
          adminRole: userInfo.adminRole,
          isActive: userInfo.isActive,
          isVerified: userInfo.isVerified
        };
        onLogin(user);
      } else {
        const errorData = await response.text();
        setError(errorData || 'Invalid email or password.');
      }
    } catch (err) {
      setError('Network error. Please check if the backend server is running.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="form-container">
      <h1>Login to vote.row</h1>
      {error && <p style={{ color: 'red' }}>{error}</p>}
      <form onSubmit={handleSubmit}>
        <div className="form-group">
          <label htmlFor="email">Email</label>
          <input
            type="email"
            id="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
        </div>
        <div className="form-group">
          <label htmlFor="password">Password</label>
          <input
            type="password"
            id="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
        </div>
        <button type="submit" className="btn btn-primary" style={{ width: '100%' }} disabled={isLoading}>
          {isLoading ? 'Logging in...' : 'Login'}
        </button>
      </form>
      <p>
        Don't have an account? <Link to="/signup">Sign Up</Link>
      </p>
    </div>
  );
};

export default Login;