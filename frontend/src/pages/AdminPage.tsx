import { useEffect, useState } from "react";
import { api, ApiError, BASE_URL } from "../api/client";
import type {
  AllowedExtra,
  DefaultIngredient,
  IngredientType,
  IngredientUnit,
  Pizza,
  PriceItem,
  RuleThreshold,
  StaffAccount,
  StaffRole,
} from "../api/types";

export default function AdminPage() {
  const [prices, setPrices] = useState<PriceItem[]>([]);
  const [rules, setRules] = useState<RuleThreshold[]>([]);
  const [pizzas, setPizzas] = useState<Pizza[]>([]);
  const [staff, setStaff] = useState<StaffAccount[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [savingId, setSavingId] = useState<string | null>(null);
  const [creatingPizza, setCreatingPizza] = useState(false);
  const [creatingPrice, setCreatingPrice] = useState(false);
  const [creatingStaff, setCreatingStaff] = useState(false);
  const [newlyCreatedStaff, setNewlyCreatedStaff] = useState<{ email: string; temporaryPassword: string } | null>(null);

  function load() {
    setError(null);
    Promise.all([
      api.get<PriceItem[]>("/v1/admin/prices"),
      api.get<RuleThreshold[]>("/v1/admin/rules"),
      api.get<Pizza[]>("/v1/admin/catalog/pizzas"),
      api.get<StaffAccount[]>("/v1/admin/staff"),
    ])
      .then(([priceList, ruleList, pizzaList, staffList]) => {
        setPrices(priceList);
        setRules(ruleList);
        setPizzas(pizzaList);
        setStaff(staffList);
      })
      .catch((err) => {
        if (err instanceof ApiError && (err.status === 401 || err.status === 403)) {
          setError("Not authorized — sign in with the admin account.");
        } else {
          setError("Could not reach admin-service.");
        }
      });
  }

  useEffect(load, []);

  async function savePrice(itemId: string, amount: number) {
    setSavingId(itemId);
    try {
      await api.put(`/v1/admin/prices/${itemId}`, { amount });
      load();
    } catch {
      setError(`Could not update ${itemId}.`);
    } finally {
      setSavingId(null);
    }
  }

  async function deletePrice(itemId: string) {
    setSavingId(itemId);
    try {
      await api.delete(`/v1/admin/prices/${itemId}`);
      load();
    } catch {
      setError(`Could not delete ${itemId}.`);
    } finally {
      setSavingId(null);
    }
  }

  async function saveRule(ruleId: string, value: string) {
    setSavingId(ruleId);
    try {
      await api.put(`/v1/admin/rules/${ruleId}`, { value });
      load();
    } catch {
      setError(`Could not update ${ruleId}.`);
    } finally {
      setSavingId(null);
    }
  }

  async function saveStaff(id: string, fullName: string, role: StaffRole) {
    setSavingId(id);
    try {
      await api.put(`/v1/admin/staff/${id}`, { fullName, role });
      load();
    } catch (err) {
      setError(err instanceof ApiError && err.status === 409 ? "Cannot remove the last remaining admin account." : `Could not update this account.`);
    } finally {
      setSavingId(null);
    }
  }

  async function deleteStaff(id: string) {
    setSavingId(id);
    try {
      await api.delete(`/v1/admin/staff/${id}`);
      load();
    } catch (err) {
      setError(err instanceof ApiError && err.status === 409 ? "Cannot remove the last remaining admin account." : "Could not remove this account.");
    } finally {
      setSavingId(null);
    }
  }

  return (
    <div className="page">
      <h1>Admin</h1>
      {error && <p className="error-banner">{error}</p>}

      <div className="card">
        <h2>Pizzas</h2>
        {pizzas.map((pizza) => (
          <PizzaEditor key={pizza.id} pizza={pizza} onSaved={load} />
        ))}
        {creatingPizza ? (
          <PizzaEditor
            pizza={null}
            onSaved={() => {
              setCreatingPizza(false);
              load();
            }}
            onCancel={() => setCreatingPizza(false)}
          />
        ) : (
          <button onClick={() => setCreatingPizza(true)}>+ Add new pizza</button>
        )}
      </div>

      <div className="card">
        <h2>Prices</h2>
        <table>
          <thead>
            <tr>
              <th>Item</th>
              <th>Type</th>
              <th>Amount (€)</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {prices.map((price) => (
              <EditablePriceRow
                key={price.itemId}
                price={price}
                saving={savingId === price.itemId}
                onSave={savePrice}
                onDelete={deletePrice}
              />
            ))}
          </tbody>
        </table>
        {creatingPrice ? (
          <NewPriceForm
            onCreated={() => {
              setCreatingPrice(false);
              load();
            }}
            onCancel={() => setCreatingPrice(false)}
          />
        ) : (
          <button onClick={() => setCreatingPrice(true)}>+ Add price item</button>
        )}
      </div>

      <div className="card">
        <h2>Staff &amp; admin accounts</h2>
        <p className="hint">Accounts are created here — there's no public signup for staff/admin.</p>
        {newlyCreatedStaff && (
          <div className="error-banner" style={{ background: "var(--success-bg)", color: "var(--success)" }}>
            Account created for <strong>{newlyCreatedStaff.email}</strong>. One-time temporary password:{" "}
            <code>{newlyCreatedStaff.temporaryPassword}</code> — this is shown only once, share it securely.{" "}
            <button onClick={() => setNewlyCreatedStaff(null)}>Dismiss</button>
          </div>
        )}
        <table>
          <thead>
            <tr>
              <th>Email</th>
              <th>Name</th>
              <th>Role</th>
              <th>Status</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {staff.map((account) => (
              <EditableStaffRow
                key={account.id}
                account={account}
                saving={savingId === account.id}
                onSave={saveStaff}
                onDelete={deleteStaff}
              />
            ))}
          </tbody>
        </table>
        {creatingStaff ? (
          <NewStaffForm
            onCreated={(result) => {
              setCreatingStaff(false);
              setNewlyCreatedStaff({ email: result.account.email, temporaryPassword: result.temporaryPassword });
              load();
            }}
            onCancel={() => setCreatingStaff(false)}
          />
        ) : (
          <button onClick={() => setCreatingStaff(true)}>+ Add staff member</button>
        )}
      </div>

      <div className="card">
        <h2>Rule thresholds</h2>
        <p className="hint">
          Changes here propagate to every rule-service replica immediately via Kafka — no restart needed.
        </p>
        <table>
          <thead>
            <tr>
              <th>Rule</th>
              <th>Value</th>
              <th>Last updated</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {rules.map((rule) => (
              <EditableRuleRow key={rule.ruleId} rule={rule} saving={savingId === rule.ruleId} onSave={saveRule} />
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function EditablePriceRow({
  price,
  saving,
  onSave,
  onDelete,
}: {
  price: PriceItem;
  saving: boolean;
  onSave: (itemId: string, amount: number) => void;
  onDelete: (itemId: string) => void;
}) {
  const [value, setValue] = useState(price.amount.toString());

  return (
    <tr>
      <td>{price.itemId}</td>
      <td>{price.itemType}</td>
      <td>
        <input value={value} onChange={(e) => setValue(e.target.value)} className="cell-input" />
      </td>
      <td>
        <button disabled={saving} onClick={() => onSave(price.itemId, Number(value))}>
          {saving ? "Saving…" : "Save"}
        </button>{" "}
        <button disabled={saving} onClick={() => onDelete(price.itemId)}>
          Delete
        </button>
      </td>
    </tr>
  );
}

function NewPriceForm({ onCreated, onCancel }: { onCreated: () => void; onCancel: () => void }) {
  const [itemId, setItemId] = useState("");
  const [itemType, setItemType] = useState("INGREDIENT");
  const [amount, setAmount] = useState("0.00");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleCreate() {
    setSubmitting(true);
    setError(null);
    try {
      await api.post("/v1/admin/prices", { itemId, itemType, amount: Number(amount) });
      onCreated();
    } catch (err) {
      setError(err instanceof ApiError && err.status === 409 ? "That item id already exists." : "Could not create price item.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="row" style={{ marginTop: 12 }}>
      <label>
        Item id
        <input value={itemId} onChange={(e) => setItemId(e.target.value)} placeholder="topping-tuna" />
      </label>
      <label>
        Type
        <select value={itemType} onChange={(e) => setItemType(e.target.value)}>
          <option value="SIZE">SIZE</option>
          <option value="DOUGH">DOUGH</option>
          <option value="INGREDIENT">INGREDIENT</option>
        </select>
      </label>
      <label>
        Amount (€)
        <input value={amount} onChange={(e) => setAmount(e.target.value)} className="cell-input" />
      </label>
      <div>
        {error && <p className="error-banner">{error}</p>}
        <button disabled={submitting || !itemId} onClick={handleCreate}>
          {submitting ? "Creating…" : "Create"}
        </button>{" "}
        <button onClick={onCancel}>Cancel</button>
      </div>
    </div>
  );
}

function EditableRuleRow({
  rule,
  saving,
  onSave,
}: {
  rule: RuleThreshold;
  saving: boolean;
  onSave: (ruleId: string, value: string) => void;
}) {
  const [value, setValue] = useState(rule.value);

  return (
    <tr>
      <td>{rule.ruleId}</td>
      <td>
        <input value={value} onChange={(e) => setValue(e.target.value)} className="cell-input" />
      </td>
      <td className="hint">{new Date(rule.updatedAt).toLocaleString()}</td>
      <td>
        <button disabled={saving} onClick={() => onSave(rule.ruleId, value)}>
          {saving ? "Saving…" : "Save"}
        </button>
      </td>
    </tr>
  );
}

function EditableStaffRow({
  account,
  saving,
  onSave,
  onDelete,
}: {
  account: StaffAccount;
  saving: boolean;
  onSave: (id: string, fullName: string, role: StaffRole) => void;
  onDelete: (id: string) => void;
}) {
  const [fullName, setFullName] = useState(account.fullName);
  const [role, setRole] = useState<StaffRole>(account.role);

  return (
    <tr>
      <td>{account.email}</td>
      <td>
        <input value={fullName} onChange={(e) => setFullName(e.target.value)} className="cell-input" />
      </td>
      <td>
        <select value={role} onChange={(e) => setRole(e.target.value as StaffRole)}>
          <option value="STAFF">STAFF</option>
          <option value="ADMIN">ADMIN</option>
        </select>
      </td>
      <td className="hint">{account.mustChangePassword ? "Awaiting first login" : "Active"}</td>
      <td>
        <button disabled={saving} onClick={() => onSave(account.id, fullName, role)}>
          {saving ? "Saving…" : "Save"}
        </button>{" "}
        <button disabled={saving} onClick={() => onDelete(account.id)}>
          Remove
        </button>
      </td>
    </tr>
  );
}

function NewStaffForm({
  onCreated,
  onCancel,
}: {
  onCreated: (result: { account: StaffAccount; temporaryPassword: string }) => void;
  onCancel: () => void;
}) {
  const [email, setEmail] = useState("");
  const [fullName, setFullName] = useState("");
  const [role, setRole] = useState<StaffRole>("STAFF");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleCreate() {
    setSubmitting(true);
    setError(null);
    try {
      const result = await api.post<{ account: StaffAccount; temporaryPassword: string }>("/v1/admin/staff", {
        email,
        fullName,
        role,
      });
      onCreated(result);
    } catch (err) {
      setError(err instanceof ApiError && err.status === 409 ? "An account with this email already exists." : "Could not create the account.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="row" style={{ marginTop: 12 }}>
      <label>
        Email
        <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} placeholder="new@pizzashop.com" />
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
      <div>
        {error && <p className="error-banner">{error}</p>}
        <button disabled={submitting || !email || !fullName} onClick={handleCreate}>
          {submitting ? "Creating…" : "Create"}
        </button>{" "}
        <button onClick={onCancel}>Cancel</button>
      </div>
    </div>
  );
}

function PizzaEditor({
  pizza,
  onSaved,
  onCancel,
}: {
  pizza: Pizza | null;
  onSaved: () => void;
  onCancel?: () => void;
}) {
  const isNew = pizza === null;
  const [id, setId] = useState(pizza?.id ?? "");
  const [name, setName] = useState(pizza?.name ?? "");
  const [description, setDescription] = useState(pizza?.description ?? "");
  const [basePrice, setBasePrice] = useState(pizza?.basePrice.toString() ?? "0.00");
  const [imageUrl, setImageUrl] = useState(pizza?.imageUrl ?? null);
  const [imageFile, setImageFile] = useState<File | null>(null);
  const [defaultIngredients, setDefaultIngredients] = useState<DefaultIngredient[]>(pizza?.defaultIngredients ?? []);
  const [allowedExtras, setAllowedExtras] = useState<AllowedExtra[]>(pizza?.allowedExtras ?? []);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

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
      const body = { id, name, description, basePrice: Number(basePrice), defaultIngredients, allowedExtras };
      const saved = isNew ? await api.post<Pizza>("/v1/admin/catalog/pizzas", body) : await api.put<Pizza>(`/v1/admin/catalog/pizzas/${id}`, body);

      let finalPizza = saved;
      if (imageFile) {
        const formData = new FormData();
        formData.append("file", imageFile);
        finalPizza = await api.upload<Pizza>(`/v1/admin/catalog/pizzas/${saved.id}/image`, formData);
      }
      setImageUrl(finalPizza.imageUrl);
      setImageFile(null);
      onSaved();
    } catch (err) {
      setError(err instanceof ApiError && err.status === 409 ? "A pizza with this id already exists." : "Could not save this pizza.");
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete() {
    setSaving(true);
    setError(null);
    try {
      await api.delete(`/v1/admin/catalog/pizzas/${id}`);
      onSaved();
    } catch {
      setError("Could not delete this pizza.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="card" style={{ marginBottom: 16 }}>
      <div className="row">
        <label>
          Id
          <input value={id} onChange={(e) => setId(e.target.value)} disabled={!isNew} placeholder="tonno" />
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
        <button disabled={saving || !id || !name} onClick={handleSave}>
          {saving ? "Saving…" : isNew ? "Create pizza" : "Save changes"}
        </button>{" "}
        {!isNew && (
          <button disabled={saving} onClick={handleDelete}>
            Delete pizza
          </button>
        )}{" "}
        {isNew && onCancel && <button onClick={onCancel}>Cancel</button>}
      </div>
    </div>
  );
}
