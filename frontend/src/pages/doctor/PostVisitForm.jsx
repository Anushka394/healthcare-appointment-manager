import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation } from '@tanstack/react-query';
import { submitPostVisit, getDoctorPostVisit } from '../../api/appointments';
import { getDoctorAppointments } from '../../api/doctors';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import ErrorAlert from '../../components/common/ErrorAlert';
import { format } from 'date-fns';

export default function PostVisitForm() {
  const { appointmentId } = useParams();
  const navigate = useNavigate();
  const [notes, setNotes] = useState({ clinicalNotes: '', diagnosis: '', followUpDate: '' });
  const [prescriptions, setPrescriptions] = useState([]);
  const [error, setError] = useState('');

  const { data: existing, isLoading: existingLoading } = useQuery({
    queryKey: ['postVisit', appointmentId],
    queryFn: () => getDoctorPostVisit(Number(appointmentId)),
    retry: false,
  });

  const mutation = useMutation({
    mutationFn: () => submitPostVisit(Number(appointmentId), {
      ...notes,
      followUpDate: notes.followUpDate || null,
      prescriptions: prescriptions.filter(p => p.medicationName),
    }),
    onSuccess: () => navigate('/doctor/appointments'),
    onError: (e) => setError(e.response?.data?.message || 'Submission failed'),
  });

  const addPrescription = () => setPrescriptions(p => [
    ...p, { medicationName: '', dosage: '', frequency: '', remindersPerDay: 1, startDate: format(new Date(), 'yyyy-MM-dd'), endDate: '', instructions: '' }
  ]);

  const updatePrescription = (i, key, value) => {
    setPrescriptions(prev => prev.map((p, idx) => idx === i ? { ...p, [key]: value } : p));
  };

  const removePrescription = (i) => setPrescriptions(prev => prev.filter((_, idx) => idx !== i));

  if (existingLoading) return <LoadingSpinner />;

  // Show read-only view if already submitted
  if (existing) return (
    <div className="max-w-2xl space-y-6">
      <h1 className="text-2xl font-bold text-gray-900">Post-Visit Record</h1>
      <div className="card space-y-4">
        <div><p className="text-xs text-gray-500">Clinical Notes</p><p className="text-sm">{existing.clinicalNotes}</p></div>
        {existing.diagnosis && <div><p className="text-xs text-gray-500">Diagnosis</p><p className="text-sm">{existing.diagnosis}</p></div>}
        {existing.llmPatientSummary && (
          <div className="bg-blue-50 rounded-lg p-4">
            <p className="text-sm font-medium text-blue-800 mb-1">AI Patient Summary</p>
            <p className="text-sm text-blue-700 whitespace-pre-wrap">{existing.llmPatientSummary}</p>
          </div>
        )}
      </div>
    </div>
  );

  return (
    <div className="max-w-2xl space-y-6">
      <h1 className="text-2xl font-bold text-gray-900">Post-Visit Notes</h1>
      <ErrorAlert message={error} />

      <div className="card space-y-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Clinical Notes *</label>
          <textarea className="input min-h-28 resize-y" placeholder="Observations, findings, clinical assessment..."
            value={notes.clinicalNotes} onChange={e => setNotes(n => ({ ...n, clinicalNotes: e.target.value }))} />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Diagnosis</label>
          <input className="input" placeholder="Primary diagnosis" value={notes.diagnosis}
            onChange={e => setNotes(n => ({ ...n, diagnosis: e.target.value }))} />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Follow-up Date</label>
          <input type="date" className="input max-w-xs" value={notes.followUpDate}
            onChange={e => setNotes(n => ({ ...n, followUpDate: e.target.value }))} />
        </div>
      </div>

      <div className="card space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="font-semibold text-gray-900">Prescriptions</h2>
          <button className="btn-secondary text-sm" onClick={addPrescription}>+ Add Medication</button>
        </div>

        {prescriptions.map((p, i) => (
          <div key={i} className="bg-gray-50 rounded-lg p-4 space-y-3 relative">
            <button className="absolute top-2 right-2 text-gray-400 hover:text-red-500 text-sm"
              onClick={() => removePrescription(i)}>✕</button>
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-xs text-gray-500 mb-1">Medication Name</label>
                <input className="input text-sm" value={p.medicationName}
                  onChange={e => updatePrescription(i, 'medicationName', e.target.value)} />
              </div>
              <div>
                <label className="block text-xs text-gray-500 mb-1">Dosage</label>
                <input className="input text-sm" placeholder="e.g. 500mg" value={p.dosage}
                  onChange={e => updatePrescription(i, 'dosage', e.target.value)} />
              </div>
              <div>
                <label className="block text-xs text-gray-500 mb-1">Frequency</label>
                <input className="input text-sm" placeholder="e.g. Twice daily" value={p.frequency}
                  onChange={e => updatePrescription(i, 'frequency', e.target.value)} />
              </div>
              <div>
                <label className="block text-xs text-gray-500 mb-1">Reminders/day</label>
                <input type="number" min={1} max={4} className="input text-sm" value={p.remindersPerDay}
                  onChange={e => updatePrescription(i, 'remindersPerDay', Number(e.target.value))} />
              </div>
              <div>
                <label className="block text-xs text-gray-500 mb-1">Start Date</label>
                <input type="date" className="input text-sm" value={p.startDate}
                  onChange={e => updatePrescription(i, 'startDate', e.target.value)} />
              </div>
              <div>
                <label className="block text-xs text-gray-500 mb-1">End Date</label>
                <input type="date" className="input text-sm" value={p.endDate}
                  onChange={e => updatePrescription(i, 'endDate', e.target.value)} />
              </div>
            </div>
            <div>
              <label className="block text-xs text-gray-500 mb-1">Instructions</label>
              <input className="input text-sm" placeholder="Take after meals, etc." value={p.instructions}
                onChange={e => updatePrescription(i, 'instructions', e.target.value)} />
            </div>
          </div>
        ))}
      </div>

      <div className="flex gap-3">
        <button className="btn-secondary" onClick={() => navigate('/doctor/appointments')}>Cancel</button>
        <button className="btn-primary" onClick={() => mutation.mutate()} disabled={mutation.isPending || !notes.clinicalNotes}>
          {mutation.isPending ? 'Submitting…' : 'Submit & Complete Appointment'}
        </button>
      </div>
    </div>
  );
}
