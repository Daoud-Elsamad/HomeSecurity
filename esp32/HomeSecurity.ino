#include <WiFi.h>
#include <FirebaseESP32.h>
#include <EEPROM.h>
#include <ArduinoJson.h>
#include <Preferences.h>
#include <time.h>
#include "config.h"

// ----- Pin Definitions -----
// Using definitions from config.h

// System identification
char systemId[37] = "system_dev_001";  // Default system ID - will be updated during setup

// ----- Firebase Data Object -----
FirebaseData fbdo;
FirebaseAuth auth;
FirebaseConfig config;

// ----- Sensor State -----
int fireTriggerCount = 0;
int vibrationTriggerCount = 0;
unsigned long lastSensorPublishTime = 0;
const int sensorPublishInterval = SENSOR_UPDATE_INTERVAL;

// ----- NFC Communication with Arduino -----
extern HardwareSerial Serial2;
boolean nfcMessageReceived = false;
String nfcMessage = "";

// ----- Preferences for storing system ID -----
Preferences preferences;

// ----- Sensor IDs -----
const char* gasId = GAS_SENSOR_ID;
const char* doorId = DOOR_SENSOR_ID;
const char* vibrationId = VIBRATION_SENSOR_ID;
const char* ultrasonicId1 = ULTRASONIC_SENSOR_1_ID;
const char* ultrasonicId2 = ULTRASONIC_SENSOR_2_ID;
const char* nfcId = NFC_READER_ID;

void setup() {
  // Initialize Serial communication
  Serial.begin(115200);
  Serial2.begin(ARDUINO_BAUD_RATE, SERIAL_8N1, ARDUINO_RX, ARDUINO_TX);

  // Initialize pins
  pinMode(FIRE_PIN, INPUT_PULLUP);
  pinMode(VIBRATION_PIN, INPUT_PULLUP);
  pinMode(TRIG_PIN_1, OUTPUT);
  pinMode(ECHO_PIN_1, INPUT);
  pinMode(TRIG_PIN_2, OUTPUT);
  pinMode(ECHO_PIN_2, INPUT);
  pinMode(BUZZER_PIN, OUTPUT);
  pinMode(RED_LED_PIN, OUTPUT);
  pinMode(GREEN_LED_PIN, OUTPUT);
  digitalWrite(BUZZER_PIN, LOW);
  digitalWrite(RED_LED_PIN, LOW);
  digitalWrite(GREEN_LED_PIN, LOW);

  // Initialize Preferences for persistent storage
  preferences.begin("security", false);
  
  // Load system ID from preferences if it exists
  if (preferences.isKey("system_id")) {
    String storedId = preferences.getString("system_id", "");
    if (storedId.length() > 0) {
      storedId.toCharArray(systemId, 37);
      Serial.print("Loaded System ID: ");
      Serial.println(systemId);
    }
  }

  // Connect to WiFi
  setupWiFi();
  
  // Setup time
  configTime(GMT_OFFSET_SEC, DAYLIGHT_OFFSET_SEC, NTP_SERVER);
  
  // Configure Firebase
  setupFirebase();

  // Initialize sensors in Firebase
  initializeSensors();

  Serial.println("✅ System initialized. Monitoring sensors...");
}

void loop() {
  // Check for NFC messages from Arduino
  checkSerialInput();
  
  // Read and publish sensor data periodically
  unsigned long currentMillis = millis();
  if (currentMillis - lastSensorPublishTime >= sensorPublishInterval) {
    lastSensorPublishTime = currentMillis;
    checkAndPublishSensors();
  }
  
  delay(100); // Small delay to avoid CPU hogging
}

void setupWiFi() {
  Serial.println("Connecting to WiFi...");
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  
  int attempts = 0;
  while (WiFi.status() != WL_CONNECTED && attempts < 20) {
    delay(500);
    Serial.print(".");
    attempts++;
  }
  
  if (WiFi.status() == WL_CONNECTED) {
    Serial.println("");
    Serial.println("WiFi connected");
    Serial.println("IP address: ");
    Serial.println(WiFi.localIP());
  } else {
    Serial.println("Failed to connect to WiFi. Please check credentials.");
  }
}

void setupFirebase() {
  config.api_key = FIREBASE_AUTH;
  config.database_url = FIREBASE_HOST;
  
  Firebase.begin(&config, &auth);
  Firebase.reconnectWiFi(true);
  
  // Set database read timeout
  Firebase.setReadTimeout(fbdo, 1000 * 60);
  // Set size and its write timeout
  Firebase.setwriteSizeLimit(fbdo, "tiny");
  
  Serial.println("Firebase initialized");
}

