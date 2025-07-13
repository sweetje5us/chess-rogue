package com.medievaldash.adplugin;

import android.util.Log;
import androidx.annotation.NonNull;

// --- Только импорты InterstitialAd ---
import com.my.target.ads.InterstitialAd;
import com.my.target.ads.InterstitialAd.InterstitialAdListener;
// --- Удаляем все импорты RewardedAd ---

import com.my.target.common.MyTargetConfig;
import com.my.target.common.MyTargetManager;
import com.my.target.common.models.IAdLoadingError;
import com.my.target.common.CustomParams;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;

public class AdPlugin extends CordovaPlugin {
    private static final String TAG = "AdPluginV3.4";
    private InterstitialAd interstitialAd;
    // --- Удаляем RewardedAd переменные ---
    private CallbackContext pendingInitializeCallbackContext;
    private CallbackContext pendingLoadCallbackContext;
    private CallbackContext pendingShowCallbackContext;
    // --- Удаляем pendingRewardedShowCallbackContext ---
    private int currentSlotId; // Сохраняем ID юнита
    // --- Удаляем currentRewardedSlotId ---
    private boolean isInterstitialCurrentlyLoaded = false;
    // --- Удаляем isRewardedCurrentlyLoaded ---

    @Override
    public void pluginInitialize() {
        super.pluginInitialize();
        Log.d(TAG, "AdPlugin: pluginInitialize called.");
    }

