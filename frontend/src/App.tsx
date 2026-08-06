import { Route, Routes } from "react-router-dom";
import NavBar from "./components/NavBar";
import ProtectedRoute from "./components/ProtectedRoute";
import AdminHomePage from "./pages/admin/AdminHomePage";
import AdminLayout from "./pages/admin/AdminLayout";
import PizzaFormPage from "./pages/admin/PizzaFormPage";
import PizzaListPage from "./pages/admin/PizzaListPage";
import PriceFormPage from "./pages/admin/PriceFormPage";
import PriceListPage from "./pages/admin/PriceListPage";
import RuleFormPage from "./pages/admin/RuleFormPage";
import RuleListPage from "./pages/admin/RuleListPage";
import StaffFormPage from "./pages/admin/StaffFormPage";
import StaffListPage from "./pages/admin/StaffListPage";
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
                <AdminLayout />
              </ProtectedRoute>
            }
          >
            <Route index element={<AdminHomePage />} />
            <Route path="pizzas" element={<PizzaListPage />} />
            <Route path="pizzas/new" element={<PizzaFormPage />} />
            <Route path="pizzas/:id" element={<PizzaFormPage />} />
            <Route path="prices" element={<PriceListPage />} />
            <Route path="prices/new" element={<PriceFormPage />} />
            <Route path="prices/:itemId" element={<PriceFormPage />} />
            <Route path="staff" element={<StaffListPage />} />
            <Route path="staff/new" element={<StaffFormPage />} />
            <Route path="staff/:id" element={<StaffFormPage />} />
            <Route path="rules" element={<RuleListPage />} />
            <Route path="rules/:ruleId" element={<RuleFormPage />} />
          </Route>
        </Routes>
      </main>
    </>
  );
}
