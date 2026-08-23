import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { adminGetDoctors, adminCreateDoctor, adminUpdateDoctor, adminAddLeave, adminGetLeaves, adminRemoveLeave } from '../../api/doctors';
import { adminGetUsers } from '../../api/patients';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import ErrorAlert from '../../components/common/ErrorAlert';
import { format } from 'date-fns';

const EMPTY_DOCTOR = { userId: '', specialisation: '', workStartTime: '09:00', workEndTime: '17:00', slotDurationMinutes: 30, maxPatientsPerDay: 20, consultationFee: '', biography: '' };

export default function ManageDoctors() {
  const qc = useQueryClient();
  const [showForm, setShowForm] = useState(false);
  const [editDoc, setEditDoc] = useState(null);
  const [form, setForm] = useState(EMPTY_DOCTOR);
  const [error, setError] = useState('');
  const [leaveDoc, setLeaveDoc] = useState(null);
  const [leaveForm, setLeaveForm] = useState({ leaveDate: '', reason: '' });

  const { data: doctors = [], isLoading } = useQuery({ queryKey: ['adminDoctors'], queryFn: adminGetDoctors });
  const { data: users = [] } = useQuery({ queryKey: ['adminUsers'], queryFn: adminGetUsers });
  const { data: leaves = [] } = useQuery({
    queryKey: ['adminLeaves', leaveDoc],
    queryFn: () => adminGetLeaves(leaveDoc),
    enabled: !!leaveDoc,
  });

  const doctorUsers = users.filter(u => u.role === 'DOCTOR');

  const saveMutation = useMutation({
    mutationFn: () => editDoc ? adminUpdateDoctor(editDoc.id, form) : adminCreateDoctor(form),
    onSuccess: () => { qc.invalidateQueries(['adminDoctors']); setShowForm(false); setEditDoc(null); setForm(EMPTY_DOCTOR); },
    onError: (e) => setError(e.response?.data?.message || 'Failed to save'),
  });

  const leaveMutation = useMutation({
    mutationFn: () => adminAddLeave(leaveDoc, leaveForm),
    onSuccess: () => { qc.invalidateQueries(['adminLeaves', leaveDoc]); setLeaveForm({ leaveDate: '', reason: '' }); },
    onError: (e) => setError(e.response?.data?.message || 'Failed to add leave'),
  });

  const removeLeave = useMutation({
    mutationFn: (id) => adminRemoveLeave(id),
    onSuccess: () => qc.invalidateQueries(['adminLeaves', leaveDoc]),
  });

  if (isLoading) return <LoadingSpinner />;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">Manage Doctors</h1>
        <button className="btn-primary text-sm" onClick={() => { setShowForm(true); setEditDoc(null); setForm(EMPTY_DOCTOR); }}>
          + Add Doctor
        </button>
      </div>
      <ErrorAlert message={error} />

      {/* Doctor form */}
      {showForm && (
        <div className="card space-y-4">
          <h2 className="font-semibold text-gray-900">{editDoc ? 'Edit Doctor' : 'New Doctor Profile'}</h2>
          {!editDoc && (
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Link to User *</label>
              <select className="input" value={form.userId} onChange={e => setForm(f => ({ ...f, userId: e.target.value }))}>
                <option value="">Select a doctor user…</option>
                {doctorUsers.map(u => <option key={u.id} value={u.id}>{u.name} ({u.email})</option>)}
              </select>
            </div>
          )}
          <div className="grid grid-cols-2 gap-4">
            {[['specialisation','Specialisation'],['consultationFee','Fee (₹)','number'],['workStartTime','Start Time','time'],['workEndTime','End Time','time'],['slotDurationMinutes','Slot (min)','number'],['maxPatientsPerDay','Max Patients','number']].map(([k, l, t = 'text']) => (
              <div key={k}>
                <label className="block text-xs text-gray-500 mb-1">{l}</label>
                <input type={t} className="input text-sm" value={form[k] || ''}
                  onChange={e => setForm(f => ({ ...f, [k]: e.target.value }))} />
              </div>
            ))}
          </div>
          <div>
            <label className="block text-xs text-gray-500 mb-1">Biography</label>
            <textarea className="input resize-none text-sm" rows={3} value={form.biography || ''}
              onChange={e => setForm(f => ({ ...f, biography: e.target.value }))} />
          </div>
          <div className="flex gap-3">
            <button className="btn-secondary" onClick={() => setShowForm(false)}>Cancel</button>
            <button className="btn-primary" onClick={() => saveMutation.mutate()} disabled={saveMutation.isPending}>
              {saveMutation.isPending ? 'Saving…' : 'Save'}
            </button>
          </div>
        </div>
      )}

      {/* Doctors list */}
      <div className="space-y-3">
        {doctors.map(doc => (
          <div key={doc.id} className="card">
            <div className="flex items-start justify-between">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 bg-primary-100 rounded-full flex items-center justify-center">👨‍⚕️</div>
                <div>
                  <p className="font-semibold">Dr. {doc.name}</p>
                  <p className="text-sm text-primary-600">{doc.specialisation}</p>
                  <p className="text-xs text-gray-500">{doc.workStartTime} – {doc.workEndTime} · {doc.slotDurationMinutes}min slots</p>
                </div>
              </div>
              <div className="flex gap-2">
                <button className="btn-secondary text-xs"
                  onClick={() => { setEditDoc(doc); setForm({ ...doc }); setShowForm(true); }}>Edit</button>
                <button className="btn-secondary text-xs" onClick={() => setLeaveDoc(doc.id)}>Leaves</button>
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Leave management modal */}
      {leaveDoc && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="card w-full max-w-lg space-y-4 max-h-[80vh] overflow-y-auto">
            <div className="flex items-center justify-between">
              <h2 className="font-semibold">Doctor Leaves</h2>
              <button onClick={() => setLeaveDoc(null)} className="text-gray-400 hover:text-gray-600 text-xl">✕</button>
            </div>
            <div className="flex gap-3">
              <input type="date" className="input text-sm" value={leaveForm.leaveDate}
                onChange={e => setLeaveForm(f => ({ ...f, leaveDate: e.target.value }))} />
              <input className="input text-sm flex-1" placeholder="Reason" value={leaveForm.reason}
                onChange={e => setLeaveForm(f => ({ ...f, reason: e.target.value }))} />
              <button className="btn-primary text-sm whitespace-nowrap" onClick={() => leaveMutation.mutate()}
                disabled={!leaveForm.leaveDate || leaveMutation.isPending}>Add</button>
            </div>
            <div className="space-y-2">
              {leaves.map(l => (
                <div key={l.id} className="flex items-center justify-between bg-gray-50 rounded-lg px-3 py-2">
                  <div>
                    <p className="text-sm font-medium">{format(new Date(l.leaveDate), 'MMM dd, yyyy')}</p>
                    {l.reason && <p className="text-xs text-gray-500">{l.reason}</p>}
                  </div>
                  <div className="flex items-center gap-2">
                    <span className={`text-xs px-2 py-0.5 rounded-full ${l.patientsNotified ? 'bg-green-100 text-green-700' : 'bg-yellow-100 text-yellow-700'}`}>
                      {l.patientsNotified ? 'Notified' : 'Pending'}
                    </span>
                    <button className="text-red-400 hover:text-red-600 text-sm"
                      onClick={() => removeLeave.mutate(l.id)}>✕</button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
