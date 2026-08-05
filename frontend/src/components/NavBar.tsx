import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export default function NavBar() {
  const { scope, fullName, logout } = useAuth();
  const navigate = useNavigate();

  function handleLogout() {
    logout();
    navigate(scope === "customer" ? "/account/login" : "/");
  }

  return (
    <header className="nav">
      <div className="nav__brand">🍕 Pizza Configurator</div>
      <nav className="nav__links">
        <Link to="/">Order</Link>
        {scope === "customer" && <Link to="/account">My Orders</Link>}
        <Link to="/kitchen">Kitchen</Link>
        <Link to="/admin">Admin</Link>
      </nav>
      <div className="nav__auth">
        {scope === "customer" ? (
          <>
            <span className="nav__scope">{fullName ?? "Signed in"}</span>
            <button onClick={handleLogout}>Log out</button>
          </>
        ) : scope ? (
          <>
            <span className="nav__scope">Signed in ({scope})</span>
            <button onClick={handleLogout}>Log out</button>
          </>
        ) : (
          <>
            <Link to="/account/login">Log in</Link>
            <Link to="/login">Staff / Admin login</Link>
          </>
        )}
      </div>
    </header>
  );
}
