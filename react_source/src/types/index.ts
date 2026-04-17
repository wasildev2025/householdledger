export interface Household {
  id: string;
  name: string;
  admin_id: string;
  created_at: string;
}

export interface Servant {
  id: string;
  name: string;
  role: string;
  phoneNumber?: string;
  salary?: number;
  budget?: number;
  pin?: string;
  balance: number; // Money held by servant to spend
  invite_code?: string | null;
  household_id?: string;
}

export interface Member {
  id: string;
  name: string;
  role: 'member';
  pin?: string;
  household_id?: string;
  invite_code?: string | null;
}

export interface Category {
  id: string;
  name: string;
  icon: string; // Ionicons name
  color: string;
  budget?: number; // Monthly budget for this category
  household_id?: string;
}

export type TransactionType = 'expense' | 'income' | 'transfer';

export interface Transaction {
  id: string;
  amount: number;
  date: string; // ISO Date string
  type: TransactionType;
  categoryId?: string; // Required for expense
  servantId?: string | null; // For expense (who spent) or transfer (recipient)
  memberId?: string | null; // For transfer (who gave money)
  description: string;
  household_id?: string;
}

export interface MoneyRequest {
  id: string;
  servantId: string;
  amount: number;
  date: string;
  status: 'pending' | 'approved' | 'rejected';
  description?: string;
  household_id?: string;
}

export type MessageType = 'text' | 'image' | 'voice';

export interface Message {
  id: string;
  sender_id: string;
  sender_name: string;
  content: string;
  created_at: string;
  household_id?: string;
  message_type?: MessageType;
  media_url?: string;
  media_duration?: number; // Duration in seconds for voice messages
}

export interface DairyLog {
  id: string;
  date: string;
  milkQty: number;
  milkPrice: number;
  yogurtQty: number;
  yogurtPrice: number;
  totalBill: number;
  household_id: string;
}

export type Frequency = 'daily' | 'weekly' | 'monthly';

export interface RecurringTransaction {
  id: string;
  amount: number;
  description: string;
  type: TransactionType;
  categoryId?: string;
  servantId?: string | null;
  memberId?: string | null;
  frequency: Frequency;
  startDate: string;
  lastGeneratedDate?: string;
  active: boolean;
  household_id?: string;
}

export type AuthStackParamList = {
  Login: undefined;
  Onboarding: undefined;
};

export type TabParamList = {
  Home: undefined;
  Transactions: undefined;
  Reports: undefined;
  People: undefined;
  Categories: undefined;
  Settings: undefined;
};

export type RootStackParamList = {
  Tabs: undefined;
  AddTransaction: { transactionId?: string; type?: TransactionType; servantId?: string; memberId?: string };
  ServantDetail: { servantId: string };
  AddServant: { servantId?: string };
  AddMember: { memberId?: string };
  AddCategory: { categoryId?: string };
  JoinHousehold: undefined;
  Messages: undefined;
  RecurringTransactions: undefined;
  DairyTracker: undefined;
};
