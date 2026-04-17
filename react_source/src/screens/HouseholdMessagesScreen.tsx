import React, { useState, useEffect, useRef } from 'react';
import { View, Text, StyleSheet, FlatList, TextInput, TouchableOpacity, KeyboardAvoidingView, Platform, Dimensions, StatusBar, Image, ActivityIndicator, Alert } from 'react-native';
import { useAuthStore } from '../features/auth/store';
import { useHouseholdStore } from '../features/household/store';
import { useMessagesStore } from '../features/messaging/store';
import { useTheme } from '../hooks/useTheme';
import { Ionicons } from '@expo/vector-icons';
import { format, parseISO } from 'date-fns';
import { SHADOWS, COLORS } from '../constants/theme';
import { LinearGradient } from 'expo-linear-gradient';
import { BlurView } from 'expo-blur';
import * as ImagePicker from 'expo-image-picker';
import { useAudioRecorder, useAudioRecorderState, createAudioPlayer, RecordingPresets, setAudioModeAsync, requestRecordingPermissionsAsync } from 'expo-audio';
import { supabase } from '../lib/supabase';
import { secureLog } from '../lib/securityUtils';
import {
    accessibleButton,
    accessibleInput,
    accessibleHeader,
    accessibleImage
} from '../lib/accessibility';

const { width } = Dimensions.get('window');

