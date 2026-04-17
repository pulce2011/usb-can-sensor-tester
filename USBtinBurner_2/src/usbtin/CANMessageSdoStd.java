/*
 * This file is part of USBtinLib.
 *
 * Copyright (C) 2014  Thomas Fischl 
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


public class CANMessageSdoStd extends CANMessage{
  /**
     * Create message with given message properties.
     * 
     * @param id Message identifier
     * @param obj_idx sdo obj index
     * @param sub_idx sdo obj sub-index
     * @param data Payload data
     * @param 
     * @param 
     */
    public CANMessageSdoStd(int id, int fn_code, int obj_idx, int sub_idx, byte[] data) {
        setId(id);
        this.extended = false;
        this.rtr = false;
		this.jumbo = false;
		this.sdo_block = false;

		if( data.length <= 4 )	// considera frames standard 
		{
		 	System.out.println("CANMessage SDO expedited:" + String.format(" len = %d - sub= %02X", data.length, sub_idx));

			this.data = new byte[8];		// per sdo lunghezza fissa a 8bytes
	        this.dlc = 8;

			this.data[0] = (byte) (fn_code & 0xFF);
	        this.data[1] = (byte) (obj_idx & 0xff);
	        this.data[2] = (byte) ((obj_idx >> 8) & 0xff);
	        this.data[3] = (byte) sub_idx;

			for (int i = 0; i < data.length; i++) {
	            this.data[4 + i] = data[i];
	        }
		}
		else					// considera frames segmentati  
		{
		 	System.out.println("CANMessage SDO segmented:" + String.format(" len = %d", data.length));

			this.data = new byte[data.length+4];		// per sdo lunghezza fissa a 8bytes
	        this.dlc = data.length+4;
	        this.extended = false;
	        this.rtr = false;		
		}
    }  

 /**
     * Create message with given message properties.
     * 
     * @param id Message identifier
     * @param obj_idx sdo obj index
     * @param sub_idx sdo obj sub-index
     * @param data Payload data
     * @param 
     * @param 
     */
    public CANMessageSdoStd(int id, int fn_code, int obj_idx, int sub_idx, int long_int_value) {
        setId(id);

		System.out.println(String.format("CANMessageSdoStd long int: %08X", long_int_value));

		this.data = new byte[8];		// per sdo lunghezza fissa a 8bytes
        this.dlc = 8;
        this.extended = false;
        this.rtr = false;

		this.data[0] = (byte) (fn_code & 0xFF);
        this.data[1] = (byte) (obj_idx & 0xff);
        this.data[2] = (byte) ((obj_idx >> 8) & 0xff);
        this.data[3] = (byte) sub_idx;
		this.data[4] = (byte) (long_int_value & 0xFF);
        this.data[5] = (byte) ((long_int_value >> 8) & 0xff);
        this.data[6] = (byte) ((long_int_value >> 16) & 0xff);
        this.data[7] = (byte) ((long_int_value >> 24) & 0xff);
    }    



  
}



