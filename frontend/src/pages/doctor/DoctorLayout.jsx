import { Outlet } from 'react-router-dom';
import Navbar from '../../components/layout/Navbar';
import Sidebar from '../../components/layout/Sidebar';

const links = [
  { to: '/doctor', label: 'Dashboard', icon: '🏠' },
  { to: '/doctor/appointments', label: 'Appointments', icon: '📅' },
  { to: '/doctor/leaves', label: 'My Leaves', icon: '🏖️' },
  { to: '/doctor/profile', label: 'My Profile', icon: '👤' },
];

export default function DoctorLayout() {
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
