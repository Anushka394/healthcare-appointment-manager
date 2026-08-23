import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { adminGetDoctors } from '../../api/doctors';
import { adminGetUsers } from '../../api/patients';
import LoadingSpinner from '../../components/common/LoadingSpinner';

export default function AdminDashboard() {
  const { data: doctors = [], isLoading: dLoading } = useQuery({ queryKey: ['adminDoctors'], queryFn: adminGetDoctors });
  const { data: users   = [], isLoading: uLoading } = useQuery({ queryKey: ['adminUsers'],   queryFn: adminGetUsers });

  const patients = users.filter(u => u.role === 'PATIENT');
  const active   = users.filter(u => u.isActive);

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-gray-900">Admin Dashboard</h1>

      <div className="grid grid-cols-1 sm:grid-cols-4 gap-4">
        <div className="card text-center">
          {dLoading ? <LoadingSpinner size="sm" /> : <p className="text-3xl font-bold text-primary-600">{doctors.length}</p>}
          <p className="text-sm text-gray-500 mt-1">Doctors</p>
        </div>
        <div className="card text-center">
          {uLoading ? <LoadingSpinner size="sm" /> : <p className="text-3xl font-bold text-green-600">{patients.length}</p>}
          <p className="text-sm text-gray-500 mt-1">Patients</p>
        </div>
        <div className="card text-center">
          {uLoading ? <LoadingSpinner size="sm" /> : <p className="text-3xl font-bold text-blue-600">{active.length}</p>}
          <p className="text-sm text-gray-500 mt-1">Active Users</p>
        </div>
        <div className="card text-center">
          {uLoading ? <LoadingSpinner size="sm" /> : <p className="text-3xl font-bold text-gray-600">{users.length}</p>}
          <p className="text-sm text-gray-500 mt-1">Total Users</p>
        </div>
      </div>

      <div className="grid grid-cols-2 gap-4">
        <div className="card">
          <h2 className="font-semibold text-gray-900 mb-3">Quick Actions</h2>
          <div className="space-y-2">
            <Link to="/admin/doctors/new" className="btn-primary w-full text-center block text-sm">+ Add Doctor</Link>
            <Link to="/admin/doctors" className="btn-secondary w-full text-center block text-sm">Manage Doctors</Link>
            <Link to="/admin/users" className="btn-secondary w-full text-center block text-sm">Manage Users</Link>
          </div>
        </div>
        <div className="card">
          <h2 className="font-semibold text-gray-900 mb-3">Recent Doctors</h2>
          {dLoading ? <LoadingSpinner size="sm" /> : (
            <div className="space-y-2">
              {doctors.slice(0, 4).map(d => (
                <div key={d.id} className="flex items-center gap-2">
                  <div className="w-7 h-7 bg-primary-100 rounded-full flex items-center justify-center text-sm">👨‍⚕️</div>
                  <div>
                    <p className="text-sm font-medium">Dr. {d.name}</p>
                    <p className="text-xs text-gray-500">{d.specialisation}</p>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
