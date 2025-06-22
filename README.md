# Home Security System

A comprehensive IoT-based home security system that combines hardware sensors, mobile app control, and cloud connectivity to provide real-time monitoring and alert capabilities.

## ⚠️ Setup Required

**This is a demonstration project.** To run this system, you need to configure the following:

### Required Configuration:
1. **WiFi Credentials**: Update `esp32/HomeSecurity_Combined.ino` with your WiFi SSID and password
2. **Firebase Setup**: 
   - Create a Firebase project
   - Download `google-services.json` from Firebase Console and place in `app/` directory
   - Update Firebase API key in ESP32 code
3. **Hardware**: Connect sensors according to pin definitions in the code

### Quick Start:
1. Copy `esp32/config.h.example` to `esp32/config.h` and update credentials
2. Follow the setup instructions in each component's README

---

## 🏠 System Overview

This project consists of three main components:
- **Android Mobile App** - User interface for monitoring and control
- **ESP32 Hub** - Central sensor management and Firebase communication
- **Arduino Door Controller** - NFC-based access control system

The system monitors various environmental and security parameters including gas levels, fire detection, vibration, proximity, and door access, with real-time alerts and remote control capabilities.

## 📱 Features

### Mobile App Features
- Real-time sensor monitoring dashboard
- Push notifications for security alerts
- Remote door lock/unlock control
- Historical data and analytics
- User authentication and access control
- Customizable alert thresholds

### Hardware Features
- **Gas Detection** - MQ-6 sensor for gas leak monitoring
- **Fire Detection** - Fire sensor for early fire warning
- **Intrusion Detection** - Vibration sensors and ultrasonic proximity detection
- **Access Control** - NFC/RFID card-based door access
- **Visual/Audio Alerts** - LED indicators and buzzer notifications
- **Cloud Connectivity** - Firebase integration for remote monitoring

## 🛠️ Technical Stack

### Android App
- **Language**: Kotlin
- **Architecture**: MVVM with Repository pattern
- **UI**: Material Design with View Binding
- **Backend**: Firebase (Realtime Database, Authentication, Analytics)
- **Dependency Injection**: Hilt
- **Async Processing**: Kotlin Coroutines
- **Navigation**: Navigation Component

### Hardware
- **Main Controller**: ESP32 Development Board
- **Door Controller**: Arduino Nano/Uno
- **Sensors**: MQ-6 Gas, Fire Sensor, SW-420 Vibration, HC-SR04 Ultrasonic
- **Access Control**: NFC-RC522 RFID Reader
- **Actuators**: Servo Motor for door lock
- **Display**: 16x2 LCD with I2C
- **Communication**: WiFi (ESP32), Serial (Arduino-ESP32)

### Cloud Services
- **Database**: Firebase Realtime Database
- **Authentication**: Firebase Auth
- **Analytics**: Firebase Analytics
- **Security Rules**: Firestore security rules

## 🏗️ System Architecture

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│                 │    │                  │    │                 │
│   Android App   │◄──►│   Firebase       │◄──►│   ESP32 Hub     │
│                 │    │   Cloud          │    │                 │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                                                        │
                                                        │ Serial
                                                        │ UART
                                                        ▼
                                                ┌─────────────────┐
                                                │                 │
                                                │ Arduino Door    │
                                                │ Controller      │
                                                │                 │
                                                └─────────────────┘
