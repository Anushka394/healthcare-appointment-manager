import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { getPublicDoctors } from '../../api/doctors';
import LoadingSpinner from '../../components/common/LoadingSpinner';

export default function FindDoctors() {
  const [search, setSearch] = useState('');
  const [submitted, setSubmitted] = useState('');

  const { data: doctors = [], isLoading } = useQuery({
    queryKey: ['publicDoctors', submitted],
    queryFn: () => getPublicDoctors(submitted || undefined),
  });

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-gray-900">Find a Doctor</h1>

      <div className="flex gap-3">
        <input
          className="input max-w-sm"
          placeholder="Search by specialisation (e.g. Cardiology)"
          value={search}
          onChange={e => setSearch(e.target.value)}
          onKeyDown={e => e.key === 'Enter' && setSubmitted(search)}
        />
        <button className="btn-primary" onClick={() => setSubmitted(search)}>Search</button>
        {submitted && (
          <button className="btn-secondary" onClick={() => { setSearch(''); setSubmitted(''); }}>Clear</button>
        )}
      </div>

      {isLoading ? <LoadingSpinner /> : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {doctors.length === 0 ? (
            <p className="text-gray-500 col-span-3 text-center py-8">No doctors found.</p>
          ) : doctors.map(doc => (
            <div key={doc.id} className="card hover:shadow-md transition-shadow">
              <div className="flex items-start gap-3">
                <div className="w-12 h-12 bg-primary-100 rounded-full flex items-center justify-center text-2xl flex-shrink-0">
                  👨‍⚕️
                </div>
                <div className="flex-1 min-w-0">
                  <h3 className="font-semibold text-gray-900 truncate">Dr. {doc.name}</h3>
                  <p className="text-sm text-primary-600">{doc.specialisation}</p>
                  {doc.consultationFee && (
                    <p className="text-xs text-gray-500 mt-1">Fee: ₹{doc.consultationFee}</p>
                  )}
                  <p className="text-xs text-gray-500">
                    {doc.workStartTime} – {doc.workEndTime} · {doc.slotDurationMinutes}min slots
                  </p>
                </div>
              </div>
              {doc.biography && (
                <p className="text-xs text-gray-500 mt-3 line-clamp-2">{doc.biography}</p>
              )}
              <Link
                to={`/patient/book/${doc.id}`}
                className="btn-primary w-full text-center text-sm mt-4 block"
              >
                Book Appointment
              </Link>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
