# ESP32 Home Security Hub

This directory contains the ESP32 firmware code for the Home Security system. The ESP32 interfaces with various sensors and communicates directly with Firebase Realtime Database, which is used by the Android app.

## Hardware Requirements

- ESP32 Development Board
- MQ-6 Gas Sensor
- Fire Sensor
- Door Magnetic Reed Switches (controlled by Arduino)
- SW-420 Vibration Sensors
- HC-SR04 Ultrasonic Sensors (x2)
- Arduino Nano/Uno with NFC-RC522 Reader
- Jumper Wires
- Breadboard/PCB
- Buzzer for alerts
- Status LEDs (Red/Green)

## Pin Configuration

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

## System Architecture

This system uses:
1. **ESP32** as the main controller for sensor reading and Firebase communication
2. **Arduino** for handling the NFC/RFID door access system
3. **Firebase Realtime Database** for communication with the Android app

The ESP32 reads sensor data, communicates with the Arduino over serial, and updates Firebase. The Android app reads from and writes to Firebase to interact with the system.

## Setup Instructions

1. Copy `config.h.example` to `config.h` and update with your credentials
2. Install required libraries:
   - Firebase ESP32 Client by Mobizt
   - ArduinoJson
3. Flash the ESP32 with the HomeSecurity.ino code
4. Flash the Arduino with the Arduino/DoorAccess.ino code (your existing Arduino code)
5. Make all connections according to the pin configuration

## Firebase Data Structure

The ESP32 updates the following nodes in Firebase Realtime Database:

```json
{
  "sensors": {
    "gas_sensor_1": {
      "value": 150.0,
      "type": "GAS",
      "location": "Kitchen",
      "isEnabled": true,
      "timestamp": 1624512345
    },
    "door_sensor_1": {
      "value": 0,
      "type": "DOOR",
      "location": "Main Door",
      "isEnabled": true,
      "isLocked": true,
      "timestamp": 1624512345
    },
    "vibration_sensor_1": {
      "value": 0.0,
      "type": "VIBRATION",
      "location": "Living Room",
      "isEnabled": true,
      "timestamp": 1624512345
    },
    "ultrasonic_sensor_1": {
      "value": 150.0,
      "type": "ULTRASONIC",
      "location": "Entrance",
      "isEnabled": true,
      "timestamp": 1624512345
    },
    "nfc_reader_1": {
      "value": 0.0,
      "type": "NFC",
      "location": "Main Door",
      "isEnabled": true,
      "timestamp": 1624512345
    }
  },
  "alerts": {
    "-NsomeLongId1": {
      "type": "GAS_LEAK", 
      "message": "High gas level detected in kitchen",
      "sensorId": "gas_sensor_1",
      "timestamp": 1624512345,
      "isAcknowledged": false
    }
  }
}
```

## Communication Protocol with Arduino

The ESP32 and Arduino communicate over Serial with the following commands:

From ESP32 to Arduino:
- `LOCK` - Lock the door
- `UNLOCK` - Unlock the door
- `ENABLE_SCAN` - Enable NFC card scanning
- `DISABLE_SCAN` - Disable NFC card scanning

From Arduino to ESP32:
- `AUTHORIZED` - Access granted with authorized NFC card
- `DENIED3` - Three failed card scanning attempts

## Troubleshooting

1. If WiFi connection fails:
   - Check WiFi credentials in config.h
   - Ensure ESP32 is within WiFi range
   - Try power cycling the ESP32

2. If Firebase connection fails:
   - Verify Firebase credentials in config.h
   - Check if Firebase rules allow write access
   - Ensure time is properly synchronized (NTP)

3. If sensors aren't reading:
   - Check wiring connections
   - Verify GPIO pin assignments
   - Test sensors individually with example sketches
   
4. If Arduino communication is not working:
   - Verify baud rate settings match (9600 baud)
   - Check RX/TX cross-connections (ESP32 TX to Arduino RX and vice versa)
   - Check serial monitor for debug messages 