import React, { useState, useEffect } from 'react';

const UserModal = ({ user, isOpen, onClose, onSave, title }) => {
    const [formData, setFormData] = useState({
        fullName: '',
        email: '',
        password: '',
        age: '',
        userType: 'ROLE_VOTER'
    });

    const [errors, setErrors] = useState({});
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        if (user) {
            // Edit mode - populate form with user data
            setFormData({
                fullName: user.fullName || '',
                email: user.email || '',
                password: '', // Don't populate password for edit
                age: user.age || '',
                userType: user.userType || 'ROLE_VOTER'
            });
        } else {
            // Add mode - reset form
            setFormData({
                fullName: '',
                email: '',
                password: '',
                age: '',
                userType: 'ROLE_VOTER'
            });
        }
        setErrors({});
    }, [user, isOpen]);

    const validateForm = () => {
        const newErrors = {};

        if (!formData.fullName.trim()) {
            newErrors.fullName = 'Full name is required';
        }

        if (!formData.email.trim()) {
            newErrors.email = 'Email is required';
        } else if (!/\S+@\S+\.\S+/.test(formData.email)) {
            newErrors.email = 'Please enter a valid email';
        }

        if (!user && !formData.password.trim()) {
            newErrors.password = 'Password is required for new users';
        } else if (!user && formData.password.length < 6) {
            newErrors.password = 'Password must be at least 6 characters';
        }

        if (!formData.age || formData.age < 18 || formData.age > 120) {
            newErrors.age = 'Age must be between 18 and 120';
        }

        setErrors(newErrors);
        return Object.keys(newErrors).length === 0;
    };

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: value
        }));

        // Clear error when user starts typing
        if (errors[name]) {
            setErrors(prev => ({
                ...prev,
                [name]: ''
            }));
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        if (!validateForm()) {
            return;
        }

        setLoading(true);
        try {
            const submitData = { ...formData };
            
            // Convert age to number
            submitData.age = parseInt(submitData.age);

            // For edit mode, don't include password if it's empty
            if (user && !submitData.password.trim()) {
                delete submitData.password;
            }

            await onSave(submitData, user?.id);
            onClose();
        } catch (error) {
            console.error('Error saving user:', error);
            setErrors({
                submit: error.message || 'Failed to save user. Please try again.'
            });
        } finally {
            setLoading(false);
        }
    };

    if (!isOpen) return null;

    return (
        <div className="modal-overlay">
            <div className="modal-content">
                <div className="modal-header">
                    <h2>{title}</h2>
                    <button className="modal-close" onClick={onClose}>×</button>
                </div>

                <form onSubmit={handleSubmit} className="user-form">
                    <div className="form-group">
                        <label htmlFor="fullName">Full Name *</label>
                        <input
                            type="text"
                            id="fullName"
                            name="fullName"
                            value={formData.fullName}
                            onChange={handleChange}
                            className={errors.fullName ? 'error' : ''}
                            placeholder="Enter full name"
                        />
                        {errors.fullName && <div className="error-message">{errors.fullName}</div>}
                    </div>

                    <div className="form-group">
                        <label htmlFor="email">Email *</label>
                        <input
                            type="email"
                            id="email"
                            name="email"
                            value={formData.email}
                            onChange={handleChange}
                            className={errors.email ? 'error' : ''}
                            placeholder="Enter email address"
                        />
                        {errors.email && <div className="error-message">{errors.email}</div>}
                    </div>

                    <div className="form-group">
                        <label htmlFor="password">
                            Password {!user && '*'} {user && '(Leave blank to keep current)'}
                        </label>
                        <input
                            type="password"
                            id="password"
                            name="password"
                            value={formData.password}
                            onChange={handleChange}
                            className={errors.password ? 'error' : ''}
                            placeholder={user ? "Enter new password (optional)" : "Enter password"}
                            autocomplete={user ? "new-password" : "current-password"}
                        />
                        {errors.password && <div className="error-message">{errors.password}</div>}
                    </div>

                    <div className="form-group">
                        <label htmlFor="age">Age *</label>
                        <input
                            type="number"
                            id="age"
                            name="age"
                            value={formData.age}
                            onChange={handleChange}
                            className={errors.age ? 'error' : ''}
                            placeholder="Enter age"
                            min="18"
                            max="120"
                        />
                        {errors.age && <div className="error-message">{errors.age}</div>}
                    </div>

                    <div className="form-group">
                        <label htmlFor="userType">User Type *</label>
                        <select
                            id="userType"
                            name="userType"
                            value={formData.userType}
                            onChange={handleChange}
                            className={errors.userType ? 'error' : ''}
                        >
                            <option value="ROLE_VOTER">Voter</option>
                            <option value="ROLE_PARTICIPANT">Participant (Candidate)</option>
                            <option value="ROLE_ADMIN">Admin</option>
                        </select>
                        {errors.userType && <div className="error-message">{errors.userType}</div>}
                    </div>

                    {errors.submit && <div className="error-message submit-error">{errors.submit}</div>}

                    <div className="modal-actions">
                        <button type="button" onClick={onClose} className="btn-secondary" disabled={loading}>
                            Cancel
                        </button>
                        <button type="submit" className="btn-primary" disabled={loading}>
                            {loading ? 'Saving...' : (user ? 'Update User' : 'Create User')}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default UserModal;