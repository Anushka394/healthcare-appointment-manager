import api from './axios';

export const getPublicDoctors = (specialisation) =>
  api.get('/api/doctors/public', { params: specialisation ? { specialisation } : {} }).then(r => r.data.data);

export const getPublicDoctor = (id) =>
  api.get(`/api/doctors/public/${id}`).then(r => r.data.data);

export const getAvailableSlots = (doctorId, date) =>
  api.get(`/api/doctors/public/${doctorId}/slots`, { params: { date } }).then(r => r.data.data);

// Admin
export const adminCreateDoctor = (data) => api.post('/api/admin/doctors', data).then(r => r.data.data);
export const adminUpdateDoctor = (id, data) => api.put(`/api/admin/doctors/${id}`, data).then(r => r.data.data);
export const adminGetDoctors = () => api.get('/api/admin/doctors').then(r => r.data.data);

export const adminAddLeave = (doctorId, data) =>
  api.post(`/api/admin/doctors/${doctorId}/leaves`, data).then(r => r.data.data);
export const adminGetLeaves = (doctorId) =>
  api.get(`/api/admin/doctors/${doctorId}/leaves`).then(r => r.data.data);
export const adminRemoveLeave = (leaveId) =>
  api.delete(`/api/admin/leaves/${leaveId}`).then(r => r.data);

// Doctor self
export const getDoctorProfile = () => api.get('/api/doctor/profile').then(r => r.data.data);
export const updateDoctorProfile = (data) => api.put('/api/doctor/profile', data).then(r => r.data.data);
export const getDoctorAppointments = () => api.get('/api/doctor/appointments').then(r => r.data.data);
export const getDoctorAppointmentsByDate = (date) =>
  api.get(`/api/doctor/appointments/date/${date}`).then(r => r.data.data);
export const getDoctorLeaves = () => api.get('/api/doctor/leaves').then(r => r.data.data);
export const requestDoctorLeave = (data) => api.post('/api/doctor/leaves', data).then(r => r.data.data);
