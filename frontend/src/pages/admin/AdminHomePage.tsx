import { Link } from "react-router-dom";

const sections = [
  { to: "/admin/pizzas", label: "Pizzas", hint: "Menu items, default ingredients, allowed extras, photos" },
  { to: "/admin/prices", label: "Prices", hint: "Size, dough, and ingredient pricing" },
  { to: "/admin/staff", label: "Staff & admin accounts", hint: "Kitchen display and admin panel access" },
  { to: "/admin/rules", label: "Rule thresholds", hint: "Global validation limits used by rule-service" },
];

export default function AdminHomePage() {
  return (
    <div className="page">
      <h1>Admin</h1>
      <div className="list">
        {sections.map((section) => (
          <Link key={section.to} to={section.to} className="list-item">
            <div>
              <div>{section.label}</div>
              <div className="list-item__meta">{section.hint}</div>
            </div>
            <span aria-hidden="true">→</span>
          </Link>
        ))}
      </div>
    </div>
  );
}
