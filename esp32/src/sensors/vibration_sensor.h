#ifndef VIBRATION_SENSOR_H
#define VIBRATION_SENSOR_H

#include "../../config.h"
#include "../firebase/firebase_handler.h"

class VibrationSensor {
private:
    FirebaseHandler* firebase;
    const char* sensorId;
    unsigned long lastUpdate = 0;
    unsigned long lastVibration = 0;
    bool isEnabled = true;
    int vibrationCount = 0;
    const unsigned long VIBRATION_WINDOW = 5000; // 5 second window for counting vibrations

    volatile bool vibrationDetected = false;

    static void IRAM_ATTR handleInterrupt() {
        vibrationDetected = true;
    }

public:
    VibrationSensor(FirebaseHandler* _firebase, const char* _sensorId) 
        : firebase(_firebase), sensorId(_sensorId) {}

    void begin() {
        pinMode(VIBRATION_SENSOR_PIN, INPUT);
        attachInterrupt(digitalPinToInterrupt(VIBRATION_SENSOR_PIN), 
                       handleInterrupt, RISING);
        
        // Initial sensor status update
        firebase->updateSensorStatus(sensorId, isEnabled);
    }

    void update() {
        if (!isEnabled) return;

        unsigned long currentMillis = millis();

        // Handle vibration detection
        if (vibrationDetected) {
            vibrationCount++;
            vibrationDetected = false;
            lastVibration = currentMillis;

            // Create alert if vibration exceeds threshold
            if (vibrationCount > VIBRATION_THRESHOLD) {
                char message[64];
                snprintf(message, sizeof(message), 
                        "High vibration detected: %d hits", vibrationCount);
                firebase->createAlert(sensorId, "VIBRATION_DETECTED", message);
            }
        }

        // Reset vibration count after window expires
        if (currentMillis - lastVibration > VIBRATION_WINDOW) {
            vibrationCount = 0;
        }

        // Update Firebase at regular intervals
        if (currentMillis - lastUpdate >= VIBRATION_UPDATE_INTERVAL) {
            // Calculate vibrations per second
            float vibrationRate = (float)vibrationCount / (VIBRATION_WINDOW / 1000.0);
            firebase->updateSensorValue(sensorId, "value", vibrationRate);
            lastUpdate = currentMillis;
        }
    }

    void setEnabled(bool enabled) {
        isEnabled = enabled;
        firebase->updateSensorStatus(sensorId, enabled);
        
        if (enabled) {
            attachInterrupt(digitalPinToInterrupt(VIBRATION_SENSOR_PIN), 
                          handleInterrupt, RISING);
        } else {
            detachInterrupt(digitalPinToInterrupt(VIBRATION_SENSOR_PIN));
        }
    }

    int getVibrationCount() {
        return vibrationCount;
    }

    float getVibrationRate() {
        return (float)vibrationCount / (VIBRATION_WINDOW / 1000.0);
    }

    bool isActive() {
        return isEnabled;
    }
};

#endif // VIBRATION_SENSOR_H 