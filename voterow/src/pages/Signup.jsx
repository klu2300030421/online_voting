import React, { useState } from 'react';
import { Link } from 'react-router-dom';

const Signup = ({ onLogin }) => {
  const [fullName, setFullName] = useState('');
  const [age, setAge] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [userType, setUserType] = useState('VOTER');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setIsLoading(true);

    try {
      // Map frontend user types to backend expected values
      let backendUserType = userType;
      if (userType === 'CANDIDATE') {
        backendUserType = 'PARTICIPANT';
      }

      const response = await fetch('http://localhost:8081/api/auth/signup', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          fullName,
          age: parseInt(age, 10),
          email,
          password,
          userType: backendUserType,
        }),
      });

      if (response.ok) {
        // Registration successful, now try to login
        const loginResponse = await fetch('http://localhost:8081/api/auth/login', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({
            email,
            password,
          }),
        });

        if (loginResponse.ok) {
          // Login successful, parse user info directly from login response
          const userInfo = await loginResponse.json();
          
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
            fullName: userInfo.fullName,
            age: userInfo.age,
            email: userInfo.email,
            userType: frontendUserType,
          };
          onLogin(user);
        } else {
          const errorData = await loginResponse.text();
          setError('Registration successful, but automatic login failed: ' + (errorData || 'Please login manually.'));
        }
      } else {
        const errorData = await response.text();
        setError(errorData || 'Registration failed. Please try again.');
      }
    } catch (err) {
      setError('Network error. Please check if the backend server is running.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="form-container">
      <h1>Create Your Account</h1>
      {error && <p style={{ color: 'red' }}>{error}</p>}
      <form onSubmit={handleSubmit}>
        <div className="form-group">
          <label htmlFor="fullName">Full Name</label>
          <input type="text" id="fullName" value={fullName} onChange={(e) => setFullName(e.target.value)} required />
        </div>
        <div className="form-group">
          <label htmlFor="age">Age</label>
          <input type="number" id="age" value={age} onChange={(e) => setAge(e.target.value)} required min="18" />
        </div>
        <div className="form-group">
          <label htmlFor="email">Email</label>
          <input type="email" id="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
        </div>
        <div className="form-group">
          <label htmlFor="password">Password</label>
          <input type="password" id="password" value={password} onChange={(e) => setPassword(e.target.value)} required minLength="8" />
        </div>
        <div className="form-group">
          <label htmlFor="userType">Register as</label>
          <select id="userType" value={userType} onChange={(e) => setUserType(e.target.value)}>
            <option value="VOTER">Voter</option>
            <option value="CANDIDATE">Candidate</option>
            <option value="ADMIN">Admin</option>
          </select>
        </div>
        <button type="submit" className="btn btn-primary" style={{ width: '100%' }} disabled={isLoading}>
          {isLoading ? 'Creating Account...' : 'Sign Up'}
        </button>
      </form>
      <p>
        Already have an account? <Link to="/login">Login</Link>
      </p>
    </div>
  );
};

export default Signup;