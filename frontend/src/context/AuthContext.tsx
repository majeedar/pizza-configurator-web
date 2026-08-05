import { createContext, useContext, useState, type ReactNode } from "react";
import { BASE_URL } from "../api/client";

interface AuthState {
  token: string | null;
  scope: string | null;
  email: string | null;
  fullName: string | null;
  phoneNumber: string | null;
  mustChangePassword: boolean;
  login: (username: string, password: string) => Promise<void>;
  register: (email: string, password: string, fullName: string, phoneNumber: string) => Promise<void>;
  changePassword: (newPassword: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthState | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(localStorage.getItem("token"));
  const [scope, setScope] = useState<string | null>(localStorage.getItem("scope"));
  const [email, setEmail] = useState<string | null>(localStorage.getItem("email"));
  const [fullName, setFullName] = useState<string | null>(localStorage.getItem("fullName"));
  const [phoneNumber, setPhoneNumber] = useState<string | null>(localStorage.getItem("phoneNumber"));
  const [mustChangePassword, setMustChangePassword] = useState(localStorage.getItem("mustChangePassword") === "true");

  async function login(username: string, password: string) {
    const response = await fetch(`${BASE_URL}/v1/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username, password }),
    });

    if (!response.ok) {
      throw new Error("Invalid username or password");
    }

    const data = (await response.json()) as {
      token: string;
      scope: string;
      email: string | null;
      fullName: string | null;
      phoneNumber: string | null;
      mustChangePassword: boolean;
    };

    const store = (key: string, value: string | null) => {
      if (value) localStorage.setItem(key, value);
      else localStorage.removeItem(key);
    };
    localStorage.setItem("token", data.token);
    localStorage.setItem("scope", data.scope);
    store("email", data.email);
    store("fullName", data.fullName);
    store("phoneNumber", data.phoneNumber);
    localStorage.setItem("mustChangePassword", String(data.mustChangePassword));

    setToken(data.token);
    setScope(data.scope);
    setEmail(data.email);
    setFullName(data.fullName);
    setPhoneNumber(data.phoneNumber);
    setMustChangePassword(data.mustChangePassword);
  }

  async function register(registerEmail: string, password: string, registerFullName: string, registerPhoneNumber: string) {
    const response = await fetch(`${BASE_URL}/v1/auth/register`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email: registerEmail, password, fullName: registerFullName, phoneNumber: registerPhoneNumber }),
    });

    if (!response.ok) {
      throw new Error(response.status === 409 ? "An account with this email already exists." : "Could not create the account.");
    }

    await login(registerEmail, password);
  }

  async function changePassword(newPassword: string) {
    const response = await fetch(`${BASE_URL}/v1/auth/staff/password`, {
      method: "PUT",
      headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
      body: JSON.stringify({ newPassword }),
    });

    if (!response.ok) {
      throw new Error("Could not change the password.");
    }

    localStorage.setItem("mustChangePassword", "false");
    setMustChangePassword(false);
  }

  function logout() {
    localStorage.removeItem("token");
    localStorage.removeItem("scope");
    localStorage.removeItem("email");
    localStorage.removeItem("fullName");
    localStorage.removeItem("phoneNumber");
    localStorage.removeItem("mustChangePassword");
    setToken(null);
    setScope(null);
    setEmail(null);
    setFullName(null);
    setPhoneNumber(null);
    setMustChangePassword(false);
  }

  return (
    <AuthContext.Provider
      value={{ token, scope, email, fullName, phoneNumber, mustChangePassword, login, register, changePassword, logout }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth must be used within AuthProvider");
  }
  return ctx;
}