void initializeSensors() {
  // Initialize Gas Sensor
  initializeSensor(gasId, "GAS", "Kitchen");
  
  // Initialize Door Sensor
  initializeSensor(doorId, "DOOR", "Main Door");
  
  // Initialize Vibration Sensor
  initializeSensor(vibrationId, "VIBRATION", "Living Room");
  
  // Initialize Ultrasonic Sensors
  initializeSensor(ultrasonicId1, "ULTRASONIC", "Entrance");
  initializeSensor(ultrasonicId2, "ULTRASONIC", "Hallway");
  
  // Initialize NFC Reader
  initializeSensor(nfcId, "NFC", "Main Door");
}

void initializeSensor(const char* sensorId, const char* type, const char* location) {
  String path = "/sensors/" + String(sensorId);
  
  FirebaseJson json;
  json.set("type", type);
  json.set("location", location);
  json.set("isEnabled", true);
  
  // Door sensors get lock status
  if (String(type) == "DOOR") {
    json.set("isLocked", true);
  }
  
  // Set initial value
  json.set("value", 0.0);
  
  // Add timestamp
  time_t now;
  time(&now);
  json.set("timestamp", (int)now);
  
  if (Firebase.setJSON(fbdo, path, json)) {
    Serial.printf("Initialized sensor %s of type %s at %s\n", sensorId, type, location);
  } else {
    Serial.printf("Failed to initialize sensor %s: %s\n", sensorId, fbdo.errorReason().c_str());
  }
}

void checkSerialInput() {
  if (Serial2.available()) {
    String msg = Serial2.readStringUntil('\n');
    msg.trim();
    Serial.print("📨 Received from Arduino: ");
    Serial.println(msg);
    
    if (msg == "DENIED3") {
      Serial.println("⛔ Triggering buzzer and red LED.");
      digitalWrite(RED_LED_PIN, HIGH);
      digitalWrite(BUZZER_PIN, HIGH);
      
      // Update NFC reader status in Firebase
      publishAlert("NFC_UNAUTHORIZED", nfcId, "Unauthorized NFC card detection");
      
      delay(5000);
      digitalWrite(RED_LED_PIN, LOW);
      digitalWrite(BUZZER_PIN, LOW);
    } 
    else if (msg == "AUTHORIZED") {
      Serial.println("✅ Access granted.");
      digitalWrite(GREEN_LED_PIN, HIGH);
      
      // Update NFC reader status in Firebase
      updateSensorValue(nfcId, 1.0);
      
      delay(5000);
      digitalWrite(GREEN_LED_PIN, LOW);
      
      // Reset NFC reader value after successful read
      updateSensorValue(nfcId, 0.0);
    }
  }
}

void checkAndPublishSensors() {
  int gasValue = analogRead(MQ6_PIN);
  int fireDetected = digitalRead(FIRE_PIN);
  int vibrationDetected = digitalRead(VIBRATION_PIN);
  long distance1 = readUltrasonic(TRIG_PIN_1, ECHO_PIN_1);
  long distance2 = readUltrasonic(TRIG_PIN_2, ECHO_PIN_2);

  Serial.print("Gas: "); Serial.print(gasValue);
  Serial.print(" | Fire: "); Serial.print(fireDetected);
  Serial.print(" | Vibration: "); Serial.print(vibrationDetected);
  Serial.print(" | Dist1: "); Serial.print(distance1); Serial.print("cm");
  Serial.print(" | Dist2: "); Serial.print(distance2); Serial.println("cm");

  // Convert raw gas reading to PPM (rough estimation)
  float gasPpm = map(gasValue, 0, 4095, 0, 1000);

  // Update sensor values in Firebase
  updateSensorValue(gasId, gasPpm);
  updateSensorValue(vibrationId, vibrationDetected == LOW ? 10.0 : 0.0);
  updateSensorValue(ultrasonicId1, (double)distance1);
  updateSensorValue(ultrasonicId2, (double)distance2);
  
  // Check for alert conditions
  if (gasValue > 900) {
    Serial.println("🔥 Gas sensor triggered!");
    publishAlert("GAS_LEAK", gasId, "High gas level detected in kitchen");
    triggerAlert();
    return;
  }

  if (fireDetected == LOW) {
    fireTriggerCount++;
    if (fireTriggerCount >= 5) {
      Serial.println("🔥 Fire sensor triggered 5x!");
      publishAlert("FIRE", gasId, "Fire detected in kitchen");
      triggerAlert();
      fireTriggerCount = 0;
      return;
    }
  } else fireTriggerCount = 0;

  if (vibrationDetected == LOW) {
    vibrationTriggerCount++;
    if (vibrationTriggerCount >= 5) {
      Serial.println("🌀 Vibration sensor triggered 5x!");
      publishAlert("VIBRATION_DETECTED", vibrationId, "Unusual vibration detected at window");
      triggerAlert();
      vibrationTriggerCount = 0;
      return;
    }
  } else vibrationTriggerCount = 0;

  if (distance1 > 0 && distance1 < 20) {
    Serial.println("👀 Ultrasonic sensor 1 triggered!");
    publishAlert("PROXIMITY", ultrasonicId1, "Movement detected at entrance");
    triggerAlert();
    return;
  }

  if (distance2 > 0 && distance2 < 20) {
    Serial.println("👀 Ultrasonic sensor 2 triggered!");
    publishAlert("PROXIMITY", ultrasonicId2, "Movement detected in hallway");
    triggerAlert();
    return;
  }
}

