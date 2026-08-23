import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { getMyAppointments } from '../../api/appointments';
import { useAuth } from '../../context/AuthContext';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import StatusBadge from '../../components/common/StatusBadge';
import { format } from 'date-fns';

export default function PatientDashboard() {
  const { user } = useAuth();
  const { data: appointments = [], isLoading } = useQuery({
    queryKey: ['patientAppointments'],
    queryFn: getMyAppointments,
  });

  const upcoming = appointments.filter(a =>
    ['CONFIRMED', 'PENDING_SYMPTOMS'].includes(a.status) &&
    new Date(a.appointmentDate) >= new Date()
  );

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Welcome, {user?.name} 👋</h1>
        <p className="text-gray-500 text-sm mt-1">Here's your health overview</p>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="card text-center">
          <p className="text-3xl font-bold text-primary-600">{upcoming.length}</p>
          <p className="text-sm text-gray-500 mt-1">Upcoming Appointments</p>
        </div>
        <div className="card text-center">
          <p className="text-3xl font-bold text-green-600">
            {appointments.filter(a => a.status === 'COMPLETED').length}
          </p>
          <p className="text-sm text-gray-500 mt-1">Completed Visits</p>
        </div>
        <div className="card text-center">
          <p className="text-3xl font-bold text-gray-600">{appointments.length}</p>
          <p className="text-sm text-gray-500 mt-1">Total Appointments</p>
        </div>
      </div>

      {/* Upcoming */}
      <div className="card">
        <div className="flex items-center justify-between mb-4">
          <h2 className="font-semibold text-gray-900">Upcoming Appointments</h2>
          <Link to="/patient/appointments" className="text-sm text-primary-600 hover:underline">View all</Link>
        </div>

        {isLoading ? <LoadingSpinner /> : upcoming.length === 0 ? (
          <div className="text-center py-8">
            <p className="text-gray-500 text-sm">No upcoming appointments</p>
            <Link to="/patient/doctors" className="btn-primary mt-3 inline-block text-sm">Book an Appointment</Link>
          </div>
        ) : (
          <div className="space-y-3">
            {upcoming.slice(0, 5).map(appt => (
              <div key={appt.id} className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                <div>
                  <p className="font-medium text-sm">Dr. {appt.doctorName}</p>
                  <p className="text-xs text-gray-500">{appt.doctorSpecialisation}</p>
                  <p className="text-xs text-gray-500 mt-1">
                    {format(new Date(appt.appointmentDate), 'MMM dd, yyyy')} · {appt.slotStartTime}
                  </p>
                </div>
                <StatusBadge status={appt.status} />
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Quick action */}
      <div className="card bg-primary-50 border-primary-100">
        <h2 className="font-semibold text-gray-900 mb-2">Need to see a doctor?</h2>
        <p className="text-sm text-gray-600 mb-4">Search by specialisation and book a slot instantly.</p>
        <Link to="/patient/doctors" className="btn-primary text-sm">Find a Doctor</Link>
      </div>
    </div>
  );
}