```

## 📋 Hardware Requirements

### ESP32 Hub
- ESP32 Development Board
- MQ-6 Gas Sensor
- Fire Sensor Module
- SW-420 Vibration Sensor
- HC-SR04 Ultrasonic Sensors (x2)
- Status LEDs (Red/Green)
- Buzzer
- Jumper wires and breadboard

### Arduino Door Controller
- Arduino Nano/Uno
- NFC-RC522 RFID Reader
- SG90 Servo Motor
- 16x2 LCD Display with I2C
- NFC/RFID Cards
- Magnetic door sensor (optional)

## 🚀 Getting Started

### Prerequisites
- Android Studio (latest version)
- Arduino IDE
- ESP32 Arduino Core
- Firebase account
- Hardware components listed above

### 1. Firebase Setup
1. Create a new Firebase project
2. Enable Realtime Database and Authentication
3. Download `google-services.json` and place in `app/` directory
4. Update Firebase security rules using `firestore.rules`

### 2. ESP32 Setup
1. Navigate to `esp32/` directory
2. Copy `config.h.example` to `config.h`
3. Update WiFi credentials and Firebase config in `config.h`
4. Install required libraries:
   - Firebase ESP32 Client by Mobizt
   - ArduinoJson
5. Flash `HomeSecurity.ino` to ESP32

### 3. Arduino Setup
1. Open `Arduino_Door_Control.ino` in Arduino IDE
2. Install required libraries:
   - MFRC522 (for RFID)
   - Servo
   - LiquidCrystal_I2C
3. Update authorized RFID card UID in the code
4. Flash to Arduino Nano/Uno

### 4. Android App Setup
1. Open project in Android Studio
2. Sync project with Gradle files
3. Ensure `google-services.json` is in place
4. Build and run on Android device (API level 28+)

### 5. Hardware Connections

#### ESP32 Pin Configuration
| Component | ESP32 GPIO |
|-----------|------------|
| MQ-6 Gas Sensor | GPIO 34 (ADC) |
| Fire Sensor | GPIO 35 |
| Vibration Sensor | GPIO 33 |
| Ultrasonic 1 (Trig/Echo) | GPIO 25/26 |
| Ultrasonic 2 (Trig/Echo) | GPIO 27/14 |
| Buzzer | GPIO 4 |
| Red LED | GPIO 5 |
| Green LED | GPIO 2 |
| Arduino Communication | GPIO 16/17 |

#### Arduino Pin Configuration
| Component | Arduino Pin |
|-----------|-------------|
| RFID (SS/RST) | Pin 10/9 |
| Servo Motor | Pin 6 |
| LCD (SDA/SCL) | A4/A5 |

## 📊 Data Structure

The system uses Firebase Realtime Database with the following structure:

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
    }
  },
  "alerts": {
    "alert_id": {
      "type": "GAS_LEAK",
      "message": "High gas level detected",
      "sensorId": "gas_sensor_1", 
      "timestamp": 1624512345,
      "isAcknowledged": false
    }
  }
}
```

## 🔧 Configuration

### ESP32 Configuration (`esp32/config.h`)
```cpp
// WiFi Configuration
#define WIFI_SSID "your_wifi_ssid"
#define WIFI_PASSWORD "your_wifi_password"

// Firebase Configuration  
#define FIREBASE_HOST "your-project.firebaseio.com"
#define FIREBASE_AUTH "your_firebase_secret"
```

### Arduino Configuration
- Update authorized RFID card UID in `Arduino_Door_Control.ino`
- Adjust servo positions for your door lock mechanism
- Configure LCD I2C address if different from 0x27

## 🔍 Troubleshooting

### Common Issues

**WiFi Connection Failed**
- Verify WiFi credentials in `config.h`
- Check signal strength and range
- Try restarting ESP32

**Firebase Connection Issues**
- Verify Firebase credentials
- Check database rules permissions
- Ensure NTP time synchronization

**Arduino Communication Problems**
- Verify baud rate (9600) on both devices
- Check RX/TX wire connections (cross-connected)
- Monitor serial output for debug messages

**Sensor Reading Issues**
- Check wiring connections
- Verify GPIO pin assignments
- Test sensors individually

**App Crashes or Data Not Loading**
- Check `google-services.json` placement
- Verify Firebase rules allow read/write access
- Check device internet connection

## 📁 Project Structure

```
HomeSecurity/
├── app/                          # Android application
│   ├── src/main/java/com/example/homesecurity/
│   │   ├── ui/                   # UI components
│   │   ├── viewmodels/           # ViewModels
│   │   ├── repository/           # Data repositories
│   │   ├── models/               # Data models
│   │   ├── services/             # Background services
│   │   └── di/                   # Dependency injection
│   └── build.gradle.kts          # App dependencies
├── esp32/                        # ESP32 firmware
│   ├── HomeSecurity.ino          # Main ESP32 code
│   ├── config.h.example          # Configuration template
│   └── libraries/                # Required libraries
├── arduino/                      # Arduino door controller
│   └── door_security/            # Door control system
├── Arduino_Door_Control.ino      # Main Arduino code
├── firestore.rules               # Firebase security rules
└── README.md                     # This file
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🆘 Support

For support and questions:
- Create an issue in the GitHub repository
- Check the troubleshooting section above
- Review the individual component README files in their respective directories

## 🔮 Future Enhancements

- [ ] Camera integration for visual monitoring
- [ ] Machine learning for anomaly detection
- [ ] Voice control integration
- [ ] Multi-zone sensor management
- [ ] SMS/Email alert notifications
- [ ] Web dashboard interface
- [ ] Energy monitoring capabilities

---

**⚠️ Safety Notice**: This system is designed for educational and hobbyist purposes. For critical security applications, please consult with professional security system providers and ensure compliance with local regulations. 