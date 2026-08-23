import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/common/ProtectedRoute';

// Auth
import Login from './pages/auth/Login';
import Register from './pages/auth/Register';

// Patient
import PatientLayout from './pages/patient/PatientLayout';
import PatientDashboard from './pages/patient/PatientDashboard';
import FindDoctors from './pages/patient/FindDoctors';
import BookAppointment from './pages/patient/BookAppointment';
import MyAppointments from './pages/patient/MyAppointments';
import PatientProfile from './pages/patient/PatientProfile';

// Doctor
import DoctorLayout from './pages/doctor/DoctorLayout';
import DoctorDashboard from './pages/doctor/DoctorDashboard';
import DoctorAppointments from './pages/doctor/DoctorAppointments';
import PostVisitForm from './pages/doctor/PostVisitForm';
import DoctorLeaves from './pages/doctor/DoctorLeaves';
import DoctorProfile from './pages/doctor/DoctorProfile';

// Admin
import AdminLayout from './pages/admin/AdminLayout';
import AdminDashboard from './pages/admin/AdminDashboard';
import ManageDoctors from './pages/admin/ManageDoctors';
import ManageUsers from './pages/admin/ManageUsers';

const qc = new QueryClient({ defaultOptions: { queries: { retry: 1, staleTime: 30_000 } } });

export default function App() {
  return (
    <QueryClientProvider client={qc}>
      <AuthProvider>
        <BrowserRouter>
          <Routes>
            {/* Public */}
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />
            <Route path="/unauthorized" element={
              <div className="min-h-screen flex items-center justify-center">
                <div className="text-center">
                  <p className="text-5xl mb-4">🚫</p>
                  <h1 className="text-2xl font-bold text-gray-900">Access Denied</h1>
                  <p className="text-gray-500 mt-2">You don't have permission to view this page.</p>
                </div>
              </div>
            } />

            {/* Patient portal */}
            <Route path="/patient" element={
              <ProtectedRoute allowedRoles={['PATIENT', 'ADMIN']}>
                <PatientLayout />
              </ProtectedRoute>
            }>
              <Route index element={<PatientDashboard />} />
              <Route path="doctors" element={<FindDoctors />} />
              <Route path="book/:doctorId" element={<BookAppointment />} />
              <Route path="appointments" element={<MyAppointments />} />
              <Route path="profile" element={<PatientProfile />} />
            </Route>

            {/* Doctor portal */}
            <Route path="/doctor" element={
              <ProtectedRoute allowedRoles={['DOCTOR', 'ADMIN']}>
                <DoctorLayout />
              </ProtectedRoute>
            }>
              <Route index element={<DoctorDashboard />} />
              <Route path="appointments" element={<DoctorAppointments />} />
              <Route path="appointments/:appointmentId" element={<PostVisitForm />} />
              <Route path="leaves" element={<DoctorLeaves />} />
              <Route path="profile" element={<DoctorProfile />} />
            </Route>

            {/* Admin portal */}
            <Route path="/admin" element={
              <ProtectedRoute allowedRoles={['ADMIN']}>
                <AdminLayout />
              </ProtectedRoute>
            }>
              <Route index element={<AdminDashboard />} />
              <Route path="doctors" element={<ManageDoctors />} />
              <Route path="users" element={<ManageUsers />} />
            </Route>

            {/* Default redirect */}
            <Route path="/" element={<Navigate to="/login" replace />} />
            <Route path="*" element={<Navigate to="/login" replace />} />
          </Routes>
        </BrowserRouter>
      </AuthProvider>
    </QueryClientProvider>
  );
}
