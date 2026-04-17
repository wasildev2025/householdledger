import React, { useState } from 'react';
import { View, Text, ScrollView, StyleSheet, TouchableOpacity, Alert, Modal, TextInput, StatusBar, Platform } from 'react-native';
import { useNavigation } from '@react-navigation/native';
import { StackNavigationProp } from '@react-navigation/stack';
import { RootStackParamList, Frequency, TransactionType } from '../types';
import { Ionicons, MaterialCommunityIcons } from '@expo/vector-icons';
import { SIZES, SHADOWS, COLORS, LAYOUT } from '../constants/theme';
import { useTheme } from '../hooks/useTheme';
import { STRINGS } from '../constants/strings';
import { LinearGradient } from 'expo-linear-gradient';
import { BlurView } from 'expo-blur';
import { format } from 'date-fns';
import {
    accessibleButton,
    accessibleInput,
    accessibleHeader
} from '../lib/accessibility';

import { useAuthStore } from '../features/auth/store';
import { useRecurringStore } from '../features/transactions/recurringStore';
import { useCategoryStore } from '../features/categories/store';
import { useSettingsStore } from '../features/settings/store';

type RecurringScreenProp = StackNavigationProp<RootStackParamList, 'RecurringTransactions'>;

export default function RecurringTransactionsScreen() {
    const { colors, isDarkMode } = useTheme();
    const navigation = useNavigation<RecurringScreenProp>();

    const { profile } = useAuthStore();
    const { recurringTransactions, addRecurringTransaction, deleteRecurringTransaction } = useRecurringStore();
    const { categories } = useCategoryStore();
    const { currencySymbol } = useSettingsStore();

    const isAdmin = profile?.role === 'admin';

    const [isAddModalVisible, setIsAddModalVisible] = useState(false);
    const [amount, setAmount] = useState('');
    const [description, setDescription] = useState('');
    const [categoryId, setCategoryId] = useState('');
    const [servantId, setServantId] = useState<string | null>(null);
    const [frequency, setFrequency] = useState<Frequency>('monthly');
    const [type, setType] = useState<TransactionType>('expense');

    const handleAdd = async () => {
        if (!amount || !description) {
            Alert.alert(STRINGS.common.error, STRINGS.login.fillAllFields);
            return;
        }

        await addRecurringTransaction({
            amount: parseFloat(amount),
            description,
            type,
            categoryId: type === 'expense' ? categoryId : undefined,
            servantId: type === 'transfer' ? servantId : undefined,
            frequency,
            startDate: new Date().toISOString(),
        });

        setIsAddModalVisible(false);
        setAmount('');
        setDescription('');
        setCategoryId('');
        setServantId(null);
    };

    const handleDelete = (id: string) => {
        Alert.alert(STRINGS.recurring.deleteConfirmTitle, STRINGS.recurring.deleteConfirmMessage, [
            { text: STRINGS.recurring.cancelAction, style: 'cancel' },
            { text: STRINGS.recurring.deleteAction, style: 'destructive', onPress: () => deleteRecurringTransaction(id) }
        ]);
    };

    return (
        <View style={[styles.container, { backgroundColor: colors.background }]}>
            <StatusBar barStyle={isDarkMode ? "light-content" : "dark-content"} />
            <LinearGradient colors={isDarkMode ? ['#0F172A', '#1E293B'] : [colors.background, colors.background]} style={StyleSheet.absoluteFill} />

            <View style={styles.header}>
                <TouchableOpacity onPress={() => navigation.goBack()} style={[styles.backBtn, { backgroundColor: isDarkMode ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.05)' }]}>
                    <Ionicons name="chevron-back" size={24} color={colors.textDark} />
                </TouchableOpacity>
                <Text style={[styles.headerTitle, { color: colors.textDark }]}>{STRINGS.recurring.title}</Text>
                {isAdmin ? (
                    <TouchableOpacity
                        onPress={() => setIsAddModalVisible(true)}
                        style={[styles.addBtn, { backgroundColor: COLORS.primary }]}
                        {...accessibleButton('Create New Template', 'Add a new recurring transaction')}
                    >
                        <Ionicons name="add" size={24} color="white" />
                    </TouchableOpacity>
                ) : <View style={{ width: 44 }} />}
            </View>

            <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={styles.scrollContent}>
                {recurringTransactions.length > 0 ? (
                    recurringTransactions.map((item) => {
                        const category = categories.find(c => c.id === item.categoryId);
                        return (
                            <BlurView key={item.id} intensity={LAYOUT.blur.low} tint={isDarkMode ? "dark" : "light"} style={[styles.card, { borderColor: isDarkMode ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.05)', backgroundColor: isDarkMode ? 'transparent' : 'rgba(255,255,255,0.8)' }]}>
                                <View style={styles.cardHeader}>
                                    <View style={[styles.iconContainer, { backgroundColor: category?.color ? `${category.color}20` : 'rgba(59, 130, 246, 0.1)' }]}>
                                        <Ionicons name={category?.icon as any || 'repeat'} size={24} color={category?.color || '#3B82F6'} />
                                    </View>
                                    <View style={styles.cardInfo}>
                                        <Text style={[styles.cardTitle, { color: colors.textDark }]}>{item.description}</Text>
                                        <Text style={[styles.cardSubtitle, { color: colors.textGrey }]}>{item.frequency.toUpperCase()} • {item.type.toUpperCase()}</Text>
                                    </View>
                                    <View style={styles.cardAction}>
                                        <Text style={[styles.cardAmount, { color: colors.textDark }]}>{currencySymbol}{item.amount}</Text>
                                        {isAdmin && (
                                            <TouchableOpacity
                                                onPress={() => handleDelete(item.id)}
                                                {...accessibleButton(`Delete ${item.description}`, 'Remove this recurring transaction template')}
                                            >
                                                <Ionicons name="trash-outline" size={20} color={COLORS.danger} />
                                            </TouchableOpacity>
                                        )}
                                    </View>
                                </View>
                            </BlurView>
                        );
                    })
                ) : (
                    <View style={styles.emptyState}>
                        <MaterialCommunityIcons name="update" size={64} color={colors.textGrey} style={{ opacity: 0.3 }} />
                        <Text style={[styles.emptyText, { color: colors.textGrey }]}>{STRINGS.recurring.emptyTitle}</Text>
                        <Text style={[styles.emptySubtext, { color: colors.textGrey }]}>{STRINGS.recurring.emptySubtitle}</Text>
                    </View>
                )}
            </ScrollView>

            {/* Add Modal */}
            <Modal visible={isAddModalVisible} animationType="slide" transparent>
                <View style={styles.modalOverlay}>
                    <BlurView intensity={95} tint={isDarkMode ? "dark" : "light"} style={styles.modalContent}>
                        <View style={styles.modalHeader}>
                            <Text style={[styles.modalTitle, { color: colors.textDark }]}>{STRINGS.recurring.newTemplate}</Text>
                            <TouchableOpacity
                                onPress={() => setIsAddModalVisible(false)}
                                {...accessibleButton('Close Modal')}
                            >
                                <Ionicons name="close" size={24} color={colors.textDark} />
                            </TouchableOpacity>
                        </View>

                        <Text style={[styles.label, { color: colors.textGrey }]}>{STRINGS.common.description.toUpperCase()}</Text>
                        <TextInput
                            style={[styles.input, { color: colors.textDark, backgroundColor: isDarkMode ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.05)' }]}
                            value={description}
                            onChangeText={setDescription}
                            placeholder={STRINGS.recurring.placeholders.description}
                            placeholderTextColor={colors.textGrey}
                            {...accessibleInput('Description', 'Enter template description')}
                        />

                        <Text style={[styles.label, { color: colors.textGrey }]}>{STRINGS.common.amount.toUpperCase()} ({currencySymbol})</Text>
                        <TextInput
                            style={[styles.input, { color: colors.textDark, backgroundColor: isDarkMode ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.05)' }]}
                            value={amount}
                            onChangeText={setAmount}
                            keyboardType="decimal-pad"
                            placeholder={STRINGS.recurring.placeholders.amount}
                            placeholderTextColor={colors.textGrey}
                            {...accessibleInput('Amount', 'Enter template amount')}
                        />

                        <Text style={[styles.label, { color: colors.textGrey }]}>{STRINGS.recurring.frequency}</Text>
                        <View style={styles.freqRow}>
                            {(['daily', 'weekly', 'monthly'] as Frequency[]).map((f) => (
                                <TouchableOpacity
                                    key={f}
                                    onPress={() => setFrequency(f)}
                                    style={[styles.freqBtn, frequency === f && { backgroundColor: COLORS.primary }]}
                                    {...accessibleButton(`${f} frequency`, `Set template frequency to ${f}`)}
                                >
                                    <Text style={[styles.freqBtnText, { color: frequency === f ? 'white' : colors.textGrey }]}>{STRINGS.recurring.frequencies[f]}</Text>
                                </TouchableOpacity>
                            ))}
                        </View>

                        <TouchableOpacity
                            onPress={handleAdd}
                            style={styles.saveBtn}
                            {...accessibleButton('Activate Template', 'Save and start this recurring transaction')}
                        >
                            <LinearGradient colors={['#3B82F6', '#1D4ED8']} style={styles.saveBtnGradient}>
                                <Text style={styles.saveBtnText}>{STRINGS.recurring.activateTemplate}</Text>
                            </LinearGradient>
                        </TouchableOpacity>
                    </BlurView>
                </View>
            </Modal>
        </View >
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
    backBtn: { width: 44, height: 44, borderRadius: 16, justifyContent: 'center', alignItems: 'center' },
    headerTitle: { fontSize: 20, fontWeight: '900', letterSpacing: -0.5 },
    addBtn: { width: 44, height: 44, borderRadius: 16, justifyContent: 'center', alignItems: 'center', ...SHADOWS.small },
    scrollContent: { padding: 20 },
    card: { borderRadius: 28, padding: 20, marginBottom: 16, borderWidth: 1, overflow: 'hidden' },
    cardHeader: { flexDirection: 'row', alignItems: 'center' },
    iconContainer: { width: 52, height: 52, borderRadius: 18, justifyContent: 'center', alignItems: 'center', marginRight: 16 },
    cardInfo: { flex: 1 },
    cardTitle: { fontSize: 16, fontWeight: '700', marginBottom: 4 },
    cardSubtitle: { fontSize: 12, fontWeight: '600' },
    cardAction: { alignItems: 'flex-end', justifyContent: 'space-between', height: 50 },
    cardAmount: { fontSize: 17, fontWeight: '800' },
    emptyState: { alignItems: 'center', justifyContent: 'center', marginTop: 100 },
    emptyText: { fontSize: 18, fontWeight: '800', marginTop: 24, textAlign: 'center' },
    emptySubtext: { fontSize: 14, marginTop: 8, textAlign: 'center', opacity: 0.7 },
    modalOverlay: { flex: 1, backgroundColor: 'rgba(0,0,0,0.5)', justifyContent: 'flex-end' },
    modalContent: { borderTopLeftRadius: 40, borderTopRightRadius: 40, padding: SIZES.marginLarge, paddingBottom: 50 },
    modalHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: SIZES.marginLarge },
    modalTitle: { fontSize: 24, fontWeight: '900' },
    label: { fontSize: 10, fontWeight: '900', letterSpacing: 1, marginBottom: 12, marginTop: 20 },
    input: { borderRadius: 16, padding: 16, fontSize: 16, fontWeight: '600' },
    freqRow: { flexDirection: 'row', justifyContent: 'space-between', marginTop: 8 },
    freqBtn: { flex: 1, paddingVertical: 12, alignItems: 'center', borderRadius: 12, marginHorizontal: 4, backgroundColor: 'rgba(0,0,0,0.05)' },
    freqBtnText: { fontSize: 11, fontWeight: '800' },
    saveBtn: { marginTop: 40, borderRadius: 22, overflow: 'hidden', ...SHADOWS.premium },
    saveBtnGradient: { paddingVertical: 18, alignItems: 'center' },
    saveBtnText: { color: 'white', fontSize: 16, fontWeight: '900' }
});
