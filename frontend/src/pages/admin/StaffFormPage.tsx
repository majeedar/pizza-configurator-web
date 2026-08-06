import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { api, ApiError } from "../../api/client";
import type { StaffAccount, StaffAccountCreateResult, StaffRole } from "../../api/types";

export default function StaffFormPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const isNew = id === undefined;

  const [loaded, setLoaded] = useState(isNew);
  const [notFound, setNotFound] = useState(false);
  const [email, setEmail] = useState("");
  const [fullName, setFullName] = useState("");
  const [role, setRole] = useState<StaffRole>("STAFF");
  const [status, setStatus] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (isNew) return;
    api
      .get<StaffAccount[]>("/v1/admin/staff")
      .then((accounts) => {
        const account = accounts.find((a) => a.id === id);
        if (!account) {
          setNotFound(true);
          return;
        }
        setEmail(account.email);
        setFullName(account.fullName);
        setRole(account.role);
        setStatus(account.mustChangePassword ? "Awaiting first login" : "Active");
      })
      .catch(() => setError("Could not load this account."))
      .finally(() => setLoaded(true));
  }, [id, isNew]);

  async function handleSave() {
    setSaving(true);
    setError(null);
    try {
      if (isNew) {
        const result = await api.post<StaffAccountCreateResult>("/v1/admin/staff", { email, fullName, role });
        navigate("/admin/staff", {
          state: { newlyCreatedStaff: { email: result.account.email, temporaryPassword: result.temporaryPassword } },
        });
      } else {
        await api.put(`/v1/admin/staff/${id}`, { fullName, role });
        navigate("/admin/staff");
      }
    } catch (err) {
      const conflict = err instanceof ApiError && err.status === 409;
      setError(
        isNew
          ? conflict
            ? "An account with this email already exists."
            : "Could not create the account."
          : conflict
            ? "Cannot remove the last remaining admin account."
            : "Could not update this account.",
      );
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete() {
    if (!window.confirm(`Remove ${email}? This cannot be undone.`)) return;
    setSaving(true);
    setError(null);
    try {
      await api.delete(`/v1/admin/staff/${id}`);
      navigate("/admin/staff");
    } catch (err) {
      setError(err instanceof ApiError && err.status === 409 ? "Cannot remove the last remaining admin account." : "Could not remove this account.");
    } finally {
      setSaving(false);
    }
  }

  if (!isNew && !loaded) {
    return (
      <div className="page page--narrow">
        <Link to="/admin/staff" className="back-link">
          ← Staff &amp; admin accounts
        </Link>
        <p className="hint">Loading…</p>
      </div>
    );
  }

  if (notFound) {
    return (
      <div className="page page--narrow">
        <Link to="/admin/staff" className="back-link">
          ← Staff &amp; admin accounts
        </Link>
        <p className="error-banner">Account not found.</p>
      </div>
    );
  }

  return (
    <div className="page page--narrow">
      <Link to="/admin/staff" className="back-link">
        ← Staff &amp; admin accounts
      </Link>
      <h1>{isNew ? "Add staff member" : fullName}</h1>
      <div className="card">
        <label>
          Email
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} disabled={!isNew} placeholder="new@pizzashop.com" />
        </label>
        <label>
          Full name
          <input value={fullName} onChange={(e) => setFullName(e.target.value)} />
        </label>
        <label>
          Role
          <select value={role} onChange={(e) => setRole(e.target.value as StaffRole)}>
            <option value="STAFF">STAFF</option>
            <option value="ADMIN">ADMIN</option>
          </select>
        </label>
        {status && <p className="hint">Status: {status}</p>}
        {error && <p className="error-banner">{error}</p>}
        <div style={{ marginTop: 12 }}>
          <button disabled={saving || !email || !fullName} onClick={handleSave}>
            {saving ? "Saving…" : isNew ? "Create" : "Save changes"}
          </button>{" "}
          {!isNew && (
            <button disabled={saving} onClick={handleDelete}>
              Remove
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
