// src/pages/Results.jsx
import React, { useState, useEffect } from 'react';

const Results = () => {
  const [completedElections, setCompletedElections] = useState([]);
   const [users, setUsers] = useState([]);

  useEffect(() => {
    const allElections = JSON.parse(localStorage.getItem('voterow_elections')) || [];
    setCompletedElections(allElections.filter(e => e.status === 'Completed'));
    setUsers(JSON.parse(localStorage.getItem('voterow_users')) || []);
  }, []);
  
  const getUserName = (id) => users.find(u => u.id === id)?.fullName || 'Unknown';

  return (
    <div>
      <div className="dashboard-header">
        <h1>Election Results</h1>
        <p>Viewing results for all completed elections.</p>
      </div>
      <div className="dashboard-grid">
        {completedElections.length > 0 ? completedElections.map(election => (
          <div key={election.id} className="card">
            <h3>{election.title}</h3>
             <p><strong>Winner:</strong> {
                // Simplified winner logic: just show the first participant
                getUserName(election.participants[0])
            }</p>
            <p><strong>Total Votes:</strong> {Object.keys(election.votes).length}</p>
          </div>
        )) : <p>No completed elections to show results for.</p>}
      </div>
    </div>
  );
};

export default Results;