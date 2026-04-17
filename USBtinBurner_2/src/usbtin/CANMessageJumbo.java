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


public class CANMessageJumbo extends CANMessage{


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
    public CANMessageJumbo(int id, int fn_code, int obj_idx, byte[] data) {


	    this.dlc = data.length;
	    this.extended = false;
	    this.rtr = false;
		this.jumbo = true;
		this.sdo_block = false;

		this.data = new byte[data.length + 4];		// per sdo lunghezza fissa a 8bytes

		setId(id);

		int sub_index = 0x80;

	 	System.out.println("CANMessage SDO Jumbo:" + String.format("Fc=%02X, Index=%04X, SubIdx=%02X, len = %d", fn_code, obj_idx, sub_index, data.length));


			// nel pacchetto jumbo estraiamo index solo sul primo frame, gli altri dati del pacchetto
			// contengono solo dati e non index + dubindex
		int total_frames = data.length / 4; 

		this.data[0] = (byte) (fn_code & 0xFF);
	    this.data[1] = (byte) (obj_idx & 0xff);
	    this.data[2] = (byte) ((obj_idx >> 8) & 0xff);
		this.data[3] = (byte) sub_index;		// per i dati partiamo da subindex 0x80

		for(int nf=0; nf<total_frames; nf++)
		{

			int frame_offset = (nf * 4);	// aggiunge 4 bytes info per SDO 

		 	//System.out.println(String.format("frame offset= %d", frame_offset + 4));

			for (int i = 0; i < 4; i++) 
			{
	            this.data[frame_offset + 4 + i] = data[frame_offset + i];
	        }
		}
    
	}




}


