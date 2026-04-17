import React, { useState, useMemo } from 'react';
import { View, Text, ScrollView, StyleSheet, TouchableOpacity, TextInput, StatusBar, Platform, Alert, Dimensions } from 'react-native';
import { useDairyStore } from '../features/dairy/store';
import { useHouseholdStore } from '../features/household/store';
import { useSettingsStore } from '../features/settings/store';
import { useTheme } from '../hooks/useTheme';
import { Ionicons, MaterialCommunityIcons } from '@expo/vector-icons';
import { LinearGradient } from 'expo-linear-gradient';
import { BlurView } from 'expo-blur';
import { format, parseISO, startOfMonth, endOfMonth, isWithinInterval } from 'date-fns';
import { SHADOWS } from '../constants/theme';
import DateTimePicker from '@react-native-community/datetimepicker';
import { VALIDATION, VALIDATION_MESSAGES } from '../constants/validation';

const { width } = Dimensions.get('window');

export default function DairyTrackerScreen() {
    const { colors, isDarkMode } = useTheme();
    const {
        dairyLogs,
        addDairyLog, deleteDairyLog
    } = useDairyStore();
    const { milkPrice, yogurtPrice, setMilkPrice, setYogurtPrice } = useHouseholdStore();
    const { currencySymbol } = useSettingsStore();

    const [milkQty, setMilkQty] = useState('');
    const [yogurtQty, setYogurtQty] = useState('');
    const [customMilkPrice, setCustomMilkPrice] = useState(milkPrice.toString());
    const [customYogurtPrice, setCustomYogurtPrice] = useState(yogurtPrice.toString());
    const [showPriceSettings, setShowPriceSettings] = useState(false);
    const [date, setDate] = useState(new Date());
    const [showDatePicker, setShowDatePicker] = useState(false);
    const [viewDate, setViewDate] = useState(new Date());

    const currentMonth = useMemo(() => {
        const start = startOfMonth(viewDate);
        const end = endOfMonth(viewDate);
        return dairyLogs.filter(log => isWithinInterval(parseISO(log.date), { start, end }));
    }, [dairyLogs, viewDate]);

    const navigateMonth = (direction: 'prev' | 'next') => {
        const newDate = new Date(viewDate);
        if (direction === 'prev') {
            newDate.setMonth(newDate.getMonth() - 1);
        } else {
            newDate.setMonth(newDate.getMonth() + 1);
        }
        setViewDate(newDate);
    };

    const stats = useMemo(() => {
        return currentMonth.reduce((acc, log) => ({
            milk: acc.milk + log.milkQuantity,
            yogurt: acc.yogurt + log.yogurtQuantity,
            total: acc.total + log.totalBill
        }), { milk: 0, yogurt: 0, total: 0 });
    }, [currentMonth]);

    const handleLogEntry = async () => {
        const mQty = parseFloat(milkQty) || 0;
        const yQty = parseFloat(yogurtQty) || 0;
        const mPrice = parseFloat(customMilkPrice) || milkPrice;
        const yPrice = parseFloat(customYogurtPrice) || yogurtPrice;

        // Validate quantities
        if (mQty === 0 && yQty === 0) {
            Alert.alert('Empty Entry', 'Please enter quantity for milk or yogurt.');
            return;
        }

        // Validate milk quantity bounds
        if (mQty < VALIDATION.MIN_QUANTITY) {
            Alert.alert('Invalid', VALIDATION_MESSAGES.QUANTITY_TOO_SMALL);
            return;
        }
        if (mQty > VALIDATION.MAX_QUANTITY) {
            Alert.alert('Invalid', VALIDATION_MESSAGES.QUANTITY_TOO_LARGE);
            return;
        }

        // Validate yogurt quantity bounds
        if (yQty < VALIDATION.MIN_QUANTITY) {
            Alert.alert('Invalid', VALIDATION_MESSAGES.QUANTITY_TOO_SMALL);
            return;
        }
        if (yQty > VALIDATION.MAX_QUANTITY) {
            Alert.alert('Invalid', VALIDATION_MESSAGES.QUANTITY_TOO_LARGE);
            return;
        }

        // Validate prices
        if (mPrice < VALIDATION.MIN_PRICE || mPrice > VALIDATION.MAX_PRICE) {
            Alert.alert('Invalid', VALIDATION_MESSAGES.PRICE_INVALID);
            return;
        }
        if (yPrice < VALIDATION.MIN_PRICE || yPrice > VALIDATION.MAX_PRICE) {
            Alert.alert('Invalid', VALIDATION_MESSAGES.PRICE_INVALID);
            return;
        }

        const totalBill = (mQty * mPrice) + (yQty * yPrice);

        await addDairyLog({
            date: date.toISOString(),
            milkQuantity: mQty,
            yogurtQuantity: yQty,
        });

        setMilkQty('');
        setYogurtQty('');
        Alert.alert('Success', 'Dairy entry logged successfully!');
    };

    const handleUpdateBasePrices = async () => {
        const mPrice = parseFloat(customMilkPrice);
        const yPrice = parseFloat(customYogurtPrice);

        // Validate prices
        if (isNaN(mPrice) || mPrice < VALIDATION.MIN_PRICE) {
            Alert.alert('Invalid', VALIDATION_MESSAGES.PRICE_TOO_SMALL);
            return;
        }
        if (mPrice > VALIDATION.MAX_PRICE) {
            Alert.alert('Invalid', VALIDATION_MESSAGES.PRICE_TOO_LARGE);
            return;
        }
        if (isNaN(yPrice) || yPrice < VALIDATION.MIN_PRICE) {
            Alert.alert('Invalid', VALIDATION_MESSAGES.PRICE_TOO_SMALL);
            return;
        }
        if (yPrice > VALIDATION.MAX_PRICE) {
            Alert.alert('Invalid', VALIDATION_MESSAGES.PRICE_TOO_LARGE);
            return;
        }

        await setMilkPrice(mPrice);
        await setYogurtPrice(yPrice);
        setShowPriceSettings(false);
        Alert.alert('Base Prices Updated', 'The new prices will be used as defaults.');
    };

    return (
        <View style={[styles.container, { backgroundColor: colors.background }]}>
            <StatusBar barStyle={isDarkMode ? 'light-content' : 'dark-content'} />
            <LinearGradient colors={isDarkMode ? ['#0F172A', '#1E293B'] : ['#F8FAFC', '#F1F5F9']} style={StyleSheet.absoluteFill} />

            <ScrollView contentContainerStyle={styles.scrollContent} showsVerticalScrollIndicator={false}>

                {/* Month Navigator */}
                <View style={styles.monthNavigator}>
                    <TouchableOpacity onPress={() => navigateMonth('prev')} style={styles.navBtn}>
                        <Ionicons name="chevron-back" size={20} color={colors.textDark} />
                    </TouchableOpacity>
                    <View style={styles.monthDisplay}>
                        <Text style={[styles.monthText, { color: colors.textDark }]}>
                            {format(viewDate, 'MMMM yyyy').toUpperCase()}
                        </Text>
                    </View>
                    <TouchableOpacity onPress={() => navigateMonth('next')} style={styles.navBtn}>
                        <Ionicons name="chevron-forward" size={20} color={colors.textDark} />
                    </TouchableOpacity>
                </View>

                {/* Summary Card */}
                <LinearGradient
                    colors={['#3B82F6', '#2563EB', '#1D4ED8']}
                    start={{ x: 0, y: 0 }}
                    end={{ x: 1, y: 1 }}
                    style={styles.statsCard}
                >
                    <View style={styles.statsHeader}>
                        <View>
                            <Text style={styles.statsLabel}>{format(viewDate, 'MMMM').toUpperCase()} DAIRY BILL</Text>
                            <Text style={styles.statsAmount}>{currencySymbol} {stats.total.toLocaleString()}</Text>
                        </View>
                        <View style={styles.statsIconBox}>
                            <MaterialCommunityIcons name="cow" size={32} color="rgba(255,255,255,0.3)" />
                        </View>
                    </View>

                    <View style={styles.statsDivider} />

                    <View style={styles.statsGrid}>
                        <View style={styles.statsItem}>
                            <Text style={styles.gridLabel}>MILK TOTAL</Text>
                            <Text style={styles.gridValue}>{stats.milk.toFixed(1)} Ltrs</Text>
                        </View>
                        <View style={styles.statsItem}>
                            <Text style={styles.gridLabel}>YOGURT TOTAL</Text>
                            <Text style={styles.gridValue}>{stats.yogurt.toFixed(1)} Kgs</Text>
                        </View>
                    </View>
                </LinearGradient>

                {/* Input Section (Only show for current month or near future/past) */}
                <View style={styles.section}>
                    <View style={styles.sectionHeader}>
                        <Text style={[styles.sectionTitle, { color: colors.textDark }]}>Log Entry</Text>
                        <View style={{ flexDirection: 'row', alignItems: 'center' }}>
                            <TouchableOpacity
                                onPress={() => setShowDatePicker(true)}
                                style={{ flexDirection: 'row', alignItems: 'center', backgroundColor: isDarkMode ? 'rgba(59, 130, 246, 0.1)' : '#EFF6FF', paddingHorizontal: 10, paddingVertical: 6, borderRadius: 10, marginRight: 12 }}
                            >
                                <Ionicons name="calendar-outline" size={16} color={colors.primary} />
                                <Text style={{ color: colors.primary, fontWeight: '700', fontSize: 13, marginLeft: 6 }}>
                                    {format(date, 'MMM dd, yyyy')}
                                </Text>
                            </TouchableOpacity>
                            <TouchableOpacity onPress={() => setShowPriceSettings(!showPriceSettings)}>
                                <Ionicons name="settings-outline" size={20} color={colors.primary} />
                            </TouchableOpacity>
                        </View>
                    </View>

                    {showDatePicker && (
                        <DateTimePicker
                            value={date}
                            mode="date"
                            display={Platform.OS === 'ios' ? 'spinner' : 'default'}
                            onChange={(event, selectedDate) => {
                                setShowDatePicker(false);
                                if (selectedDate) setDate(selectedDate);
                            }}
                        />
                    )}

                    {showPriceSettings && (
                        <BlurView intensity={20} tint={isDarkMode ? "dark" : "light"} style={styles.settingsPanel}>
                            <Text style={[styles.panelTitle, { color: colors.textDark }]}>Manage Base Prices</Text>
                            <View style={styles.inputRow}>
                                <View style={styles.halfInput}>
                                    <Text style={styles.inputLabel}>Milk / Ltr</Text>
                                    <TextInput
                                        style={[styles.input, { color: colors.textDark, backgroundColor: isDarkMode ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.03)' }]}
                                        value={customMilkPrice}
                                        onChangeText={setCustomMilkPrice}
                                        keyboardType="numeric"
                                    />
                                </View>
                                <View style={styles.halfInput}>
                                    <Text style={styles.inputLabel}>Yogurt / Kg</Text>
                                    <TextInput
                                        style={[styles.input, { color: colors.textDark, backgroundColor: isDarkMode ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.03)' }]}
                                        value={customYogurtPrice}
                                        onChangeText={setCustomYogurtPrice}
                                        keyboardType="numeric"
                                    />
                                </View>
                            </View>
                            <TouchableOpacity style={styles.updateBtn} onPress={handleUpdateBasePrices}>
                                <Text style={styles.updateBtnText}>Update Defaults</Text>
                            </TouchableOpacity>
                        </BlurView>
                    )}

                    <View style={styles.entryGrid}>
                        <View style={styles.entryCard}>
                            <View style={[styles.entryIcon, { backgroundColor: '#E0F2FE' }]}>
                                <MaterialCommunityIcons name="water" size={24} color="#0EA5E9" />
                            </View>
                            <Text style={[styles.entryLabel, { color: colors.textDark }]}>Milk (Ltrs)</Text>
                            <TextInput
                                style={[styles.entryInput, { color: colors.textDark, backgroundColor: isDarkMode ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.03)' }]}
                                placeholder="0.0"
                                placeholderTextColor={colors.textGrey}
                                value={milkQty}
                                onChangeText={setMilkQty}
                                keyboardType="numeric"
                            />
                            <Text style={styles.priceHint}>At {currencySymbol}{customMilkPrice}/L</Text>
                        </View>

                        <View style={styles.entryCard}>
                            <View style={[styles.entryIcon, { backgroundColor: '#F0FDF4' }]}>
                                <MaterialCommunityIcons name="spoon-sugar" size={24} color="#22C55E" />
                            </View>
                            <Text style={[styles.entryLabel, { color: colors.textDark }]}>Yogurt (Kgs)</Text>
                            <TextInput
                                style={[styles.entryInput, { color: colors.textDark, backgroundColor: isDarkMode ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.03)' }]}
                                placeholder="0.0"
                                placeholderTextColor={colors.textGrey}
                                value={yogurtQty}
                                onChangeText={setYogurtQty}
                                keyboardType="numeric"
                            />
                            <Text style={styles.priceHint}>At {currencySymbol}{customYogurtPrice}/K</Text>
                        </View>
                    </View>

                    <TouchableOpacity style={styles.logBtn} onPress={handleLogEntry}>
                        <LinearGradient colors={['#3B82F6', '#1D4ED8']} style={styles.logBtnGradient}>
                            <Text style={styles.logBtnText}>Save Entry</Text>
                            <Ionicons name="checkmark-circle" size={20} color="white" style={{ marginLeft: 8 }} />
                        </LinearGradient>
                    </TouchableOpacity>
                </View>

                {/* History Section */}
                <View style={styles.section}>
                    <Text style={[styles.sectionTitle, { color: colors.textDark, marginBottom: 16 }]}>History for {format(viewDate, 'MMMM')}</Text>
                    {currentMonth.length === 0 ? (
                        <View style={styles.emptyState}>
                            <MaterialCommunityIcons name="calendar-blank" size={48} color={isDarkMode ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.1)'} />
                            <Text style={{ color: colors.textGrey, marginTop: 12 }}>No entries for this month.</Text>
                        </View>
                    ) : (
                        currentMonth.map(log => (
                            <View key={log.id} style={[styles.historyItem, { backgroundColor: isDarkMode ? 'rgba(255,255,255,0.03)' : 'white' }]}>
                                <View style={styles.historyInfo}>
                                    <Text style={[styles.historyDate, { color: colors.textDark }]}>{format(parseISO(log.date), 'EEE, d MMM')}</Text>
                                    <View style={styles.historyDetails}>
                                        {log.milkQuantity > 0 && <Text style={styles.historyDetailText}>🥛 {log.milkQuantity}L</Text>}
                                        {log.yogurtQuantity > 0 && <Text style={styles.historyDetailText}>🍦 {log.yogurtQuantity}Kg</Text>}
                                    </View>
                                </View>
                                <View style={{ alignItems: 'flex-end' }}>
                                    <Text style={[styles.historyTotal, { color: colors.primary }]}>{currencySymbol}{log.totalBill.toLocaleString()}</Text>
                                    <TouchableOpacity onPress={() => deleteDairyLog(log.id)}>
                                        <Ionicons name="trash-outline" size={16} color="#EF4444" style={{ marginTop: 4 }} />
                                    </TouchableOpacity>
                                </View>
                            </View>
                        ))
                    )}
                </View>

            </ScrollView>
        </View>
    );
}

const styles = StyleSheet.create({
    container: { flex: 1 },
    scrollContent: { padding: 20, paddingBottom: 100 },
    statsCard: {
        padding: 24,
        borderRadius: 32,
        marginBottom: 24,
        ...SHADOWS.premium,
    },
    statsHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
    statsLabel: { color: 'rgba(255,255,255,0.7)', fontSize: 12, fontWeight: '800', letterSpacing: 1 },
    statsAmount: { color: 'white', fontSize: 32, fontWeight: '900', marginTop: 4 },
    statsIconBox: { width: 48, height: 48, borderRadius: 16, backgroundColor: 'rgba(255,255,255,0.1)', justifyContent: 'center', alignItems: 'center' },
    statsDivider: { height: 1, backgroundColor: 'rgba(255,255,255,0.1)', marginVertical: 20 },
    statsGrid: { flexDirection: 'row' },
    statsItem: { flex: 1 },
    gridLabel: { color: 'rgba(255,255,255,0.6)', fontSize: 10, fontWeight: '700', letterSpacing: 0.5 },
    gridValue: { color: 'white', fontSize: 18, fontWeight: '800', marginTop: 2 },

    monthNavigator: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        marginBottom: 16,
        paddingHorizontal: 8,
    },
    navBtn: {
        width: 36,
        height: 36,
        borderRadius: 12,
        backgroundColor: 'rgba(0,0,0,0.03)',
        justifyContent: 'center',
        alignItems: 'center',
    },
    monthDisplay: {
        flex: 1,
        alignItems: 'center',
    },
    monthText: {
        fontSize: 16,
        fontWeight: '900',
        letterSpacing: 1,
    },

    section: { marginBottom: 32 },
    sectionHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 },
    sectionTitle: { fontSize: 20, fontWeight: '900', letterSpacing: -0.5 },

    settingsPanel: {
        padding: 16,
        borderRadius: 20,
        marginBottom: 20,
        borderWidth: 1,
        borderColor: 'rgba(59, 130, 246, 0.2)',
        overflow: 'hidden'
    },
    panelTitle: { fontSize: 14, fontWeight: '800', marginBottom: 12 },
    inputRow: { flexDirection: 'row', justifyContent: 'space-between' },
    halfInput: { width: '48%' },
    inputLabel: { fontSize: 11, fontWeight: '700', color: '#888', marginBottom: 6 },
    input: { height: 44, borderRadius: 12, paddingHorizontal: 12, fontSize: 15, fontWeight: '700' },
    updateBtn: { backgroundColor: '#3B82F6', height: 40, borderRadius: 10, justifyContent: 'center', alignItems: 'center', marginTop: 12 },
    updateBtnText: { color: 'white', fontWeight: '800', fontSize: 12 },

    entryGrid: { flexDirection: 'row', justifyContent: 'space-between', marginBottom: 20 },
    entryCard: {
        width: (width - 60) / 2,
        padding: 16,
        borderRadius: 24,
        backgroundColor: 'white',
        alignItems: 'center',
        ...SHADOWS.small,
        borderWidth: 1,
        borderColor: 'rgba(0,0,0,0.03)'
    },
    entryIcon: { width: 44, height: 44, borderRadius: 14, justifyContent: 'center', alignItems: 'center', marginBottom: 12 },
    entryLabel: { fontSize: 13, fontWeight: '800', marginBottom: 12 },
    entryInput: { width: '100%', height: 48, borderRadius: 12, textAlign: 'center', fontSize: 20, fontWeight: '900' },
    priceHint: { fontSize: 10, color: '#888', marginTop: 8, fontWeight: '600' },

    logBtn: { borderRadius: 18, overflow: 'hidden', ...SHADOWS.medium },
    logBtnGradient: { height: 56, flexDirection: 'row', justifyContent: 'center', alignItems: 'center' },
    logBtnText: { color: 'white', fontSize: 16, fontWeight: '900' },

    historyItem: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        padding: 16,
        borderRadius: 20,
        marginBottom: 12,
        ...SHADOWS.small,
        borderWidth: 1,
        borderColor: 'rgba(0,0,0,0.02)'
    },
    historyInfo: { flex: 1 },
    historyDate: { fontSize: 14, fontWeight: '800' },
    historyDetails: { flexDirection: 'row', marginTop: 6 },
    historyDetailText: { fontSize: 12, fontWeight: '600', color: '#888', marginRight: 12 },
    historyTotal: { fontSize: 16, fontWeight: '900' },
    emptyState: { alignItems: 'center', marginTop: 20 }
});
