/** @type {import('jest').Config} */
module.exports = {
    testEnvironment: 'node',
    roots: ['<rootDir>/__tests__'],
    testMatch: ['**/*.test.ts', '**/*.test.tsx'],
    transform: {
        '^.+\\.tsx?$': ['ts-jest', {
            useESM: false,
            tsconfig: {
                module: 'commonjs',
                target: 'ES2020',
                esModuleInterop: true,
                allowSyntheticDefaultImports: true,
                strict: true,
                skipLibCheck: true,
                jsx: 'react-native',
            },
        }],
    },
    moduleFileExtensions: ['ts', 'tsx', 'js', 'jsx', 'json', 'node'],
    setupFilesAfterEnv: ['<rootDir>/jest.setup.js'],
    collectCoverageFrom: [
        'src/**/*.{ts,tsx}',
        '!src/**/*.d.ts',
        '!src/types/**',
    ],
    moduleNameMapper: {
        '^react-native-get-random-values$': '<rootDir>/__mocks__/react-native-get-random-values.js',
        '^../config/env$': '<rootDir>/__mocks__/config.js',
        '^react-native$': '<rootDir>/__mocks__/react-native.js',
    },
};
