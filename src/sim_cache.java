import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

class sim_cache {
	public static void main(String[] args) {
		new cache(args);
	}
} 

class cache {
	public int[] input;
	public String traceFile;
	public String[] address;
	public List<String[]> addressAfterSplit= new ArrayList<String[]>();
	public List<String[]> addressAfterSplitL2 = new ArrayList<String[]>();
	public String[][] cacheL1;
	public String[][] cacheL2;
	public String[][] optimalCounterL2;
	public String[][] optimalAddCounterL2;
	public String[][] optimalCounterL1;
	public String[][] optimalAddCounterL1;
	public boolean[][] dirtyBlocksL1;
	public boolean[][] dirtyBlocksL2;
	public int[][] countQueue;
	public int[][] countLRU;
	public int[][] countQueueL2;
	public int[][] countLRUL2;

	//0-reads, 1-read misses, 2-writes, 3-write misses, 4-write backs
	public int[] L1Tracker;
	public int[] L2Tracker;

	public int cacheBlockCountL1;
	public int setCountL1;
	public int indexBitsCountL1;
	public int offset;
	public int bitsCountL1;
	public int setCountL2;
	public int indexBitsCountL2;
	public int bitsCountL2;

	public int conflictMissRate=0; 
	public int memTraffic=0;
	
	public cache(String[] args){
		//Input variables 0-blocksize, 1-L1_Size, 2-L1_Assoc, 3-L2_Size, 4-L2_Assoc, 5-Replacement Policy, 6-Inclusion Property
		input = new int[7];
		for(int i = 0 ; i<7; i++) {
			input[i] = Integer.valueOf(args[i]);
		}
		traceFile=String.valueOf(args[7]);

		 // Calculates and sets configuration details for Level 1 cache
		calculateDetails(1,input[1],input[2]);

		// Initializes tracking arrays for L1 cache metadata and cache lines
		L1Tracker = new int[5];
		countQueue=new int[setCountL1][input[2]];
		dirtyBlocksL1=new boolean[setCountL1][input[2]];
		optimalAddCounterL1=new String[setCountL1][input[2]];
		optimalCounterL1=new String[setCountL1][input[2]];
		countLRU=new int[setCountL1][input[2]];
		cacheL1=new String[setCountL1][input[2]];

		// Initializes tracking arrays for L2 cache metadata and cache lines if L2 is enabled
		L2Tracker = new int[5];
		if(input[4]!=0){
			calculateDetails(2,input[3],input[4]);
			cacheL2=new String[setCountL2][input[4]];
			countQueueL2=new int[setCountL2][input[4]];
			dirtyBlocksL2=new boolean[setCountL2][input[4]];
			countLRUL2=new int[setCountL2][input[4]];
			optimalCounterL2=new String[setCountL2][input[4]];
			optimalAddCounterL2=new String[setCountL2][input[4]];
		}

		getInstructions(bitsCountL1, indexBitsCountL1, offset);
        CacheDisplay display = new CacheDisplay();
        display.displayResults(this);
		
	}
	
	void calculateDetails(int cacheLevel, int size, int associativityNo){
		// Checks if cacheLevel is 1
		if(cacheLevel==1){
			cacheBlockCountL1=size/input[0];
			setCountL1=cacheBlockCountL1/associativityNo;
			indexBitsCountL1=Integer.valueOf((int) (Math.log(setCountL1)/Math.log(2)));
			offset=Integer.valueOf((int) (Math.log(input[0])/Math.log(2)));
			bitsCountL1=32-indexBitsCountL1-offset;
		}
		// Checks if cacheLevel is 2
		else if(cacheLevel==2){
			int count=size/input[0];
			setCountL2=count/associativityNo;
			indexBitsCountL2=Integer.valueOf((int) (Math.log(setCountL2)/Math.log(2)));
			int num =Integer.valueOf((int) (Math.log(input[0])/Math.log(2)));
			bitsCountL2=32-indexBitsCountL2-num;
		}
		
	}
	
