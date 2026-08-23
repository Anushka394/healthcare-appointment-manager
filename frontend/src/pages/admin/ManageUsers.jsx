import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { adminGetUsers, adminActivateUser, adminDeactivateUser } from '../../api/patients';
import LoadingSpinner from '../../components/common/LoadingSpinner';

const ROLE_COLORS = { ADMIN: 'bg-purple-100 text-purple-700', DOCTOR: 'bg-blue-100 text-blue-700', PATIENT: 'bg-green-100 text-green-700' };

export default function ManageUsers() {
  const qc = useQueryClient();
  const { data: users = [], isLoading } = useQuery({ queryKey: ['adminUsers'], queryFn: adminGetUsers });

  const activate = useMutation({ mutationFn: adminActivateUser, onSuccess: () => qc.invalidateQueries(['adminUsers']) });
  const deactivate = useMutation({ mutationFn: adminDeactivateUser, onSuccess: () => qc.invalidateQueries(['adminUsers']) });

  if (isLoading) return <LoadingSpinner />;

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-gray-900">Manage Users</h1>

      <div className="card overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="text-left border-b border-gray-200">
              <th className="pb-3 font-medium text-gray-500">Name</th>
              <th className="pb-3 font-medium text-gray-500">Email</th>
              <th className="pb-3 font-medium text-gray-500">Role</th>
              <th className="pb-3 font-medium text-gray-500">Status</th>
              <th className="pb-3 font-medium text-gray-500">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {users.map(u => (
              <tr key={u.id} className="hover:bg-gray-50">
                <td className="py-3 font-medium">{u.name}</td>
                <td className="py-3 text-gray-500">{u.email}</td>
                <td className="py-3">
                  <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${ROLE_COLORS[u.role]}`}>{u.role}</span>
                </td>
                <td className="py-3">
                  <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${u.isActive ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'}`}>
                    {u.isActive ? 'Active' : 'Inactive'}
                  </span>
                </td>
                <td className="py-3">
                  {u.role !== 'ADMIN' && (
                    u.isActive ? (
                      <button className="text-xs text-red-600 hover:underline" onClick={() => deactivate.mutate(u.id)}>Deactivate</button>
                    ) : (
                      <button className="text-xs text-green-600 hover:underline" onClick={() => activate.mutate(u.id)}>Activate</button>
                    )
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
