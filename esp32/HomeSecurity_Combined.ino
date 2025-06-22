/*
 * Home Security System - ESP32 Combined File
 * 
 * This file combines the mechatronics working code with Firebase functionality.
 */

#include <WiFi.h>
#include <FirebaseESP32.h>
#include <EEPROM.h>
#include <ArduinoJson.h>
#include <Preferences.h>
#include <time.h>

// ===== CONFIGURATION SETTINGS =====
// WiFi Configuration
#define WIFI_SSID "Your_WiFi_SSID"  // CHANGE THIS TO YOUR WIFI NAME
#define WIFI_PASSWORD "Your_WiFi_Password"  // CHANGE THIS TO YOUR WIFI PASSWORD

// Firebase Configuration
#define FIREBASE_HOST "your-firebase-project-url"  // Do not include https:// prefix
#define FIREBASE_AUTH "Your_Firebase_API_Key"  // Project API key


// NTP Server for time synchronization
#define NTP_SERVER "pool.ntp.org"
#define GMT_OFFSET_SEC 0  // Change according to your timezone
#define DAYLIGHT_OFFSET_SEC 3600

// Pin Definitions - MECHANTRONICS VERIFIED
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

// Alert thresholds
#define GAS_THRESHOLD 900
#define VIBRATION_THRESHOLD 5
#define DISTANCE_THRESHOLD 20

// Update intervals (in milliseconds)
#define SENSOR_UPDATE_INTERVAL 5000  // Update sensor readings every 5 seconds

// ===== MAIN CODE =====
// ----- Firebase Data Object -----
FirebaseData fbdo;
FirebaseAuth auth;
FirebaseConfig config;
bool signupOK = false;

// ----- Sensor State -----
int fireTriggerCount = 0;
int vibrationTriggerCount = 0;
unsigned long lastFirebaseUpdateTime = 0;

// ----- Preferences for storing system ID -----
Preferences preferences;
char systemId[37] = "system_dev_001";  // Default system ID

// ----- Sensor IDs -----
const char* gasId = GAS_SENSOR_ID;
const char* doorId = DOOR_SENSOR_ID;
const char* vibrationId = VIBRATION_SENSOR_ID;
const char* ultrasonicId1 = ULTRASONIC_SENSOR_1_ID;
const char* ultrasonicId2 = ULTRASONIC_SENSOR_2_ID;
const char* nfcId = NFC_READER_ID;

extern HardwareSerial Serial2;

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
  
  // Initialize Preferences
  preferences.begin("security", false);
  
  // Load system ID if exists
  if (preferences.isKey("system_id")) {
    String storedId = preferences.getString("system_id", "");
    if (storedId.length() > 0) {
      storedId.toCharArray(systemId, 37);
      Serial.print("Loaded System ID: ");
      Serial.println(systemId);
    }
  }

  // Connect to WiFi and setup Firebase
  setupWiFi();
  setupFirebase();
  initializeSensors();

  Serial.println("✅ System initialized. Monitoring sensors...");
}

void loop() {
  // Check and publish all sensor readings
  checkSensors();
  
  // Check for NFC messages from Arduino
  checkSerialInput();
  
  // Monitor door lock commands from Firebase
  monitorDoorCommands();
  
  // For testing - uncomment to directly test the unlock functionality
  // Uncomment this line, upload once, then comment it back out and upload again
  // forceUnlock();
  
  // For testing - uncomment to use direct command testing
  // testDoorCommands();
  
  // Update Firebase periodically
  unsigned long currentMillis = millis();
  if (currentMillis - lastFirebaseUpdateTime >= SENSOR_UPDATE_INTERVAL) {
    lastFirebaseUpdateTime = currentMillis;
    updateFirebase();
  }
  
  delay(1000);
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
      
      // Update Firebase with unauthorized access
      publishAlert("NFC_UNAUTHORIZED", nfcId, "Unauthorized NFC card detection");
      
      delay(5000);
      digitalWrite(RED_LED_PIN, LOW);
      digitalWrite(BUZZER_PIN, LOW);
    } 
    else if (msg == "AUTHORIZED") {
      Serial.println("✅ Access granted.");
      digitalWrite(GREEN_LED_PIN, HIGH);
      
      // Update Firebase with authorized access
      updateSensorValue(nfcId, 1.0);
      
      delay(5000);
      digitalWrite(GREEN_LED_PIN, LOW);
      
      // Reset NFC reader value after successful read
      updateSensorValue(nfcId, 0.0);
    }
    else if (msg == "Arduino ready for commands") {
      Serial.println("✅ Arduino communication established.");
    }
    // Note: LOCK_ACK and UNLOCK_ACK are now handled in monitorDoorCommands
  }
}

