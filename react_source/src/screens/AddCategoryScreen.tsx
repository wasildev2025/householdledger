import React, { useState, useEffect, useLayoutEffect } from 'react';
import { View, Text, TextInput, StyleSheet, TouchableOpacity, Alert, ScrollView, StatusBar, Platform } from 'react-native';
import { useNavigation, useRoute, RouteProp } from '@react-navigation/native';
import { Ionicons } from '@expo/vector-icons';
import { useCategoryStore } from '../features/categories/store';
import { RootStackParamList } from '../types';
import { SIZES, SHADOWS } from '../constants/theme';
import { useTheme } from '../hooks/useTheme';
import { LinearGradient } from 'expo-linear-gradient';
import { BlurView } from 'expo-blur';

type AddCategoryRouteProp = RouteProp<RootStackParamList, 'AddCategory'>;

const CATEGORY_COLORS = [
  '#FF6B6B', '#4ECDC4', '#45B7D1', '#96CEB4',
  '#FFEEAD', '#FF9F43', '#54a0ff', '#5f27cd',
  '#ff4757', '#2ed573', '#1e90ff', '#3742fa'
];

const ICONS = [
  'cart', 'flash', 'bus', 'cash', 'ellipsis-horizontal',
  'home', 'construct', 'restaurant', 'medical', 'school',
  'airplane', 'barbell', 'book', 'cafe', 'car',
  'gift', 'game-controller', 'paw', 'shirt', 'wifi',
  'wallet', 'card', 'desktop', 'phone-portrait', 'bed',
  'water', 'sunny', 'rainy', 'leaf', 'flower',
  'hammer', 'pricetag', 'stats-chart', 'map', 'navigate',
  'bicycle', 'train', 'boat', 'basket', 'briefcase',
  'calculator', 'camera', 'videocam', 'musical-notes', 'headset',
  'fitness', 'football', 'medkit', 'bandage', 'thermometer',
  'wine', 'beer', 'pizza', 'fast-food', 'ice-cream'
];

