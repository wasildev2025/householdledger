// Mock for react-native module
module.exports = {
    Alert: {
        alert: jest.fn(),
    },
    Platform: {
        OS: 'ios',
        select: jest.fn((obj) => obj.ios),
    },
    StyleSheet: {
        create: jest.fn((styles) => styles),
    },
    View: 'View',
    Text: 'Text',
    TouchableOpacity: 'TouchableOpacity',
    TextInput: 'TextInput',
};