void checkSensors() {
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

  if (gasValue > GAS_THRESHOLD) {
    Serial.println("🔥 Gas sensor triggered!");
    publishAlert("GAS_LEAK", gasId, "High gas level detected in kitchen");
    triggerAlert();
    return;
  }

  if (fireDetected == LOW) {
    fireTriggerCount++;
    if (fireTriggerCount >= VIBRATION_THRESHOLD) {
      Serial.println("🔥 Fire sensor triggered 5x!");
      publishAlert("FIRE", gasId, "Fire detected in kitchen");
      triggerAlert();
      fireTriggerCount = 0;
      return;
    }
  } else fireTriggerCount = 0;

  if (vibrationDetected == LOW) {
    vibrationTriggerCount++;
    if (vibrationTriggerCount >= VIBRATION_THRESHOLD) {
      Serial.println("🌀 Vibration sensor triggered 5x!");
      publishAlert("VIBRATION_DETECTED", vibrationId, "Unusual vibration detected at window");
      triggerAlert();
      vibrationTriggerCount = 0;
      return;
    }
  } else vibrationTriggerCount = 0;

  if (distance1 > 0 && distance1 < DISTANCE_THRESHOLD) {
    Serial.println("👀 Ultrasonic sensor 1 triggered!");
    publishAlert("PROXIMITY", ultrasonicId1, "Movement detected at entrance");
    triggerAlert();
    return;
  }

  if (distance2 > 0 && distance2 < DISTANCE_THRESHOLD) {
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

// ===== FIREBASE FUNCTIONS =====

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
    Serial.println("Failed to connect to WiFi. Running in offline mode.");
  }
}

void setupFirebase() {
  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("WiFi not connected, skipping Firebase setup");
    return;
  }

  // Initialize time for timestamps
  configTime(GMT_OFFSET_SEC, DAYLIGHT_OFFSET_SEC, NTP_SERVER);
  
  // Initialize Firebase Config
  config.database_url = "https://" + String(FIREBASE_HOST);
  config.api_key = FIREBASE_AUTH;
  
  // Sign in anonymously
  Serial.println("Attempting anonymous authentication...");
  if (Firebase.signUp(&config, &auth, "", "")) {
    Serial.println("Authentication successful");
    signupOK = true;
  } else {
    Serial.printf("Authentication failed: %s\n", config.signer.signupError.message.c_str());
  }
  
  // Initialize Firebase with configuration
  Firebase.begin(&config, &auth);
  Firebase.reconnectWiFi(true);
  
  // Set database read timeout
  Firebase.setReadTimeout(fbdo, 1000 * 60);
  // Tiny size for saving memory
  Firebase.setwriteSizeLimit(fbdo, "tiny");
  
  // Debug information
  Serial.print("Firebase Host: ");
  Serial.println(FIREBASE_HOST);
  Serial.print("Authentication Status: ");
  Serial.println(signupOK ? "Signed in" : "Not signed in");
  Serial.println("Firebase initialized successfully");
}

void initializeSensors() {
  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("WiFi not connected, skipping sensor initialization in Firebase");
    return;
  }

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
  if (!signupOK) {
    Serial.printf("Failed to initialize sensor %s: Not authenticated\n", sensorId);
    return;
  }
  
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

// Update Firebase with sensor values
void updateFirebase() {
  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("WiFi not connected, skipping Firebase update");
    return;
  }

  int gasValue = analogRead(MQ6_PIN);
  float gasPpm = map(gasValue, 0, 4095, 0, 1000);
  int vibrationDetected = digitalRead(VIBRATION_PIN);
  long distance1 = readUltrasonic(TRIG_PIN_1, ECHO_PIN_1);
  long distance2 = readUltrasonic(TRIG_PIN_2, ECHO_PIN_2);

  // Update sensor values in Firebase
  updateSensorValue(gasId, gasPpm);
  updateSensorValue(vibrationId, vibrationDetected == LOW ? 10.0 : 0.0);
  updateSensorValue(ultrasonicId1, (double)distance1);
  updateSensorValue(ultrasonicId2, (double)distance2);
}

void updateSensorValue(const char* sensorId, double value) {
  if (WiFi.status() != WL_CONNECTED) {
    Serial.printf("Failed to update %s: WiFi not connected\n", sensorId);
    return;
  }
  
  if (!signupOK) {
    Serial.printf("Failed to update %s: Not authenticated\n", sensorId);
    return;
  }

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
  if (!Firebase.setInt(fbdo, timestampPath, (int)now)) {
    Serial.printf("Failed to update timestamp for %s: %s\n", sensorId, fbdo.errorReason().c_str());
  }
}

void publishAlert(const char* alertType, const char* sensorId, const char* message) {
  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("WiFi not connected, skipping alert publishing");
    return;
  }
  
  if (!signupOK) {
    Serial.println("Not authenticated, skipping alert publishing");
    return;
  }

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
    Serial.printf("Firebase error: %s\n", fbdo.errorReason().c_str());
  }
}

void toggleDoorLock(const char* doorId, bool isLocked) {
  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("WiFi not connected, skipping door lock toggle");
    return;
  }
  
  if (!signupOK) {
    Serial.println("Not authenticated, skipping door lock toggle");
    return;
  }
  
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

