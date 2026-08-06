import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { api } from "../../api/client";
import type { RuleThreshold } from "../../api/types";

export default function RuleFormPage() {
  const { ruleId } = useParams<{ ruleId: string }>();
  const navigate = useNavigate();

  const [loaded, setLoaded] = useState(false);
  const [notFound, setNotFound] = useState(false);
  const [value, setValue] = useState("");
  const [updatedAt, setUpdatedAt] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api
      .get<RuleThreshold[]>("/v1/admin/rules")
      .then((rules) => {
        const rule = rules.find((r) => r.ruleId === ruleId);
        if (!rule) {
          setNotFound(true);
          return;
        }
        setValue(rule.value);
        setUpdatedAt(rule.updatedAt);
      })
      .catch(() => setError("Could not load this threshold."))
      .finally(() => setLoaded(true));
  }, [ruleId]);

  async function handleSave() {
    setSaving(true);
    setError(null);
    try {
      await api.put(`/v1/admin/rules/${ruleId}`, { value });
      navigate("/admin/rules");
    } catch {
      setError(`Could not update ${ruleId}.`);
    } finally {
      setSaving(false);
    }
  }

  if (!loaded) {
    return (
      <div className="page page--narrow">
        <Link to="/admin/rules" className="back-link">
          ← Rule thresholds
        </Link>
        <p className="hint">Loading…</p>
      </div>
    );
  }

  if (notFound) {
    return (
      <div className="page page--narrow">
        <Link to="/admin/rules" className="back-link">
          ← Rule thresholds
        </Link>
        <p className="error-banner">Threshold not found.</p>
      </div>
    );
  }

  return (
    <div className="page page--narrow">
      <Link to="/admin/rules" className="back-link">
        ← Rule thresholds
      </Link>
      <h1>{ruleId}</h1>
      <div className="card">
        <label>
          Value
          <input value={value} onChange={(e) => setValue(e.target.value)} />
        </label>
        {updatedAt && <p className="hint">Last updated {new Date(updatedAt).toLocaleString()}</p>}
        {error && <p className="error-banner">{error}</p>}
        <div style={{ marginTop: 12 }}>
          <button disabled={saving} onClick={handleSave}>
            {saving ? "Saving…" : "Save changes"}
          </button>
        </div>
      </div>
    </div>
  );
}
