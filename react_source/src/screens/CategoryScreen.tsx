import React from 'react';
import { View, Text, FlatList, TouchableOpacity, StyleSheet, Alert, StatusBar, Platform, RefreshControl } from 'react-native';
import { useNavigation } from '@react-navigation/native';
import { StackNavigationProp } from '@react-navigation/stack';
import { Ionicons, MaterialCommunityIcons } from '@expo/vector-icons';
import { useCategoryStore } from '../features/categories/store';
import { Category, RootStackParamList } from '../types';
import { SHADOWS, SIZES } from '../constants/theme';
import { useTheme } from '../hooks/useTheme';
import { LinearGradient } from 'expo-linear-gradient';
import { BlurView } from 'expo-blur';

type NavigationProp = StackNavigationProp<RootStackParamList>;

export default function CategoryScreen() {
  const navigation = useNavigation<NavigationProp>();
  const { colors, isDarkMode } = useTheme();
  const { categories, deleteCategory, loadCategories } = useCategoryStore();
  const [refreshing, setRefreshing] = React.useState(false);

  const onRefresh = React.useCallback(async () => {
    setRefreshing(true);
    try {
      await loadCategories();
    } finally {
      setRefreshing(false);
    }
  }, [loadCategories]);

  const handleDelete = (id: string) => {
    Alert.alert('Delete Category', 'Are you sure? Transactions in this category will become uncategorized.', [
      { text: 'Cancel', style: 'cancel' },
      { text: 'Delete', style: 'destructive', onPress: () => deleteCategory(id) },
    ]);
  };

  const renderItem = ({ item }: { item: Category }) => (
    <BlurView intensity={10} tint={isDarkMode ? "dark" : "light"} style={[styles.card, { borderColor: isDarkMode ? 'rgba(255,255,255,0.03)' : 'rgba(0,0,0,0.05)', backgroundColor: isDarkMode ? 'transparent' : 'rgba(255,255,255,0.8)' }]}>
      <TouchableOpacity
        style={styles.cardMain}
        activeOpacity={0.7}
        onPress={() => navigation.navigate('AddCategory', { categoryId: item.id })}
      >
        <View style={[styles.iconContainer, { backgroundColor: `${item.color}20` }]}>
          <Ionicons name={item.icon as any} size={22} color={item.color} />
        </View>
        <View style={styles.content}>
          <Text style={[styles.name, { color: colors.textDark }]}>{item.name}</Text>
          {item.budget && (
            <Text style={[styles.budgetLabel, { color: colors.textGrey }]}>MONTHLY LIMIT: {(item.budget).toLocaleString()}</Text>
          )}
        </View>
        <TouchableOpacity onPress={() => handleDelete(item.id)} style={styles.deleteBtn}>
          <Ionicons name="trash-outline" size={18} color={isDarkMode ? 'rgba(255,255,255,0.2)' : 'rgba(0,0,0,0.2)'} />
        </TouchableOpacity>
      </TouchableOpacity>
    </BlurView>
  );

  return (
    <View style={[styles.container, { backgroundColor: colors.background }]}>
      <StatusBar barStyle={isDarkMode ? "light-content" : "dark-content"} />
      <LinearGradient colors={isDarkMode ? ['#0F172A', '#1E293B'] : [colors.background, colors.background]} style={StyleSheet.absoluteFill} />

      <View style={styles.header}>
        <Text style={[styles.headerTitle, { color: colors.textDark }]}>Categories</Text>
        <TouchableOpacity
          style={styles.addBtn}
          onPress={() => navigation.navigate('AddCategory', {})}
        >
          <Ionicons name="add" size={24} color="white" />
        </TouchableOpacity>
      </View>

      <FlatList
        data={categories}
        keyExtractor={(item) => item.id}
        renderItem={renderItem}
        contentContainerStyle={styles.list}
        showsVerticalScrollIndicator={false}
        ListEmptyComponent={
          <View style={styles.emptyContainer}>
            <MaterialCommunityIcons name="shape-outline" size={64} color={isDarkMode ? "rgba(255,255,255,0.05)" : "rgba(0,0,0,0.05)"} />
            <Text style={[styles.emptyText, { color: colors.textDark }]}>No Categories</Text>
            <Text style={[styles.emptySubText, { color: colors.textGrey }]}>Create categories to organize your spending.</Text>
          </View>
        }
        refreshControl={
          <RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={isDarkMode ? "#ffffff" : "#000000"} />
        }
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  header: {
    paddingTop: Platform.OS === 'ios' ? 60 : 40,
    paddingHorizontal: 20,
    paddingBottom: 20,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  headerTitle: {
    fontSize: 26,
    fontWeight: '900',
    letterSpacing: -1,
  },
  addBtn: {
    width: 44,
    height: 44,
    borderRadius: 16,
    backgroundColor: '#3B82F6',
    justifyContent: 'center',
    alignItems: 'center',
    ...SHADOWS.premium,
  },
  list: { padding: 20, paddingBottom: 100 },
  card: {
    borderRadius: 24,
    marginBottom: 16,
    borderWidth: 1,
    overflow: 'hidden',
  },
  cardMain: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 16,
  },
  iconContainer: {
    width: 48,
    height: 48,
    borderRadius: 16,
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 16,
  },
  content: { flex: 1 },
  name: {
    fontSize: 18,
    fontWeight: '700',
    marginBottom: 4,
  },
  budgetLabel: {
    fontSize: 10,
    fontWeight: '800',
    letterSpacing: 1,
  },
  deleteBtn: {
    padding: 8,
  },
  emptyContainer: {
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: 100,
  },
  emptyText: {
    fontSize: 20,
    fontWeight: '800',
    marginTop: 20,
  },
  emptySubText: {
    fontSize: 14,
    fontWeight: '500',
    marginTop: 8,
    textAlign: 'center',
    paddingHorizontal: 40,
    lineHeight: 20,
  },
});
