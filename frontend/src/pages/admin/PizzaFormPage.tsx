import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { api, ApiError, BASE_URL } from "../../api/client";
import type { AllowedExtra, DefaultIngredient, IngredientType, IngredientUnit, Pizza } from "../../api/types";

export default function PizzaFormPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const isNew = id === undefined;

  const [loaded, setLoaded] = useState(isNew);
  const [notFound, setNotFound] = useState(false);

  const [pizzaId, setPizzaId] = useState("");
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [basePrice, setBasePrice] = useState("0.00");
  const [imageUrl, setImageUrl] = useState<string | null>(null);
  const [imageFile, setImageFile] = useState<File | null>(null);
  const [defaultIngredients, setDefaultIngredients] = useState<DefaultIngredient[]>([]);
  const [allowedExtras, setAllowedExtras] = useState<AllowedExtra[]>([]);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (isNew) return;
    api
      .get<Pizza[]>("/v1/admin/catalog/pizzas")
      .then((pizzas) => {
        const pizza = pizzas.find((p) => p.id === id);
        if (!pizza) {
          setNotFound(true);
          return;
        }
        setPizzaId(pizza.id);
        setName(pizza.name);
        setDescription(pizza.description);
        setBasePrice(pizza.basePrice.toString());
        setImageUrl(pizza.imageUrl);
        setDefaultIngredients(pizza.defaultIngredients);
        setAllowedExtras(pizza.allowedExtras);
      })
      .catch(() => setError("Could not load this pizza."))
      .finally(() => setLoaded(true));
  }, [id, isNew]);

  function updateIngredient(index: number, patch: Partial<DefaultIngredient>) {
    setDefaultIngredients((prev) => prev.map((ing, i) => (i === index ? { ...ing, ...patch } : ing)));
  }

  function updateExtra(index: number, patch: Partial<AllowedExtra>) {
    setAllowedExtras((prev) => prev.map((extra, i) => (i === index ? { ...extra, ...patch } : extra)));
  }

  async function handleSave() {
    setSaving(true);
    setError(null);
    try {
      const body = { id: pizzaId, name, description, basePrice: Number(basePrice), defaultIngredients, allowedExtras };
      const saved = isNew
        ? await api.post<Pizza>("/v1/admin/catalog/pizzas", body)
        : await api.put<Pizza>(`/v1/admin/catalog/pizzas/${pizzaId}`, body);

      if (imageFile) {
        const formData = new FormData();
        formData.append("file", imageFile);
        await api.upload<Pizza>(`/v1/admin/catalog/pizzas/${saved.id}/image`, formData);
      }
      navigate("/admin/pizzas");
    } catch (err) {
      setError(err instanceof ApiError && err.status === 409 ? "A pizza with this id already exists." : "Could not save this pizza.");
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete() {
    if (!window.confirm(`Delete "${name}"? This cannot be undone.`)) return;
    setSaving(true);
    setError(null);
    try {
      await api.delete(`/v1/admin/catalog/pizzas/${pizzaId}`);
      navigate("/admin/pizzas");
    } catch {
      setError("Could not delete this pizza.");
    } finally {
      setSaving(false);
    }
  }

  if (!isNew && !loaded) {
    return (
      <div className="page">
        <Link to="/admin/pizzas" className="back-link">
          ← Pizzas
        </Link>
        <p className="hint">Loading…</p>
      </div>
    );
  }

  if (notFound) {
    return (
      <div className="page">
        <Link to="/admin/pizzas" className="back-link">
          ← Pizzas
        </Link>
        <p className="error-banner">Pizza not found.</p>
      </div>
    );
  }

  return (
    <div className="page">
      <Link to="/admin/pizzas" className="back-link">
        ← Pizzas
      </Link>
      <h1>{isNew ? "Add new pizza" : name}</h1>

      <div className="card">
        <div className="row">
          <label>
            Id
            <input value={pizzaId} onChange={(e) => setPizzaId(e.target.value)} disabled={!isNew} placeholder="tonno" />
          </label>
          <label>
            Name
            <input value={name} onChange={(e) => setName(e.target.value)} placeholder="Tonno" />
          </label>
          <label>
            Base price (€)
            <input value={basePrice} onChange={(e) => setBasePrice(e.target.value)} className="cell-input" />
          </label>
        </div>
        <label>
          Description
          <input value={description} onChange={(e) => setDescription(e.target.value)} placeholder="Tomato, mozzarella, tuna, red onion" />
        </label>

        <label>
          Image
          <input type="file" accept="image/*" onChange={(e) => setImageFile(e.target.files?.[0] ?? null)} />
        </label>
        {imageUrl && <img src={`${BASE_URL}${imageUrl}`} alt={name} style={{ maxWidth: 120, display: "block", marginBottom: 8 }} />}

        <h3>Default ingredients</h3>
        {defaultIngredients.map((ing, index) => (
          <div className="row" key={index}>
            <input
              value={ing.ingredientId}
              onChange={(e) => updateIngredient(index, { ingredientId: e.target.value })}
              placeholder="ingredient id"
            />
            <input value={ing.name} onChange={(e) => updateIngredient(index, { name: e.target.value })} placeholder="name" />
            <label className="checkbox-row">
              <input type="checkbox" checked={ing.removable} onChange={(e) => updateIngredient(index, { removable: e.target.checked })} />
              Removable
            </label>
            <button onClick={() => setDefaultIngredients((prev) => prev.filter((_, i) => i !== index))}>Remove</button>
          </div>
        ))}
        <button onClick={() => setDefaultIngredients((prev) => [...prev, { ingredientId: "", name: "", removable: true }])}>
          + Add default ingredient
        </button>

        <h3>Allowed extras</h3>
        {allowedExtras.map((extra, index) => (
          <div className="row" key={index}>
            <input
              value={extra.ingredientId}
              onChange={(e) => updateExtra(index, { ingredientId: e.target.value })}
              placeholder="ingredient id"
            />
            <input value={extra.name} onChange={(e) => updateExtra(index, { name: e.target.value })} placeholder="name" />
            <select value={extra.type} onChange={(e) => updateExtra(index, { type: e.target.value as IngredientType })}>
              <option value="CHEESE">CHEESE</option>
              <option value="TOPPING">TOPPING</option>
            </select>
            <select value={extra.unit} onChange={(e) => updateExtra(index, { unit: e.target.value as IngredientUnit })}>
              <option value="PORTION">PORTION</option>
              <option value="PIECE">PIECE</option>
            </select>
            <button onClick={() => setAllowedExtras((prev) => prev.filter((_, i) => i !== index))}>Remove</button>
          </div>
        ))}
        <button onClick={() => setAllowedExtras((prev) => [...prev, { ingredientId: "", name: "", type: "TOPPING", unit: "PORTION" }])}>
          + Add allowed extra
        </button>

        {error && <p className="error-banner">{error}</p>}
        <div style={{ marginTop: 12 }}>
          <button disabled={saving || !pizzaId || !name} onClick={handleSave}>
            {saving ? "Saving…" : isNew ? "Create pizza" : "Save changes"}
          </button>{" "}
          {!isNew && (
            <button disabled={saving} onClick={handleDelete}>
              Delete pizza
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