long readUltrasonic(int trigPin, int echoPin) {
  digitalWrite(trigPin, LOW); delayMicroseconds(2);
  digitalWrite(trigPin, HIGH); delayMicroseconds(10);
  digitalWrite(trigPin, LOW);
  long duration = pulseIn(echoPin, HIGH, 30000);
  return duration * 0.034 / 2;
}

void triggerAlert() {
  Serial.println("🚨 ALERT TRIGGERED!");
  digitalWrite(BUZZER_PIN, HIGH);
  digitalWrite(RED_LED_PIN, HIGH);
  delay(2000);
  digitalWrite(BUZZER_PIN, LOW);
  digitalWrite(RED_LED_PIN, LOW);
}

void stopAlert() {
  digitalWrite(BUZZER_PIN, LOW);
  digitalWrite(RED_LED_PIN, LOW);
}

void updateSensorValue(const char* sensorId, double value) {
  String path = "/sensors/" + String(sensorId) + "/value";
  
  // Update value
  if (Firebase.setDouble(fbdo, path, value)) {
    Serial.printf("Updated %s value to %.2f\n", sensorId, value);
  } else {
    Serial.printf("Failed to update %s: %s\n", sensorId, fbdo.errorReason().c_str());
  }
  
  // Update timestamp
  time_t now;
  time(&now);
  String timestampPath = "/sensors/" + String(sensorId) + "/timestamp";
  Firebase.setInt(fbdo, timestampPath, (int)now);
}

void publishAlert(const char* alertType, const char* sensorId, const char* message) {
  String path = "/alerts/" + String(random(0xFFFFFFFF));
  
  FirebaseJson json;
  json.set("type", alertType);
  json.set("message", message);
  json.set("sensorId", sensorId);
  json.set("isAcknowledged", false);
  
  // Add timestamp
  time_t now;
  time(&now);
  json.set("timestamp", (int)now);
  
  if (Firebase.setJSON(fbdo, path, json)) {
    Serial.printf("Alert published: %s\n", message);
  } else {
    Serial.printf("Failed to publish alert: %s\n", fbdo.errorReason().c_str());
  }
}

void toggleDoorLock(const char* doorId, bool isLocked) {
  String path = "/sensors/" + String(doorId) + "/isLocked";
  
  if (Firebase.setBool(fbdo, path, isLocked)) {
    Serial.printf("Door %s %s\n", doorId, isLocked ? "locked" : "unlocked");
    
    // Send command to Arduino
    if (isLocked) {
      Serial2.println("LOCK");
    } else {
      Serial2.println("UNLOCK");
    }
  } else {
    Serial.printf("Failed to toggle door lock: %s\n", fbdo.errorReason().c_str());
  }
}

// Monitor Firebase for door lock commands
void monitorDoorCommands() {
  String path = "/sensors/" + String(doorId) + "/isLocked";
  
  if (Firebase.getBool(fbdo, path)) {
    bool isLocked = fbdo.boolData();
    
    // Send command to Arduino
    if (isLocked) {
      Serial2.println("LOCK");
    } else {
      Serial2.println("UNLOCK");
    }
  }
}

// Function to set system ID (can be called via a setup procedure)
void setSystemId(String newId) {
  if (newId.length() > 0 && newId.length() <= 36) {
    newId.toCharArray(systemId, 37);
    
    // Save to preferences
    preferences.putString("system_id", newId);
    
    Serial.print("System ID updated to: ");
    Serial.println(systemId);
  }
} 