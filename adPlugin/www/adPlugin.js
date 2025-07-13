var exec = require('cordova/exec');

// Глобальные флаги window.adIsLoaded, window.adPluginInitialized, window.adPluginError
// управляются из index.js и обновляются командами sendJavascript() из Java.

var AdPlugin = {
    /**
     * Initializes the ad plugin and preloads an interstitial ad.
     * @param {string} adUnitId - The Ad Unit ID for interstitial ads.
     * @returns {Promise<string>} A promise that resolves with a success message when the ad is loaded,
     *                            or rejects with an error message.
     */
    initialize: function(adUnitId) {
        console.log('[AdPlugin.js V3] initialize called with adUnitId:', adUnitId);
        return new Promise(function(resolve, reject) {
            if (!adUnitId || typeof adUnitId !== 'string') {
                console.error('[AdPlugin.js V3] Ad Unit ID (string) is required for initialization.');
                // Обновляем глобальный флаг ошибки, т.к. Java не будет вызвана
                if (typeof window !== 'undefined') { 
                    window.adPluginError = 'Ad Unit ID (string) required (JS validation)';
                    window.adPluginInitialized = false;
                }
                reject("Ad Unit ID (string) is required for initialization.");
                return;
            }
            exec(
                function(successMessage) { // Ожидаем "Native plugin initialization sequence started."
                    console.log('[AdPlugin.js V3] Native initialize success:', successMessage);
                    // Не устанавливаем window.adPluginInitialized здесь, это делает index.js
                    // Не устанавливаем window.adIsLoaded, это делает Java через sendJavascript
                    resolve(successMessage); 
                },
                function(errorMessage) {
                    console.error('[AdPlugin.js V3] Native initialize error:', errorMessage);
                    if (typeof window !== 'undefined') { 
                        window.adPluginError = errorMessage || 'Unknown native initialization error';
                        window.adPluginInitialized = false; // Явно ставим false при ошибке натива
                        window.adIsLoaded = false;
                    }
                    reject(errorMessage);
                },
                'AdPlugin',
                'initialize',
                [adUnitId]
            );
        });
    },

    loadInterstitialAd: function(adUnitId) {
        console.log('[AdPlugin.js V3] loadInterstitialAd called with adUnitId:', adUnitId);
        return new Promise(function(resolve, reject) {
            // Убираем проверку window.adPluginInitialized, так как она устанавливается из Java
            // и может быть не синхронизирована с JavaScript
            
            // Проверяем, что передан adUnitId
            if (!adUnitId) {
                console.error('[AdPlugin.js V3] Ad Unit ID is required for loadInterstitialAd.');
                reject('Ad Unit ID is required for loadInterstitialAd.');
                return;
            }
            
            exec(
                function(successMessage) { // Ожидаем "Ad loaded explicitly."
                    console.log('[AdPlugin.js V3] Native loadInterstitialAd success:', successMessage);
                    // window.adIsLoaded будет обновлен Java через sendJavascript
                    resolve(successMessage);
                },
                function(errorMessage) {
                    console.error('[AdPlugin.js V3] Native loadInterstitialAd error:', errorMessage);
                    // window.adIsLoaded и window.adPluginError будут обновлены Java через sendJavascript
                    reject(errorMessage);
                },
                'AdPlugin',
                'loadInterstitialAd',
                [adUnitId] 
            );
        });
    },

    /**
     * Shows a preloaded interstitial ad if available.
     * @returns {Promise<string>} A promise that resolves with a success message when the ad is dismissed,
     *                            or rejects with an error message if the ad fails to show or is not ready.
     */
    showInterstitialAd: function() {
        console.log('[AdPlugin.js V3] showInterstitialAd called.');
        return new Promise(function(resolve, reject) {
            // Убираем проверку window.adPluginInitialized, так как она устанавливается из Java
            // и может быть не синхронизирована с JavaScript

            exec(
                function(successMessage) { // Ожидаем "InterstitialAd dismissed."
                    console.log('[AdPlugin.js V3] Native showInterstitialAd success (dismissed):', successMessage);
                    
                    // Автоматически загружаем новую рекламу после закрытия
                    setTimeout(() => {
                        console.log('[AdPlugin.js V3] Auto-loading new ad after dismiss');
                        // Используем тот же slotId что и при инициализации
                        exec(
                            function(loadResult) {
                                console.log('[AdPlugin.js V3] Auto-load success:', loadResult);
                            },
                            function(loadError) {
                                console.error('[AdPlugin.js V3] Auto-load error:', loadError);
                            },
                            'AdPlugin',
                            'loadInterstitialAd',
                            ["1814479"] // Используем тот же slotId
                        );
                    }, 1000); // Задержка 1 секунда
                    
                    resolve(successMessage);
                },
                function(errorMessage) {
                    console.error('[AdPlugin.js V3] Native showInterstitialAd error:', errorMessage);
                    reject(errorMessage);
                },
                'AdPlugin',
                'showInterstitialAd',
                []
            );
        });
    },

    /**
     * Checks if interstitial ad is ready to show.
     * @returns {boolean} True if interstitial ad is loaded and ready.
     */
    isInterstitialAdReady: function() {
        return window.adIsLoaded || false;
    }
};

console.log('[AdPlugin.js V3] Module loaded. AdPlugin object defined.');
if (AdPlugin.initialize && AdPlugin.loadInterstitialAd && AdPlugin.showInterstitialAd) {
    console.log('[AdPlugin.js V3] Core ad functions exist on AdPlugin object.');
} else {
    console.error('[AdPlugin.js V3] One or more core ad functions are MISSING on AdPlugin object!');
}

function dispatchAdStateChanged() {
    window.dispatchEvent(new Event('adStateChanged'));
}

module.exports = AdPlugin; 