export default function AddCategoryScreen() {
  const navigation = useNavigation();
  const route = useRoute<AddCategoryRouteProp>();
  const { colors, isDarkMode } = useTheme();
  const { categoryId } = route.params || {};
  const { categories, addCategory, updateCategory } = useCategoryStore();

  const [name, setName] = useState('');
  const [color, setColor] = useState(CATEGORY_COLORS[0]);
  const [icon, setIcon] = useState(ICONS[0]);

  useLayoutEffect(() => {
    navigation.setOptions({
      headerTitle: categoryId ? 'Edit Category' : 'New Category',
      headerStyle: { backgroundColor: colors.background, shadowColor: 'transparent', elevation: 0 },
      headerTitleStyle: { color: colors.textDark, fontWeight: '900', fontSize: 18 },
      headerTintColor: colors.textDark,
    });
  }, [navigation, categoryId, colors]);

  useEffect(() => {
    if (categoryId) {
      const category = categories.find((c) => c.id === categoryId);
      if (category) {
        setName(category.name);
        setColor(category.color);
        setIcon(category.icon);
      }
    }
  }, [categoryId, categories]);

  const handleSave = () => {
    if (!name) {
      Alert.alert('Required', 'Please enter a name for the category.');
      return;
    }

    const categoryData = { name, color, icon, budget: 0 };

    if (categoryId) {
      updateCategory(categoryId, categoryData);
    } else {
      addCategory(categoryData);
    }
    navigation.goBack();
  };

  return (
    <View style={[styles.mainContainer, { backgroundColor: colors.background }]}>
      <StatusBar barStyle={isDarkMode ? "light-content" : "dark-content"} />
      <LinearGradient colors={isDarkMode ? ['#0F172A', '#1E293B'] : [colors.background, colors.background]} style={StyleSheet.absoluteFill} />

      <ScrollView style={styles.container} contentContainerStyle={{ paddingBottom: 60 }} showsVerticalScrollIndicator={false}>
        <BlurView intensity={20} tint={isDarkMode ? "dark" : "light"} style={[styles.sectionCard, { borderColor: isDarkMode ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.05)', backgroundColor: isDarkMode ? 'transparent' : 'rgba(255,255,255,0.8)' }]}>
          <Text style={[styles.label, { color: colors.textGrey }]}>NAME</Text>
          <TextInput
            style={[styles.input, { color: colors.textDark, backgroundColor: isDarkMode ? 'rgba(0,0,0,0.2)' : 'rgba(255,255,255,0.5)', borderColor: isDarkMode ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.1)' }]}
            value={name}
            onChangeText={setName}
            placeholder="e.g. Groceries"
            placeholderTextColor={colors.textGrey}
          />
        </BlurView>

        <View style={styles.sectionHeader}>
          <Text style={[styles.sectionTitle, { color: colors.textDark }]}>Style</Text>
        </View>

        <BlurView intensity={10} tint={isDarkMode ? "dark" : "light"} style={[styles.sectionCard, { borderColor: isDarkMode ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.05)', backgroundColor: isDarkMode ? 'transparent' : 'rgba(255,255,255,0.8)' }]}>
          <Text style={[styles.label, { color: colors.textGrey, marginBottom: 15 }]}>COLOR</Text>
          <View style={styles.colorGrid}>
            {CATEGORY_COLORS.map((c) => (
              <TouchableOpacity
                key={c}
                style={[
                  styles.colorOption,
                  { backgroundColor: c },
                  color === c && styles.selectedColor
                ]}
                onPress={() => setColor(c)}
              >
                {color === c && <Ionicons name="checkmark" size={18} color="white" />}
              </TouchableOpacity>
            ))}
          </View>

          <Text style={[styles.label, { color: colors.textGrey, marginTop: 25, marginBottom: 15 }]}>ICON</Text>
          <View style={styles.iconGrid}>
            {ICONS.map((i) => (
              <TouchableOpacity
                key={i}
                style={[
                  styles.iconOption,
                  { backgroundColor: icon === i ? colors.primary : (isDarkMode ? 'rgba(255,255,255,0.03)' : 'rgba(0,0,0,0.03)'), borderColor: icon === i ? colors.primary : (isDarkMode ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.05)') }
                ]}
                onPress={() => setIcon(i)}
              >
                <Ionicons name={i as any} size={20} color={icon === i ? 'white' : colors.textGrey} />
              </TouchableOpacity>
            ))}
          </View>
        </BlurView>

        <TouchableOpacity style={styles.buttonWrapper} onPress={handleSave}>
          <LinearGradient colors={['#3B82F6', '#1D4ED8']} style={styles.button}>
            <Text style={styles.buttonText}>{categoryId ? 'Save Changes' : 'Add Category'}</Text>
          </LinearGradient>
        </TouchableOpacity>
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  mainContainer: { flex: 1 },
  container: { flex: 1, padding: 20 },
  sectionCard: {
    padding: 20,
    borderRadius: 24,
    borderWidth: 1,
    marginBottom: 24,
    overflow: 'hidden',
  },
  sectionHeader: {
    marginBottom: 12,
    paddingLeft: 4,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: '900',
    letterSpacing: 0.5,
  },
  label: {
    fontSize: 10,
    fontWeight: '800',
    letterSpacing: 1.5,
    marginBottom: 10,
  },
  input: {
    borderWidth: 1,
    padding: 16,
    borderRadius: 16,
    fontSize: 16,
    fontWeight: '600',
  },
  colorGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'flex-start'
  },
  colorOption: {
    width: 44,
    height: 44,
    borderRadius: 14,
    marginRight: 10,
    marginBottom: 10,
    justifyContent: 'center',
    alignItems: 'center',
    ...SHADOWS.small,
  },
  selectedColor: {
    outlineWidth: 2,
    outlineColor: '#3B82F6',
    transform: [{ scale: 1.1 }]
  },
  iconGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'flex-start'
  },
  iconOption: {
    width: 48,
    height: 48,
    borderRadius: 14,
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 10,
    marginBottom: 10,
    borderWidth: 1,
  },
  buttonWrapper: {
    marginTop: 10,
    borderRadius: 16,
    overflow: 'hidden',
    ...SHADOWS.premium,
  },
  button: {
    padding: 18,
    alignItems: 'center',
  },
  buttonText: {
    color: 'white',
    fontSize: 16,
    fontWeight: '900',
    letterSpacing: 1,
  },
});
