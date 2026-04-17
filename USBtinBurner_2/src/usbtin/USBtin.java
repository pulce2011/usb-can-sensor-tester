/*
 * This file is part of USBtinLib.
 *
 * Copyright (C) 2014-2015  Thomas Fischl 
 *
 * USBtinLib is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * USBtinLib is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with USBtinLib.  If not, see <http://www.gnu.org/licenses/>.
 */

package usbtin;

import java.util.Queue;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortTimeoutException;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;


class ConnectAnswer{

}



/**
 * Represents an USBtin device.
 * Provide access to an USBtin device (http://www.fischl.de/usbtin) over virtual
 * serial port (CDC).
 *
 * @author Thomas Fischl <tfischl@gmx.de>
 */
public class USBtin implements SerialPortEventListener {

    /** Serial port (virtual) to which USBtin is connected */
    protected SerialPort serialPort;
    
    /** Characters coming from USBtin are collected in this StringBuilder */
    protected StringBuilder incomingMessage = new StringBuilder();
    
    /** Listener for CAN messages */
    protected ArrayList<CANMessageListener> listeners = new ArrayList<CANMessageListener>();
    
    /** Transmit fifo */
    //protected LinkedList<CANMessage> fifoTX = new LinkedList<CANMessage>();

	protected Queue<CANMessage> fifoTX = new ArrayDeque<CANMessage>();

	protected CANMessage lastTxMsg;
	
    /** Timeout for response from USBtin */
    protected static final int TIMEOUT = 1000;



	// dal decoder stringhe ricevute 

	protected String answerFirmwareVersion = null;	// stringhe versione ricevute da evento rx seriale
	protected String answerHardwareVersion = null;
	protected String answerSerialNumber = null;

	protected int cntAckReceived;				// contatore eventi ack ricevuti, utile per verifica ack su comandi

	protected ConnectAnswer connAnswer;			// evento per sync da thread seriale non utilizzabile xche' i comandi
												// di open e close non hanno risposta se non un ACK....

	

    /** Modes for opening a CAN channel */
    public enum OpenMode {
        /** Send and receive on CAN bus */
        ACTIVE,
        /** Listen only, sending messages is not possible */
        LISTENONLY,
        /** Loop back the sent CAN messages. Disconnected from physical CAN bus */
        LOOPBACK
    }

    /**
     * Get firmware version string.
     * During connect() the firmware version is requested from USBtin.
     * 
     * @return Firmware version
     */
    public String getFirmwareVersion() throws USBtinException {
    	try{

			if( answerFirmwareVersion != null )		// risposta precedente  ?
				answerFirmwareVersion = null;

			transmitCmd("v");

			Thread.sleep(50);		// aspetta per una eventuale risposta senza sync
		}
		catch (SerialPortException e) {
            throw new USBtinException(e.getPortName() + " - " + e.getExceptionType());
        } 
		catch (SerialPortTimeoutException e){
            throw new USBtinException("Write Timeout!!");
        } 
		catch (InterruptedException e) 
        {
            throw new USBtinException(e);
        }                                

		return answerFirmwareVersion;
    }

    /**
     * Get hardware version string.
     * During connect() the hardware version is requested from USBtin.
     * 
     * @return Hardware version
     */
    public String getHardwareVersion() throws USBtinException{
    	try{

			if( answerHardwareVersion != null )
				answerHardwareVersion = null;

			transmitCmd("V");

			Thread.sleep(50);		// aspetta per una eventuale risposta senza sync
		}
		catch (SerialPortException e) {
            throw new USBtinException(e.getPortName() + " - " + e.getExceptionType());
        } 
		catch (SerialPortTimeoutException e){
            throw new USBtinException("Write Timeout!!");
        } 
		catch (InterruptedException e) 
        {
            throw new USBtinException(e);
        }                                

		return answerHardwareVersion;
    }

    /**
     * Get serial number string.
     * During connect() the serial number is requested from USBtin.
     *
     * @return Serial number
     */
    public String getSerialNumber() throws USBtinException{

    	try{

    		transmitCmd("N");

			Thread.sleep(50);		// aspetta per una eventuale risposta senza sync
		}
		catch (SerialPortException e) {
            throw new USBtinException(e.getPortName() + " - " + e.getExceptionType());
        } 
		catch (SerialPortTimeoutException e){
            throw new USBtinException("Write Timeout!!");
        } 
		catch (InterruptedException e) 
        {
            throw new USBtinException(e);
        }                                

        return answerSerialNumber;
    }

