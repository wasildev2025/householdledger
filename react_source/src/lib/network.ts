import NetInfo from '@react-native-community/netinfo';

export type NetworkStatusCallback = (isConnected: boolean) => void;

class NetworkMonitor {
    private callbacks: Set<NetworkStatusCallback> = new Set();
    private isConnected: boolean = true;

    constructor() {
        NetInfo.addEventListener(state => {
            const connected = !!state.isConnected && !!state.isInternetReachable;
            if (this.isConnected !== connected) {
                this.isConnected = connected;
                this.notifyCallbacks();
            }
        });
    }

    public subscribe(callback: NetworkStatusCallback) {
        this.callbacks.add(callback);
        callback(this.isConnected);
        return () => this.callbacks.delete(callback);
    }

    public getStatus(): boolean {
        return this.isConnected;
    }

    private notifyCallbacks() {
        this.callbacks.forEach(callback => callback(this.isConnected));
    }
}

export const networkMonitor = new NetworkMonitor();