// Function for explicit door control testing - call this from loop to send direct commands
void testDoorCommands() {
  static unsigned long lastTestTime = 0;
  static int testState = 0;
  
  // Run test every 30 seconds
  if (millis() - lastTestTime > 30000) {
    lastTestTime = millis();
    
    // Alternate between lock and unlock
    if (testState == 0) {
      Serial.println("\n⚙️ TEST: Sending direct UNLOCK command to Arduino");
      Serial2.println("UNLOCK");
      testState = 1;
    } else {
      Serial.println("\n⚙️ TEST: Sending direct LOCK command to Arduino");
      Serial2.println("LOCK");
      testState = 0;
    }
  }
}

// Monitor Firebase for door lock commands
void monitorDoorCommands() {
  static bool previousLockState = true;  // Start with door locked
  static unsigned long lastCommandTime = 0;
  static unsigned long lastCommandSentTime = 0;
  static int commandRetryCount = 0;
  static bool pendingCommand = false;
  static unsigned long commandAttempts = 0;
  static unsigned long commandSuccesses = 0;
  
  unsigned long currentTime = millis();
  const unsigned long commandDelay = 1000; // 1 second minimum between checks
  const unsigned long retryDelay = 3000;  // 3 seconds between retries
  const int maxRetries = 3;  // Maximum number of retries
  
  // Check if we need to retry a previous command
  if (pendingCommand && commandRetryCount < maxRetries && (currentTime - lastCommandSentTime) > retryDelay) {
    // Resend the last command
    Serial.println("Retrying previous door command...");
    if (previousLockState) {
      Serial.println("Resending LOCK command to Arduino");
      Serial2.println("LOCK");
    } else {
      Serial.println("Resending UNLOCK command to Arduino");
      Serial2.println("UNLOCK");
    }
    
    lastCommandSentTime = currentTime;
    commandRetryCount++;
    return;
  }
  
  // Only check Firebase periodically to avoid hammering the service
  if (!pendingCommand && (currentTime - lastCommandTime) >= commandDelay) {
    lastCommandTime = currentTime;
    String path = "/sensors/" + String(doorId) + "/isLocked";
    
    // Debug output - log Firebase check
    Serial.print("⚙️ Checking door lock state in Firebase: ");
    
    if (Firebase.getBool(fbdo, path)) {
      bool isLocked = fbdo.boolData();
      Serial.println(isLocked ? "LOCKED" : "UNLOCKED");
      
      // Only send command if the state has changed
      if (isLocked != previousLockState) {
        commandAttempts++;
        Serial.printf("🔑 Door state change detected in Firebase to: %s (attempt #%lu)\n", 
                    isLocked ? "LOCKED" : "UNLOCKED", commandAttempts);
        
        // Send command to Arduino
        if (isLocked) {
          Serial.println("Sending LOCK command to Arduino");
          Serial2.println("LOCK");
        } else {
          Serial.println("Sending UNLOCK command to Arduino");
          Serial2.println("UNLOCK");
        }
        
        // Set pending command flag and reset retry count
        pendingCommand = true;
        commandRetryCount = 1;  // First attempt already made
        lastCommandSentTime = currentTime;
        
        // Update the previous state
        previousLockState = isLocked;
      }
    } else {
      Serial.printf("Failed to read door lock state: %s\n", fbdo.errorReason().c_str());
    }
  }
  
  // Check for acknowledgments from Arduino
  if (Serial2.available() > 0) {
    String response = Serial2.readStringUntil('\n');
    response.trim();
    
    if (response == "LOCK_ACK" || response == "UNLOCK_ACK") {
      commandSuccesses++;
      Serial.printf("✅ Received acknowledgment from Arduino: %s (success #%lu)\n", 
                   response.c_str(), commandSuccesses);
      pendingCommand = false;  // Command acknowledged, no need to retry
      commandRetryCount = 0;
    }
  }
  
  // Periodically display stats
  static unsigned long lastStatsTime = 0;
  if (currentTime - lastStatsTime > 60000) { // Every minute
    lastStatsTime = currentTime;
    Serial.println("\n----- DOOR COMMAND STATS -----");
    Serial.printf("Command attempts: %lu\n", commandAttempts);
    Serial.printf("Command successes: %lu\n", commandSuccesses);
    Serial.printf("Success rate: %d%%\n", 
                 commandAttempts > 0 ? (int)((float)commandSuccesses / commandAttempts * 100) : 0);
    Serial.println("-----------------------------\n");
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

// Function to force unlock directly through Firebase for testing
void forceUnlock() {
  Serial.println("\n🔑 FORCE UNLOCK: Setting door to unlocked state in Firebase");
  
  String path = "/sensors/" + String(doorId) + "/isLocked";
  if (Firebase.setBool(fbdo, path, false)) {
    Serial.println("✅ Successfully set door state to UNLOCKED in Firebase");
    Serial.println("If the door doesn't unlock, check the monitorDoorCommands function");
  } else {
    Serial.printf("❌ Failed to set door state: %s\n", fbdo.errorReason().c_str());
  }
} 