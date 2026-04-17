// Jest setup file
// Add any global test setup here

// Mock console methods to reduce noise during tests
global.console = {
    ...console,
    // Uncomment to suppress logs during tests
    // log: jest.fn(),
    // warn: jest.fn(),
    // error: jest.fn(),
};

// Mock Alert for React Native
jest.mock('react-native/Libraries/Alert/Alert', () => ({
    alert: jest.fn(),
}));

// Mock AsyncStorage
jest.mock('@react-native-async-storage/async-storage', () =>
    require('@react-native-async-storage/async-storage/jest/async-storage-mock')
);
