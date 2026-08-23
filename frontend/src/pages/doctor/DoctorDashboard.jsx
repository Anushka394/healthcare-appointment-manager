import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { getDoctorAppointments, getDoctorAppointmentsByDate } from '../../api/doctors';
import { useAuth } from '../../context/AuthContext';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import StatusBadge from '../../components/common/StatusBadge';
import { format } from 'date-fns';

const today = format(new Date(), 'yyyy-MM-dd');

export default function DoctorDashboard() {
  const { user } = useAuth();

  const { data: all = [], isLoading } = useQuery({
    queryKey: ['doctorAppointments'],
    queryFn: getDoctorAppointments,
  });

  const { data: todayAppts = [] } = useQuery({
    queryKey: ['doctorAppointmentsDate', today],
    queryFn: () => getDoctorAppointmentsByDate(today),
  });

  const confirmed = all.filter(a => a.status === 'CONFIRMED');
  const pending   = all.filter(a => a.status === 'PENDING_SYMPTOMS');

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Welcome, Dr. {user?.name} 👋</h1>
        <p className="text-gray-500 text-sm mt-1">Here's your schedule overview</p>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-4 gap-4">
        <div className="card text-center">
          <p className="text-3xl font-bold text-blue-600">{todayAppts.length}</p>
          <p className="text-sm text-gray-500 mt-1">Today's Appointments</p>
        </div>
        <div className="card text-center">
          <p className="text-3xl font-bold text-green-600">{confirmed.length}</p>
          <p className="text-sm text-gray-500 mt-1">Confirmed</p>
        </div>
        <div className="card text-center">
          <p className="text-3xl font-bold text-yellow-600">{pending.length}</p>
          <p className="text-sm text-gray-500 mt-1">Pending Symptoms</p>
        </div>
        <div className="card text-center">
          <p className="text-3xl font-bold text-gray-600">{all.length}</p>
          <p className="text-sm text-gray-500 mt-1">Total</p>
        </div>
      </div>

      <div className="card">
        <div className="flex items-center justify-between mb-4">
          <h2 className="font-semibold text-gray-900">Today's Schedule</h2>
          <Link to="/doctor/appointments" className="text-sm text-primary-600 hover:underline">View all</Link>
        </div>
        {isLoading ? <LoadingSpinner /> : todayAppts.length === 0 ? (
          <p className="text-gray-500 text-sm text-center py-8">No appointments today.</p>
        ) : (
          <div className="space-y-3">
            {todayAppts.map(appt => (
              <div key={appt.id} className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                <div>
                  <p className="font-medium text-sm">{appt.patientName}</p>
                  <p className="text-xs text-gray-500">{appt.slotStartTime} – {appt.slotEndTime}</p>
                </div>
                <div className="flex items-center gap-2">
                  <StatusBadge status={appt.status} />
                  <Link to={`/doctor/appointments/${appt.id}`} className="text-xs text-primary-600 hover:underline">View</Link>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
