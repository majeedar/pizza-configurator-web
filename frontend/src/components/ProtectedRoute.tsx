import type { ReactNode } from "react";
import { Navigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

interface ProtectedRouteProps {
  requireScope?: string;
  children: ReactNode;
}

export default function ProtectedRoute({ requireScope, children }: ProtectedRouteProps) {
  const { token, scope, mustChangePassword } = useAuth();

  if (!token) {
    return <Navigate to={requireScope === "customer" ? "/account/login" : "/login"} replace />;
  }

  if (mustChangePassword) {
    return <Navigate to="/change-password" replace />;
  }

  if (requireScope && scope !== requireScope) {
    return (
      <div className="page">
        <p className="error-banner">
          This page needs a "{requireScope}" login — you're signed in with "{scope}".
        </p>
      </div>
    );
  }

  return <>{children}</>;
}
