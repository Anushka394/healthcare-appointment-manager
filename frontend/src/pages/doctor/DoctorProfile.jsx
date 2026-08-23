import { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getDoctorProfile, updateDoctorProfile } from '../../api/doctors';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import ErrorAlert from '../../components/common/ErrorAlert';

export default function DoctorProfile() {
  const qc = useQueryClient();
  const [editing, setEditing] = useState(false);
  const [form, setForm] = useState({});
  const [success, setSuccess] = useState('');
  const [error, setError] = useState('');

  const { data: profile, isLoading } = useQuery({
    queryKey: ['doctorProfile'],
    queryFn: getDoctorProfile,
  });

  useEffect(() => { if (profile) setForm(profile); }, [profile]);

  const mutation = useMutation({
    mutationFn: () => updateDoctorProfile(form),
    onSuccess: () => {
      qc.invalidateQueries(['doctorProfile']);
      setEditing(false);
      setSuccess('Profile updated.');
      setTimeout(() => setSuccess(''), 3000);
    },
    onError: (e) => setError(e.response?.data?.message || 'Update failed'),
  });

  if (isLoading) return <LoadingSpinner />;

  const field = (key, label, type = 'text') => (
    <div>
      <label className="block text-xs font-medium text-gray-500 mb-1">{label}</label>
      {editing ? (
        <input type={type} className="input text-sm" value={form[key] || ''}
          onChange={e => setForm(f => ({ ...f, [key]: e.target.value }))} />
      ) : <p className="text-sm text-gray-900">{profile?.[key] || '—'}</p>}
    </div>
  );

  return (
    <div className="max-w-2xl space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">My Profile</h1>
        {!editing && <button className="btn-secondary text-sm" onClick={() => setEditing(true)}>Edit</button>}
      </div>
      {success && <div className="bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded-lg text-sm">{success}</div>}
      <ErrorAlert message={error} />

      <div className="card space-y-4">
        <h2 className="font-semibold text-gray-900 border-b pb-2">Professional Info</h2>
        <div className="grid grid-cols-2 gap-4">
          {field('name', 'Name')}
          {field('email', 'Email')}
          {field('specialisation', 'Specialisation')}
          {field('consultationFee', 'Consultation Fee (₹)', 'number')}
          {field('workStartTime', 'Work Start Time', 'time')}
          {field('workEndTime', 'Work End Time', 'time')}
          {field('slotDurationMinutes', 'Slot Duration (min)', 'number')}
          {field('maxPatientsPerDay', 'Max Patients/Day', 'number')}
        </div>
        <div>
          <label className="block text-xs font-medium text-gray-500 mb-1">Biography</label>
          {editing ? (
            <textarea className="input resize-y text-sm" rows={4} value={form.biography || ''}
              onChange={e => setForm(f => ({ ...f, biography: e.target.value }))} />
          ) : <p className="text-sm text-gray-900">{profile?.biography || '—'}</p>}
        </div>
      </div>

      {editing && (
        <div className="flex gap-3">
          <button className="btn-secondary" onClick={() => { setEditing(false); setForm(profile); }}>Cancel</button>
          <button className="btn-primary" onClick={() => mutation.mutate()} disabled={mutation.isPending}>
            {mutation.isPending ? 'Saving…' : 'Save Changes'}
          </button>
        </div>
      )}
    </div>
  );
}
