import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api, ApiError } from "../../api/client";
import type { PriceItem } from "../../api/types";

export default function PriceListPage() {
  const [prices, setPrices] = useState<PriceItem[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api
      .get<PriceItem[]>("/v1/admin/prices")
      .then(setPrices)
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
        <h1>Prices</h1>
        <Link to="/admin/prices/new" className="button-link">
          + Add price item
        </Link>
      </div>
      {error && <p className="error-banner">{error}</p>}
      <div className="list">
        {prices.map((price) => (
          <Link key={price.itemId} to={`/admin/prices/${price.itemId}`} className="list-item">
            <div>
              <div>{price.itemId}</div>
              <div className="list-item__meta">{price.itemType}</div>
            </div>
            <span className="price">€{price.amount.toFixed(2)}</span>
          </Link>
        ))}
        {prices.length === 0 && !error && <p className="hint">No price items yet.</p>}
      </div>
    </div>
  );
}
