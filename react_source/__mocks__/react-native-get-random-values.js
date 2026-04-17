// Mock for react-native-get-random-values
// Required for crypto.getRandomValues in test environment

const crypto = require('crypto');

// Mock crypto.getRandomValues
global.crypto = {
    getRandomValues: function (buffer) {
        return crypto.randomFillSync(buffer);
    },
    subtle: {
        digest: async function (algorithm, data) {
            const hash = crypto.createHash('sha256');
            hash.update(Buffer.from(data));
            return hash.digest().buffer;
        },
    },
};

module.exports = {};
