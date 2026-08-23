import { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getPatientProfile, updatePatientProfile } from '../../api/patients';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import ErrorAlert from '../../components/common/ErrorAlert';

export default function PatientProfile() {
  const qc = useQueryClient();
  const [editing, setEditing] = useState(false);
  const [form, setForm] = useState({});
  const [success, setSuccess] = useState('');
  const [error, setError] = useState('');

  const { data: profile, isLoading } = useQuery({
    queryKey: ['patientProfile'],
    queryFn: getPatientProfile,
  });

  useEffect(() => { if (profile) setForm(profile); }, [profile]);

  const mutation = useMutation({
    mutationFn: () => updatePatientProfile(form),
    onSuccess: () => {
      qc.invalidateQueries(['patientProfile']);
      setEditing(false);
      setSuccess('Profile updated successfully.');
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
      ) : (
        <p className="text-sm text-gray-900">{profile?.[key] || '—'}</p>
      )}
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
        <h2 className="font-semibold text-gray-900 border-b pb-2">Personal Information</h2>
        <div className="grid grid-cols-2 gap-4">
          {field('name', 'Full Name')}
          {field('email', 'Email')}
          {field('phoneNumber', 'Phone')}
          {field('dateOfBirth', 'Date of Birth', 'date')}
        </div>
        <div className="grid grid-cols-3 gap-4">
          <div>
            <label className="block text-xs font-medium text-gray-500 mb-1">Gender</label>
            {editing ? (
              <select className="input text-sm" value={form.gender || ''}
                onChange={e => setForm(f => ({ ...f, gender: e.target.value }))}>
                <option value="">Select</option>
                <option>MALE</option><option>FEMALE</option><option>OTHER</option>
              </select>
            ) : <p className="text-sm text-gray-900">{profile?.gender || '—'}</p>}
          </div>
          {field('bloodGroup', 'Blood Group')}
        </div>
        {field('allergies', 'Allergies')}
      </div>

      <div className="card space-y-4">
        <h2 className="font-semibold text-gray-900 border-b pb-2">Emergency Contact</h2>
        <div className="grid grid-cols-2 gap-4">
          {field('emergencyContactName', 'Name')}
          {field('emergencyContactPhone', 'Phone')}
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
