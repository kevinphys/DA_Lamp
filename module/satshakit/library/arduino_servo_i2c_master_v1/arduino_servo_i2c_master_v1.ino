/*   DA Lamp (documentation assistant lamp)
 *    
 *   In v1, we are sending commands including servo motor index and angles from computer serial to Arduino/Satshakit, and Arduino/Satshakit would 
 *   becaome an I2C master controlling servo motors.
 *   
 *   By Kevin Cheng
 *   Fablab Taipei, Taiwan.
 *   
 *   DA Lamp project under the MIT license.
 */

#include <Wire.h>

String inputString = "";

int indexServo;
int angle;

boolean stringComplete = false;  // whether the string is complete

void setup() {
  Serial.begin(115200);     // opens serial port, sets data rate to 9600 bps
  Wire.begin();
  inputString.reserve(200);
}

void loop() {

  if (stringComplete) {
    indexServo = inputString.substring(0,inputString.indexOf(',')).toInt();
    angle = inputString.substring(inputString.indexOf(',')+1).toInt();

    Serial.print("Sending to ");
    Serial.print(indexServo);
    Serial.print(" and ");

    sendToServo(indexServo, angle);

    inputString = "";
    indexServo = 0;
    
    stringComplete = false;
  }
}

void sendToServo(int index, int angle) {
  Wire.beginTransmission(index); // transmit to device #2
  Wire.write(angle);  // sends one byte
  Serial.print("turning to ");
  Serial.print(angle);
  Serial.print("\n");
  Wire.endTransmission();    // stop transmitting
}


void serialEvent() {
  while (Serial.available()) {
    // get the new byte:
    char inChar = (char)Serial.read();

    inputString += inChar;

    // if the incoming character is a newline, set a flag
    // so the main loop can do something about it:
    if (inChar == '\n') {
      stringComplete = true;
    }
  }
}
