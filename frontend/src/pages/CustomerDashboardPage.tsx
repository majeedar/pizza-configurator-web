import { useEffect, useState } from "react";
import { api } from "../api/client";
import type { OrderResponse } from "../api/types";

export default function CustomerDashboardPage() {
  const [orders, setOrders] = useState<OrderResponse[]>([]);
  const [error, setError] = useState<string | null>(null);

  function loadOrders() {
    api
      .get<OrderResponse[]>("/v1/customer/orders/mine")
      .then(setOrders)
      .catch(() => setError("Could not load your orders — is order-service reachable?"));
  }

  useEffect(() => {
    loadOrders();
    const interval = setInterval(loadOrders, 5000);
    return () => clearInterval(interval);
  }, []);

  return (
    <div className="page">
      <h1>My orders</h1>
      {error && <p className="error-banner">{error}</p>}
      {orders.length === 0 && !error && <p className="hint">You haven't placed any orders yet.</p>}
      <div className="board">
        {orders.map((order) => (
          <div key={order.orderId} className={`ticket ticket--${order.status.toLowerCase()}`}>
            <div className="ticket__header">
              <strong>{order.displayNumber}</strong>
              <span>{order.status.replaceAll("_", " ")}</span>
            </div>
            <p>Total: €{order.totalPrice.toFixed(2)}</p>
            <p className="hint">Placed {new Date(order.createdAt).toLocaleString()}</p>
            {order.customNotes && <p className="ticket__notes">📝 {order.customNotes}</p>}
          </div>
        ))}
      </div>
    </div>
  );
}
