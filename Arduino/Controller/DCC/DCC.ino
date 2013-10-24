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
#define MAX_TRAINS 10                           // Maximum number of trains we can address

typedef struct msg{                             // Structure of message to be sent
  byte address;                                 // Address of DCC decoder
  byte data;                                    // Data to be sent to DCC decoder
  byte error;                                   // Error Checksum
};

msg currentTrain;                               // Single copy of train to hold current message being sent.

msg trainQueue[MAX_TRAINS];                     // Array of trains

byte countSent = 0;                             // Counter for sent messages
byte countTrains = 0;                           // Counter for active trains, 
                                                // assuming sequentially located in the array

byte bitCount = 0;                              // used for keeping track of what stage in packet program is

void setup()
{
  DDRD = 0x60;                                  // enable PB5 and PB6 as outputs
  PORTD |= 0x20;                                // set PB5 HIGH
  Timer1.initialize(ONE_BIT);                   // initialize timer1, and set to 56 µs 
  Timer1.attachInterrupt(callback);             // attaches callback() as a timer overflow interrupt
  
  trainQueue[0].address = 0x03;                 // statically assigning train 0 in array to physical train 3
  trainQueue[0].data = 0x78;                    // statically assigning train 0 to go forward at step 14
  trainQueue[0].error = 0x7b;                   // checksum of address ^ data
  countTrains++;                                // increase train count

  trainQueue[0].address = 0x05;                 // statically assigning train 0 in array to physical train 3
  trainQueue[0].data = 0x78;                    // statically assigning train 0 to go forward at step 14
  trainQueue[0].error = 0x7d;                   // checksum of address ^ data
  countTrains++;                                // increase train count

  currentTrain = trainQueue[0];                 // sets the current train to the first in the queue.
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
      setBit(currentTrain.address,(22-bitCount));            // send Address Byte

    else if(bitCount == 23)
      Timer1.initialize(ZERO_BIT);              // send Data Start Bit
    else if(bitCount > 23 && bitCount < 32)
      setBit(currentTrain.data,(31-bitCount));               // send Data Byte

    else if(bitCount == 32)
      Timer1.initialize(ZERO_BIT);              // send Error Start Bit
    else if(bitCount > 32 && bitCount < 41)
      setBit(currentTrain.error,(40-bitCount));              // send Error Byte

    else if(bitCount == 41)
      Timer1.initialize(ONE_BIT);               // send Packet End Bit

    if(bitCount > 41)
    {
      bitCount = 0;                                   // reset bit counter
      countSent ++;                                   // increase sent message count
      byte nextTrain = (countSent % countTrains);     // [# msgs sent] modulo [number of trains]
      currentTrain = trainQueue[nextTrain];           // so that you only loop thru active trains
    }
    else if(errorCheck(currentTrain.address,currentTrain.data,currentTrain.error))
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



