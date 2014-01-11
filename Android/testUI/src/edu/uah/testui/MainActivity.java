package edu.uah.testui;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

public class MainActivity extends Activity {
	
	final int MAX_TRAINS = 10;
	
	EditText edtxtCustomCommAdd; 		//edit text for custom address
	EditText edtxtCustomCommSpeed;		//edit text for custom speed
	
	SeekBar skbarSpeed;					//seek bar for speed control
	Spinner spnTrain;					//spinner for train selection
	Button btnSend;						//button for sending packet
	Button btnDirection;				//button for changing direction
	Button btnAdd;						//button for adding a train
	Button btnConnect;					//button for connecting to bluetooth
	CheckBox chkbxRawComm;				//check box for raw command or speed/direction
	TextView txtvwStatus;				//textview for Status
	List<String> list;					//string list for train names
	ArrayAdapter<String> dataAdapter;	//adapter for spinner and string list connection
	int[] trainNum;						//int array to hold train numbers
	int index = 0;						//current index to train array
	
	private static String address = "00:06:66:61:7A:AA";
    private static final UUID MY_UUID = UUID
                    .fromString("00001101-0000-1000-8000-00805F9B34FB");

	
	BluetoothAdapter btAdapter = null;
	BluetoothDevice btDevice = null;
	BluetoothSocket btSocket = null;
	OutputStream btOutStream = null;
	InputStream btInStream = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d("OnTrack", "On Create");
        init();							//initialize all values and tie ui components to code
        setupButtonListeners();			//sets up button listeners for each button
        BTSetup();
        
    }
    
    public void Connect() {
    	if(BluetoothAdapter.checkBluetoothAddress(address)){
		BluetoothDevice device = btAdapter.getRemoteDevice(address);
		Log.d("OnTrack", "Connecting to ... " + device);
		btAdapter.cancelDiscovery();
		Log.d("OnTrack", "Canceled Discovery");
		try {

			Log.d("OnTrack", "trying to create socket");
              btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
/* Here is the part the connection is made, by asking the device to create a RfcommSocket (Unsecure socket I guess), It map a port for us or something like that */
			btSocket.connect();
			Log.d("OnTrack", "Connection made.");
		} catch (IOException e) {

			Log.d("OnTrack", "failed to create socket");
			try {
				btSocket.close();
			} catch (IOException e2) {
				Log.d("OnTrack", "Unable to end the connection");
			}
			Log.d("OnTrack", "Socket creation failed");
		}
    	}
    	else
    		Log.d("OnTrack", "Bad Address");
               /* this is a method used to read what the Arduino says for example when you write Serial.print("Hello world.") in your Arduino code */
	}
    
    private void BTSetup(){
    	btAdapter = BluetoothAdapter.getDefaultAdapter();
    	if(btAdapter != null){
    		if(!btAdapter.isEnabled()){
    			Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
    			startActivityForResult(enableBluetooth, 0);
    		}
    		else
    		{
    			Log.d("OnTrack", "BT is Enabled.");
    		}
    		
    	}
    	else{
    		Log.d("OnTrack", "BT is null");
    	
    	}
    }
    
    private void init(){
    	Log.d("OnTrack", "Starting Init");
    	/*
    	 * Handles all of the UI components
    	 * Sets up the train int array
    	 * Sets up the max for the seekbar
    	 * Defaults the direction to forward.
    	 * Adds a few trains to the string list array and the int array
    	 * 		while increasing the index each time.
    	 * Attaches the list to the spinner via an adapter
    	 */
		edtxtCustomCommAdd = (EditText) findViewById(R.id.edtxtCustCommAddress);
		edtxtCustomCommSpeed = (EditText) findViewById(R.id.edtxtCustCommSpeed);
		skbarSpeed = (SeekBar) findViewById(R.id.skbarSpeed);
		spnTrain = (Spinner) findViewById(R.id.spnTrain);
		btnSend = (Button) findViewById(R.id.btnSend);
		btnDirection = (Button) findViewById(R.id.btnDirection);
		btnAdd = (Button) findViewById(R.id.btnAdd);
		btnConnect = (Button) findViewById(R.id.btnConnect);
		txtvwStatus = (TextView) findViewById(R.id.txtvwStatus);
		chkbxRawComm = (CheckBox)findViewById(R.id.chkbxRawComm);
		trainNum = new int[MAX_TRAINS];
		 
		skbarSpeed.setMax(31);
		btnDirection.setText("Forward");
		//edtxtCustomCommAdd.setText("");
		//edtxtCustomCommSpeed.setText("");
		 
		 
		list = new ArrayList<String>();
		
		//default starting values
		trainNum[0] = 1;
		index++;
		list.add("Train 1");
		trainNum[1] = 2;
		index++;
		list.add("Train 2");
		trainNum[2] = 3;
		index++;
		list.add("Train 3");
		trainNum[3] = 5;
		index++;
		list.add("Train 5");
		trainNum[4] = 15;
		index++;
		list.add("Train 15");
		
		dataAdapter = new ArrayAdapter<String>(this,
			android.R.layout.simple_spinner_item, list);
		dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spnTrain.setAdapter(dataAdapter);
    }
    
    private void sendMessage(int train, int speed, int checksum){
    	/*
    	 * Currently only sets the custom text edit box to what should be sent.
    	 */
    	
    	txtvwStatus.setText("Sending... \" train# 0x"+ Integer.toHexString(train) + " commandbits: 0x"+ Integer.toHexString(speed) + " checksum: 0x" + Integer.toHexString(checksum) + "\".");
		byte[] buffer = new byte[3];
    	buffer[0] = (byte)(train & 0xff);
    	buffer[1] = (byte)(speed & 0xff);
    	buffer[2] = (byte)(checksum & 0xff);
    	txtvwStatus.setText("Sending... \" train# 0x"+ Integer.toHexString(buffer[0]) + " commandbits: 0x"+ Integer.toHexString(buffer[1]) + " checksum: 0x" + Integer.toHexString(buffer[2]) + "\".");
		
    	try {
			btOutStream = btSocket.getOutputStream();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
    	if(buffer != null)
    		try {
    				btOutStream.write(buffer);
    				btOutStream.flush();
    		} catch (IOException e) {
    			e.printStackTrace();
    		}
    	edtxtCustomCommAdd.setText("");
		edtxtCustomCommSpeed.setText("");
    }
    
    public void btnSendListener(){
    	if(btSocket != null && btSocket.isConnected()){ //check to see if it is not null, then see if it is connected.
    		
			// variables
    		int address = 0;
			int speed = 0;
			int commandbits = 0;
			if(edtxtCustomCommAdd.getText().toString().length() > 0 && edtxtCustomCommSpeed.getText().toString().length() > 0 && chkbxRawComm.isChecked()){
				try {
					address =  Integer.parseInt(edtxtCustomCommAdd.getText().toString());
					commandbits =  Integer.parseInt(edtxtCustomCommSpeed.getText().toString());
				}
				catch(NumberFormatException nfe) {
		        	   System.out.println("Could not parse " + nfe);
		        	   txtvwStatus.setText("Could not Parse");
	        	} 
			}
			else if(edtxtCustomCommAdd.getText().toString().length() > 0 && edtxtCustomCommSpeed.getText().toString().length() > 0){
				try {
					address =  Integer.parseInt(edtxtCustomCommAdd.getText().toString());
					speed =  Integer.parseInt(edtxtCustomCommSpeed.getText().toString());
				}
				catch(NumberFormatException nfe) {
		        	   System.out.println("Could not parse " + nfe);
		        	   txtvwStatus.setText("Could not Parse");
	        	}
				if(speed <= 31 && speed >= 0){
					for (int i = 1; i <= speed; i++){
						commandbits^= 0x10;
						if (i%2 == 0)
							commandbits+= 0x01;							
					}
			
			
					if(btnDirection.getText() == "Forward" )
						commandbits^= 96; //64 is for the 01 in the packet format 32 for the forward direction
					else
						commandbits^= 64;
				}
				else{
					txtvwStatus.setText("Speed not in range for non-raw input, please use 0-32");
				}
					
			}
			else  {
				address = trainNum[spnTrain.getSelectedItemPosition()];
				speed = skbarSpeed.getProgress();
				for (int i = 1; i <= speed; i++){
					commandbits^= 0x10;
					if (i%2 == 0)
						commandbits+= 0x01;							
				}
			
				if(btnDirection.getText() == "Forward" )
					commandbits^= 96; //64 is for the 01 in the packet format 32 for the forward direction
				else
					commandbits^= 64;
			}
			int checksum = address ^ commandbits;
			try{
				sendMessage(address, commandbits, checksum);
			}
			finally{
				Log.d("OnTrack", "Sent Message");
			}
    	}
    else
    	txtvwStatus.setText("Bluetooth Not Connected!");
    }
    
    public void btnAddListener(){

		final EditText input = new EditText(MainActivity.this);
		if(index < MAX_TRAINS){			//Checks to make sure the maximum number of trains hasn't been exceeded.
			new AlertDialog.Builder(MainActivity.this)
		    .setTitle("Add A Train")
		    .setMessage("Please input the train number")
		    .setView(input)
		    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
		        public void onClick(DialogInterface dialog, int whichButton) {
		        	int value;
		        	try {
		        	    value = Integer.parseInt(input.getText().toString());
		        	    if (value >= 0 && value <= 127){
			        	    trainNum[index] = value;
				            index++;
				            list.add("Train " + value);
		        	    }
		        	    else
		        	    	txtvwStatus.setText("Train out of range.");
		        	} catch(NumberFormatException nfe) {
		        	   System.out.println("Could not parse " + nfe);
		        	} 
		        	
		        }
		    }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
		        public void onClick(DialogInterface dialog, int whichButton) {
		            // Do nothing.
		        }
		    }).show();
		}
		else
			txtvwStatus.setText("No more trains allowed.");
	}
    
    
    private void setupButtonListeners(){

	    btnSend.setOnClickListener(new View.OnClickListener() {
			/*
			 * (non-Javadoc)
			 * @see android.view.View.OnClickListener#onClick(android.view.View)
			 * Description: Send button handler.
			 * Expected Results: Capture current train being selected,
			 * 		calculate speed value from the seek bar position.
			 * 		calculate checksum from train address and speed information.
			 * 		Call send message function with formed message. 		
			 */
			@Override
			public void onClick(View arg0) {
				btnSendListener();
			}
		});
	    btnAdd.setOnClickListener(new View.OnClickListener() {
    		/*
    		 * (non-Javadoc)
    		 * @see android.view.View.OnClickListener#onClick(android.view.View)
    		 * Description: Add button handler
    		 * Expected Results: Opens dialog for input. Should only take integers
    		 * 		between 1 and 127, 0 is reserved for broadcast messages.
    		 * 		Currently takes the address number and appends it to "Train "
    		 * 		to handle naming from the list, future should allow naming of trains.
    		 * 		Adds to both the string list for selection, and also the int array for
    		 * 		easier calculation.
    		 */
			@Override
			public void onClick(View arg0) {
				btnAddListener();
			}
		});
	    
	    btnDirection.setOnClickListener(new View.OnClickListener() {
			/*
			 * (non-Javadoc)
			 * @see android.view.View.OnClickListener#onClick(android.view.View)
			 * Description: On click listener for direction button.
			 * Expected Results: Click button to toggle between forward and reverse.
			 * 		Logic is inside the send function to control messages related to
			 * 		direction, simply toggles the button text.
			 */
			@Override
			public void onClick(View arg0) {
				if (btnDirection.getText() == "Forward")
					btnDirection.setText("Reverse");
				else
					btnDirection.setText("Forward");
			}
		});
	    btnConnect.setOnClickListener(new View.OnClickListener() {
			/*
			 * (non-Javadoc)
			 * @see android.view.View.OnClickListener#onClick(android.view.View)
			 * Description: Send button handler.
			 * Expected Results: Capture current train being selected,
			 * 		calculate speed value from the seek bar position.
			 * 		calculate checksum from train address and speed information.
			 * 		Call send message function with formed message. 		
			 */
			@Override
			public void onClick(View arg0) {
				Connect();
			}
		});
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
}
