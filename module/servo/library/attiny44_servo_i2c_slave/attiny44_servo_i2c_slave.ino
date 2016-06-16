// Get this from https://github.com/rambo/TinyWire
#include <TinyWireS.h>

#define I2C_SLAVE_ADDRESS 0x01 // the 7-bit address (remember to change this when adapting this example)

#define position_delay() _delay_ms(1000)

uint8_t incomingByte;   // for incoming serial data

uint8_t pwm;

void receiveEvent(uint8_t howMany)
{
    if (howMany < 1)
    {
        // Sanity-check
        return;
    }
    if (howMany >= 2)
    {
        // Also insane number
        return;
    }

    incomingByte = TinyWireS.receive();

    pwm = map(incomingByte, 0, 180, 5, 24);
}

void setup()
{
  TCCR0A = (1 << COM0B1) | (0 << COM0B0) | (0 << WGM01) | (1 << WGM00); // clear OC0B on compare match
  TCCR0B = (1 << CS02) | (0 << CS01) | (1 << CS00) | (1 << WGM02); // prescaler /8, phase and frequency correct PWM, OCRA TOP
  
  TCNT0 = 0x00;
  OCR0A = 0xC3;
  OCR0B = 0x05;
  
  PORTA |= (1 << PA7);
  DDRA |= (1 << PA7);
  
  TinyWireS.begin(I2C_SLAVE_ADDRESS);
  TinyWireS.onReceive(receiveEvent);
}

void loop()
{
  /**
   * This is the only way we can detect stop condition (http://www.avrfreaks.net/index.php?name=PNphpBB2&file=viewtopic&p=984716&sid=82e9dc7299a8243b86cf7969dd41b5b5#984716)
   * it needs to be called in a very tight loop in order not to miss any.
   * It will call the function registered via TinyWireS.onReceive(); if there is data in the buffer on stop.
   */
  TinyWireS_stop_check();
  
  OCR0B = pwm;
  position_delay();
 
}
