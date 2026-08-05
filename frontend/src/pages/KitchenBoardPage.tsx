import { useEffect, useState } from "react";
import { api, ApiError, streamServerSentEvents } from "../api/client";
import type { Addition, PendingReview, Pizza, TicketView } from "../api/types";

export default function KitchenBoardPage() {
  const [tickets, setTickets] = useState<TicketView[]>([]);
  const [connectionError, setConnectionError] = useState<string | null>(null);
  const [advancing, setAdvancing] = useState<string | null>(null);

  const [pizzas, setPizzas] = useState<Pizza[]>([]);
  const [pendingReviews, setPendingReviews] = useState<PendingReview[]>([]);

  useEffect(() => {
    const controller = new AbortController();
    streamServerSentEvents<TicketView[]>("/v1/kitchen/board/stream", setTickets, controller.signal).catch((err) => {
      if (controller.signal.aborted) return;
      setConnectionError(err instanceof Error ? err.message : "Lost connection to the kitchen board.");
    });
    return () => controller.abort();
  }, []);

  // catalog-service isn't behind /v1/kitchen/**, so the resolution form fetches the
  // menu via the same public customer route the ordering page uses.
  useEffect(() => {
    api
      .get<Pizza[]>("/v1/customer/catalog/pizzas")
      .then(setPizzas)
      .catch(() => {
        /* resolution form will just show no pizza options if this fails */
      });
  }, []);

  function loadPendingReviews() {
    api
      .get<PendingReview[]>("/v1/kitchen/pending-reviews")
      .then(setPendingReviews)
      .catch(() => {
        /* transient — keep showing the last known list */
      });
  }

  useEffect(() => {
    loadPendingReviews();
    const interval = setInterval(loadPendingReviews, 5000);
    return () => clearInterval(interval);
  }, []);

  async function handleAdvance(orderId: string) {
    setAdvancing(orderId);
    try {
      await api.post(`/v1/kitchen/board/${orderId}/advance`);
    } catch {
      setConnectionError("Could not advance that ticket.");
    } finally {
      setAdvancing(null);
    }
  }

  return (
    <div className="page">
      <h1>Kitchen Display</h1>
      {connectionError && <p className="error-banner">{connectionError}</p>}
      <div className="board">
        {tickets.length === 0 && <p className="hint">No active tickets.</p>}
        {tickets.map((ticket) => (
          <div key={ticket.orderId} className={`ticket ticket--${ticket.status.toLowerCase()}`}>
            <div className="ticket__header">
              <strong>{ticket.displayNumber}</strong>
              <span>{ticket.status.replaceAll("_", " ")}</span>
            </div>
            <ul>
              {ticket.items.map((item, idx) => (
                <li key={idx}>
                  {item.basePizzaId} ({item.chosenSize}, {item.chosenDough})
                  {item.additions.length > 0 && (
                    <div className="hint">
                      + {item.additions.map((a) => `${a.ingredientId} ×${a.quantity}`).join(", ")}
                    </div>
                  )}
                  {item.removals.length > 0 && <div className="hint">− {item.removals.join(", ")}</div>}
                </li>
              ))}
            </ul>
            {ticket.customNotes && <p className="ticket__notes">📝 {ticket.customNotes}</p>}
            {ticket.status !== "READY_FOR_COLLECTION" && (
              <button onClick={() => handleAdvance(ticket.orderId)} disabled={advancing === ticket.orderId}>
                {ticket.status === "PLACED" ? "Start processing" : "Mark ready"}
              </button>
            )}
          </div>
        ))}
      </div>

      <h1>Pending Reviews</h1>
      <p className="hint">Comments the AI couldn't confidently turn into a change — resolve them for the customer.</p>
      {pendingReviews.length === 0 && <p className="hint">Nothing waiting on review.</p>}
      <div className="board">
        {pendingReviews.map((review) => (
          <PendingReviewCard key={review.id} review={review} pizzas={pizzas} onResolved={loadPendingReviews} />
        ))}
      </div>
    </div>
  );
}

