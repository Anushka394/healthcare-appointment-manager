import api from './axios';

// Patient booking flow
export const holdSlot = (data) => api.post('/api/patient/appointments/hold', data).then(r => r.data.data);
export const confirmBooking = (holdId) =>
  api.post(`/api/patient/appointments/${holdId}/confirm`).then(r => r.data.data);
export const getMyAppointments = () => api.get('/api/patient/appointments').then(r => r.data.data);
export const cancelAppointment = (id, reason) =>
  api.delete(`/api/patient/appointments/${id}`, { data: { reason } }).then(r => r.data.data);
export const rescheduleAppointment = (id, data) =>
  api.put(`/api/patient/appointments/${id}/reschedule`, data).then(r => r.data.data);

// Symptom form
export const submitSymptomForm = (appointmentId, data) =>
  api.post(`/api/patient/appointments/${appointmentId}/symptoms`, data).then(r => r.data.data);
export const getSymptomForm = (appointmentId) =>
  api.get(`/api/patient/appointments/${appointmentId}/symptoms`).then(r => r.data.data);

// Post-visit
export const getPostVisitSummary = (appointmentId) =>
  api.get(`/api/patient/appointments/${appointmentId}/post-visit`).then(r => r.data.data);

// Doctor
export const submitPostVisit = (appointmentId, data) =>
  api.post(`/api/doctor/appointments/${appointmentId}/post-visit`, data).then(r => r.data.data);
export const getDoctorPostVisit = (appointmentId) =>
  api.get(`/api/doctor/appointments/${appointmentId}/post-visit`).then(r => r.data.data);