export default function HouseholdMessagesScreen() {
    const { colors, isDarkMode } = useTheme();
    const { user } = useAuthStore();
    const { household } = useHouseholdStore();
    const { messages, addMessage, setLastNotificationCheck } = useMessagesStore();
    const [newMessage, setNewMessage] = useState('');
    const [isUploading, setIsUploading] = useState(false);
    const [isRecording, setIsRecording] = useState(false);
    const [recordingDuration, setRecordingDuration] = useState(0);
    const recorder = useAudioRecorder(RecordingPresets.HIGH_QUALITY);
    const recorderState = useAudioRecorderState(recorder);
    const soundRef = useRef<any>(null);
    const [playingId, setPlayingId] = useState<string | null>(null);

    // Sync isRecording state with recorder
    useEffect(() => {
        setIsRecording(recorderState.isRecording);
        if (recorderState.isRecording) {
            setRecordingDuration(Math.floor(recorderState.durationMillis / 1000));
        }
    }, [recorderState.isRecording, recorderState.durationMillis]);

    // Mark notifications as read when viewing this screen
    useEffect(() => {
        setLastNotificationCheck(new Date().toISOString());
    }, [setLastNotificationCheck]);

    const handleSendMessage = () => {
        if (newMessage.trim()) {
            addMessage(newMessage.trim());
            setNewMessage('');
        }
    };

    // Upload file to Supabase Storage
    const uploadMedia = async (uri: string, type: 'image' | 'voice'): Promise<string | null> => {
        try {
            const extension = type === 'image' ? 'jpg' : 'm4a';
            const fileName = `${household?.id}/${Date.now()}.${extension}`;

            const response = await fetch(uri);
            const blob = await response.blob();

            const { data, error } = await supabase.storage
                .from('chat-media')
                .upload(fileName, blob, {
                    contentType: type === 'image' ? 'image/jpeg' : 'audio/m4a',
                });

            if (error) {
                secureLog.error('Upload error:', error);
                Alert.alert('Upload Failed', error.message);
                return null;
            }

            const { data: urlData } = supabase.storage.from('chat-media').getPublicUrl(fileName);
            return urlData.publicUrl;
        } catch (error) {
            console.error('Upload error:', error);
            Alert.alert('Upload Failed', 'Could not upload media');
            return null;
        }
    };

    // Image Picker
    const pickImage = async () => {
        const { status } = await ImagePicker.requestMediaLibraryPermissionsAsync();
        if (status !== 'granted') {
            Alert.alert('Permission Required', 'Please allow access to your photo library');
            return;
        }

        const result = await ImagePicker.launchImageLibraryAsync({
            mediaTypes: ImagePicker.MediaTypeOptions.Images,
            allowsEditing: true,
            quality: 0.8,
        });

        if (!result.canceled && result.assets[0]) {
            setIsUploading(true);
            const mediaUrl = await uploadMedia(result.assets[0].uri, 'image');
            if (mediaUrl) {
                await addMessage('📷 Photo', 'image', mediaUrl);
            }
            setIsUploading(false);
        }
    };

    // Camera
    const takePhoto = async () => {
        const { status } = await ImagePicker.requestCameraPermissionsAsync();
        if (status !== 'granted') {
            Alert.alert('Permission Required', 'Please allow access to your camera');
            return;
        }

        const result = await ImagePicker.launchCameraAsync({
            allowsEditing: true,
            quality: 0.8,
        });

        if (!result.canceled && result.assets[0]) {
            setIsUploading(true);
            const mediaUrl = await uploadMedia(result.assets[0].uri, 'image');
            if (mediaUrl) {
                await addMessage('📷 Photo', 'image', mediaUrl);
            }
            setIsUploading(false);
        }
    };

    // Voice Recording
    const startRecording = async () => {
        try {
            const { granted } = await requestRecordingPermissionsAsync();
            if (!granted) {
                Alert.alert('Permission Required', 'Please allow microphone access');
                return;
            }

            await setAudioModeAsync({
                allowsRecording: true,
                playsInSilentMode: true,
            });

            await recorder.prepareToRecordAsync();
            recorder.record();
        } catch (error) {
            secureLog.error('Failed to start recording:', error);
            Alert.alert('Recording Failed', 'Could not start recording');
        }
    };

    const stopRecording = async () => {
        if (!recorder) return;

        try {
            await recorder.stop();
            const uri = recorder.uri;
            const duration = Math.max(1, Math.floor(recorderState.durationMillis / 1000));

            if (uri && duration > 0) {
                setIsUploading(true);
                const mediaUrl = await uploadMedia(uri, 'voice');
                if (mediaUrl) {
                    await addMessage('🎤 Voice message', 'voice', mediaUrl, duration);
                }
                setIsUploading(false);
            }
        } catch (error) {
            secureLog.error('Failed to stop recording:', error);
        }
    };

    // Play Voice Message
    const playVoice = async (url: string, messageId: string) => {
        try {
            if (soundRef.current) {
                soundRef.current.pause();
                soundRef.current = null;
            }

            if (playingId === messageId) {
                setPlayingId(null);
                return;
            }

            const player = createAudioPlayer(url);
            soundRef.current = player;
            setPlayingId(messageId);

            player.addListener('playbackStatusUpdate', (status) => {
                if (status.didJustFinish) {
                    setPlayingId(null);
                }
            });

            player.play();
        } catch (error) {
            secureLog.error('Play error:', error);
        }
    };

    const formatDuration = (seconds: number) => {
        const mins = Math.floor(seconds / 60);
        const secs = seconds % 60;
        return `${mins}:${secs.toString().padStart(2, '0')}`;
    };

    const renderItem = ({ item }: { item: any }) => {
        const isMe = item.senderId === user?.id;
        const messageType = item.messageType || 'text';

        return (
            <View style={[styles.messageWrapper, isMe ? styles.myMessageWrapper : styles.theirMessageWrapper]}>
                {!isMe && (
                    <View style={styles.senderHeader}>
                        <View style={[styles.senderAvatar, { backgroundColor: isDarkMode ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.05)' }]}>
                            <Text style={[styles.avatarText, { color: colors.textDark }]}>{item.senderName?.charAt(0) || '?'}</Text>
                        </View>
                        <Text style={[styles.senderName, { color: colors.textGrey }]}>{item.senderName}</Text>
                    </View>
                )}
                <BlurView intensity={isMe ? 20 : 10} tint={isDarkMode ? (isMe ? "light" : "dark") : (isMe ? "light" : "light")} style={[
                    styles.messageBubble,
                    isMe ? styles.myMessageBubble : styles.theirMessageBubble,
                    isMe && { borderColor: 'rgba(59, 130, 246, 0.3)' },
                    !isMe && { borderColor: isDarkMode ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.05)', backgroundColor: isDarkMode ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.03)' },
                    messageType === 'image' && styles.imageBubble
                ]}>
                    {messageType === 'image' && item.mediaUrl ? (
                        <Image source={{ uri: item.mediaUrl }} style={styles.messageImage} resizeMode="cover" />
                    ) : messageType === 'voice' && item.mediaUrl ? (
                        <TouchableOpacity
                            onPress={() => playVoice(item.mediaUrl, item.id)}
                            style={styles.voiceContainer}
                            {...accessibleButton(playingId === item.id ? 'Pause Voice Message' : 'Play Voice Message', `Duration: ${formatDuration(item.mediaDuration || 0)}`)}
                        >
                            <View style={[styles.playButton, { backgroundColor: isMe ? 'rgba(255,255,255,0.2)' : 'rgba(59, 130, 246, 0.2)' }]}>
                                <Ionicons name={playingId === item.id ? "pause" : "play"} size={20} color={isMe ? 'white' : '#3B82F6'} />
                            </View>
                            <View style={styles.voiceWave}>
                                {[...Array(12)].map((_, i) => (
                                    <View key={i} style={[styles.waveBar, { height: 8 + Math.random() * 16, backgroundColor: isMe ? 'rgba(255,255,255,0.4)' : 'rgba(59, 130, 246, 0.4)' }]} />
                                ))}
                            </View>
                            <Text style={[styles.voiceDuration, { color: isMe ? 'rgba(255,255,255,0.7)' : colors.textGrey }]}>
                                {formatDuration(item.mediaDuration || 0)}
                            </Text>
                        </TouchableOpacity>
                    ) : (
                        <Text style={[styles.messageText, { color: isMe ? 'white' : colors.textDark }]}>{item.content}</Text>
                    )}
                    <View style={styles.messageFooter}>
                        <Text style={[styles.messageTime, { color: isMe ? 'rgba(255,255,255,0.5)' : colors.textGrey }]}>
                            {format(parseISO(item.createdAt), 'HH:mm')}
                        </Text>
                        {isMe && <Ionicons name="checkmark-done" size={14} color="rgba(255,255,255,0.3)" style={{ marginLeft: 4 }} />}
                    </View>
                </BlurView>
            </View>
        );
    };

    return (
        <View style={[styles.container, { backgroundColor: colors.background }]}>
            <StatusBar barStyle={isDarkMode ? "light-content" : "dark-content"} />
            <LinearGradient colors={isDarkMode ? ['#0F172A', '#1E293B'] : [colors.background, colors.background]} style={StyleSheet.absoluteFill} />

            <View style={styles.header}>
                <View>
                    <Text style={[styles.headerSubtitle, { color: colors.textGrey }]}>{household?.name?.toUpperCase() || 'HOME'}</Text>
                    <Text style={[styles.headerTitle, { color: colors.textDark }]}>Messages</Text>
                </View>
                <BlurView intensity={10} tint={isDarkMode ? "dark" : "light"} style={[styles.memberCount, { borderColor: isDarkMode ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.05)' }]}>
                    <Ionicons name="people" size={14} color="#3B82F6" />
                    <Text style={styles.memberCountText}>CHAT</Text>
                </BlurView>
            </View>

            <KeyboardAvoidingView
                behavior={Platform.OS === 'ios' ? 'padding' : undefined}
                style={{ flex: 1 }}
                keyboardVerticalOffset={Platform.OS === 'ios' ? 100 : 0}
            >
                <FlatList
                    data={[...messages].reverse()}
                    renderItem={renderItem}
                    keyExtractor={(item) => item.id}
                    contentContainerStyle={styles.listContent}
                    inverted
                    showsVerticalScrollIndicator={false}
                />

                {isUploading && (
                    <View style={styles.uploadingOverlay}>
                        <ActivityIndicator size="large" color="#3B82F6" />
                        <Text style={[styles.uploadingText, { color: colors.textDark }]}>Uploading...</Text>
                    </View>
                )}

                <BlurView intensity={30} tint={isDarkMode ? "dark" : "light"} style={[styles.inputArea, { borderTopColor: isDarkMode ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.05)' }]}>
                    {isRecording ? (
                        <View style={styles.recordingContainer}>
                            <View style={styles.recordingIndicator}>
                                <View style={styles.recordingDot} />
                                <Text style={styles.recordingTime}>{formatDuration(recordingDuration)}</Text>
                            </View>
                            <Text style={[styles.recordingHint, { color: colors.textGrey }]}>Recording...</Text>
                            <TouchableOpacity
                                onPress={stopRecording}
                                style={styles.stopRecordBtn}
                                {...accessibleButton('Stop Recording', 'Stop voice recording and send')}
                            >
                                <LinearGradient colors={['#EF4444', '#DC2626']} style={styles.sendGradient}>
                                    <Ionicons name="stop" size={20} color="white" />
                                </LinearGradient>
                            </TouchableOpacity>
                        </View>
                    ) : (
                        <View style={[styles.inputWrapper, { backgroundColor: isDarkMode ? 'rgba(0,0,0,0.2)' : 'rgba(0,0,0,0.03)', borderColor: isDarkMode ? 'rgba(255,255,255,0.03)' : 'rgba(0,0,0,0.03)' }]}>
                            <TouchableOpacity
                                onPress={pickImage}
                                style={styles.mediaBtn}
                                {...accessibleButton('Pick Image', 'Select an image to send')}
                            >
                                <Ionicons name="image-outline" size={22} color={colors.textGrey} />
                            </TouchableOpacity>
                            <TouchableOpacity
                                onPress={takePhoto}
                                style={styles.mediaBtn}
                                {...accessibleButton('Take Photo', 'Open camera to take a photo')}
                            >
                                <Ionicons name="camera-outline" size={22} color={colors.textGrey} />
                            </TouchableOpacity>
                            <TextInput
                                style={[styles.input, { color: colors.textDark }]}
                                placeholder="Write a message..."
                                placeholderTextColor={isDarkMode ? "rgba(255,255,255,0.2)" : "rgba(0,0,0,0.2)"}
                                value={newMessage}
                                onChangeText={setNewMessage}
                                multiline
                                {...accessibleInput('Message input', 'Type your message')}
                            />
                            {newMessage.trim() ? (
                                <TouchableOpacity
                                    style={styles.sendBtn}
                                    onPress={handleSendMessage}
                                    {...accessibleButton('Send Message')}
                                >
                                    <LinearGradient colors={['#3B82F6', '#1D4ED8']} style={styles.sendGradient}>
                                        <Ionicons name="send" size={18} color="white" />
                                    </LinearGradient>
                                </TouchableOpacity>
                            ) : (
                                <TouchableOpacity
                                    onPress={startRecording}
                                    style={styles.sendBtn}
                                    {...accessibleButton('Record Voice', 'Hold to record voice message')}
                                >
                                    <LinearGradient colors={['#3B82F6', '#1D4ED8']} style={styles.sendGradient}>
                                        <Ionicons name="mic" size={20} color="white" />
                                    </LinearGradient>
                                </TouchableOpacity>
                            )}
                        </View>
                    )}
                </BlurView>
            </KeyboardAvoidingView>
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
        alignItems: 'flex-end',
    },
    headerSubtitle: {
        fontSize: 10,
        fontWeight: '900',
        letterSpacing: 2,
        marginBottom: 4,
    },
    headerTitle: {
        fontSize: 28,
        fontWeight: '900',
    },
    memberCount: {
        flexDirection: 'row',
        alignItems: 'center',
        paddingHorizontal: 12,
        paddingVertical: 6,
        borderRadius: 20,
        borderWidth: 1,
        overflow: 'hidden',
    },
    memberCountText: {
        fontSize: 10,
        fontWeight: '800',
        marginLeft: 6,
        color: '#3B82F6',
        letterSpacing: 1,
    },
    listContent: {
        paddingHorizontal: 16,
        paddingBottom: 20,
    },
    messageWrapper: {
        marginVertical: 4,
        maxWidth: '80%',
    },
    myMessageWrapper: {
        alignSelf: 'flex-end',
    },
    theirMessageWrapper: {
        alignSelf: 'flex-start',
    },
    senderHeader: {
        flexDirection: 'row',
        alignItems: 'center',
        marginBottom: 4,
    },
    senderAvatar: {
        width: 20,
        height: 20,
        borderRadius: 10,
        justifyContent: 'center',
        alignItems: 'center',
        marginRight: 6,
    },
    avatarText: {
        fontSize: 10,
        fontWeight: '700',
    },
    senderName: {
        fontSize: 11,
        fontWeight: '600',
    },
    messageBubble: {
        padding: 12,
        borderRadius: 18,
        borderWidth: 1,
        overflow: 'hidden',
    },
    imageBubble: {
        padding: 4,
    },
    myMessageBubble: {
        backgroundColor: 'rgba(59, 130, 246, 0.9)',
        borderBottomRightRadius: 4,
    },
    theirMessageBubble: {
        borderBottomLeftRadius: 4,
    },
    messageText: {
        fontSize: 15,
        lineHeight: 20,
    },
    messageImage: {
        width: width * 0.55,
        height: width * 0.55,
        borderRadius: 14,
    },
    voiceContainer: {
        flexDirection: 'row',
        alignItems: 'center',
        minWidth: 180,
    },
    playButton: {
        width: 36,
        height: 36,
        borderRadius: 18,
        justifyContent: 'center',
        alignItems: 'center',
    },
    voiceWave: {
        flexDirection: 'row',
        alignItems: 'center',
        marginLeft: 8,
        flex: 1,
    },
    waveBar: {
        width: 3,
        borderRadius: 2,
        marginHorizontal: 1,
    },
    voiceDuration: {
        fontSize: 12,
        marginLeft: 8,
        fontWeight: '600',
    },
    messageFooter: {
        flexDirection: 'row',
        alignItems: 'center',
        marginTop: 4,
        justifyContent: 'flex-end',
    },
    messageTime: {
        fontSize: 10,
    },
    inputArea: {
        paddingHorizontal: 16,
        paddingVertical: 12,
        paddingBottom: Platform.OS === 'ios' ? 28 : 12,
        borderTopWidth: 1,
    },
    inputWrapper: {
        flexDirection: 'row',
        alignItems: 'center',
        borderRadius: 25,
        paddingHorizontal: 8,
        borderWidth: 1,
    },
    mediaBtn: {
        padding: 8,
    },
    input: {
        flex: 1,
        paddingVertical: 10,
        paddingHorizontal: 8,
        fontSize: 15,
        maxHeight: 100,
    },
    sendBtn: {
        borderRadius: 20,
        overflow: 'hidden',
    },
    sendGradient: {
        width: 36,
        height: 36,
        borderRadius: 18,
        justifyContent: 'center',
        alignItems: 'center',
    },
    recordingContainer: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        paddingVertical: 8,
    },
    recordingIndicator: {
        flexDirection: 'row',
        alignItems: 'center',
    },
    recordingDot: {
        width: 10,
        height: 10,
        borderRadius: 5,
        backgroundColor: '#EF4444',
        marginRight: 8,
    },
    recordingTime: {
        fontSize: 18,
        fontWeight: '700',
        color: '#EF4444',
    },
    recordingHint: {
        fontSize: 14,
    },
    stopRecordBtn: {
        borderRadius: 20,
        overflow: 'hidden',
    },
    uploadingOverlay: {
        position: 'absolute',
        top: '50%',
        left: '50%',
        transform: [{ translateX: -60 }, { translateY: -40 }],
        backgroundColor: 'rgba(0,0,0,0.8)',
        paddingHorizontal: 30,
        paddingVertical: 20,
        borderRadius: 16,
        alignItems: 'center',
    },
    uploadingText: {
        marginTop: 10,
        fontSize: 14,
        fontWeight: '600',
    },
});
