import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { api, ApiError } from "../../api/client";
import type { PriceItem } from "../../api/types";

export default function PriceFormPage() {
  const { itemId: paramItemId } = useParams<{ itemId: string }>();
  const navigate = useNavigate();
  const isNew = paramItemId === undefined;

  const [loaded, setLoaded] = useState(isNew);
  const [notFound, setNotFound] = useState(false);
  const [itemId, setItemId] = useState("");
  const [itemType, setItemType] = useState("INGREDIENT");
  const [amount, setAmount] = useState("0.00");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (isNew) return;
    api
      .get<PriceItem[]>("/v1/admin/prices")
      .then((prices) => {
        const price = prices.find((p) => p.itemId === paramItemId);
        if (!price) {
          setNotFound(true);
          return;
        }
        setItemId(price.itemId);
        setItemType(price.itemType);
        setAmount(price.amount.toString());
      })
      .catch(() => setError("Could not load this price item."))
      .finally(() => setLoaded(true));
  }, [paramItemId, isNew]);

  async function handleSave() {
    setSaving(true);
    setError(null);
    try {
      if (isNew) {
        await api.post("/v1/admin/prices", { itemId, itemType, amount: Number(amount) });
      } else {
        await api.put(`/v1/admin/prices/${itemId}`, { amount: Number(amount) });
      }
      navigate("/admin/prices");
    } catch (err) {
      setError(err instanceof ApiError && err.status === 409 ? "That item id already exists." : "Could not save this price item.");
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete() {
    if (!window.confirm(`Delete "${itemId}"? This cannot be undone.`)) return;
    setSaving(true);
    setError(null);
    try {
      await api.delete(`/v1/admin/prices/${itemId}`);
      navigate("/admin/prices");
    } catch {
      setError("Could not delete this price item.");
    } finally {
      setSaving(false);
    }
  }

  if (!isNew && !loaded) {
    return (
      <div className="page page--narrow">
        <Link to="/admin/prices" className="back-link">
          ← Prices
        </Link>
        <p className="hint">Loading…</p>
      </div>
    );
  }

  if (notFound) {
    return (
      <div className="page page--narrow">
        <Link to="/admin/prices" className="back-link">
          ← Prices
        </Link>
        <p className="error-banner">Price item not found.</p>
      </div>
    );
  }

  return (
    <div className="page page--narrow">
      <Link to="/admin/prices" className="back-link">
        ← Prices
      </Link>
      <h1>{isNew ? "Add price item" : itemId}</h1>
      <div className="card">
        <label>
          Item id
          <input value={itemId} onChange={(e) => setItemId(e.target.value)} disabled={!isNew} placeholder="topping-tuna" />
        </label>
        <label>
          Type
          <select value={itemType} onChange={(e) => setItemType(e.target.value)} disabled={!isNew}>
            <option value="SIZE">SIZE</option>
            <option value="DOUGH">DOUGH</option>
            <option value="INGREDIENT">INGREDIENT</option>
          </select>
        </label>
        <label>
          Amount (€)
          <input value={amount} onChange={(e) => setAmount(e.target.value)} className="cell-input" />
        </label>
        {error && <p className="error-banner">{error}</p>}
        <div style={{ marginTop: 12 }}>
          <button disabled={saving || !itemId} onClick={handleSave}>
            {saving ? "Saving…" : isNew ? "Create" : "Save changes"}
          </button>{" "}
          {!isNew && (
            <button disabled={saving} onClick={handleDelete}>
              Delete
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
