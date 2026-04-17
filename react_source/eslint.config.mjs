import coreConfig from "eslint-config-expo/flat/utils/core.js";
import expoConfig from "eslint-config-expo/flat/utils/expo.js";
import reactConfig from "eslint-config-expo/flat/utils/react.js";
import typescriptConfig from "eslint-config-expo/flat/utils/typescript.js";
import extensions from "eslint-config-expo/flat/utils/extensions.js";
import globals from "globals";

export default [
    ...coreConfig,
    ...typescriptConfig,
    ...reactConfig,
    ...expoConfig,
    {
        settings: {
            'import/extensions': extensions.allExtensions,
            'import/resolver': {
                node: { extensions: extensions.allExtensions },
            },
        },
        languageOptions: {
            globals: {
                ...globals.browser,
                __DEV__: 'readonly',
                ErrorUtils: false,
                FormData: false,
                XMLHttpRequest: false,
                alert: false,
                cancelAnimationFrame: false,
                cancelIdleCallback: false,
                clearImmediate: false,
                fetch: false,
                navigator: false,
                process: false,
                requestAnimationFrame: false,
                requestIdleCallback: false,
                setImmediate: false,
                window: false,
                'shared-node-browser': true,
            },
        },
    },
    {
        files: ['*.web.*'],
    },
    {
        files: ["**/*.{js,jsx,ts,tsx}"],
        rules: {
            "no-console": "warn",
            "import/no-unresolved": ["error", { ignore: ["^@env$"] }],
        },
    },
];
