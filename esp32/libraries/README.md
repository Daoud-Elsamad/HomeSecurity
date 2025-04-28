# Required Libraries

This project requires the following Arduino libraries. Install them through the Arduino Library Manager:

1. **Firebase ESP32 Client by Mobizt**
   - Version: 4.3.0 or later
   - Used for Firebase Realtime Database communication
   - [GitHub Repository](https://github.com/mobizt/Firebase-ESP32)

2. **MFRC522**
   - Version: 1.4.10 or later
   - Used for NFC/RFID card reading
   - [GitHub Repository](https://github.com/miguelbalboa/rfid)

3. **ArduinoJson**
   - Version: 6.21.0 or later
   - Required by Firebase ESP32 Client
   - [GitHub Repository](https://github.com/bblanchon/ArduinoJson)

## Installation Instructions

1. Open Arduino IDE
2. Go to Tools > Manage Libraries...
3. Search for each library by name
4. Click Install for each required library
5. Select "Install All" if prompted for dependencies

## ESP32 Board Support

Make sure you have ESP32 board support installed:

1. Go to File > Preferences
2. Add this URL to Additional Boards Manager URLs:
   ```
   https://raw.githubusercontent.com/espressif/arduino-esp32/gh-pages/package_esp32_index.json
   ```
3. Go to Tools > Board > Boards Manager
4. Search for "esp32"
5. Install "ESP32 by Espressif Systems"

## Troubleshooting

If you encounter compilation errors:
1. Make sure all libraries are installed
2. Check library versions match or are newer than specified
3. Try restarting Arduino IDE
4. Verify ESP32 board support is properly installed 