import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getDoctorLeaves, requestDoctorLeave } from '../../api/doctors';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import ErrorAlert from '../../components/common/ErrorAlert';
import { format } from 'date-fns';

export default function DoctorLeaves() {
  const qc = useQueryClient();
  const [form, setForm] = useState({ leaveDate: '', reason: '' });
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const { data: leaves = [], isLoading } = useQuery({
    queryKey: ['doctorLeaves'],
    queryFn: getDoctorLeaves,
  });

  const mutation = useMutation({
    mutationFn: () => requestDoctorLeave(form),
    onSuccess: () => {
      qc.invalidateQueries(['doctorLeaves']);
      setForm({ leaveDate: '', reason: '' });
      setSuccess('Leave added. Affected patients will be notified automatically.');
      setTimeout(() => setSuccess(''), 4000);
    },
    onError: (e) => setError(e.response?.data?.message || 'Failed to add leave'),
  });

  return (
    <div className="max-w-2xl space-y-6">
      <h1 className="text-2xl font-bold text-gray-900">My Leaves</h1>

      <div className="card space-y-4">
        <h2 className="font-semibold text-gray-900">Request New Leave</h2>
        {success && <div className="bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded-lg text-sm">{success}</div>}
        <ErrorAlert message={error} />
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Date *</label>
            <input type="date" className="input" value={form.leaveDate}
              onChange={e => setForm(f => ({ ...f, leaveDate: e.target.value }))} />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Reason</label>
            <input className="input" placeholder="Optional" value={form.reason}
              onChange={e => setForm(f => ({ ...f, reason: e.target.value }))} />
          </div>
        </div>
        <button className="btn-primary" onClick={() => mutation.mutate()}
          disabled={mutation.isPending || !form.leaveDate}>
          {mutation.isPending ? 'Adding…' : 'Add Leave'}
        </button>
      </div>

      <div className="card">
        <h2 className="font-semibold text-gray-900 mb-4">Leave History</h2>
        {isLoading ? <LoadingSpinner /> : leaves.length === 0 ? (
          <p className="text-gray-500 text-sm text-center py-6">No leaves recorded.</p>
        ) : (
          <div className="space-y-2">
            {leaves.map(l => (
              <div key={l.id} className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                <div>
                  <p className="font-medium text-sm">{format(new Date(l.leaveDate), 'MMMM dd, yyyy')}</p>
                  {l.reason && <p className="text-xs text-gray-500">{l.reason}</p>}
                </div>
                <span className={`text-xs px-2 py-1 rounded-full ${l.patientsNotified ? 'bg-green-100 text-green-700' : 'bg-yellow-100 text-yellow-700'}`}>
                  {l.patientsNotified ? 'Patients notified' : 'Notifying…'}
                </span>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
