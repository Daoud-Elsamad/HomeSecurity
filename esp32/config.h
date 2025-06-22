#ifndef CONFIG_H
#define CONFIG_H

// WiFi Configuration
#define WIFI_SSID "Your_WiFi_SSID"  // CHANGE THIS TO YOUR WIFI NAME
#define WIFI_PASSWORD "Your_WiFi_Password"  // CHANGE THIS TO YOUR WIFI PASSWORD

// Firebase Configuration
#define FIREBASE_HOST 
#define FIREBASE_AUTH 

// NTP Server for time synchronization
#define NTP_SERVER "pool.ntp.org"
#define GMT_OFFSET_SEC 0  // Change according to your timezone
#define DAYLIGHT_OFFSET_SEC 3600

// Sensor Configuration
#define GAS_THRESHOLD 900  // Gas sensor threshold in analog reading
#define DOOR_OPEN_THRESHOLD 2000  // Door open alert threshold in milliseconds
#define VIBRATION_THRESHOLD 5  // Vibration threshold (count of triggers)
#define DISTANCE_THRESHOLD 20  // Ultrasonic distance threshold in cm

// Pin Definitions
#define MQ6_PIN 34  // Gas sensor
#define FIRE_PIN 35  // Fire sensor
#define VIBRATION_PIN 33  // Vibration sensor
#define TRIG_PIN_1 25  // Ultrasonic sensor 1 trigger
#define ECHO_PIN_1 26  // Ultrasonic sensor 1 echo
#define TRIG_PIN_2 27  // Ultrasonic sensor 2 trigger
#define ECHO_PIN_2 14  // Ultrasonic sensor 2 echo
#define BUZZER_PIN 4  // Buzzer for alerts
#define RED_LED_PIN 5  // Red LED for alerts
#define GREEN_LED_PIN 2  // Green LED for success

// Arduino Serial Communication
#define ARDUINO_BAUD_RATE 9600
#define ARDUINO_RX 16  // ESP32 RX pin connected to Arduino TX
#define ARDUINO_TX 17  // ESP32 TX pin connected to Arduino RX

// Sensor IDs (for Firebase paths)
#define GAS_SENSOR_ID "gas_sensor_1"
#define DOOR_SENSOR_ID "door_sensor_1"
#define VIBRATION_SENSOR_ID "vibration_sensor_1"
#define ULTRASONIC_SENSOR_1_ID "ultrasonic_sensor_1"
#define ULTRASONIC_SENSOR_2_ID "ultrasonic_sensor_2"
#define NFC_READER_ID "nfc_reader_1"

// Update intervals (in milliseconds)
#define SENSOR_UPDATE_INTERVAL 5000  // Update sensor readings every 5 seconds

#endif // CONFIG_H 