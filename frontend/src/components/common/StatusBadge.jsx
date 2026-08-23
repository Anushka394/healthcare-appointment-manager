const STATUS_STYLES = {
  CONFIRMED:              'bg-green-100 text-green-800',
  PENDING_SYMPTOMS:       'bg-yellow-100 text-yellow-800',
  COMPLETED:              'bg-blue-100 text-blue-800',
  CANCELLED:              'bg-red-100 text-red-800',
  CANCELLED_DOCTOR_LEAVE: 'bg-orange-100 text-orange-800',
  SLOT_HELD:              'bg-purple-100 text-purple-800',
};

export default function StatusBadge({ status }) {
  const cls = STATUS_STYLES[status] || 'bg-gray-100 text-gray-800';
  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${cls}`}>
      {status?.replace(/_/g, ' ')}
    </span>
  );
}
