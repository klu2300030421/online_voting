// Secure storage utility for sensitive data
class SecureStorage {
  constructor() {
    this.keyPrefix = 'voterow_';
  }

  // Simple encryption for demo purposes - in production use proper encryption
  encrypt(data) {
    try {
      const jsonString = JSON.stringify(data);
      return btoa(jsonString); // Base64 encoding
    } catch (error) {
      console.error('Encryption failed:', error);
      return null;
    }
  }

  decrypt(encryptedData) {
    try {
      const jsonString = atob(encryptedData); // Base64 decoding
      return JSON.parse(jsonString);
    } catch (error) {
      console.error('Decryption failed:', error);
      return null;
    }
  }

  setItem(key, value) {
    try {
      const encryptedValue = this.encrypt(value);
      if (encryptedValue) {
        sessionStorage.setItem(this.keyPrefix + key, encryptedValue);
        return true;
      }
    } catch (error) {
      console.error('Failed to store item:', error);
    }
    return false;
  }

  getItem(key) {
    try {
      const encryptedValue = sessionStorage.getItem(this.keyPrefix + key);
      if (encryptedValue) {
        return this.decrypt(encryptedValue);
      }
    } catch (error) {
      console.error('Failed to retrieve item:', error);
    }
    return null;
  }

  removeItem(key) {
    try {
      sessionStorage.removeItem(this.keyPrefix + key);
      return true;
    } catch (error) {
      console.error('Failed to remove item:', error);
      return false;
    }
  }

  clear() {
    try {
      const keys = Object.keys(sessionStorage);
      keys.forEach(key => {
        if (key.startsWith(this.keyPrefix)) {
          sessionStorage.removeItem(key);
        }
      });
      return true;
    } catch (error) {
      console.error('Failed to clear storage:', error);
      return false;
    }
  }

  // Token-specific methods
  setToken(token) {
    return this.setItem('token', token);
  }

  getToken() {
    return this.getItem('token');
  }

  removeToken() {
    return this.removeItem('token');
  }

  setUser(user) {
    return this.setItem('currentUser', user);
  }

  getUser() {
    return this.getItem('currentUser');
  }

  removeUser() {
    return this.removeItem('currentUser');
  }
}

export default new SecureStorage();