function PendingReviewCard({
  review,
  pizzas,
  onResolved,
}: {
  review: PendingReview;
  pizzas: Pizza[];
  onResolved: () => void;
}) {
  const [basePizzaId, setBasePizzaId] = useState(review.basePizzaId);
  const [size, setSize] = useState(review.size);
  const [dough, setDough] = useState(review.dough);
  // Modifications only carries {id, qty} (no type — see order-service's Modifications.Addition),
  // so quantities are tracked untyped here and the actual CHEESE/TOPPING type is looked up
  // from the selected pizza's allowedExtras at submit time, not guessed at load time.
  const [extraQty, setExtraQty] = useState<Record<string, number>>(
    Object.fromEntries(review.modifications.additions.map((a) => [a.id, a.qty])),
  );
  const [removedIds, setRemovedIds] = useState<Set<string>>(new Set(review.modifications.removals));
  const [submitting, setSubmitting] = useState(false);
  const [failures, setFailures] = useState<string[] | null>(null);

  const selectedPizza = pizzas.find((p) => p.id === basePizzaId);

  function getQty(ingredientId: string): number {
    return extraQty[ingredientId] ?? 0;
  }

  function setQty(ingredientId: string, quantity: number) {
    setExtraQty((prev) => {
      const next = { ...prev };
      if (quantity > 0) next[ingredientId] = quantity;
      else delete next[ingredientId];
      return next;
    });
  }

  function buildAdditions(): Addition[] {
    return (selectedPizza?.allowedExtras ?? [])
      .filter((extra) => getQty(extra.ingredientId) > 0)
      .map((extra) => ({ ingredientId: extra.ingredientId, type: extra.type, quantity: getQty(extra.ingredientId) }));
  }

  if (review.status !== "PENDING") {
    return (
      <div className="ticket">
        <div className="ticket__header">
          <strong>{review.status}</strong>
        </div>
        <p className="hint">"{review.rawComment}"</p>
        {review.resolvedTotalPrice != null && <p>Resolved at €{review.resolvedTotalPrice.toFixed(2)}</p>}
      </div>
    );
  }

  async function handleResolve() {
    setSubmitting(true);
    setFailures(null);
    try {
      await api.put(`/v1/kitchen/pending-reviews/${review.id}/resolve`, {
        basePizzaId,
        size,
        dough,
        additions: buildAdditions(),
        removals: [...removedIds],
      });
      onResolved();
    } catch (err) {
      if (err instanceof ApiError && err.status === 422) {
        try {
          const body = JSON.parse(err.message) as { failures?: { messageDe: string }[] };
          setFailures((body.failures ?? []).map((f) => f.messageDe));
        } catch {
          setFailures(["The kitchen's proposed change was rejected."]);
        }
      } else {
        setFailures(["Could not resolve — is order-service reachable?"]);
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="ticket">
      <div className="ticket__header">
        <strong>Pending</strong>
      </div>
      <p className="ticket__notes">📝 "{review.rawComment}"</p>
      <p className="hint">
        Originally: {review.basePizzaId} ({review.size}, {review.dough})
      </p>

      <label>
        Base pizza
        <select value={basePizzaId} onChange={(e) => setBasePizzaId(e.target.value)}>
          {pizzas.map((p) => (
            <option key={p.id} value={p.id}>
              {p.name}
            </option>
          ))}
        </select>
      </label>
      <div className="row">
        <label>
          Size
          <select value={size} onChange={(e) => setSize(e.target.value)}>
            {["S", "M", "L"].map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>
        </label>
        <label>
          Dough
          <select value={dough} onChange={(e) => setDough(e.target.value)}>
            {["classic", "gluten-free"].map((d) => (
              <option key={d} value={d}>
                {d}
              </option>
            ))}
          </select>
        </label>
      </div>

      {selectedPizza?.defaultIngredients.map((ing) => (
        <label key={ing.ingredientId} className="checkbox-row">
          <input
            type="checkbox"
            checked={!removedIds.has(ing.ingredientId)}
            disabled={!ing.removable}
            onChange={(e) => {
              setRemovedIds((prev) => {
                const next = new Set(prev);
                if (e.target.checked) next.delete(ing.ingredientId);
                else next.add(ing.ingredientId);
                return next;
              });
            }}
          />
          {ing.name}
        </label>
      ))}

      {selectedPizza?.allowedExtras.map((extra) => (
        <div className="quantity-row" key={extra.ingredientId}>
          <span>{extra.name}</span>
          <div className="stepper">
            <button onClick={() => setQty(extra.ingredientId, Math.max(0, getQty(extra.ingredientId) - 1))}>−</button>
            <span>{getQty(extra.ingredientId)}</span>
            <button onClick={() => setQty(extra.ingredientId, getQty(extra.ingredientId) + 1)}>+</button>
          </div>
        </div>
      ))}

      {failures && (
        <div className="error-banner">
          <ul>
            {failures.map((f, i) => (
              <li key={i}>{f}</li>
            ))}
          </ul>
        </div>
      )}

      <button onClick={handleResolve} disabled={submitting}>
        {submitting ? "Sending…" : "Validate & send to customer"}
      </button>
    </div>
  );
}
