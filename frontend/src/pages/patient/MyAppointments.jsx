import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getMyAppointments, cancelAppointment, getPostVisitSummary } from '../../api/appointments';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import StatusBadge from '../../components/common/StatusBadge';
import ErrorAlert from '../../components/common/ErrorAlert';
import { format } from 'date-fns';

export default function MyAppointments() {
  const qc = useQueryClient();
  const [cancelId, setCancelId] = useState(null);
  const [reason, setReason] = useState('');
  const [summaryAppt, setSummaryAppt] = useState(null);
  const [summary, setSummary] = useState(null);
  const [error, setError] = useState('');

  const { data: appointments = [], isLoading } = useQuery({
    queryKey: ['patientAppointments'],
    queryFn: getMyAppointments,
  });

  const cancelMutation = useMutation({
    mutationFn: () => cancelAppointment(cancelId, reason),
    onSuccess: () => { qc.invalidateQueries(['patientAppointments']); setCancelId(null); setReason(''); },
    onError: (e) => setError(e.response?.data?.message || 'Cancellation failed'),
  });

  const loadSummary = async (id) => {
    try {
      const data = await getPostVisitSummary(id);
      setSummary(data);
      setSummaryAppt(id);
    } catch { setSummary(null); setSummaryAppt(id); }
  };

  if (isLoading) return <LoadingSpinner />;

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-gray-900">My Appointments</h1>
      <ErrorAlert message={error} />

      {appointments.length === 0 ? (
        <div className="card text-center py-12">
          <p className="text-gray-500">No appointments yet.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {appointments.map(appt => (
            <div key={appt.id} className="card space-y-3">
              <div className="flex items-start justify-between">
                <div>
                  <p className="font-semibold">Dr. {appt.doctorName}</p>
                  <p className="text-sm text-gray-500">{appt.doctorSpecialisation}</p>
                  <p className="text-sm text-gray-600 mt-1">
                    📅 {format(new Date(appt.appointmentDate), 'MMM dd, yyyy')} &nbsp;
                    🕐 {appt.slotStartTime} – {appt.slotEndTime}
                  </p>
                </div>
                <StatusBadge status={appt.status} />
              </div>

              <div className="flex gap-2 flex-wrap">
                {appt.status === 'COMPLETED' && (
                  <button className="btn-secondary text-xs" onClick={() => loadSummary(appt.id)}>
                    View Summary
                  </button>
                )}
                {['CONFIRMED', 'PENDING_SYMPTOMS'].includes(appt.status) && (
                  <button className="btn-danger text-xs" onClick={() => setCancelId(appt.id)}>
                    Cancel
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Cancel modal */}
      {cancelId && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="card w-full max-w-md space-y-4">
            <h2 className="font-semibold text-gray-900">Cancel Appointment</h2>
            <textarea className="input resize-none" rows={3} placeholder="Reason for cancellation (optional)"
              value={reason} onChange={e => setReason(e.target.value)} />
            <div className="flex gap-3">
              <button className="btn-secondary flex-1" onClick={() => setCancelId(null)}>Keep It</button>
              <button className="btn-danger flex-1" onClick={() => cancelMutation.mutate()}
                disabled={cancelMutation.isPending}>
                {cancelMutation.isPending ? 'Cancelling…' : 'Yes, Cancel'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Summary modal */}
      {summaryAppt && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="card w-full max-w-lg max-h-[80vh] overflow-y-auto space-y-4">
            <div className="flex items-center justify-between">
              <h2 className="font-semibold text-gray-900">Visit Summary</h2>
              <button onClick={() => { setSummaryAppt(null); setSummary(null); }} className="text-gray-400 hover:text-gray-600 text-xl">✕</button>
            </div>
            {summary ? (
              <>
                {summary.llmPatientSummary && (
                  <div className="bg-blue-50 rounded-lg p-4">
                    <p className="text-sm font-medium text-blue-800 mb-2">AI Patient Summary</p>
                    <p className="text-sm text-blue-700 whitespace-pre-wrap">{summary.llmPatientSummary}</p>
                  </div>
                )}
                {summary.diagnosis && (
                  <div><p className="text-sm font-medium text-gray-700">Diagnosis</p>
                  <p className="text-sm text-gray-600">{summary.diagnosis}</p></div>
                )}
                {summary.followUpDate && (
                  <p className="text-sm text-gray-600">📅 Follow-up: {summary.followUpDate}</p>
                )}
                {summary.prescriptions?.length > 0 && (
                  <div>
                    <p className="text-sm font-medium text-gray-700 mb-2">Prescriptions</p>
                    <div className="space-y-2">
                      {summary.prescriptions.map(p => (
                        <div key={p.id} className="bg-gray-50 rounded-lg p-3 text-sm">
                          <p className="font-medium">{p.medicationName}</p>
                          <p className="text-gray-500">{p.dosage} · {p.frequency}</p>
                          {p.instructions && <p className="text-gray-500 text-xs mt-1">{p.instructions}</p>}
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </>
            ) : (
              <p className="text-gray-500 text-sm">No summary available yet.</p>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
