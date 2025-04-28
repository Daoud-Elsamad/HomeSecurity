#ifndef GAS_SENSOR_H
#define GAS_SENSOR_H

#include "../../config.h"
#include "../firebase/firebase_handler.h"

class GasSensor {
private:
    FirebaseHandler* firebase;
    const char* sensorId;
    unsigned long lastUpdate = 0;
    float lastValue = 0;
    bool isEnabled = true;

    float readRawValue() {
        // Read analog value from MQ-6 sensor
        int rawValue = analogRead(GAS_SENSOR_PIN);
        // Convert to voltage (0-3.3V range for ESP32)
        float voltage = (rawValue / 4095.0) * 3.3;
        // Convert voltage to ppm (parts per million)
        // This conversion depends on your specific MQ-6 calibration
        float ppm = voltage * 1000; // Simplified conversion
        return ppm;
    }

public:
    GasSensor(FirebaseHandler* _firebase, const char* _sensorId) 
        : firebase(_firebase), sensorId(_sensorId) {
        // Configure ADC for gas sensor
        analogSetWidth(12); // Set ADC resolution to 12 bits
        analogSetAttenuation(ADC_11db); // Set ADC attenuation for 3.3V range
    }

    void begin() {
        pinMode(GAS_SENSOR_PIN, INPUT);
        // Initial sensor status update
        firebase->updateSensorStatus(sensorId, isEnabled);
    }

    void update() {
        if (!isEnabled) return;

        unsigned long currentMillis = millis();
        if (currentMillis - lastUpdate >= GAS_UPDATE_INTERVAL) {
            float gasValue = readRawValue();
            
            // Update Firebase if value changed significantly or timeout reached
            if (abs(gasValue - lastValue) > 5.0 || currentMillis - lastUpdate >= GAS_UPDATE_INTERVAL * 2) {
                firebase->updateSensorValue(sensorId, "gas_value", gasValue);
                lastValue = gasValue;

                // Check for dangerous gas levels
                if (gasValue > GAS_THRESHOLD) {
                    char message[64];
                    snprintf(message, sizeof(message), 
                            "High gas level detected: %.1f ppm", gasValue);
                    firebase->createAlert(sensorId, "GAS_LEAK", message);
                }
            }
            
            lastUpdate = currentMillis;
        }
    }

    void setEnabled(bool enabled) {
        isEnabled = enabled;
        firebase->updateSensorStatus(sensorId, enabled);
    }

    float getCurrentValue() {
        return lastValue;
    }

    bool isActive() {
        return isEnabled;
    }
};

#endif // GAS_SENSOR_H 