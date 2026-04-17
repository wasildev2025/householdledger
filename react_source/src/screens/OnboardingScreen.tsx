import React, { useState } from 'react';
import { View, Text, StyleSheet, Dimensions, FlatList, TouchableOpacity, StatusBar, Platform } from 'react-native';
import { useNavigation } from '@react-navigation/native';
import { useTheme } from '../hooks/useTheme';
import { Ionicons } from '@expo/vector-icons';
import { SHADOWS, COLORS } from '../constants/theme';
import { LinearGradient } from 'expo-linear-gradient';
import { BlurView } from 'expo-blur';

const { width, height } = Dimensions.get('window');

const SLIDES = [
  {
    id: '1',
    title: 'SMART HOME FINANCE',
    description: 'Manage your household expenses and budget in one simple place.',
    icon: 'cube',
    color: '#3B82F6'
  },
  {
    id: '2',
    title: 'MANAGE STAFF',
    description: 'Give your staff budgets and track their spending easily.',
    icon: 'wallet',
    color: '#10B981'
  },
  {
    id: '3',
    title: 'FAMILY SHARING',
    description: 'Keep everyone in the family in sync with shared tracking.',
    icon: 'infinite',
    color: '#8B5CF6'
  },
  {
    id: '4',
    title: 'CLEAR REPORTS',
    description: 'See where your money goes with clear, simple charts.',
    icon: 'analytics',
    color: '#6366F1'
  }
];

export default function OnboardingScreen() {
  const navigation = useNavigation<any>();
  const { colors, isDarkMode } = useTheme();
  const [currentIndex, setCurrentIndex] = useState(0);
  const ref = React.useRef<FlatList>(null);

  const onViewableItemsChanged = React.useRef(({ viewableItems }: any) => {
    if (viewableItems && viewableItems.length > 0) {
      setCurrentIndex(viewableItems[0].index);
    }
  }).current;

  const viewabilityConfig = React.useRef({
    itemVisiblePercentThreshold: 50
  }).current;

  const goNextSlide = () => {
    const nextSlideIndex = currentIndex + 1;
    if (nextSlideIndex != SLIDES.length) {
      ref?.current?.scrollToIndex({ index: nextSlideIndex, animated: true });
    }
  };

  const skip = () => {
    navigation.replace('Login');
  };

  const Slide = ({ item, isLast }: { item: typeof SLIDES[0], isLast: boolean }) => {
    return (
      <View style={styles.slideContainer}>
        <View style={styles.iconWrapper}>
          <LinearGradient
            colors={[`${item.color}30`, 'transparent']}
            style={styles.iconGradient}
          />
          <Ionicons name={item.icon as any} size={140} color={item.color} />
        </View>

        <View style={styles.textContainer}>
          <Text style={[styles.title, { color: colors.textDark }]}>{item.title}</Text>
          <Text style={[styles.subtitle, { color: colors.textGrey }]}>{item.description}</Text>
        </View>

        {isLast && (
          <TouchableOpacity
            style={styles.getStartedBtn}
            onPress={() => navigation.replace('Login')}
            activeOpacity={0.8}
          >
            <LinearGradient colors={['#3B82F6', '#1D4ED8']} style={styles.btnGradient}>
              <Text style={styles.btnText}>GET STARTED</Text>
              <Ionicons name="arrow-forward" size={18} color="white" style={{ marginLeft: 12 }} />
            </LinearGradient>
          </TouchableOpacity>
        )}
      </View>
    );
  };

  return (
    <View style={[styles.container, { backgroundColor: colors.background }]}>
      <StatusBar barStyle={isDarkMode ? "light-content" : "dark-content"} />
      <LinearGradient
        colors={isDarkMode ? ['#020617', '#0F172A'] : ['#F8FAFC', '#F1F5F9', '#CBD5E1']}
        style={StyleSheet.absoluteFill}
      />

      <FlatList
        ref={ref}
        onViewableItemsChanged={onViewableItemsChanged}
        viewabilityConfig={viewabilityConfig}
        showsHorizontalScrollIndicator={false}
        horizontal
        data={SLIDES}
        pagingEnabled
        renderItem={({ item, index }) => <Slide item={item} isLast={index === SLIDES.length - 1} />}
        keyExtractor={(item) => item.id}
      />

      <View style={styles.footer}>
        <View style={styles.indicatorRow}>
          {SLIDES.map((_, index) => (
            <View
              key={index}
              style={[
                styles.indicator,
                { backgroundColor: isDarkMode ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.1)' },
                currentIndex === index && styles.indicatorActive,
              ]}
            />
          ))}
        </View>

        {currentIndex < SLIDES.length - 1 && (
          <View style={styles.navRow}>
            <TouchableOpacity onPress={skip} style={styles.skipBtn}>
              <Text style={[styles.skipText, { color: colors.textGrey }]}>SKIP</Text>
            </TouchableOpacity>

            <TouchableOpacity onPress={goNextSlide} style={styles.nextBtn}>
              <BlurView intensity={Platform.OS === 'ios' ? 20 : 100} tint={isDarkMode ? "dark" : "light"} style={styles.nextBtnBlur}>
                <Text style={[styles.nextText, { color: colors.textDark }]}>NEXT</Text>
                <Ionicons name="chevron-forward" size={16} color={colors.textDark} />
              </BlurView>
            </TouchableOpacity>
          </View>
        )}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  slideContainer: { width, padding: 40, alignItems: 'center', justifyContent: 'center' },
  iconWrapper: {
    width: width * 0.7,
    height: width * 0.7,
    borderRadius: (width * 0.7) / 2,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 60,
  },
  iconGradient: {
    ...StyleSheet.absoluteFillObject,
    borderRadius: (width * 0.7) / 2,
  },
  textContainer: {
    alignItems: 'center',
    paddingHorizontal: 10,
  },
  title: {
    fontSize: 26,
    fontWeight: '900',
    textAlign: 'center',
    letterSpacing: 1.5,
  },
  subtitle: {
    fontSize: 14,
    textAlign: 'center',
    marginTop: 16,
    lineHeight: 22,
    fontWeight: '600',
    paddingHorizontal: 20,
  },
  getStartedBtn: {
    marginTop: 60,
    width: '100%',
    borderRadius: 16,
    overflow: 'hidden',
    ...SHADOWS.premium,
  },
  btnGradient: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 20,
  },
  btnText: {
    color: 'white',
    fontSize: 15,
    fontWeight: '900',
    letterSpacing: 2,
  },
  footer: {
    position: 'absolute',
    bottom: Platform.OS === 'ios' ? 60 : 40,
    left: 0,
    right: 0,
    paddingHorizontal: 40,
  },
  indicatorRow: {
    flexDirection: 'row',
    justifyContent: 'center',
    marginBottom: 40,
  },
  indicator: {
    height: 4,
    width: 12,
    marginHorizontal: 4,
    borderRadius: 2,
  },
  indicatorActive: {
    backgroundColor: '#3B82F6',
    width: 32,
  },
  navRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  skipBtn: {
    padding: 10,
  },
  skipText: {
    fontSize: 12,
    fontWeight: '900',
    letterSpacing: 1,
  },
  nextBtn: {
    borderRadius: 12,
    overflow: 'hidden',
  },
  nextBtnBlur: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 12,
    paddingHorizontal: 24,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.1)',
    backgroundColor: 'rgba(255,255,255,0.05)',
  },
  nextText: {
    fontSize: 13,
    fontWeight: '900',
    marginRight: 6,
    letterSpacing: 1,
  }
});
