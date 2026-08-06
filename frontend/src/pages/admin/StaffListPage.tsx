import { useEffect, useState } from "react";
import { Link, useLocation } from "react-router-dom";
import { api, ApiError } from "../../api/client";
import type { StaffAccount } from "../../api/types";

interface LocationState {
  newlyCreatedStaff?: { email: string; temporaryPassword: string };
}

export default function StaffListPage() {
  const location = useLocation();
  const [staff, setStaff] = useState<StaffAccount[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [newlyCreatedStaff, setNewlyCreatedStaff] = useState(
    (location.state as LocationState | null)?.newlyCreatedStaff ?? null,
  );

  useEffect(() => {
    api
      .get<StaffAccount[]>("/v1/admin/staff")
      .then(setStaff)
      .catch((err) => {
        setError(
          err instanceof ApiError && (err.status === 401 || err.status === 403)
            ? "Not authorized — sign in with the admin account."
            : "Could not reach admin-service.",
        );
      });
  }, []);

  return (
    <div className="page">
      <Link to="/admin" className="back-link">
        ← Admin
      </Link>
      <div className="section-header">
        <h1>Staff &amp; admin accounts</h1>
        <Link to="/admin/staff/new" className="button-link">
          + Add staff member
        </Link>
      </div>
      <p className="hint">Accounts are created here — there's no public signup for staff/admin.</p>
      {newlyCreatedStaff && (
        <div className="error-banner" style={{ background: "var(--success-bg)", color: "var(--success)" }}>
          Account created for <strong>{newlyCreatedStaff.email}</strong>. One-time temporary password:{" "}
          <code>{newlyCreatedStaff.temporaryPassword}</code> — this is shown only once, share it securely.{" "}
          <button onClick={() => setNewlyCreatedStaff(null)}>Dismiss</button>
        </div>
      )}
      {error && <p className="error-banner">{error}</p>}
      <div className="list">
        {staff.map((account) => (
          <Link key={account.id} to={`/admin/staff/${account.id}`} className="list-item">
            <div>
              <div>{account.email}</div>
              <div className="list-item__meta">
                {account.fullName} · {account.role}
              </div>
            </div>
            <span className="hint">{account.mustChangePassword ? "Awaiting first login" : "Active"}</span>
          </Link>
        ))}
        {staff.length === 0 && !error && <p className="hint">No staff accounts yet.</p>}
      </div>
    </div>
  );
}
