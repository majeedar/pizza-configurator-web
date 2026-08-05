export type IngredientType = "CHEESE" | "TOPPING";
export type IngredientUnit = "PORTION" | "PIECE";

export interface DefaultIngredient {
  ingredientId: string;
  name: string;
  removable: boolean;
}

export interface AllowedExtra {
  ingredientId: string;
  name: string;
  type: IngredientType;
  unit: IngredientUnit;
}

export interface Pizza {
  id: string;
  name: string;
  description: string;
  basePrice: number;
  imageUrl: string | null;
  defaultIngredients: DefaultIngredient[];
  allowedExtras: AllowedExtra[];
}

export interface Addition {
  ingredientId: string;
  type: IngredientType;
  quantity: number;
}

export type ChangeSource = "BUTTON" | "FREE_TEXT";

export interface ChangeRequest {
  basePizzaId: string;
  size: string;
  dough: string;
  additions: Addition[];
  removals: string[];
  source: ChangeSource;
  ambiguous: boolean;
}

export interface FailedRule {
  ruleId: string;
  messageDe: string;
}

export type RuleOutcome = "APPROVED" | "INVALID" | "MANUAL_REVIEW";

export interface ValidationResult {
  outcome: RuleOutcome;
  failures: FailedRule[];
  recommendations: Pizza[];
  referencePrices: PriceItem[];
}

export interface PriceItem {
  itemId: string;
  itemType: string;
  amount: number;
}

export interface ParsedProposal {
  basePizzaId: string;
  size: string;
  dough: string;
  additions: Addition[];
  removals: string[];
  ambiguous: boolean;
}

export type OrderStatus = "PLACED" | "PROCESSING" | "READY_FOR_COLLECTION";

export interface OrderResponse {
  orderId: string;
  displayNumber: string;
  status: OrderStatus;
  totalPrice: number;
  customNotes: string;
  pickupSecurityToken: string;
  phoneNumber: string;
  createdAt: string;
}

export interface TicketAddition {
  ingredientId: string;
  quantity: number;
}

export interface TicketItem {
  basePizzaId: string;
  chosenSize: string;
  chosenDough: string;
  additions: TicketAddition[];
  removals: string[];
}

export interface TicketView {
  orderId: string;
  displayNumber: string;
  status: OrderStatus;
  customNotes: string;
  items: TicketItem[];
}

export interface RuleThreshold {
  ruleId: string;
  value: string;
  updatedAt: string;
}

export type StaffRole = "STAFF" | "ADMIN";

export interface StaffAccount {
  id: string;
  email: string;
  fullName: string;
  role: StaffRole;
  mustChangePassword: boolean;
  createdAt: string;
}

export interface StaffAccountCreateResult {
  account: StaffAccount;
  temporaryPassword: string;
}

export interface ConfigureResponse {
  basePizzaId: string;
  size: string;
  dough: string;
  additions: Addition[];
  removals: string[];
  ambiguous: boolean;
  kitchenNote: string | null;
  validation: ValidationResult;
}

export interface ModificationsPayload {
  additions: { id: string; qty: number }[];
  removals: string[];
}

export type PendingReviewStatus = "PENDING" | "RESOLVED" | "CONFIRMED";

export interface PendingReview {
  id: string;
  status: PendingReviewStatus;
  basePizzaId: string;
  size: string;
  dough: string;
  modifications: ModificationsPayload;
  rawComment: string;
  resolvedBasePizzaId: string | null;
  resolvedSize: string | null;
  resolvedDough: string | null;
  resolvedModifications: ModificationsPayload | null;
  resolvedTotalPrice: number | null;
  createdAt: string;
  resolvedAt: string | null;
}
