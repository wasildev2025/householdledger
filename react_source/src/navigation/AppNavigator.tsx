import React, { Suspense } from 'react';
import { NavigationContainer, DefaultTheme, DarkTheme } from '@react-navigation/native';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { createStackNavigator } from '@react-navigation/stack';
import { Ionicons } from '@expo/vector-icons';
import { Platform, View, ActivityIndicator } from 'react-native';

// Core Application Screens — Lazy loaded to improve startup performance
const HomeScreen = React.lazy(() => import('../screens/HomeScreen'));
const TransactionListScreen = React.lazy(() => import('../screens/TransactionListScreen'));
const PeopleScreen = React.lazy(() => import('../screens/PeopleScreen'));
const CategoryScreen = React.lazy(() => import('../screens/CategoryScreen'));
const SettingsScreen = React.lazy(() => import('../screens/SettingsScreen'));
const AddTransactionScreen = React.lazy(() => import('../screens/AddTransactionScreen'));
const AddServantScreen = React.lazy(() => import('../screens/AddServantScreen'));
const AddCategoryScreen = React.lazy(() => import('../screens/AddCategoryScreen'));
const ServantDetailScreen = React.lazy(() => import('../screens/ServantDetailScreen'));
const AddMemberScreen = React.lazy(() => import('../screens/AddMemberScreen'));
const LoginScreen = React.lazy(() => import('../screens/LoginScreen'));
const OnboardingScreen = React.lazy(() => import('../screens/OnboardingScreen'));
const JoinHouseholdScreen = React.lazy(() => import('../screens/JoinHouseholdScreen'));
const ReportsScreen = React.lazy(() => import('../screens/ReportsScreen'));
const HouseholdMessagesScreen = React.lazy(() => import('../screens/HouseholdMessagesScreen'));
const RecurringTransactionsScreen = React.lazy(() => import('../screens/RecurringTransactionsScreen'));
const DairyTrackerScreen = React.lazy(() => import('../screens/DairyTrackerScreen'));

import { RootStackParamList, AuthStackParamList, TabParamList } from '../types';
import { SHADOWS, COLORS } from '../constants/theme';
import { useTheme } from '../hooks/useTheme';
import { useAuthStore } from '../features/auth/store';
import { useHouseholdStore } from '../features/household/store';

const Tab = createBottomTabNavigator<TabParamList>();
const Stack = createStackNavigator<RootStackParamList>();
const AuthStack = createStackNavigator<AuthStackParamList>();

function TabNavigator() {
  const { colors, isDarkMode } = useTheme();
  const profile = useAuthStore(s => s.profile);
  const isAdmin = profile?.role === 'admin';

  return (
    <Tab.Navigator
      screenOptions={({ route }) => ({
        tabBarIcon: ({ focused, color, size }) => {
          let iconName: keyof typeof Ionicons.glyphMap = 'help';

          if (route.name === 'Home') {
            iconName = focused ? 'grid' : 'grid-outline';
          } else if (route.name === 'Transactions') {
            iconName = focused ? 'receipt' : 'receipt-outline';
          } else if (route.name === 'Reports') {
            iconName = focused ? 'analytics' : 'analytics-outline';
          } else if (route.name === 'People') {
            iconName = focused ? 'people' : 'people-outline';
          } else if (route.name === 'Categories') {
            iconName = focused ? 'layers' : 'layers-outline';
          } else if (route.name === 'Settings') {
            iconName = focused ? 'options' : 'options-outline';
          }

          return <Ionicons name={iconName} size={24} color={color} />;
        },
        tabBarActiveTintColor: colors.primary,
        tabBarInactiveTintColor: isDarkMode ? 'rgba(255,255,255,0.3)' : 'rgba(0,0,0,0.3)',
        tabBarShowLabel: false,
        tabBarStyle: {
          position: 'absolute',
          bottom: 24,
          left: 20,
          right: 20,
          backgroundColor: isDarkMode ? 'rgba(30, 41, 59, 0.95)' : 'rgba(255, 255, 255, 0.95)',
          borderRadius: 30,
          height: 64,
          borderWidth: 1,
          borderColor: isDarkMode ? 'rgba(255,255,255,0.08)' : 'rgba(0,0,0,0.05)',
          ...SHADOWS.premium,
          paddingBottom: 0,
          elevation: 10,
        },
        headerShown: false,
      })}
    >
      <Tab.Screen name="Home" component={HomeScreen} />
      <Tab.Screen name="Transactions" component={TransactionListScreen} />
      <Tab.Screen name="Reports" component={ReportsScreen} />
      {isAdmin && (
        <>
          <Tab.Screen name="People" component={PeopleScreen} />
          <Tab.Screen name="Categories" component={CategoryScreen} />
        </>
      )}
      <Tab.Screen name="Settings" component={SettingsScreen} />
    </Tab.Navigator>
  );
}

