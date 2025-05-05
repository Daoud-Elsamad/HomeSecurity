#include <SPI.h>
#include <MFRC522.h>
#include <Servo.h>
#include <Wire.h>
#include <LiquidCrystal_I2C.h>

// ----- RFID Setup -----
#define SS_PIN 10
#define RST_PIN 9
MFRC522 mfrc522(SS_PIN, RST_PIN);

// ----- Servo Setup -----
#define SERVO_PIN 6
Servo myServo;

// ----- LCD Setup -----
LiquidCrystal_I2C lcd(0x27, 16, 2); // Address 0x27, 16 columns, 2 rows

// ----- Authorized UID -----
byte authorizedUID[4] = {0x93, 0x32, 0xC2, 0x01}; // Replace with your card UID
int deniedAttempts = 0;

// ----- Command Processing -----
bool isProcessingCommand = false;
String lastCommand = "";
unsigned long lastCommandTime = 0;
const unsigned long commandDebounceTime = 500; // Reduced to 500ms for faster response

// ----- LCD Message Tracking -----
String currentLcdMessage = ""; // Track current message to avoid redundant updates

// ----- Debug Counters -----
unsigned long commandsReceived = 0;
unsigned long unlockCommandsReceived = 0;
unsigned long lockCommandsReceived = 0;

void setup() {
  Serial.begin(9600);  // UART to ESP32
  SPI.begin();
  mfrc522.PCD_Init();
  myServo.attach(SERVO_PIN);
  myServo.write(0); // Locked position

  // Initialize LCD
  lcd.init();
  lcd.backlight();
  
  // Set welcome message
  setLcdMessage("System Ready!");
  delay(2000);
  
  // Set initial display
  setLcdMessage("Scan to unlock");
  
  // Print debug message to confirm communication ready
  Serial.println("Arduino ready for commands");
}

void loop() {
  // Always update the display if not processing a command and default message isn't shown
  if (!isProcessingCommand && currentLcdMessage != "Scan to unlock") {
    setLcdMessage("Scan to unlock");
  }

  // Check for NFC Card if not currently processing a command
  if (!isProcessingCommand && mfrc522.PICC_IsNewCardPresent() && mfrc522.PICC_ReadCardSerial()) {
    setLcdMessage("Verifying...");
    delay(1000);

    if (isAuthorized()) {
      isProcessingCommand = true; // Set flag to prevent reprocessing
      
      setLcdMessage("Access Granted");
      
      // Access granted - unlock
      myServo.write(90);  // Unlock
      Serial.println("AUTHORIZED");  // Send to ESP32
      deniedAttempts = 0;  // Reset counter
      
      delay(5000);
      myServo.write(0);   // Lock again
      
      isProcessingCommand = false; // Reset flag when done
      setLcdMessage("Scan to unlock"); // Restore default message
    } else {
      isProcessingCommand = true; // Set flag to prevent reprocessing
      
      setLcdMessage("Access Denied");
      
      // Access denied
      deniedAttempts++;

      if (deniedAttempts >= 3) {
        Serial.println("DENIED3");  // Notify ESP32
        deniedAttempts = 0;         // Reset counter
      }

      delay(2000);
      
      isProcessingCommand = false; // Reset flag when done
      setLcdMessage("Scan to unlock"); // Restore default message
    }

    mfrc522.PICC_HaltA();  // Halt reader
  }

  // Check for commands from ESP32
  checkSerialCommands();
  
  // Display debug info periodically
  static unsigned long lastDebugTime = 0;
  if (millis() - lastDebugTime > 30000) { // Every 30 seconds
    lastDebugTime = millis();
    displayDebugInfo();
  }
  
  // Add a small delay to prevent excessive looping
  delay(100);
}

// Helper function to check for ESP32 commands
void checkSerialCommands() {
  if (Serial.available() > 0) {
    String command = Serial.readStringUntil('\n');
    command.trim();
    
    // Increment the command counter
    commandsReceived++;
    
    // Debug - show command received on LCD
    setLcdMessage("Command: " + command);
    
    // Log to serial
    Serial.print("Received command: ");
    Serial.println(command);
    
    // Only process if not currently handling a command
    if (!isProcessingCommand) {
      // Add debounce to prevent repeated commands
      unsigned long currentTime = millis();
      
      // Only process command if it's different from last one OR sufficient time has passed
      if (command != lastCommand || (currentTime - lastCommandTime > commandDebounceTime)) {
        lastCommand = command;
        lastCommandTime = currentTime;
        
        if (command == "UNLOCK") {
          unlockCommandsReceived++;
          Serial.println("Processing UNLOCK command");
          isProcessingCommand = true; // Set flag to prevent reprocessing
          
          setLcdMessage("Remote Unlock");
          
          myServo.write(90);  // Unlock
          Serial.println("UNLOCK_ACK"); // Acknowledge command
          
          delay(5000);
          myServo.write(0);   // Lock again after 5 seconds
          
          isProcessingCommand = false; // Reset flag when done
          setLcdMessage("Scan to unlock"); // Restore default message
        } 
        else if (command == "LOCK") {
          lockCommandsReceived++;
          Serial.println("Processing LOCK command");
          isProcessingCommand = true; // Set flag to prevent reprocessing
          
          setLcdMessage("Remote Lock");
          
          myServo.write(0);   // Lock
          Serial.println("LOCK_ACK"); // Acknowledge command
          
          delay(2000);
          
          isProcessingCommand = false; // Reset flag when done
          setLcdMessage("Scan to unlock"); // Restore default message
        }
        else {
          Serial.print("Unknown command: ");
          Serial.println(command);
        }
      }
      else {
        Serial.println("Command ignored due to debounce");
      }
    }
    else {
      Serial.println("Command ignored - busy processing another command");
    }
    
    // Clear any remaining data in the buffer
    clearSerialBuffer();
  }
}

// Function to clear any data in the serial buffer
void clearSerialBuffer() {
  while (Serial.available() > 0) {
    Serial.read();
  }
}

// Function to update the LCD with a new message only if it's different from the current one
void setLcdMessage(String message) {
  if (message != currentLcdMessage) {
    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print(message);
    currentLcdMessage = message;
  }
}

// Function to display debug information on the LCD
void displayDebugInfo() {
  setLcdMessage("Cmds: " + String(commandsReceived));
  delay(2000);
  setLcdMessage("Unlock: " + String(unlockCommandsReceived));
  delay(2000);
  setLcdMessage("Lock: " + String(lockCommandsReceived));
  delay(2000);
  setLcdMessage("Scan to unlock");
}

bool isAuthorized() {
  for (byte i = 0; i < 4; i++) {
    if (mfrc522.uid.uidByte[i] != authorizedUID[i])
      return false;
  }
  return true;
} 