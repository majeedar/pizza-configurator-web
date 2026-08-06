import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api, ApiError } from "../../api/client";
import type { RuleThreshold } from "../../api/types";

export default function RuleListPage() {
  const [rules, setRules] = useState<RuleThreshold[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api
      .get<RuleThreshold[]>("/v1/admin/rules")
      .then(setRules)
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
      <h1>Rule thresholds</h1>
      <p className="hint">
        Changes propagate to every rule-service replica immediately via Kafka — no restart
        needed. This is a fixed set of admin-configurable thresholds; each one is tied to a
        specific validation rule in rule-service, so new thresholds aren't created here.
      </p>
      {error && <p className="error-banner">{error}</p>}
      <div className="list">
        {rules.map((rule) => (
          <Link key={rule.ruleId} to={`/admin/rules/${rule.ruleId}`} className="list-item">
            <div>
              <div>{rule.ruleId}</div>
              <div className="list-item__meta">Updated {new Date(rule.updatedAt).toLocaleString()}</div>
            </div>
            <span>{rule.value}</span>
          </Link>
        ))}
        {rules.length === 0 && !error && <p className="hint">No thresholds found.</p>}
      </div>
    </div>
  );
}
