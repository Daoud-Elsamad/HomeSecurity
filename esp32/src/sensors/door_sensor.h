#ifndef DOOR_SENSOR_H
#define DOOR_SENSOR_H

#include "../../config.h"
#include "../firebase/firebase_handler.h"

class DoorSensor {
private:
    FirebaseHandler* firebase;
    const char* sensorId;
    unsigned long lastUpdate = 0;
    unsigned long doorOpenTime = 0;
    bool isEnabled = true;
    bool isLocked = false;
    bool lastState = false; // false = closed, true = open

    bool readDoorState() {
        // Reed switch is typically LOW when door is closed (magnet near)
        return digitalRead(DOOR_SENSOR_PIN) == HIGH;
    }

public:
    DoorSensor(FirebaseHandler* _firebase, const char* _sensorId) 
        : firebase(_firebase), sensorId(_sensorId) {}

    void begin() {
        pinMode(DOOR_SENSOR_PIN, INPUT_PULLUP);
        // Initial status updates
        firebase->updateSensorStatus(sensorId, isEnabled);
        firebase->updateDoorLock(sensorId, isLocked);
    }

    void update() {
        if (!isEnabled) return;

        unsigned long currentMillis = millis();
        bool currentState = readDoorState();

        // Check for state change or update interval
        if (currentState != lastState || currentMillis - lastUpdate >= DOOR_UPDATE_INTERVAL) {
            // Update door state in Firebase
            firebase->updateSensorValue(sensorId, "value", currentState ? 1.0 : 0.0);
            
            // Handle door state changes
            if (currentState != lastState) {
                if (currentState) { // Door opened
                    doorOpenTime = currentMillis;
                    
                    // Check if door was locked
                    if (isLocked) {
                        firebase->createAlert(sensorId, "DOOR_UNAUTHORIZED", 
                            "Door opened while locked!");
                    }
                } else { // Door closed
                    doorOpenTime = 0;
                }
                lastState = currentState;
            }
            
            // Check if door has been open too long
            if (currentState && doorOpenTime > 0) {
                unsigned long openDuration = currentMillis - doorOpenTime;
                if (openDuration > DOOR_OPEN_THRESHOLD) {
                    char message[64];
                    snprintf(message, sizeof(message), 
                            "Door left open for %d seconds", 
                            (int)(openDuration / 1000));
                    firebase->createAlert(sensorId, "DOOR_LEFT_OPEN", message);
                }
            }
            
            lastUpdate = currentMillis;
        }
    }

    void setLocked(bool locked) {
        isLocked = locked;
        firebase->updateDoorLock(sensorId, locked);
        
        // If door is being locked while open, create an alert
        if (locked && lastState) {
            firebase->createAlert(sensorId, "DOOR_LEFT_OPEN", 
                "Attempting to lock while door is open!");
        }
    }

    void setEnabled(bool enabled) {
        isEnabled = enabled;
        firebase->updateSensorStatus(sensorId, enabled);
    }

    bool isOpen() {
        return lastState;
    }

    bool isDoorLocked() {
        return isLocked;
    }

    bool isActive() {
        return isEnabled;
    }

    unsigned long getOpenDuration() {
        if (!lastState) return 0;
        return doorOpenTime > 0 ? (millis() - doorOpenTime) : 0;
    }
};

#endif // DOOR_SENSOR_H 