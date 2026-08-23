import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { getDoctorAppointments } from '../../api/doctors';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import StatusBadge from '../../components/common/StatusBadge';
import { format } from 'date-fns';

const FILTERS = ['ALL', 'CONFIRMED', 'PENDING_SYMPTOMS', 'COMPLETED', 'CANCELLED'];

export default function DoctorAppointments() {
  const [filter, setFilter] = useState('ALL');

  const { data: appointments = [], isLoading } = useQuery({
    queryKey: ['doctorAppointments'],
    queryFn: getDoctorAppointments,
  });

  const filtered = filter === 'ALL' ? appointments : appointments.filter(a => a.status === filter);

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-gray-900">Appointments</h1>

      <div className="flex gap-2 flex-wrap">
        {FILTERS.map(f => (
          <button key={f} onClick={() => setFilter(f)}
            className={`text-sm px-3 py-1.5 rounded-full border transition-colors ${
              filter === f ? 'bg-primary-600 text-white border-primary-600' : 'bg-white text-gray-600 border-gray-300 hover:border-primary-400'
            }`}>
            {f.replace(/_/g, ' ')}
          </button>
        ))}
      </div>

      {isLoading ? <LoadingSpinner /> : (
        <div className="space-y-3">
          {filtered.length === 0 ? (
            <p className="text-gray-500 text-center py-8">No appointments found.</p>
          ) : filtered.map(appt => (
            <div key={appt.id} className="card flex items-center justify-between">
              <div>
                <p className="font-semibold text-sm">{appt.patientName}</p>
                <p className="text-xs text-gray-500">{appt.patientEmail}</p>
                <p className="text-sm text-gray-600 mt-1">
                  📅 {format(new Date(appt.appointmentDate), 'MMM dd, yyyy')} &nbsp;
                  🕐 {appt.slotStartTime} – {appt.slotEndTime}
                </p>
              </div>
              <div className="flex items-center gap-3">
                <StatusBadge status={appt.status} />
                <Link to={`/doctor/appointments/${appt.id}`} className="btn-secondary text-xs">
                  {appt.status === 'CONFIRMED' ? 'Post-Visit Notes' : 'View'}
                </Link>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