	void getInstructions(int bits,int indexBits,int offsetBits) {
		// List to store instructions from the trace file
		List<String> instructionsList = new ArrayList<String>();

		int totalBits=bits+indexBits+offsetBits; // Total number of bits in the address
		int lineNo=0;

		// File object for the trace file
		File traceFileRead = new File("./"+traceFile);
		try{
			 // Read all lines from the trace file and store in instructionsList
			List<String> instructions=Files.readAllLines(Paths.get("./"+traceFile));
		    for(String eachInstruction:instructions){
		    	lineNo++;
		    	instructionsList.add(eachInstruction);
		    }
		}
		catch (IOException e){
			e.printStackTrace();
		}
		int eachInst=0;
		String[] readOrWrite = new String[instructionsList.size()];
		address = new String[instructionsList.size()];
		// Parse each instruction to separate read/write operation and address
		for(String perInstruction:instructionsList){
			readOrWrite[eachInst]=perInstruction.split(" ")[0];
			address[eachInst]=perInstruction.split(" ")[1];
			eachInst++;
		}
		// Process each address to convert and split based on bit counts
		for(String perAddress:address){
			String binary=hexToBinary(perAddress, totalBits);
			String tagsHexConv=binaryToHex(binary.substring(0, bits));      
		    String indexsBinConv;

			// Check if index section is empty, set index as "0" if so
		    if(binary.substring(bits, bits+indexBits).equals("")){
		    	indexsBinConv="0";
		    }
		    else{
		    	indexsBinConv=binaryToDec(binary.substring(bits, bits+indexBits));
		    }
		    String offsetsHexConv=binaryToHex(binary.substring(bits+indexBits, bits+indexBits+offsetBits));
		    addressAfterSplit.add(new String[] {tagsHexConv,indexsBinConv,offsetsHexConv});
		}
		// Check if L2 cache is enabled (input[4] != 0) and process addresses for L2 if so
		if(input[4]!=0){
			for(String perAddress:address){
				String binary=hexToBinary(perAddress, totalBits);
				String tagsHexConv=binaryToHex(binary.substring(0, bitsCountL2));      
			    String indexsBinConv;
			    if(binary.substring(bits, bits+indexBits).equals("")){
			    	indexsBinConv="0";
			    }
			    else{
			    	indexsBinConv=binaryToDec(binary.substring(bits, bits+indexBits));
			    }
				  // Convert offset bits to hexadecimal and add to addressAfterSplitL2
			    String offsetsHexConv=binaryToHex(binary.substring(bitsCountL2+indexBitsCountL2, bitsCountL2+indexBitsCountL2+offsetBits));
			    addressAfterSplitL2.add(new String[] {tagsHexConv,indexsBinConv,offsetsHexConv});
			}
		}
	  
	  String operation;
	  int indexBit;
	  int temp=0;
	  int index;
	  first:
	  for(int i=0;i<addressAfterSplit.size();i++){
		// Extracts the index bit from the split address
		  indexBit=Integer.valueOf(addressAfterSplit.get(i)[1]);
		  operation=readOrWrite[i];
		   // Determines the operation type for this address (read or write)
		  if(operation.equalsIgnoreCase("r")){
			  L1Tracker[0]++;
			  // Loops through each cache level for the specific index
			  for(int j=0;j<cacheL1.length;j++){
				  if(j!=indexBit)
					  continue;
				  for(int k=0;k<cacheL1[j].length;k++){
					 // Checks for empty or null entries in L1 cache
					  if(Objects.isNull(cacheL1[j][k]) || cacheL1[j][k]==null){
						  L1Tracker[1]++;
						  if (input[4]!=0){
			        	    L2R(addressAfterSplit.get(i)[0],j,i);
			        	  }
						  cacheL1[j][k]=addressAfterSplit.get(i)[0];
						  optimalCounterL1[j][k]=addressAfterSplit.get(i)[0];
						  optimalAddCounterL1[j][k]=address[i];
						  countQueue[j][k]=(Arrays.stream(countQueue[j]).max().getAsInt())+1;
						  countLRU[j][k]=(Arrays.stream(countLRU[j]).max().getAsInt())+1;
						  continue first;
					  }
					  // If cache entry matches the address, updates LRU count and continues to next iteration
					  else if(cacheL1[j][k].equalsIgnoreCase(addressAfterSplit.get(i)[0])){
						  countLRU[j][k]=(Arrays.stream(countLRU[j]).max().getAsInt())+1;
						  continue first;
					  }
				  }
				  // Checks remaining entries in L1 that don't match the current address
				  for(int k=0;k<cacheL1[j].length;k++){
					  if(!cacheL1[j][k].equalsIgnoreCase(addressAfterSplit.get(i)[0])){
						  temp++;
					  }
				  }
				  if(temp==input[2]){
					// Increment conflict miss rate and reset temp counter
					  conflictMissRate++;
					  temp=0;
					  L1Tracker[1]++;
					   // Selects replacement policy based on input[5]
				      switch(input[5]){
				        case 0:
				        	index=LRU(countLRU[j]);
				        	if(dirtyBlocksL1[j][index]){
				        		L1Tracker[4]++;
				        		if (input[4]!=0){
				        			L2W(cacheL1[j][index],j,i);
				        		}
							}
							// Read from L2 if enabled
				        	if (input[4]!=0){
				        	   L2R(addressAfterSplit.get(i)[0],j,i);
				        	}
				        	dirtyBlocksL1[j][index]=false;// Marks the block as clean in L1
				        	cacheL1[j][index]=addressAfterSplit.get(i)[0];
				        	optimalCounterL1[j][index]=addressAfterSplit.get(i)[0];
				        	optimalAddCounterL1[j][index]=address[i];
							countLRU[j][index]=(Arrays.stream(countLRU[j]).max().getAsInt())+1;
				        	break;
						// Case 1: Use FIFO replacement policy
						case 1:
							index=queue(countQueue[j]);
							if(dirtyBlocksL1[j][index]){
								L1Tracker[4]++;
								if (input[4]!=0){
									L2W(cacheL1[j][index],j,i);
								}
							}
							if (input[4]!=0){
								L2R(addressAfterSplit.get(i)[0],j,i);
				        	}
							dirtyBlocksL1[j][index]=false;
							cacheL1[j][index]=addressAfterSplit.get(i)[0];
							optimalCounterL1[j][index]=addressAfterSplit.get(i)[0];
							optimalAddCounterL1[j][index]=address[i];
							countQueue[j][index]=(Arrays.stream(countQueue[j]).max().getAsInt())+1;
							break;
						// Case 2: Use an optimal replacement policy
						case 2:
							index=optimalPolicy("L1",optimalCounterL1[j],i,optimalAddCounterL1[j],j);
							if(dirtyBlocksL1[j][index]){
								L1Tracker[4]++;
								if (input[4]!=0){
									L2W(cacheL1[j][index],j,i);
								}
							}
							 // If L2 caching is enabled, fetches the data from L2 to L1
							if (input[4]!=0){
								L2R(addressAfterSplit.get(i)[0],j,i);
				        	}
							dirtyBlocksL1[j][index]=false;//same as above
							cacheL1[j][index]=addressAfterSplit.get(i)[0];// Updates the cache with the current address
							optimalCounterL1[j][index]=addressAfterSplit.get(i)[0];
							optimalAddCounterL1[j][index]=addressAfterSplit.get(i)[0];
							break;
				      }
				  }
			  }
		  }
		  if(operation.equalsIgnoreCase("w")){
			  L1Tracker[2]++;
			  for(int j=0;j<cacheL1.length;j++){
				  if(j!=indexBit){
					  continue;
				  }
				  for(int k=0;k<cacheL1[j].length;k++){
					 // Check for empty or null entries in L1 cache
					  if(Objects.isNull(cacheL1[j][k]) || cacheL1[j][k]==null){
						  L1Tracker[3]++;
						  if (input[4]!=0){
			        	    L2R(addressAfterSplit.get(i)[0],j,i);
			        	  }

						// Update cache and counters
						  cacheL1[j][k]=addressAfterSplit.get(i)[0];
						  optimalCounterL1[j][k]=addressAfterSplit.get(i)[0];
						  optimalAddCounterL1[j][k]=address[i];
						  countQueue[j][k]=(Arrays.stream(countQueue[j]).max().getAsInt())+1;
						  countLRU[j][k]=(Arrays.stream(countLRU[j]).max().getAsInt())+1;
						  dirtyBlocksL1[j][k]=true;
						  continue first;
					  }
					  // Update LRU if cache entry matches address
					  else if(cacheL1[j][k].equalsIgnoreCase(addressAfterSplit.get(i)[0])){
						  countLRU[j][k]=(Arrays.stream(countLRU[j]).max().getAsInt())+1;
						  dirtyBlocksL1[j][k]=true;
						  continue first;
					  }
				  }
				  // Handle remaining entries in cache if no matches were found
				  for(int k=0;k<cacheL1[j].length;k++){
					  if(!cacheL1[j][k].equalsIgnoreCase(addressAfterSplit.get(i)[0])){
						  temp++;
					  }
					  // Update LRU and mark as dirty if cache entry matches
					  if(cacheL1[j][k].equalsIgnoreCase(addressAfterSplit.get(i)[0])){
						  countLRU[j][k]=(Arrays.stream(countLRU[j]).max().getAsInt())+1;
						  dirtyBlocksL1[j][k]=true;
						  continue first;
					  }
				  }
				  if(temp==input[2]){
					  temp=0;
					  conflictMissRate++;
					  L1Tracker[3]++;
					   // Choose replacement policy based on input
				      switch(input[5]){
						// LRU replacement
				        case 0:
				        	index=LRU(countLRU[j]);
				        	if(dirtyBlocksL1[j][index]){
				        		L1Tracker[4]++;
				        		if (input[4]!=0){
				        			L2W(cacheL1[j][index],j,i);
				        		}
							}
				        	if (input[4]!=0){
				        	   L2R(addressAfterSplit.get(i)[0],j,i);
				        	}
				        	cacheL1[j][index]=addressAfterSplit.get(i)[0]; // Read from L2 if enabled
				        	optimalCounterL1[j][index]=addressAfterSplit.get(i)[0]; // Update cache with new address
				        	optimalAddCounterL1[j][index]=addressAfterSplit.get(i)[0];
							countLRU[j][index]=(Arrays.stream(countLRU[j]).max().getAsInt())+1;
				        	dirtyBlocksL1[j][index]=true;
				        	break;
						// Case 1: FIFO replacement
						case 1:
							index=queue(countQueue[j]);
							if(dirtyBlocksL1[j][index]){
								L1Tracker[4]++;
								if (input[4]!=0){
									L2W(cacheL1[j][index],j,i);
								}
							}
							// Read from L2 if enabled
							if (input[4]!=0){
				        	   L2R(addressAfterSplit.get(i)[0],j,i);
				        	}
							cacheL1[j][index]=addressAfterSplit.get(i)[0];  // Update cache with new address
							optimalCounterL1[j][index]=addressAfterSplit.get(i)[0];  // Update cache with new address
							optimalAddCounterL1[j][index]=addressAfterSplit.get(i)[0];
							countQueue[j][index]=(Arrays.stream(countQueue[j]).max().getAsInt())+1;
							dirtyBlocksL1[j][index]=true;
							break;
						case 2:
							 // Get the index based on FIFO queue
							index=optimalPolicy("L1",optimalCounterL1[j],i,optimalAddCounterL1[j],j);
							if(dirtyBlocksL1[j][index]){
								L1Tracker[4]++;
								if (input[4]!=0){
									L2W(cacheL1[j][index],j,i);
								}
							}
							// Read from L2 if enabled
							if (input[4]!=0){
				        	   L2R(addressAfterSplit.get(i)[0],j,i);
				        	}
							cacheL1[j][index]=addressAfterSplit.get(i)[0]; // Update cache with new address
							optimalCounterL1[j][index]=addressAfterSplit.get(i)[0]; // Update optimal counter
							optimalAddCounterL1[j][index]=addressAfterSplit.get(i)[0];
							dirtyBlocksL1[j][index]=true;
							break;	  
				      }
				  }
			  }
		  }
	  }
}

// Converts the decimal number to a binary string
public String decToBinary(int decNum, int totalBits) {
    String binaryString = Integer.toBinaryString(decNum);
    return String.format("%" + totalBits + "s", binaryString).replace(' ', '0');
}

// Converts the hexadecimal code to an decimal value
public String hexToBinary(String hexCode, int totalBits) {
    int decimalValue = Integer.parseInt(hexCode, 16);
    String binaryString = Integer.toBinaryString(decimalValue);
    return String.format("%" + totalBits + "s", binaryString).replace(' ', '0');
}

// Converts the binary string to a decimal integer
public String binaryToHex(String binaryCode) {
    int decimalValue = Integer.parseInt(binaryCode, 2);
    return Integer.toHexString(decimalValue);
}

// Converts the binary string to a decimal integer
public String binaryToDec(String binaryCode) {
    int decimalValue = Integer.parseInt(binaryCode, 2);
    return Integer.toString(decimalValue);
}


public void L2R(String tag, int indexTag,int i) {
	// Convert the tag to binary and concatenate with binary representation of indexTag to form full address
	String binaryTag = hexToBinary(tag, bitsCountL1);
	String binaryIndex = decToBinary(indexTag, indexBitsCountL1);
	String fullAddressBinary = binaryTag + binaryIndex;

	// Extract the L2 tag from the binary address and convert it to hexadecimal
	String l2Tag = binaryToHex(fullAddressBinary.substring(0, bitsCountL2));
	int l2Index=Integer.valueOf(binaryToDec(fullAddressBinary.substring(bitsCountL2, bitsCountL2+indexBitsCountL2)));
	int temp=0;
	int index;
	L2Tracker[0]++;
	  Second:
	  for(int j=0;j<cacheL2.length;j++){                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            
		  if(j!=l2Index)
			  continue;
		  // Iterate over cache lines within the selected set
		  for(int k=0;k<cacheL2[j].length;k++){
		      // Handle a cache miss if the entry is null or empty
			  if(Objects.isNull(cacheL2[j][k])||cacheL2[j][k].isEmpty()||cacheL2[j][k]==null){
				  L2Tracker[1]++;
				  memTraffic++;

				    // Update the cache entry with the new tag and set counters
				  cacheL2[j][k]=l2Tag;
				  optimalCounterL2[j][k]=l2Tag;
				  countQueueL2[j][k]=(Arrays.stream(countQueueL2[j]).max().getAsInt())+1;
				  countLRUL2[j][k]=(Arrays.stream(countLRUL2[j]).max().getAsInt())+1;
				  break Second; // Exit both loops after handling the hit
			  }
			  // Handle a cache hit if the tag matches
			  else if(cacheL2[j][k].equalsIgnoreCase(l2Tag)){
				  countLRUL2[j][k]=(Arrays.stream(countLRUL2[j]).max().getAsInt())+1;
				  break Second; // Exit both loops after handling the hit
			  }
		  }
		  // Secondary loop to count non-matching entries within the set
		  for(int k=0;k<cacheL2[j].length;k++){
			  if(!cacheL2[j][k].equalsIgnoreCase(l2Tag)){
				  temp++;
			  }
		  }
		  if(temp==input[4]){
			  temp=0;
			  L2Tracker[1]++;
			  memTraffic++;
			  // Replacement policy switch based on input
		      switch(input[5]){
				// LRU) replacement
		        case 0:
		        	index=LRU(countLRUL2[j]);
		        	if(dirtyBlocksL2[j][index]){
		        		L2Tracker[4]++;
		        		memTraffic++;
					}
		        	if (input[6]!=0){
						// Apply inclusion property if enabled
	        			InclusionProperty(cacheL2[j][index],j);
	        		}
		        	cacheL2[j][index]=l2Tag; // Update cache with new tag
		        	optimalCounterL2[j][index]=l2Tag; // Set optimal counter
					countLRUL2[j][index]=(Arrays.stream(countLRUL2[j]).max().getAsInt())+1;
					dirtyBlocksL2[j][index]=false;  // Mark block as clean
		        	break;
				// FIFO replacement
				case 1:
					index=queue(countQueueL2[j]);
					if(dirtyBlocksL2[j][index]){
		        		L2Tracker[4]++;
		        		memTraffic++; // Increment memory traffic for write-back
					}
		        	if (input[6]!=0){
	        			//call the inclusive function
	        			InclusionProperty(cacheL2[j][index],j);
	        		}
					dirtyBlocksL2[j][index]=false;
					cacheL2[j][index]=l2Tag;
					optimalCounterL2[j][index]=l2Tag;
					countQueueL2[j][index]=(Arrays.stream(countQueueL2[j]).max().getAsInt())+1;
					break;
				// optimal replacement
				case 2:
					index=optimalPolicy("L2",optimalCounterL2[j],i,optimalAddCounterL2[j],j);
					if(dirtyBlocksL2[j][index]){
		        		L2Tracker[4]++;
		        		memTraffic++;
					}
		        	if (input[6]!=0){
						// Apply inclusion property if enabled
	        			InclusionProperty(cacheL2[j][index],j);
	        		}
					cacheL2[j][index]=l2Tag; // Update cache with new tag
					optimalCounterL2[j][index]=l2Tag; // Set optimal counter
					optimalAddCounterL2[j][index]=l2Tag;
					dirtyBlocksL2[j][index]=false; // Mark block as clean
					break;
		      }
		  }
	  }
}		

public void L2W(String tag,int indexTag,int addrIndex){
	int temp=0;
	int index;
	// Convert the tag to binary and concatenate with binary representation of indexTag to form full address
	String binaryTag = hexToBinary(tag, bitsCountL1);
	String binaryIndex = decToBinary(indexTag, indexBitsCountL1);
	String fullAddressBinary = binaryTag + binaryIndex;

	// Extract the L2 tag from the binary address and convert it to hexadecimal
	String l2Tag = binaryToHex(fullAddressBinary.substring(0, bitsCountL2)); 
	int l2Index=Integer.valueOf(binaryToDec(fullAddressBinary.substring(bitsCountL2, bitsCountL2+indexBitsCountL2)));
	L2Tracker[2]++;
 
	Second:
	for(int j=0;j<cacheL2.length;j++){
		 // Skip sets that do not match the calculated L2 index
		if(j!=l2Index){
			continue;
		}
		// Iterate over cache lines within the selected set
		for(int k=0;k<cacheL2[j].length;k++){
			// Check for an empty or null cache entry and handle as a cache miss
			if(Objects.isNull(cacheL2[j][k])||cacheL2[j][k].isEmpty()||cacheL2[j][k]==null){
				L2Tracker[3]++;
				memTraffic++;

				// Update cache and counters with the new tag and set it as dirty
				cacheL2[j][k]=l2Tag;
				optimalCounterL2[j][k]=l2Tag;
				optimalAddCounterL2[j][k]=address[addrIndex];
				countQueueL2[j][k]=(Arrays.stream(countQueueL2[j]).max().getAsInt())+1;
				countLRUL2[j][k]=(Arrays.stream(countLRUL2[j]).max().getAsInt())+1;
				dirtyBlocksL2[j][k]=true;
				// Exit both inner loops and proceed to the next operation
				break Second;
			}
			// If there is a cache hit (matching tag), update LRU counter and mark as dirty
			else if(cacheL2[j][k].equalsIgnoreCase(l2Tag)){
				countLRUL2[j][k]=(Arrays.stream(countLRUL2[j]).max().getAsInt())+1;
				dirtyBlocksL2[j][k]=true;
				// Exit both inner loops and proceed to the next operation
				break Second;
			}
		}
		// Secondary loop to count non-matching entries within the set
		for(int k=0;k<cacheL2[j].length;k++){
			if(!cacheL2[j][k].equalsIgnoreCase(l2Tag)){
				temp++;
			}
		}
		if(temp==input[4]){
			temp=0;
			L2Tracker[3]++;
			memTraffic++;
			switch(input[5]){
			case 0:
				index=LRU(countLRUL2[j]);
				// Check if the selected block is dirty
				if(dirtyBlocksL2[j][index]){
					L2Tracker[4]++;
					memTraffic++;
				}
				if (input[6]!=0){
					// Apply inclusion property if enabled
					InclusionProperty(cacheL2[j][index],j);
				}
				// Update cache and counters with the new tag
				cacheL2[j][index]=l2Tag;
				optimalCounterL2[j][index]=l2Tag;
				optimalAddCounterL2[j][index]=l2Tag;
				countLRUL2[j][index]=(Arrays.stream(countLRUL2[j]).max().getAsInt())+1;
				dirtyBlocksL2[j][index]=true;
				break;
			// FIFO replacement policy
			case 1:
				index=queue(countQueueL2[j]);
				if(dirtyBlocksL2[j][index]){
					L2Tracker[4]++;
					memTraffic++;
				}
				if(input[6]!=0){
					InclusionProperty(cacheL2[j][index],j);
				}
				cacheL2[j][index]=l2Tag;
				optimalCounterL2[j][index]=l2Tag;
				optimalAddCounterL2[j][index]=l2Tag;
				countQueueL2[j][index]=(Arrays.stream(countQueueL2[j]).max().getAsInt())+1;
				dirtyBlocksL2[j][index]=true;
				break;
			//  Optimal replacement policy
			case 2:
				index=optimalPolicy("L2",optimalCounterL2[j],addrIndex,optimalAddCounterL2[j],j);
				if(dirtyBlocksL2[j][index]){
					L2Tracker[4]++;
					memTraffic++;
				}
				if (input[6]!=0){
					// Apply inclusion property if enabled
					InclusionProperty(cacheL2[j][index],j);
				}
				cacheL2[j][index]=l2Tag;
				optimalCounterL2[j][index]=l2Tag;
				optimalAddCounterL2[j][index]=l2Tag;
				dirtyBlocksL2[j][index]=true;
				break;
			}
		}
	}
}

public void InclusionProperty(String tagL2, int indexL2){
	// Converts L2 tag and index to a binary address and extracts L1 tag and index from it
	String addressBinary=hexToBinary(tagL2, bitsCountL2);
	String indexBinary=decToBinary(indexL2, indexBitsCountL2);
	String finalBinary=addressBinary+indexBinary;
	
	// Use a substring to extract the L1 tag from the full binary address and convert to hexadecimal
	String l1TagHex = binaryToHex(finalBinary.substring(0, bitsCountL1));

	// Use a substring to extract the L1 index from the full binary address and convert to decimal
	int l1Index = Integer.parseInt(binaryToDec(finalBinary.substring(bitsCountL1, bitsCountL1 + indexBitsCountL1)));

	// Iterates through L1 cache rows, skipping rows that don't match the index
	for(int i=0;i<cacheL1.length;i++){
		  if(i!=l1Index){
			  continue;
		  }
		  loop:
		  for(int j=0;j<cacheL1[i].length;j++){
			  if(Objects.isNull(cacheL1[i][j])||cacheL1[i][j].isEmpty()){
				  continue loop;
			  }
			  if(cacheL1[i][j].equalsIgnoreCase(l1TagHex)){
				  if(dirtyBlocksL1[i][j]){ 
					memTraffic++;}
				  cacheL1[i][j]="";
				  dirtyBlocksL1[i][j]=false;
				  return;
			  }
		  }
	  }
}


int getValue(int[] arr) {
	// Initializes variables to store the current highest value and its index
	int value=arr[0];
	int index=0;

	// Iterates through the array to find the largest value and its index
	for (int i=0; i<arr.length; i++){
		if (value < arr[i]){
			value=arr[i];
			index=i;
		}
	}
	return index;
}

int optimalPolicy(String cacheLevel, String[] setVals, int addressInt, String[] setValAddresses,int indexBitNo){
	// Initializes a list and array to store split address components and indices
	List<String[]> subAddressAfterSplit;
	int[] subArrInt;
	 // Determines whether to use L2 or L1 split addresses and initializes subArrInt size accordingly
	if(cacheLevel.equalsIgnoreCase("L2")){
		subAddressAfterSplit=addressAfterSplitL2.subList(addressInt+1, address.length);
		subArrInt=new int[input[4]];
	}
	else{
		subAddressAfterSplit=addressAfterSplit.subList(addressInt+1, address.length);
		subArrInt=new int[input[2]];
	}
	// Iterates over set values to find matching addresses in subAddressAfterSplit
	tempLoop:
	for(int i=0;i<setVals.length;i++){
		for(int j=0;j<subAddressAfterSplit.size();j++){
			if(subAddressAfterSplit.get(j)[0].equalsIgnoreCase(setVals[i]) && subAddressAfterSplit.get(j)[1].equalsIgnoreCase(String.valueOf(indexBitNo))){
				subArrInt[i]=j;
				continue tempLoop;
			}
			if(j==subAddressAfterSplit.size()-1 && subArrInt[i]==0){
				subArrInt[i]=address.length;
			}
		}
	}
	int maxIndex=getValue(subArrInt);
	return maxIndex;
}

int queue(int[] queueArr){
	// Initializes min with the first element and minIndex with 0
	int min=queueArr[0];
	int minIndex=0;
	// Iterates through the array to find the next value in queue
	for (int i=0; i<queueArr.length; i++){
		if (min>queueArr[i]){
			min=queueArr[i];
			minIndex=i;
		}
	}
	return minIndex;
}

int LRU(int[] LRUCountArr){
	// Initializes min with the first element and minIndex with 0
	int min=LRUCountArr[0];
	int minIndex=0;
	// Iterates through the array to find the minimum value and its index
	for (int i=0;i<LRUCountArr.length;i++){
		if (min>LRUCountArr[i]){
			min=LRUCountArr[i];
			minIndex=i;
		}
	}
	return minIndex;
}




	

}