    /**
     * Connect to USBtin on given port.
     * Opens the serial port, clears pending characters and send close command
     * to make sure that we are in configuration mode.
     * 
     * @param portName Name of virtual serial port
     * @throws USBtinException Error while connecting to USBtin
     */
    public boolean connectSerialPort(String portName) throws USBtinException {


		boolean connected = false;

        // create serial port object
        serialPort = new SerialPort(portName);

		try {
            
            // open serial port and initialize it
            serialPort.openPort();
            serialPort.setParams(115200, 8, 1, 0);
            serialPort.purgePort(SerialPort.PURGE_RXCLEAR | SerialPort.PURGE_TXCLEAR);

            // register serial port event listener
            serialPort.setEventsMask(SerialPort.MASK_RXCHAR);
            serialPort.addEventListener(this);

			connected = true;
        } 
		catch (SerialPortException e) 
		{
            throw new USBtinException(e.getPortName() + " - " + e.getExceptionType());
        } 

		return connected;
    }
    
    /**
     * Disconnect.
     * Close serial port connection
     * 
     * @throws USBtinException Error while closing connection
     */
    public void disconnectSerialPort() throws USBtinException {
        
        try {
            serialPort.removeEventListener();			
            serialPort.closePort();
        } catch (SerialPortException e) {
            throw new USBtinException(e.getExceptionType());
        }    
    }

    
    /**
     * Open CAN channel.
     * Set given baudrate and open the CAN channel in given mode.
     * 
     * @param baudrate Baudrate in bits/second
     * @param mode CAN bus accessing mode
     * @throws USBtinException Error while opening CAN channel
     */
    public boolean openCANChannel(OpenMode mode) throws USBtinException {

		int cntAckNow = cntAckReceived;
		boolean opened = false;
        char modeCh;

		try {

            // open can channel			
            switch (mode) 
			{
                default:
                    System.err.println("Mode " + mode + " not supported. Opening listen only.");
                case LISTENONLY: modeCh = 'L'; break;
                case LOOPBACK: modeCh = 'l'; break;
                case ACTIVE: modeCh = 'O'; break;
            }

			this.transmitCmd(modeCh + "");

			Thread.sleep(50);		// aspetta per una eventuale risposta senza sync

            System.out.println(String.format("openCANChannel (%c)-> %d", modeCh, cntAckReceived - cntAckNow));

			opened = cntAckReceived == cntAckNow +1;
        }
		catch (SerialPortException e) {
            throw new USBtinException(e);
        } 
		catch (SerialPortTimeoutException e) {
            throw new USBtinException("Timeout! USBtin doesn't answer. Right port?");            
        }
		catch (InterruptedException e) {
            throw new USBtinException(e);
        }                                

		return opened;
    }

    /**
     * Close CAN channel.
     * 
     * @throws USBtinException Error while closing CAN channel
     */
    public boolean closeCANChannel() throws USBtinException {

		int cntAckNow = cntAckReceived;
		boolean closed = false;

        try {

			fifoTX.clear();

            transmitCmd("C");

			Thread.sleep(50);		// aspetta per una eventuale risposta senza sync: il proto non prevede una risposta formata

            System.out.println(String.format("closeCANChannel -> %d", cntAckReceived - cntAckNow));

			closed = cntAckReceived == cntAckNow + 1;
		} 
		catch (SerialPortException e) {
            throw new USBtinException(e.getPortName() + " - " + e.getExceptionType());
        } 
		catch (SerialPortTimeoutException e){
            throw new USBtinException("Write Timeout!!");
        } 
		catch (InterruptedException e) {
            throw new USBtinException(e);
        }                                

		return closed;
    }


    /**
     * Setup CAN baudrate.
     * Set given baudrate 
     * 
     * @param baudrate Baudrate in bits/second
     * @param mode CAN bus accessing mode
     * @throws USBtinException Error while opening CAN channel
     */
    public boolean setCANBaudrate(int baudrate) throws USBtinException {

		int cntAckNow = cntAckReceived;
		boolean baud_set = false;

		try {

            // set baudrate
            char baudCh = ' ';

            switch (baudrate) {
                case 10000: baudCh = '0'; break;
                case 20000: baudCh = '1'; break;
                case 50000: baudCh = '2'; break;
                case 100000: baudCh = '3'; break;
                case 125000: baudCh = '4'; break;
                case 250000: baudCh = '5'; break;
                case 500000: baudCh = '6'; break;
                case 800000: baudCh = '7'; break;
                case 1000000: baudCh = '8'; break;
            }
            
            if (baudCh != ' ') 
			{
                // use preset baudrate
                this.transmitCmd("S" + baudCh);

				Thread.sleep(50);		// aspetta per una eventuale risposta senza sync

	            System.out.println(String.format("setCANBaudrate (%c) -> %d", baudCh, cntAckReceived - cntAckNow));

				baud_set = cntAckReceived == cntAckNow + 1;
            } 
			else
			{
                System.out.println("No preset for given baudrate !!");
            }

        }
		
		catch (SerialPortException e) {
            throw new USBtinException(e);
        } 
		catch (SerialPortTimeoutException e) {
            throw new USBtinException("Timeout! USBtin doesn't answer. Right port?");            
        }
		catch (InterruptedException e) {
            throw new USBtinException(e);
        }                                

		return baud_set;
    }


