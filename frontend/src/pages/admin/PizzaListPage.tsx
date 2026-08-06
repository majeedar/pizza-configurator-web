import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api, ApiError, BASE_URL } from "../../api/client";
import type { Pizza } from "../../api/types";

export default function PizzaListPage() {
  const [pizzas, setPizzas] = useState<Pizza[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api
      .get<Pizza[]>("/v1/admin/catalog/pizzas")
      .then(setPizzas)
      .catch((err) => {
        setError(
          err instanceof ApiError && (err.status === 401 || err.status === 403)
            ? "Not authorized — sign in with the admin account."
            : "Could not reach catalog-service.",
        );
      });
  }, []);

  return (
    <div className="page">
      <Link to="/admin" className="back-link">
        ← Admin
      </Link>
      <div className="section-header">
        <h1>Pizzas</h1>
        <Link to="/admin/pizzas/new" className="button-link">
          + Add new pizza
        </Link>
      </div>
      {error && <p className="error-banner">{error}</p>}
      <div className="list">
        {pizzas.map((pizza) => (
          <Link key={pizza.id} to={`/admin/pizzas/${pizza.id}`} className="list-item">
            <div className="list-item__main">
              {pizza.imageUrl && <img src={`${BASE_URL}${pizza.imageUrl}`} alt="" className="list-item__thumb" />}
              <div>
                <div>{pizza.name}</div>
                <div className="list-item__meta">{pizza.description}</div>
              </div>
            </div>
            <span className="price">€{pizza.basePrice.toFixed(2)}</span>
          </Link>
        ))}
        {pizzas.length === 0 && !error && <p className="hint">No pizzas yet.</p>}
      </div>
    </div>
  );
}
