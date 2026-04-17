

public class SREC {

	//all data are to be considered as hexadecimal!!!
	private String record = null;

	public String type = null; //2 // chars ex. S3
	public String counter = null; //2 // chars, address + data + checksum
	public String address = null; //2 //to 4 chars
	public String data = null; //up to 249 bytes
	public String cksum = null; //2 //chars
	public int 	  dataSize;		// numero di bytes dati utili
	public int    dataAddress; 	//2 // numero di bytes dati utili	
	public int    dataOffset;
	public byte[] dataBytes = null; //2 // data bytes 
	
	public SREC(String line) throws NumberFormatException
	{
		record = line;
		type = record.substring(0, 2);
		counter = record.substring(2, 4);


		if(type.equalsIgnoreCase("S0") || type.equalsIgnoreCase("S1") || type.equalsIgnoreCase("S5") || type.equalsIgnoreCase("S9"))
		{
			address = record.substring(4, 8);
			data = record.substring(8, record.length() - 2);

			dataAddress = Integer.parseInt(address, 16);
			dataSize = (Integer.parseInt(counter, 16) -2);
			dataOffset = 8;
		}
		else
		{
			if(type.equalsIgnoreCase("S2") || type.equalsIgnoreCase("S8"))
			{
				address = record.substring(4, 10);
				data = record.substring(10, record.length() - 2);

				dataAddress = Integer.parseInt(address, 16);
				dataSize = (Integer.parseInt(counter, 16) -4);
				dataOffset = 10;
			}
			else
			{
				if(type.equalsIgnoreCase("S3") || type.equalsIgnoreCase("S7"))
				{
					address = record.substring(4, 12);
					data = record.substring(12, record.length() - 2);

					dataAddress = Integer.parseInt(address, 16);
					dataSize = (Integer.parseInt(counter, 16) -6);
					dataOffset = 12;
				}
			}
		}
		cksum = record.substring(record.length() - 2, record.length());

		if( dataSize != 0 )
			dataBytes = new byte[dataSize];

		if( dataBytes != null )
		{
			for (int i = 0; i < dataSize; i++) 
			{
            	try 
				{
                	dataBytes[i] = (byte) Integer.parseInt(data.substring(i*2, i*2+2), 16);
            	} 
				catch (java.lang.NumberFormatException e) 
            	{
                	dataBytes[i] = 0;
				}
            }
        }    
	}
	
}
