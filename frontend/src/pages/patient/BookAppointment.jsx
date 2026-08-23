import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { getPublicDoctor, getAvailableSlots } from '../../api/doctors';
import { holdSlot, confirmBooking, submitSymptomForm } from '../../api/appointments';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import ErrorAlert from '../../components/common/ErrorAlert';
import { format, addDays } from 'date-fns';

const STEPS = ['Select Slot', 'Symptoms', 'Confirm'];

export default function BookAppointment() {
  const { doctorId } = useParams();
  const navigate = useNavigate();

  const [step, setStep] = useState(0);
  const [selectedDate, setSelectedDate] = useState(format(addDays(new Date(), 1), 'yyyy-MM-dd'));
  const [selectedSlot, setSelectedSlot] = useState(null);
  const [holdId, setHoldId] = useState(null);
  const [appointmentId, setAppointmentId] = useState(null);
  const [symptoms, setSymptoms] = useState({ symptoms: '', symptomDuration: '', severitySelfReported: '', currentMedications: '' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const { data: doctor } = useQuery({
    queryKey: ['doctor', doctorId],
    queryFn: () => getPublicDoctor(doctorId),
  });

  const { data: slotsData, isLoading: slotsLoading } = useQuery({
    queryKey: ['slots', doctorId, selectedDate],
    queryFn: () => getAvailableSlots(doctorId, selectedDate),
    enabled: !!selectedDate,
  });

  // Step 0 → hold slot
  const handleHold = async () => {
    if (!selectedSlot) return;
    setError(''); setLoading(true);
    try {
      const id = await holdSlot({ doctorId: Number(doctorId), appointmentDate: selectedDate, slotStartTime: selectedSlot });
      setHoldId(id);
      setStep(1);
    } catch (e) {
      setError(e.response?.data?.message || 'Could not hold slot. Please try again.');
    } finally { setLoading(false); }
  };

  // Step 1 → confirm booking + submit symptoms
  const handleConfirm = async () => {
    if (!symptoms.symptoms.trim()) { setError('Please describe your symptoms.'); return; }
    setError(''); setLoading(true);
    try {
      const appt = await confirmBooking(holdId);
      setAppointmentId(appt.id);
      await submitSymptomForm(appt.id, symptoms);
      setStep(2);
    } catch (e) {
      setError(e.response?.data?.message || 'Booking failed. Please try again.');
    } finally { setLoading(false); }
  };

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      <h1 className="text-2xl font-bold text-gray-900">Book Appointment</h1>

      {/* Doctor info */}
      {doctor && (
        <div className="card flex items-center gap-4">
          <div className="w-14 h-14 bg-primary-100 rounded-full flex items-center justify-center text-3xl">👨‍⚕️</div>
          <div>
            <h2 className="font-semibold text-gray-900">Dr. {doctor.name}</h2>
            <p className="text-sm text-primary-600">{doctor.specialisation}</p>
            {doctor.consultationFee && <p className="text-xs text-gray-500">Fee: ₹{doctor.consultationFee}</p>}
          </div>
        </div>
      )}

      {/* Step indicator */}
      <div className="flex items-center gap-2">
        {STEPS.map((s, i) => (
          <div key={s} className="flex items-center gap-2">
            <div className={`w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold ${
              i <= step ? 'bg-primary-600 text-white' : 'bg-gray-200 text-gray-500'
            }`}>{i + 1}</div>
            <span className={`text-sm ${i === step ? 'font-semibold text-primary-700' : 'text-gray-400'}`}>{s}</span>
            {i < STEPS.length - 1 && <div className="w-8 h-px bg-gray-300" />}
          </div>
        ))}
      </div>

      <ErrorAlert message={error} />

      {/* Step 0: Slot selection */}
      {step === 0 && (
        <div className="card space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Select Date</label>
            <input
              type="date" className="input max-w-xs"
              min={format(addDays(new Date(), 1), 'yyyy-MM-dd')}
              value={selectedDate}
              onChange={e => { setSelectedDate(e.target.value); setSelectedSlot(null); }}
            />
          </div>

          {slotsLoading ? <LoadingSpinner /> : (
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Available Slots</label>
              <div className="grid grid-cols-3 sm:grid-cols-4 gap-2">
                {slotsData?.availableSlots?.filter(s => s.available).map(slot => (
                  <button
                    key={slot.startTime}
                    onClick={() => setSelectedSlot(slot.startTime)}
                    className={`py-2 px-3 rounded-lg text-sm border transition-colors ${
                      selectedSlot === slot.startTime
                        ? 'bg-primary-600 text-white border-primary-600'
                        : 'bg-white text-gray-700 border-gray-300 hover:border-primary-400'
                    }`}
                  >
                    {slot.startTime}
                  </button>
                ))}
                {slotsData?.availableSlots?.filter(s => s.available).length === 0 && (
                  <p className="text-gray-500 text-sm col-span-4">No slots available on this date.</p>
                )}
              </div>
            </div>
          )}

          <button className="btn-primary" onClick={handleHold} disabled={!selectedSlot || loading}>
            {loading ? 'Holding slot…' : 'Hold Slot & Continue'}
          </button>
        </div>
      )}

      {/* Step 1: Symptom form */}
      {step === 1 && (
        <div className="card space-y-4">
          <h2 className="font-semibold text-gray-900">Tell the doctor about your symptoms</h2>
          <p className="text-sm text-gray-500">This helps the doctor prepare before your visit. An AI summary will be generated.</p>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Symptoms *</label>
            <textarea
              className="input min-h-24 resize-y"
              placeholder="Describe what you're experiencing..."
              value={symptoms.symptoms}
              onChange={e => setSymptoms(s => ({ ...s, symptoms: e.target.value }))}
            />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Duration</label>
              <input className="input" placeholder="e.g. 3 days" value={symptoms.symptomDuration}
                onChange={e => setSymptoms(s => ({ ...s, symptomDuration: e.target.value }))} />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Severity (self-assessed)</label>
              <select className="input" value={symptoms.severitySelfReported}
                onChange={e => setSymptoms(s => ({ ...s, severitySelfReported: e.target.value }))}>
                <option value="">Select...</option>
                <option>Mild</option><option>Moderate</option><option>Severe</option>
              </select>
            </div>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Current Medications</label>
            <input className="input" placeholder="List any medications you're currently taking"
              value={symptoms.currentMedications}
              onChange={e => setSymptoms(s => ({ ...s, currentMedications: e.target.value }))} />
          </div>

          <div className="flex gap-3">
            <button className="btn-secondary" onClick={() => setStep(0)}>Back</button>
            <button className="btn-primary" onClick={handleConfirm} disabled={loading}>
              {loading ? 'Confirming…' : 'Confirm Booking'}
            </button>
          </div>
        </div>
      )}

      {/* Step 2: Success */}
      {step === 2 && (
        <div className="card text-center space-y-4">
          <div className="text-5xl">✅</div>
          <h2 className="text-xl font-bold text-gray-900">Appointment Booked!</h2>
          <p className="text-gray-500 text-sm">
            You'll receive a confirmation email shortly. The doctor has been notified.
          </p>
          <p className="text-sm text-gray-600">
            <span className="font-medium">Date:</span> {selectedDate} &nbsp;·&nbsp;
            <span className="font-medium">Time:</span> {selectedSlot}
          </p>
          <div className="flex gap-3 justify-center">
            <button className="btn-secondary" onClick={() => navigate('/patient/appointments')}>
              View Appointments
            </button>
            <button className="btn-primary" onClick={() => navigate('/patient/doctors')}>
              Book Another
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
