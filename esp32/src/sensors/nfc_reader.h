#ifndef NFC_READER_H
#define NFC_READER_H

#include <MFRC522.h>
#include <SPI.h>
#include "../../config.h"
#include "../firebase/firebase_handler.h"

class NfcReader {
private:
    MFRC522 rfid;
    FirebaseHandler* firebase;
    const char* sensorId;
    unsigned long lastUpdate = 0;
    bool isEnabled = true;
    String lastCardId = "";

    String getCardId(MFRC522::Uid uid) {
        String cardId = "";
        for (byte i = 0; i < uid.size; i++) {
            if (uid.uidByte[i] < 0x10) {
                cardId += "0";
            }
            cardId += String(uid.uidByte[i], HEX);
        }
        cardId.toUpperCase();
        return cardId;
    }

public:
    NfcReader(FirebaseHandler* _firebase, const char* _sensorId)
        : rfid(RFID_SS_PIN, RFID_RST_PIN), firebase(_firebase), sensorId(_sensorId) {}

    void begin() {
        SPI.begin(RFID_SCK_PIN, RFID_MISO_PIN, RFID_MOSI_PIN, RFID_SS_PIN);
        rfid.PCD_Init();
        
        // Set the gain to max
        rfid.PCD_SetAntennaGain(MFRC522::RxGain_max);
        
        // Initial sensor status update
        firebase->updateSensorStatus(sensorId, isEnabled);
    }

    void update() {
        if (!isEnabled) return;

        unsigned long currentMillis = millis();

        // Check for NFC cards at regular intervals
        if (currentMillis - lastUpdate >= NFC_CHECK_INTERVAL) {
            // Reset the RFID module if needed
            if (!rfid.PCD_PerformSelfTest()) {
                rfid.PCD_Reset();
                rfid.PCD_Init();
            }

            // Look for new cards
            if (rfid.PICC_IsNewCardPresent() && rfid.PICC_ReadCardSerial()) {
                String cardId = getCardId(rfid.uid);
                
                // Only process if it's a different card
                if (cardId != lastCardId) {
                    lastCardId = cardId;
                    
                    // Update Firebase with card detection
                    firebase->updateSensorValue(sensorId, "value", 1.0);
                    
                    // Create alert for unauthorized access
                    char message[128];
                    snprintf(message, sizeof(message), 
                            "NFC Card detected: %s", cardId.c_str());
                    firebase->createAlert(sensorId, "NFC_UNAUTHORIZED", message);
                    
                    // Reset last card ID after a delay
                    delay(1000);
                    lastCardId = "";
                }
                
                rfid.PICC_HaltA();
                rfid.PCD_StopCrypto1();
            }
            
            lastUpdate = currentMillis;
        }
    }

    void setEnabled(bool enabled) {
        isEnabled = enabled;
        firebase->updateSensorStatus(sensorId, enabled);
        
        if (enabled) {
            rfid.PCD_Init();
        } else {
            rfid.PCD_SoftPowerDown();
        }
    }

    bool isActive() {
        return isEnabled;
    }

    bool isCardPresent() {
        return rfid.PICC_IsNewCardPresent();
    }

    String getLastCardId() {
        return lastCardId;
    }
};

#endif // NFC_READER_H 