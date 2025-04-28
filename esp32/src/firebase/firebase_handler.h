#ifndef FIREBASE_HANDLER_H
#define FIREBASE_HANDLER_H

#include <FirebaseESP32.h>
#include "../../config.h"
#include "../utils/wifi_manager.h"

FirebaseData fbdo;
FirebaseAuth auth;
FirebaseConfig fbConfig;

class FirebaseHandler {
private:
    WifiManager* wifiManager;
    bool isInitialized = false;

    String generateSensorPath(const char* sensorId) {
        return String("/sensors/") + sensorId;
    }

public:
    FirebaseHandler(WifiManager* _wifiManager) : wifiManager(_wifiManager) {}

    bool begin() {
        if (!wifiManager->isWifiConnected()) {
            Serial.println("WiFi not connected. Cannot initialize Firebase.");
            return false;
        }

        fbConfig.host = FIREBASE_HOST;
        fbConfig.api_key = FIREBASE_AUTH;
        
        Firebase.begin(&fbConfig, &auth);
        Firebase.reconnectWiFi(true);

        // Set database read timeout to 1 minute
        Firebase.setReadTimeout(fbdo, 1000 * 60);
        // Size and its write timeout e.g. tiny (1s), small (10s), medium (30s) and large (60s).
        Firebase.setwriteSizeLimit(fbdo, "tiny");

        isInitialized = true;
        Serial.println("Firebase initialized");
        return true;
    }

    bool updateSensorValue(const char* sensorId, const char* field, float value) {
        if (!isInitialized || !wifiManager->isWifiConnected()) {
            return false;
        }

        String path = generateSensorPath(sensorId) + "/" + field;
        if (Firebase.setFloat(fbdo, path, value)) {
            return true;
        } else {
            Serial.println("Failed to update sensor value");
            Serial.println("REASON: " + fbdo.errorReason());
            return false;
        }
    }

    bool updateSensorStatus(const char* sensorId, bool isEnabled) {
        if (!isInitialized || !wifiManager->isWifiConnected()) {
            return false;
        }

        String path = generateSensorPath(sensorId) + "/isEnabled";
        if (Firebase.setBool(fbdo, path, isEnabled)) {
            return true;
        } else {
            Serial.println("Failed to update sensor status");
            Serial.println("REASON: " + fbdo.errorReason());
            return false;
        }
    }

    bool updateDoorLock(const char* doorId, bool isLocked) {
        if (!isInitialized || !wifiManager->isWifiConnected()) {
            return false;
        }

        String path = generateSensorPath(doorId) + "/isLocked";
        if (Firebase.setBool(fbdo, path, isLocked)) {
            return true;
        } else {
            Serial.println("Failed to update door lock status");
            Serial.println("REASON: " + fbdo.errorReason());
            return false;
        }
    }

    bool createAlert(const char* sensorId, const char* alertType, const char* message) {
        if (!isInitialized || !wifiManager->isWifiConnected()) {
            return false;
        }

        FirebaseJson json;
        json.set("type", alertType);
        json.set("message", message);
        json.set("timestamp", (int)time(nullptr));
        json.set("sensorId", sensorId);
        json.set("isAcknowledged", false);

        String path = "/alerts/" + String(random(0xFFFFFFFF));
        if (Firebase.pushJSON(fbdo, path, json)) {
            return true;
        } else {
            Serial.println("Failed to create alert");
            Serial.println("REASON: " + fbdo.errorReason());
            return false;
        }
    }

    bool isFirebaseConnected() {
        return isInitialized && wifiManager->isWifiConnected();
    }
};

#endif // FIREBASE_HANDLER_H 