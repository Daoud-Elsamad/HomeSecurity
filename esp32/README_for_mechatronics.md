# Home Security System - Combined File for Testing

This is a combined file for the mechatronics team that combines the working mechatronics code with the Firebase integration functionality.

## What This Code Does

The `HomeSecurity_Combined.ino` file combines:
1. The working mechatronics code that reads sensors and interfaces with the Arduino
2. Firebase connectivity to store sensor data and alerts in the cloud database

## Hardware Connections

The code uses the following confirmed pin connections:

| Sensor/Module | ESP32 GPIO Pins |
|--------------|-----------------|
| MQ-6 Gas     | GPIO 34 (ADC)  |
| Fire Sensor  | GPIO 35        |
| Vibration    | GPIO 33        |
| Ultrasonic 1 Trig | GPIO 25   |
| Ultrasonic 1 Echo | GPIO 26   |
| Ultrasonic 2 Trig | GPIO 27   |
| Ultrasonic 2 Echo | GPIO 14   |
| Buzzer       | GPIO 4         |
| Red LED      | GPIO 5         |
| Green LED    | GPIO 2         |
| Arduino RX   | GPIO 16        |
| Arduino TX   | GPIO 17        |

## Setup Instructions

1. Open `HomeSecurity_Combined.ino` in the Arduino IDE
2. Make sure you have the following libraries installed:
   - WiFi
   - FirebaseESP32
   - ArduinoJson
   - Preferences
   - time.h

3. Before uploading, update the WiFi credentials in the code:
   ```
   #define WIFI_SSID "Your_WiFi_SSID"  // CHANGE THIS TO YOUR WIFI NAME
   #define WIFI_PASSWORD "Your_WiFi_Password"  // CHANGE THIS TO YOUR WIFI PASSWORD
   ```

## How It Works

The code performs three main functions:
1. **Sensor Monitoring**: Reads all sensors regularly and checks for alert conditions
2. **Arduino Communication**: Exchanges messages with the Arduino that handles NFC reader
3. **Firebase Integration**: Uploads sensor data and alerts to Firebase database

### Key Features

- Code will run even if WiFi is not available (operates in offline mode)
- All Firebase operations have WiFi connection checks to ensure the system works without internet
- Alert thresholds can be adjusted in the code:
  ```
  #define GAS_THRESHOLD 900
  #define VIBRATION_THRESHOLD 5
  #define DISTANCE_THRESHOLD 20
  ```

### Arduino Communication

The ESP32 expects to communicate with an Arduino that handles the NFC/RFID door access system. 

The Arduino should send:
- "AUTHORIZED" - When an authorized NFC card is detected
- "DENIED3" - After three failed card reading attempts

## Testing

1. Connect all sensors according to the pin configuration table
2. Make sure the Arduino is connected and running the appropriate NFC reader code
3. Upload the code to your ESP32 board
4. Open Serial Monitor at 115200 baud rate to view debug messages

## Troubleshooting

1. If sensors aren't reading properly:
   - Check wiring connections
   - Verify pin assignments match the code

2. If Arduino communication isn't working:
   - Ensure the Arduino is sending "AUTHORIZED" or "DENIED3" messages
   - Check that RX/TX pins are crossed correctly (ESP32 TX to Arduino RX)
   - Verify both devices are using the same baud rate (9600) 