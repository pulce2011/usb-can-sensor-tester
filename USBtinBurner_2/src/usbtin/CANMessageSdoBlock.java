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

public class CANMessageSdoBlock extends CANMessage{


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
    public CANMessageSdoBlock(int id, int fn_code, int obj_idx, byte[] data) {

		this.data = new byte[data.length];		// per sdo lunghezza fissa a 8bytes
	    this.dlc = data.length;
	    this.extended = false;
	    this.rtr = false;
		this.jumbo = false;
		this.sdo_block = true;

		setId(id);

	 	System.out.println("CANMessage SDO Blocks:" + String.format(" len = %d", data.length));

		int total_frames = (data.length / 8); 

		for(int nf=0; nf<total_frames; nf++)
		{

			int frame_offset = nf * 8;	// per SDO tutti frame da 8bytes

		 	// System.out.println(String.format("frame offset= %d", frame_offset));

			this.data[frame_offset+0] = (byte) (fn_code & 0xFF);
	        this.data[frame_offset+1] = (byte) (obj_idx & 0xff);
	        this.data[frame_offset+2] = (byte) ((obj_idx >> 8) & 0xff);

			this.data[frame_offset+3] = data[frame_offset + 3];		// mantiene sub_index richiesto 

			for (int i = 0; i < 4; i++) 
			{
	            this.data[frame_offset+4 + i] = data[frame_offset + 4 + i];
	        }
		}
	}


}


    

