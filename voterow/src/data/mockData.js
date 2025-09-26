// This file simulates a database using localStorage.

// Initialize with some default data if none exists
export const initializeData = () => {
  const users = localStorage.getItem('voterow_users');
  const elections = localStorage.getItem('voterow_elections');

  // This now initializes the user list as an empty array
  if (!users) {
    localStorage.setItem('voterow_users', JSON.stringify([]));
  }

  // We'll keep the sample elections so you have something to interact with
  if (!elections) {
    localStorage.setItem('voterow_elections', JSON.stringify([
        {
            id: 1,
            title: 'Annual Board Election',
            startDate: '2025-10-15T09:00:00+05:30', // 9:00 AM IST
            endDate: '2025-10-15T17:00:00+05:30',   // 5:00 PM IST
            participants: ['participant1', 'participant2'],
            voters: ['voter1', 'voter2'],
            votes: {}, 
        },
        {
            id: 2,
            title: 'Project Lead Vote',
            startDate: '2025-09-01T09:00:00+05:30',
            endDate: '2025-09-01T17:00:00+05:30',
            participants: ['participant1'],
            voters: ['voter1', 'voter2'],
            votes: { voter1: 'participant1' },
        },
        {
            id: 3,
            title: 'Live Municipal Election',
            startDate: '2025-09-19T14:00:00+05:30', // 2:00 PM IST
            endDate: '2025-09-19T22:00:00+05:30',   // 10:00 PM IST
            participants: ['participant1', 'participant2'],
            voters: ['voter1', 'voter2'],
            votes: {}, 
        }
    ]));
  }
};