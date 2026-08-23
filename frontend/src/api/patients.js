import api from './axios';

export const getPatientProfile = () => api.get('/api/patient/profile').then(r => r.data.data);
export const updatePatientProfile = (data) => api.put('/api/patient/profile', data).then(r => r.data.data);

// Admin
export const adminGetUsers = () => api.get('/api/admin/users').then(r => r.data.data);
export const adminActivateUser = (id) => api.patch(`/api/admin/users/${id}/activate`).then(r => r.data);
export const adminDeactivateUser = (id) => api.patch(`/api/admin/users/${id}/deactivate`).then(r => r.data);
