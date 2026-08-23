import { Outlet } from 'react-router-dom';
import Navbar from '../../components/layout/Navbar';
import Sidebar from '../../components/layout/Sidebar';

const links = [
  { to: '/admin', label: 'Dashboard', icon: '🏠' },
  { to: '/admin/doctors', label: 'Doctors', icon: '👨‍⚕️' },
  { to: '/admin/users', label: 'Users', icon: '👥' },
];

export default function AdminLayout() {
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
