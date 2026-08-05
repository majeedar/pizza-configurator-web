import { Route, Routes } from "react-router-dom";
import NavBar from "./components/NavBar";
import ProtectedRoute from "./components/ProtectedRoute";
import AdminPage from "./pages/AdminPage";
import ChangePasswordPage from "./pages/ChangePasswordPage";
import CustomerDashboardPage from "./pages/CustomerDashboardPage";
import CustomerLoginPage from "./pages/CustomerLoginPage";
import CustomerOrderPage from "./pages/CustomerOrderPage";
import CustomerRegisterPage from "./pages/CustomerRegisterPage";
import KitchenBoardPage from "./pages/KitchenBoardPage";
import LoginPage from "./pages/LoginPage";

export default function App() {
  return (
    <>
      <NavBar />
      <main>
        <Routes>
          <Route
            path="/"
            element={
              <ProtectedRoute requireScope="customer">
                <CustomerOrderPage />
              </ProtectedRoute>
            }
          />
          <Route path="/account/login" element={<CustomerLoginPage />} />
          <Route path="/account/register" element={<CustomerRegisterPage />} />
          <Route
            path="/account"
            element={
              <ProtectedRoute requireScope="customer">
                <CustomerDashboardPage />
              </ProtectedRoute>
            }
          />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/change-password" element={<ChangePasswordPage />} />
          <Route
            path="/kitchen"
            element={
              <ProtectedRoute>
                <KitchenBoardPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin"
            element={
              <ProtectedRoute requireScope="admin">
                <AdminPage />
              </ProtectedRoute>
            }
          />
        </Routes>
      </main>
    </>
  );
}
