import { Outlet } from 'react-router-dom';
import Navbar from '../../components/layout/Navbar';
import Sidebar from '../../components/layout/Sidebar';

const links = [
  { to: '/patient', label: 'Dashboard', icon: '🏠' },
  { to: '/patient/doctors', label: 'Find Doctors', icon: '🔍' },
  { to: '/patient/appointments', label: 'My Appointments', icon: '📅' },
  { to: '/patient/profile', label: 'My Profile', icon: '👤' },
];

export default function PatientLayout() {
  return (
    <div className="flex flex-col min-h-screen">
      <Navbar />
      <div className="flex flex-1">
        <Sidebar links={links} />
        <main className="flex-1 p-6 overflow-auto">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
