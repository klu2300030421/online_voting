import React, { useState, useEffect, useCallback } from 'react';
import { 
  FaUsers, FaVoteYea, FaChartBar, FaShieldAlt, FaBell, FaCog, FaPlay, 
  FaStop, FaUser, FaHome, FaSearch, FaPlus, FaEdit, FaTrash, 
  FaDownload, FaUpload, FaCheck, FaUserCheck, FaTimes, FaCalendarAlt,
  FaSms, FaEnvelope, FaEye, FaFileExport, FaUserTie, FaWarning
} from 'react-icons/fa';

const ElectionManagementModule = () => {
  const [showScheduleModal, setShowScheduleModal] = useState(false);
  const [showAssignCandidatesModal, setShowAssignCandidatesModal] = useState(false);
  const [selectedElection, setSelectedElection] = useState(null);
  const [scheduleForm, setScheduleForm] = useState({
    startDate: '',
    endDate: '',
  });
  const [availableCandidates, setAvailableCandidates] = useState([]);
  const [selectedCandidates, setSelectedCandidates] = useState([]);
  
  // Handlers for election management
  const handleScheduleElection = (election) => {
    setSelectedElection(election);
    setScheduleForm({
      startDate: election.startDate ? election.startDate.split('T')[0] : '',
      endDate: election.endDate ? election.endDate.split('T')[0] : ''
    });
    setShowScheduleModal(true);
  };
  
  const handleAssignCandidates = (election) => {
    setSelectedElection(election);
    
    // Load candidates for this election
    apiCall('/candidates')
      .then(candidates => {
        setAvailableCandidates(candidates || []);
        
        // Get currently assigned candidates
        apiCall(`/elections/${election.id}/candidates`)
          .then(assigned => {
            setSelectedCandidates(assigned.map(c => c.id) || []);
          })
          .catch(err => {
            console.error("Error loading assigned candidates:", err);
            setSelectedCandidates([]);
          });
      })
      .catch(err => {
        console.error("Error loading candidates:", err);
        setAvailableCandidates([]);
      });
    
    setShowAssignCandidatesModal(true);
  };
  
  // Save functions
  const handleSaveSchedule = async () => {
    try {
      if (!scheduleForm.startDate || !scheduleForm.endDate) {
        alert("Please set both start and end dates");
        return;
      }
      
      await apiCall(`/elections/${selectedElection.id}/schedule`, {
        method: 'POST',
        body: JSON.stringify(scheduleForm)
      });
      
      alert("Election schedule updated successfully!");
      setShowScheduleModal(false);
      loadData('elections');
    } catch (err) {
      console.error("Error saving schedule:", err);
      alert("Failed to update election schedule. Please try again.");
    }
  };
  
  const handleSaveAssignments = async () => {
    try {
      await apiCall(`/elections/candidates/assign`, {
        method: 'POST',
        body: JSON.stringify({
          electionId: selectedElection.id,
          candidateIds: selectedCandidates
        })
      });
      
      alert("Candidate assignments updated successfully!");
      setShowAssignCandidatesModal(false);
      loadData('elections');
    } catch (err) {
      console.error("Error saving candidate assignments:", err);
      alert("Failed to update candidate assignments. Please try again.");
    }
  };
  
  const toggleCandidateSelection = (candidateId) => {
    if (selectedCandidates.includes(candidateId)) {
      setSelectedCandidates(selectedCandidates.filter(id => id !== candidateId));
    } else {
      setSelectedCandidates([...selectedCandidates, candidateId]);
    }
  };
  
  return (
    <div className="admin-module">
      <h3 className="module-title"><FaVoteYea /> Election Management</h3>
      <div className="module-content">
        <div className="action-buttons">
          <button className="btn btn-primary" onClick={handleCreateElection}>
            <FaPlus /> Create Election
          </button>
          <button className="btn btn-secondary" onClick={() => {
            if (data.elections && data.elections.length > 0) {
              // Auto-select first election if available
              const firstElection = data.elections[0];
              handleScheduleElection(firstElection);
            } else {
              alert("Please create an election first.");
            }
          }}>
            <FaCalendarAlt /> Schedule Elections
          </button>
          <button className="btn btn-info" onClick={() => {
            if (data.elections && data.elections.length > 0) {
              // Auto-select first election if available
              const firstElection = data.elections[0];
              handleAssignCandidates(firstElection);
            } else {
              alert("Please create an election first.");
            }
          }}>
            <FaUserTie /> Assign Candidates
          </button>
          <button className="btn btn-secondary" onClick={() => loadData('elections')}>
            <FaSearch /> Refresh Data
          </button>
        </div>
        <div className="data-table">
          <h4>All Elections ({data.elections.length || 0})</h4>
          {data.elections && data.elections.length > 0 ? (
            <table className="table">
              <thead>
                <tr>
                  <th>Title</th>
                  <th>Start Date</th>
                  <th>End Date</th>
                  <th>Status</th>
                  <th>Candidates</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {data.elections.map(election => (
                  <tr key={election.id}>
                    <td>{election.title}</td>
                    <td>{election.startDate ? new Date(election.startDate).toLocaleDateString() : 'Not Set'}</td>
                    <td>{election.endDate ? new Date(election.endDate).toLocaleDateString() : 'Not Set'}</td>
                    <td>
                      <span className={`status ${election.status?.toLowerCase()}`}>
                        {election.status}
                      </span>
                    </td>
                    <td>{election.candidateCount || 0}</td>
                    <td>
                      <button 
                        className="btn btn-sm btn-primary" 
                        onClick={() => handleEditElection(election)}
                        title="Edit Election"
                      >
                        <FaEdit />
                      </button>
                      <button 
                        className="btn btn-sm btn-info" 
                        onClick={() => viewEnrollmentRequests(election.id)}
                        title="View Enrollment Requests"
                      >
                        <FaUserCheck />
                      </button>
                      <button 
                        className="btn btn-sm btn-success" 
                        onClick={() => handleStartElection(election)}
                        disabled={election.status === 'ONGOING' || election.status === 'COMPLETED'}
                        title="Start Election"
                      >
                        <FaPlay />
                      </button>
                      <button 
                        className="btn btn-sm btn-danger" 
                        onClick={() => handleEndElection(election)}
                        disabled={election.status !== 'ONGOING'}
                        title="End Election"
                      >
                        <FaStop />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          ) : (
            <p>No elections found. <button className="btn btn-primary btn-sm" onClick={handleCreateElection}>Create your first election</button></p>
          )}
        </div>
        
        {/* Schedule Election Modal */}
        {showScheduleModal && (
          <div className="modal-overlay">
            <div className="modal-content">
              <div className="modal-header">
                <h3>Schedule Election</h3>
                <button className="close-btn" onClick={() => setShowScheduleModal(false)}>×</button>
              </div>
              <div className="modal-body">
                {selectedElection && (
                  <>
                    <h4>{selectedElection.title}</h4>
                    <div className="form-group">
                      <label>Start Date</label>
                      <input 
                        type="date" 
                        className="form-control" 
                        value={scheduleForm.startDate} 
                        onChange={(e) => setScheduleForm({...scheduleForm, startDate: e.target.value})}
                      />
                    </div>
                    <div className="form-group">
                      <label>End Date</label>
                      <input 
                        type="date" 
                        className="form-control" 
                        value={scheduleForm.endDate} 
                        onChange={(e) => setScheduleForm({...scheduleForm, endDate: e.target.value})}
                      />
                    </div>
                  </>
                )}
              </div>
              <div className="modal-footer">
                <button className="btn btn-secondary" onClick={() => setShowScheduleModal(false)}>
                  Cancel
                </button>
                <button className="btn btn-primary" onClick={handleSaveSchedule}>
                  Update Schedule
                </button>
              </div>
            </div>
          </div>
        )}
        
        {/* Assign Candidates Modal */}
        {showAssignCandidatesModal && (
          <div className="modal-overlay">
            <div className="modal-content">
              <div className="modal-header">
                <h3>Assign Candidates</h3>
                <button className="close-btn" onClick={() => setShowAssignCandidatesModal(false)}>×</button>
              </div>
              <div className="modal-body">
                {selectedElection && (
                  <>
                    <h4>Election: {selectedElection.title}</h4>
                    {availableCandidates.length === 0 ? (
                      <p>No candidates available. Please add candidates first.</p>
                    ) : (
                      <div className="candidate-list">
                        {availableCandidates.map(candidate => (
                          <div className="candidate-item" key={candidate.id}>
                            <label className="checkbox-label">
                              <input 
                                type="checkbox"
                                checked={selectedCandidates.includes(candidate.id)}
                                onChange={() => toggleCandidateSelection(candidate.id)}
                              />
                              <span>{candidate.name}</span> 
                              <span className="candidate-party">({candidate.party || 'Independent'})</span>
                            </label>
                          </div>
                        ))}
                      </div>
                    )}
                  </>
                )}
              </div>
              <div className="modal-footer">
                <button className="btn btn-secondary" onClick={() => setShowAssignCandidatesModal(false)}>
                  Cancel
                </button>
                <button 
                  className="btn btn-primary" 
                  onClick={handleSaveAssignments}
                  disabled={availableCandidates.length === 0}
                >
                  Save Assignments
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};