    /**
     * Transmit given command to USBtin
     *
     * @param cmd Command
     * @return Response from USBtin
     * @throws SerialPortException Error while talking to USBtin
     * @throws SerialPortTimeoutException Timeout of serial port
     */
    public void transmitCmd(String cmd) throws SerialPortException, SerialPortTimeoutException {

        String cmdline = cmd + "\r";
        serialPort.writeBytes(cmdline.getBytes());
    }


    /**
     * Handle serial port event.
     * Read single byte and check for end of line character.
     * If end of line is reached, parse command and dispatch it.
     * 
     * @param event Serial port event
     */
    @Override
    public void serialEvent(SerialPortEvent event) {

		// System.out.println("Serial Event!!");

        if (event.isRXCHAR() && event.getEventValue() > 0) 
		{
            try {

				byte buffer[] = serialPort.readBytes();

                for (byte b : buffer) 
				{
					if( (b == '\r') )	// endof string found 
					{
					    if( incomingMessage.length() > 0 )
						{
							parseIncoming();

							incomingMessage.setLength(0);
						}
						else
						{
							cntAckReceived++;	// semplice ack ricevuto
						}
					}
					else
					{ 
						if (b == 0x07) 	// BELL ?? need to repeat 
		    			{
							if( lastTxMsg != null )
							{
								System.out.println("Bell: Repet Last Tx msg !!");

						        try 
								{
						            serialPort.writeBytes((lastTxMsg.toString() + "\r").getBytes());
						        } 
								catch (SerialPortException ex) 
						        {
						            System.err.println(ex);
						        }
							}
					    } 
						else
						{
                        	incomingMessage.append((char) b);
						}
                    }
                }
            } 
			catch (SerialPortException ex) {
                System.err.println(ex);
            }
        }
    }


	private void parseIncoming(){

        String message = incomingMessage.toString();
        char cmd = message.charAt(0);

        // check if this is a CAN message
        if (cmd == 't' || cmd == 'T' || cmd == 'r' || cmd == 'R') 
		{
            // create CAN message from message string
            CANMessage canmsg = new CANMessage(message);
        
            // give the CAN message to the listeners
            for (CANMessageListener listener : listeners) 
			{
                listener.receiveCANMessage(canmsg);
            }
        } 
		else
		{
			switch(cmd)
			{
				case	'Z':
				case	'z':
	                try 
					{
	                	// remove first message from transmit fifo and send next one

	                    sendNextTXFifoMessage();
	                } 
					catch (USBtinException ex) 
					{
	                    System.err.println(ex);
	                }
					break;
					

				case 'V': // Get hardware version
					answerHardwareVersion = incomingMessage.substring(1);
					break;
					
        		case 'v': // Get firmware version
					answerFirmwareVersion = incomingMessage.substring(1);
					break;

		        case 'N': // Get serial number					
					answerSerialNumber = incomingMessage.substring(1);
					break;

			}
            
        }
	       
	}

    
    /**
     * Add message listener
     * 
     * @param listener Listener object
     */
    public void addMessageListener(CANMessageListener listener) {
        listeners.add(listener);
    }
    
