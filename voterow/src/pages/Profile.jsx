import React, { useState } from 'react';

const Profile = ({ user, onUpdateUser }) => {
  const [fullName, setFullName] = useState(user.fullName);
  const [age, setAge] = useState(user.age);
  const [email] = useState(user.email); // Email is not editable
  const [message, setMessage] = useState('');

  const handleUpdate = (e) => {
    e.preventDefault();
    const updatedUser = {
      ...user,
      fullName,
      age: parseInt(age, 10),
    };
    onUpdateUser(updatedUser);
    setMessage('Profile updated successfully!');
  };

  return (
    <div className="form-container">
      <h1>Edit Your Profile</h1>
       {message && <p style={{ color: 'green' }}>{message}</p>}
      <form onSubmit={handleUpdate}>
        <div className="form-group">
          <label>Email (cannot be changed)</label>
          <input type="email" value={email} disabled />
        </div>
        <div className="form-group">
          <label htmlFor="fullName">Full Name</label>
          <input
            type="text"
            id="fullName"
            value={fullName}
            onChange={(e) => setFullName(e.target.value)}
            required
          />
        </div>
        <div className="form-group">
          <label htmlFor="age">Age</label>
          <input
            type="number"
            id="age"
            value={age}
            onChange={(e) => setAge(e.target.value)}
            required
            min="18"
          />
        </div>
        <button type="submit" className="btn btn-primary" style={{ width: '100%' }}>
          Update Profile
        </button>
      </form>
    </div>
  );
};

export default Profile;
