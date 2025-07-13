package com.medievaldash.adplugin;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;
import android.app.Activity;

import androidx.annotation.NonNull;

import com.my.target.ads.InterstitialAd;
import com.my.target.common.models.IAdLoadingError;
import com.my.target.common.MyTargetManager;

public class AdPlugin extends CordovaPlugin {
    private static final String TAG = "AdPluginV3.3";

    private InterstitialAd interstitialAd;
    private CallbackContext pendingInitializeCallbackContext;
    private CallbackContext pendingLoadCallbackContext;
    private CallbackContext pendingShowCallbackContext;
    private int currentSlotId; // Сохраняем ID юнита
    private boolean isInterstitialCurrentlyLoaded = false; // <<< НОВЫЙ ФЛАГ

    @Override
    public void pluginInitialize() {
        Log.d(TAG, "AdPlugin: pluginInitialize called.");
    }

    private void sendJavascript(final String jsCode) {
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                webView.loadUrl(jsCode);
            }
        });
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        Log.d(TAG, "execute called with action: " + action);
        
        switch (action) {
            case "initialize":
                cordova.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            int slotId = Integer.parseInt(args.getString(0));
                            initialize(slotId);
                        } catch (JSONException e) {
                            Log.e(TAG, "Error parsing slotId: " + e.getMessage());
                            callbackContext.error("Invalid slot ID");
                        }
                }
            });
            return true;

            case "loadInterstitialAd":
                cordova.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            int slotId = Integer.parseInt(args.getString(0));
                            explicitLoadInterstitialAd(callbackContext);
                        } catch (JSONException e) {
                            Log.e(TAG, "Error parsing slotId: " + e.getMessage());
                            callbackContext.error("Invalid slot ID");
                        }
                }
            });
            return true;

            case "showInterstitialAd":
            cordova.getActivity().runOnUiThread(new Runnable() {
                    @Override
                public void run() {
                        showInterstitialAd(callbackContext);
                }
            });
            return true;

            default:
                Log.w(TAG, "Unknown action: " + action);
                return false;
            }
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
        
        // НЕ создаем InterstitialAd и НЕ загружаем рекламу при инициализации
        // Это должно решить проблему "reloading not allowed error"
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
                this.pendingShowCallbackContext = callbackContext;
        } else {
            Log.d(TAG, "Ad not loaded. Loading ad first for slot ID: " + AdPlugin.this.currentSlotId);
            Log.d(TAG, "Interstitial ad object: " + (this.interstitialAd != null ? "exists" : "null"));
            Log.d(TAG, "Is interstitial currently loaded: " + this.isInterstitialCurrentlyLoaded);
            
            // Сохраняем callback для показа после загрузки
            this.pendingShowCallbackContext = callbackContext;
            // Загружаем рекламу
            explicitLoadInterstitialAd(null);
        }
    }

    private void explicitLoadInterstitialAd(CallbackContext callbackContext) {
        Log.d(TAG, "explicitLoadInterstitialAd called on UI thread for current Slot ID: " + this.currentSlotId);
        
        if (this.currentSlotId == 0) {
            Log.e(TAG, "[ExplicitLoad] currentSlotId is 0. Cannot load ad.");
            if (callbackContext != null) {
                callbackContext.error("Cannot load ad: Slot ID is not set or invalid.");
            }
            return;
        }

        // <<< НАЧАЛО ИЗМЕНЕНИЙ >>>
        // Создаем НОВЫЙ экземпляр InterstitialAd для явной загрузки
        // Это должно помочь с ошибкой "reloading not allowed error"
        this.interstitialAd = new InterstitialAd(this.currentSlotId, cordova.getActivity());
        Log.d(TAG, "[ExplicitLoad] New InterstitialAd instance CREATED for slot ID: " + this.currentSlotId);

        // Устанавливаем тот же самый листенер
        // Важно: pendingLoadCallbackContext будет использоваться в этом листенере
        this.interstitialAd.setListener(new InterstitialAd.InterstitialAdListener() {
            @Override
            public void onLoad(InterstitialAd ad) {
                isInterstitialCurrentlyLoaded = true;
                Log.d(TAG, "[ExplicitLoad Listener] InterstitialAd loaded successfully for slot ID " + AdPlugin.this.currentSlotId);
                sendJavascript("javascript:window.adIsLoaded = true; window.adPluginError = null; console.log('[JS Ad Listener] Ad loaded successfully via EXPLICIT Java load.');");
                if (pendingLoadCallbackContext != null) {
                    pendingLoadCallbackContext.success("Ad loaded successfully (explicitly).");
                    pendingLoadCallbackContext = null; // Используем один раз
                }
            }

            @Override
            public void onNoAd(@NonNull IAdLoadingError error, InterstitialAd ad) {
                isInterstitialCurrentlyLoaded = false;
                String errorMessage = error.getMessage();
                Log.e(TAG, "[ExplicitLoad Listener] InterstitialAd loading failed for slot ID " + AdPlugin.this.currentSlotId + ": " + errorMessage);
                sendJavascript("javascript:window.adIsLoaded = false; window.adPluginError = 'Failed to load ad (explicit native): " + escapeJsString(errorMessage) + "'; console.warn('[JS Ad Listener] Ad EXPLICIT load failed via Java: " + escapeJsString(errorMessage) + "');");
                if (pendingLoadCallbackContext != null) {
                    pendingLoadCallbackContext.error("Failed to load ad explicitly: " + errorMessage);
                    pendingLoadCallbackContext = null;
                }
            }
            
            @Override
            public void onClick(InterstitialAd ad) {
                Log.d(TAG, "[ExplicitLoad Listener] InterstitialAd clicked for slot ID " + AdPlugin.this.currentSlotId);
                sendJavascript("javascript:console.log('[JS Ad Listener] Ad (from explicit load) clicked.');");
            }

            @Override
            public void onDisplay(InterstitialAd ad) {
                Log.d(TAG, "[ExplicitLoad Listener] InterstitialAd displayed for slot ID " + AdPlugin.this.currentSlotId);
                sendJavascript("javascript:console.log('[JS Ad Listener] Ad (from explicit load) displayed.');");
                 // Если showInterstitialAd был вызван и ожидает, отвечаем ему здесь
                if (pendingShowCallbackContext != null) {
                    pendingShowCallbackContext.success("Ad displayed");
                    pendingShowCallbackContext = null;
                }
            }

            @Override
            public void onDismiss(InterstitialAd ad) {
                isInterstitialCurrentlyLoaded = false;
                Log.d(TAG, "[ExplicitLoad Listener] InterstitialAd dismissed for slot ID " + AdPlugin.this.currentSlotId);
                sendJavascript("javascript:window.adIsLoaded = false; console.log('[JS Ad Listener] Ad dismissed, adIsLoaded set to false.');");
                
                // НЕ загружаем рекламу автоматически - это будет делать JavaScript
                Log.d(TAG, "[ExplicitLoad Listener] Ad dismissed - waiting for JS to request new ad");
            }

            @Override
            public void onVideoCompleted(InterstitialAd ad) {
                Log.d(TAG, "[ExplicitLoad Listener] InterstitialAd video completed for slot ID " + AdPlugin.this.currentSlotId);
            }

            
            public void onFailedToShow(InterstitialAd ad) {
                isInterstitialCurrentlyLoaded = false;
                Log.e(TAG, "[ExplicitLoad Listener] InterstitialAd failed to show for slot ID " + AdPlugin.this.currentSlotId);
                sendJavascript("javascript:window.adIsLoaded = false; window.adPluginError = 'Failed to show ad (explicit native)'; console.error('[JS Ad Listener] Ad (from explicit load) failed to show.');");
                if (pendingShowCallbackContext != null) {
                    pendingShowCallbackContext.error("Failed to show ad (from explicit load).");
                    pendingShowCallbackContext = null;
                }
            }
        });
        Log.d(TAG, "[ExplicitLoad] InterstitialAd listener RE-SET for new instance.");
        // <<< КОНЕЦ ИЗМЕНЕНИЙ >>>

        // Сохраняем callbackContext, если он еще не установлен (хотя должен быть установлен в execute)
        if (this.pendingLoadCallbackContext == null && callbackContext != null) {
            this.pendingLoadCallbackContext = callbackContext;
        } else if (callbackContext != null && this.pendingLoadCallbackContext != callbackContext) {
            // Если новый callback пришел, а старый еще не разрешен, это может быть проблемой.
            // Пока что предполагаем, что JS сторона дождется ответа перед новым вызовом load.
            Log.w(TAG, "[ExplicitLoad] A new load callback was provided while a previous one might be pending.");
            this.pendingLoadCallbackContext = callbackContext; // Используем новый
        }

        Log.d(TAG, "[ExplicitLoad] Calling interstitialAd.load() for slot ID: " + this.currentSlotId);
        this.interstitialAd.load();
        // Ответ callbackContext будет из листенера (onLoad/onNoAd)
    }

    // Вспомогательная функция для экранирования строк для JS
    private String escapeJsString(String value) {
        if (value == null) return "null";
        return value.replace("\'", "\\\'").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
} 