    private void sendJavascript(final String jsCode) {
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    webView.loadUrl("javascript:" + jsCode);
                } catch (Exception e) {
                    Log.e(TAG, "Error sending JavaScript: " + e.getMessage());
                }
            }
        });
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        Log.d(TAG, "execute called with action: " + action);
        
        if ("initialize".equals(action)) {
            int slotId = args.optInt(0);
            if (slotId == 0) {
                Log.e(TAG, "Initialize: Slot ID is missing or invalid.");
                callbackContext.error("Slot ID is required for initialize.");
                return false;
            }
            this.currentSlotId = slotId;
            this.pendingInitializeCallbackContext = callbackContext;
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    initialize(currentSlotId); 
                }
            });
            return true;
        } else if ("loadInterstitialAd".equals(action)) {
            int slotId = args.optInt(0);
            if (slotId == 0) {
                Log.e(TAG, "LoadInterstitialAd: Slot ID is missing or invalid.");
                callbackContext.error("Slot ID is required for loadInterstitialAd.");
                return false;
            }
            this.pendingLoadCallbackContext = callbackContext;
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    explicitLoadInterstitialAd(slotId, pendingLoadCallbackContext);
                }
            });
            return true;
        } else if ("showInterstitialAd".equals(action)) {
            this.pendingShowCallbackContext = callbackContext;
            cordova.getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    showInterstitialAd(pendingShowCallbackContext);
                }
            });
            return true;
        }
        // --- Удаляем все методы для RewardedAd ---
        return false;
    }

    private void initialize(int slotId) {
        Log.d(TAG, "initialize runnable started on UI thread for Slot ID: " + slotId);
        
        // Проверяем, не инициализирован ли уже плагин
        if (this.currentSlotId != 0) {
            Log.w(TAG, "Plugin already initialized with slot ID: " + this.currentSlotId + ". Skipping re-initialization.");
            if (pendingInitializeCallbackContext != null) {
                pendingInitializeCallbackContext.success("Plugin already initialized.");
                pendingInitializeCallbackContext = null;
            }
            return;
        }
        
        Log.d(TAG, "Setting MyTarget debug mode...");
        MyTargetManager.setDebugMode(true);

        // Сохраняем slotId для использования в других методах
        this.currentSlotId = slotId;
        Log.d(TAG, "currentSlotId set to: " + this.currentSlotId);
        
        // Устанавливаем глобальный флаг инициализации
        Log.d(TAG, "Setting JavaScript initialization flag...");
        sendJavascript("javascript:window.adPluginInitialized = true; console.log('[JS Ad Listener] Ad plugin initialized.');");
        
        Log.d(TAG, "Plugin initialized successfully with slot ID: " + slotId);
        
        // НЕ загружаем рекламу автоматически - только инициализируем плагин
        if (pendingInitializeCallbackContext != null) {
            Log.d(TAG, "Sending success callback for initialization");
            pendingInitializeCallbackContext.success("Plugin initialized successfully.");
            pendingInitializeCallbackContext = null;
        } else {
            Log.w(TAG, "No pending initialize callback context found");
        }
    }

    private void showInterstitialAd(CallbackContext callbackContext) {
        Log.d(TAG, "showInterstitialAd runnable started on UI thread.");
        Log.d(TAG, "Current slot ID: " + this.currentSlotId);
        Log.d(TAG, "Interstitial ad object: " + (this.interstitialAd != null ? "exists" : "null"));
        Log.d(TAG, "Is interstitial currently loaded: " + this.isInterstitialCurrentlyLoaded);
        
        // Проверяем, что плагин инициализирован
        if (this.currentSlotId == 0) {
            Log.e(TAG, "ShowInterstitialAd: Plugin not initialized (currentSlotId is 0).");
            if (callbackContext != null) {
                callbackContext.error("Plugin not initialized. Call initialize() first.");
            }
            return;
        }
        
        if (this.interstitialAd != null && isInterstitialCurrentlyLoaded) {
            Log.d(TAG, "Attempting to show interstitialAd for slot ID: " + AdPlugin.this.currentSlotId);
            this.interstitialAd.show();
            // Ответ callbackContext будет в onDisplay
        } else {
            String reason = "Interstitial ad not ready";
            if (this.interstitialAd == null) reason = "interstitialAd object is null";
            else if (!isInterstitialCurrentlyLoaded) reason = "interstitial ad not loaded";
            
            Log.e(TAG, "Cannot show interstitial ad: " + reason);
            if (callbackContext != null) {
                callbackContext.error("Interstitial ad not ready: " + reason);
            }
        }
    }

    private void explicitLoadInterstitialAd(int slotId, CallbackContext callbackContext) {
        Log.d(TAG, "explicitLoadInterstitialAd called with slot ID: " + slotId);
        
        // Проверяем, что плагин инициализирован
        if (this.currentSlotId == 0) {
            Log.e(TAG, "LoadInterstitialAd: Plugin not initialized (currentSlotId is 0).");
            if (callbackContext != null) {
                callbackContext.error("Plugin not initialized. Call initialize() first.");
            }
            return;
        }
        
        // Очищаем предыдущий экземпляр, если он существует
        if (interstitialAd != null) {
            Log.d(TAG, "Clearing previous interstitial ad instance");
            interstitialAd.setListener(null);
            interstitialAd = null;
            isInterstitialCurrentlyLoaded = false;
        }
        
        // Создаем новый экземпляр InterstitialAd
        Log.d(TAG, "New InterstitialAd instance CREATED for slot ID: " + slotId);
        interstitialAd = new InterstitialAd(slotId, cordova.getActivity());
        
        // Устанавливаем слушатель
        interstitialAd.setListener(createInterstitialListener());
        
        // Загружаем рекламу
        Log.d(TAG, "Loading interstitial ad for slot ID: " + slotId);
        interstitialAd.load();
        
        // Отправляем callback о начале загрузки
        if (callbackContext != null) {
            callbackContext.success("Ad loading started.");
        }
    }

    private InterstitialAd.InterstitialAdListener createInterstitialListener() {
        return new InterstitialAd.InterstitialAdListener() {
            @Override
            public void onLoad(InterstitialAd ad) {
                Log.d(TAG, "[Interstitial Listener] InterstitialAd loaded successfully for slot ID " + currentSlotId);
                isInterstitialCurrentlyLoaded = true;
                sendJavascript("window.adIsLoaded = true; window.adPluginError = null; console.log('[JS Ad Listener] Interstitial ad loaded successfully');");
            }

            @Override
            public void onNoAd(@NonNull IAdLoadingError error, InterstitialAd ad) {
                Log.e(TAG, "[Interstitial Listener] InterstitialAd loading failed for slot ID " + currentSlotId + ": " + error.getMessage());
                isInterstitialCurrentlyLoaded = false;
                sendJavascript("window.adIsLoaded = false; window.adPluginError = '" + escapeJsString(error.getMessage()) + "'; console.warn('[JS Ad Listener] Interstitial ad load failed via Java: " + error.getMessage() + "');");
            }

            @Override
            public void onClick(InterstitialAd ad) {
                Log.d(TAG, "[Interstitial Listener] InterstitialAd clicked for slot ID " + currentSlotId);
            }

            @Override
            public void onDisplay(InterstitialAd ad) {
                Log.d(TAG, "[Interstitial Listener] InterstitialAd displayed for slot ID " + currentSlotId);
                if (pendingShowCallbackContext != null) {
                    pendingShowCallbackContext.success("InterstitialAd displayed.");
                    pendingShowCallbackContext = null;
                }
            }

            @Override
            public void onDismiss(InterstitialAd ad) {
                Log.d(TAG, "[Interstitial Listener] InterstitialAd dismissed for slot ID " + currentSlotId);
                isInterstitialCurrentlyLoaded = false;
                
                // Отправляем JavaScript событие о закрытии рекламы
                sendJavascript("window.adIsLoaded = false; console.log('[JS Ad Listener] Interstitial ad dismissed');");
                
                // Отправляем callback о закрытии рекламы
                if (pendingShowCallbackContext != null) {
                    cordova.getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                pendingShowCallbackContext.success("InterstitialAd dismissed.");
                                pendingShowCallbackContext = null;
                            } catch (Exception e) {
                                Log.e(TAG, "Error sending dismiss callback: " + e.getMessage());
                            }
                        }
                    });
                }
            }

            @Override
            public void onVideoCompleted(InterstitialAd ad) {
                Log.d(TAG, "[Interstitial Listener] InterstitialAd video completed for slot ID " + currentSlotId);
            }

            @Override
            public void onFailedToShow(InterstitialAd ad) {
                Log.e(TAG, "[Interstitial Listener] InterstitialAd failed to show for slot ID " + currentSlotId);
                isInterstitialCurrentlyLoaded = false;
                sendJavascript("window.adIsLoaded = false; window.adPluginError = 'Failed to show interstitial ad';");
                
                if (pendingShowCallbackContext != null) {
                    pendingShowCallbackContext.error("Failed to show interstitial ad.");
                    pendingShowCallbackContext = null;
                }
            }
        };
    }

    // --- Удаляем все методы для RewardedAd ---

    private String escapeJsString(String value) {
        if (value == null) return "";
        return value.replace("'", "\\'").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
} 