#ifndef WIFI_MANAGER_H
#define WIFI_MANAGER_H

#include <WiFi.h>
#include <time.h>
#include "../../config.h"

class WifiManager {
private:
    bool isConnected = false;
    unsigned long lastReconnectAttempt = 0;
    const unsigned long reconnectInterval = 30000; // 30 seconds

    void setupTime() {
        configTime(GMT_OFFSET_SEC, DAYLIGHT_OFFSET_SEC, NTP_SERVER);
        while (!time(nullptr)) {
            delay(500);
            Serial.print(".");
        }
        Serial.println("\nTime synchronized");
    }

public:
    bool begin() {
        Serial.println("Connecting to WiFi...");
        WiFi.mode(WIFI_STA);
        WiFi.begin(WIFI_SSID, WIFI_PASSWORD);

        unsigned long startAttempt = millis();
        while (WiFi.status() != WL_CONNECTED && millis() - startAttempt < 10000) {
            delay(500);
            Serial.print(".");
        }

        if (WiFi.status() == WL_CONNECTED) {
            Serial.println("\nWiFi connected");
            Serial.print("IP address: ");
            Serial.println(WiFi.localIP());
            isConnected = true;
            setupTime();
            return true;
        } else {
            Serial.println("\nWiFi connection failed");
            isConnected = false;
            return false;
        }
    }

    bool checkConnection() {
        if (WiFi.status() != WL_CONNECTED) {
            isConnected = false;
            unsigned long currentMillis = millis();
            
            // Try to reconnect every 30 seconds
            if (currentMillis - lastReconnectAttempt >= reconnectInterval) {
                Serial.println("Attempting to reconnect to WiFi...");
                lastReconnectAttempt = currentMillis;
                WiFi.disconnect();
                return begin();
            }
            return false;
        }
        return true;
    }

    bool isWifiConnected() {
        return isConnected;
    }

    String getLocalIP() {
        return WiFi.localIP().toString();
    }

    int getRSSI() {
        return WiFi.RSSI();
    }
};

#endif // WIFI_MANAGER_H 