    /**
     * Remove message listener.
     * 
     * @param listener Listener object
     */
    public void removeMessageListener(CANMessageListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Send first message in tx fifo
     * 
     * @throws USBtinException On serial port errors
     */
    protected void sendNextTXFifoMessage() throws USBtinException {
        
//        if( fifoTX.size() == 0 ) 
//		{
//            System.out.println("Tx Fifo is empty !!");
//            return;
//        }

		lastTxMsg = fifoTX.poll();

		if( lastTxMsg != null )
		{
	        try {
	            serialPort.writeBytes((lastTxMsg.toString() + "\r").getBytes());
	        } catch (SerialPortException e) {
	            throw new USBtinException(e);
	        }
		}
    }
    
    /**
     * Send given can message.
     * 
     * @param canmsg Can message to send
     * @throws USBtinException  On serial port errors
     */
    public void send(CANMessage canmsg) throws USBtinException {
        
        fifoTX.add(canmsg);

        if ( fifoTX.size() > 1 ) 
			return;
        
        sendNextTXFifoMessage();
    }
    
    /**
     * Write given register of MCP2515
     * 
     * @param register Register address
     * @param value Value to write
     * @throws USBtinException On serial port errors
     */
    public void writeMCPRegister(int register, byte value) throws USBtinException {
        
        try {
            
            String cmd = "W" + String.format("%02x", register) + String.format("%02x", value);
            transmitCmd(cmd);
            
        } catch (SerialPortException e) {
            throw new USBtinException(e);
        } catch (SerialPortTimeoutException e) {
            throw new USBtinException("Timeout! USBtin doesn't answer. Right port?");            
        }               
    }
    
    /**
     * Write given mask registers to MCP2515
     * 
     * @param maskid Mask identifier (0 = RXM0, 1 = RXM1)
     * @param registers Register values to write
     * @throws USBtinException On serial port errors
     */
    protected void writeMCPFilterMaskRegisters(int maskid, byte[] registers) throws USBtinException {
        
        for (int i = 0; i < 4; i++) {
            writeMCPRegister(0x20 + maskid * 4 + i, registers[i]);
        }                
    }
    
    /**
     * Write given filter registers to MCP2515
     * 
     * @param filterid Filter identifier (0 = RXF0, ... 5 = RXF5)
     * @param registers Register values to write
     * @throws USBtinException On serial port errors
     */
    protected void writeMCPFilterRegisters(int filterid, byte[] registers) throws USBtinException {

        int startregister[] = {0x00, 0x04, 0x08, 0x10, 0x14, 0x18};
        
        for (int i = 0; i < 4; i++) {
            writeMCPRegister(startregister[filterid] + i, registers[i]);
        }        
    }
    
    /**
     * Set hardware filters.
     * Call this function after connect() and before openCANChannel()!
     * 
     * @param fc Filter chains (USBtin supports maximum 2 hardware filter chains)
     * @throws USBtinException On serial port errors
     */
    public void setFilter(FilterChain[] fc) throws USBtinException {

        /*
         * The MCP2515 offers two filter chains. Each chain consists of one mask
         * and a set of filters:
         * 
         * RXM0         RXM1
         *   |            |
         * RXF0         RXF2
         * RXF1         RXF3
         *              RXF4
         *              RXF5
         */
        
        // if no filter chain given, accept all messages
        if ((fc == null) || (fc.length == 0)) {

            byte[] registers = {0, 0, 0, 0};
            writeMCPFilterMaskRegisters(0, registers);
            writeMCPFilterMaskRegisters(1, registers);

            return;
        }
        
        // check maximum filter channels
        if (fc.length > 2) {
            throw new USBtinException("Too many filter chains: " + fc.length + " (maximum is 2)!");
        }
        
        // swap channels if necessary and check filter chain length
        if (fc.length == 2) {
            
            if (fc[0].getFilters().length > fc[1].getFilters().length) {
                FilterChain temp = fc[0];
                fc[0] = fc[1];
                fc[1] = temp;
            }
            
            if ((fc[0].getFilters().length > 2) || (fc[1].getFilters().length > 4)) {
                throw new USBtinException("Filter chain too long: " + fc[0].getFilters().length + "/" + fc[1].getFilters().length + " (maximum is 2/4)!");
            }
            
        } else if (fc.length == 1) {
            
            if ((fc[0].getFilters().length > 4)) {
                throw new USBtinException("Filter chain too long: " + fc[0].getFilters().length + " (maximum is 4)!");
            }
        }
        
        // set MCP2515 filter/mask registers; walk through filter channels
        int filterid = 0;
        int fcidx = 0;
        for (int channel = 0; channel < 2; channel++) {
                                    
            // set mask
            writeMCPFilterMaskRegisters(channel, fc[fcidx].getMask().getRegisters());
           
            // set filters
            byte[] registers = {0, 0, 0, 0};
            for (int i = 0; i < (channel == 0 ? 2 : 4); i++) {

                if (fc[fcidx].getFilters().length > i) {
                    registers = fc[fcidx].getFilters()[i].getRegisters();
                }

                writeMCPFilterRegisters(filterid, registers);
                filterid++;
            }
            
            // go to next filter chain if available
            if (fc.length - 1 > fcidx) {
                fcidx++;
            }
        }
    }
}