export default function AppNavigator() {
  const { colors, isDarkMode } = useTheme();
  const { session, isProfileLoading, hasCompletedOnboarding, profile } = useAuthStore();
  const { household } = useHouseholdStore();

  const navigationTheme = {
    ...DefaultTheme,
    colors: {
      ...DefaultTheme.colors,
      background: colors.background,
      card: colors.card,
      text: colors.textDark,
      border: colors.border,
      primary: colors.primary,
    },
  };

  return (
    <NavigationContainer theme={navigationTheme}>
      <Suspense fallback={
        <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: colors.background }}>
          <ActivityIndicator size="large" color={colors.primary} />
        </View>
      }>
        {session && isProfileLoading ? (
          <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: colors.background }}>
            <ActivityIndicator size="large" color={colors.primary} />
          </View>
        ) : !session ? (
          <AuthStack.Navigator screenOptions={{ headerShown: false }}>
            {!hasCompletedOnboarding && (
              <AuthStack.Screen name="Onboarding" component={OnboardingScreen} />
            )}
            <AuthStack.Screen name="Login" component={LoginScreen} />
          </AuthStack.Navigator>
        ) : (!profile || !household) ? (
          <Stack.Navigator screenOptions={{ headerShown: false }}>
            <Stack.Screen name="JoinHousehold" component={JoinHouseholdScreen} />
          </Stack.Navigator>
        ) : (
          <Stack.Navigator
            screenOptions={{
              headerStyle: {
                backgroundColor: colors.background,
                shadowColor: 'transparent',
                elevation: 0,
              },
              headerTintColor: colors.textDark,
              headerTitleStyle: {
                fontWeight: 'bold',
                color: colors.textDark,
              },
              cardStyle: { backgroundColor: colors.background },
            }}
          >
            <Stack.Screen name="Tabs" component={TabNavigator} options={{ headerShown: false }} />
            <Stack.Screen name="AddTransaction" component={AddTransactionScreen} options={{ presentation: 'modal', title: 'Transaction' }} />
            <Stack.Screen name="AddServant" component={AddServantScreen} options={{ presentation: 'modal', title: 'Staff' }} />
            <Stack.Screen name="ServantDetail" component={ServantDetailScreen} options={{ title: 'Staff Details' }} />
            <Stack.Screen name="AddCategory" component={AddCategoryScreen} options={{ presentation: 'modal', title: 'Category' }} />
            <Stack.Screen name="AddMember" component={AddMemberScreen} options={{ presentation: 'modal', title: 'Family Member' }} />
            <Stack.Screen name="Messages" component={HouseholdMessagesScreen} options={{ title: 'Messages' }} />
            <Stack.Screen name="RecurringTransactions" component={RecurringTransactionsScreen} options={{ title: 'Recurring Transactions' }} />
            <Stack.Screen name="DairyTracker" component={DairyTrackerScreen} options={{ title: 'Daily Dairy Tracker' }} />
          </Stack.Navigator>
        )}
      </Suspense>
    </NavigationContainer>
  );
}
