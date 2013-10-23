/*
Title:  DCC Controller
 Author: Scott Schiavone
 Data:   10/21/2013
 
 Software Description:  
 
 This code takes three bytes of data (address, data, error) and sends them out
 one bit at a time as a DCC signal. The packet format should conform to NMRA 
 standard S 9.2 July 2004.
 
 Hardware Description:
 
 Atmega328P running at 16MHz
 
 The DCC signal is sent to model rail road train track rails through four mosfets
 configured in an H-Bridge pattern. The output pins to the H-Bridge from the Atmega328P 
 are PB5 and PB6. 
 
 
 */

#include "TimerOne.h"

#define ONE_BIT 56                              // 56 µs
#define ZERO_BIT 112                            // 112 µs


byte address = 0x00;
byte data = 0x00;
byte error = 0x00;

byte address1 = 0x03;
byte data1 = 0x78;
byte error1 = 0x7b;

byte address2 = 0x05;
byte data2 = 0x78;
byte error2 = 0x7d;

byte doneflag = 0;

byte bitCount = 0;                              // used for keeping track of what stage in packet program is

void setup()
{
  DDRD = 0x60;                                  // enable PB5 and PB6 as outputs
  PORTD |= 0x20;                                // set PB5 HIGH
  Timer1.initialize(ONE_BIT);                   // initialize timer1, and set to 56 µs 
  Timer1.attachInterrupt(callback);             // attaches callback() as a timer overflow interrupt
}

void callback()                                 // callback for timer ISR
{
  PORTD ^= 0x60;                                // toggle DCC polarity      

  if(0x20 == (PORTD & 0x20))                    // if ready for new bit
  {    
    
    if(bitCount < 14)
      Timer1.initialize(ONE_BIT);               // send preamble

    else if(bitCount == 14)
      Timer1.initialize(ZERO_BIT);              // send Packet Start Bit
    else if(bitCount > 14 && bitCount < 23)
      setBit(address,(22-bitCount));            // send Address Byte

    else if(bitCount == 23)
      Timer1.initialize(ZERO_BIT);              // send Data Start Bit
    else if(bitCount > 23 && bitCount < 32)
      setBit(data,(31-bitCount));               // send Data Byte

    else if(bitCount == 32)
      Timer1.initialize(ZERO_BIT);              // send Error Start Bit
    else if(bitCount > 32 && bitCount < 41)
      setBit(error,(40-bitCount));              // send Error Byte

    else if(bitCount == 41)
      Timer1.initialize(ONE_BIT);               // send Packet End Bit

    if(bitCount > 41)
    {
      bitCount = 0;
      if(!doneflag)
      {
        doneflag = 1;
        address = address2;
        data = data2;
        error = error2;
      }
      else
      {
        address = address1;
        doneflag = 0; 
        data = data1;
        error = error1;
      }
    }
    else if(errorCheck(address,data,error))
      bitCount++;
    else
      bitCount = 0;      
  }    
}

void loop()
{

}


// Begin helper functions


// sends out zero, or one on DCC depending on input values
void setBit(byte dataByte, byte index)            
{
  if(0 == (bit(index) & dataByte))
    Timer1.initialize(ZERO_BIT);
  else
    Timer1.initialize(ONE_BIT);
}

// checks to make sure the error byte is correct
boolean errorCheck(byte address, byte data, byte error)
{
  if(error == (address ^ data))
    return true;
  else
    return false;
}

