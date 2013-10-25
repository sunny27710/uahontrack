/*
Title:  DCC Controller
 Author: Scott Schiavone
 Data:   10/25/2013
 
 Software Description:  
 
 This code takes three bytes of data (address, data, error) and sends them out
 one bit at a time as a DCC signal. The packet format should conform to NMRA 
 standard S 9.2 July 2004.
 
 Hardware Description:
 
 Atmega328P running at 16MHz
 
 The DCC signal is sent to model rail road train track rails through four mosfets
 configured in an H-Bridge pattern. The output pins to the H-Bridge from the Atmega328P 
 are PD5 and PD6.  
 */

#include "TimerOne.h"

#define ONE_BIT 56                              // 56 µs
#define ZERO_BIT 112                            // 112 µs
#define MAX_TRAINS 10

byte address = 0x00;
byte data = 0x00;
byte error = 0x00;

byte currentTrain = 0;                          // current train being written to
byte bitCount = 0;                              // used for keeping track of what stage in packet program is
byte numberOfTrains = 0;                        // number of trains on track

struct trainStruct{
  byte address;
  byte data;
  byte error;
};

trainStruct train[MAX_TRAINS];

// runs only once at startup
void setup()
{
  DDRD = 0x60;                                  // enable PD5 and PD6 as outputs
  PORTD |= 0x20;                                // set PD5 HIGH
  Serial.begin(9600);
  Timer1.initialize(ONE_BIT);                   // initialize timer1, and set to 56 µs 
  Timer1.attachInterrupt(callback);             // attaches callback()
}

// callback for timer ISR. Gets called every 56-112µs
void callback()                                 
{
  PORTD ^= 0x60;                                // toggle DCC polarity      

  if(0x20 == (PORTD & 0x20))                    // if ready for new bit
  {
    if(numberOfTrains>0)
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
        
        if(currentTrain < numberOfTrains-1)
          currentTrain++;
        else
          currentTrain = 0;  
          
        address = train[currentTrain].address;
        data = train[currentTrain].data;
        error = train[currentTrain].error;   
        bitCount = 0; 
      }
      else if(errorCheck(address,data,error))
        bitCount++;
      else
        bitCount = 0;   
    }
    else
      Timer1.initialize(ONE_BIT);            
  }
}

// main loop
void loop()
{  
  checkForNewData();
}



//**********************************
//***** Begin helper functions *****
//**********************************

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

// check to see if there is new data on the serial port from the PC or Tablet.
// if there is 3 bytes of data, then check to make sure the error byte is correct
// if the error byte checks out, then either add the 3 bytes into the structure or
// edit the structure if the address is already pressent in the structure.
// If the error byte does not match, then clear the serial buffer
void checkForNewData()
{
  trainStruct tempStruct;
  byte i;
  boolean alreadyInStruct = false;
  
  if(Serial.available()>=3)
  {
    tempStruct.address = Serial.read();
    tempStruct.data = Serial.read();
    tempStruct.error = Serial.read();
    if(errorCheck(tempStruct.address,tempStruct.data,tempStruct.error))
    {
      i=0;
      while(i < numberOfTrains-1)
      {
        if(train[i].address == tempStruct.address)
        {
          alreadyInStruct = true;
          break;
        }
        else
          i++;
      }
      if(alreadyInStruct == true)
      {
        train[i].data = tempStruct.data;
        train[i].error = tempStruct.error;
      }
      else
      {
        if(numberOfTrains < MAX_TRAINS)
        {
          train[numberOfTrains].address = tempStruct.address;
          train[numberOfTrains].data = tempStruct.data;
          train[numberOfTrains].error = tempStruct.error;
          numberOfTrains++;
        }             
         //ERROR Too many Trains
      }
    } 
    else
    {      
      while(Serial.available())
        Serial.read();
    }
  }
}
