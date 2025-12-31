import java.text.DecimalFormat;

public class CacheDisplay {

    // Method to display results, accepting a `cache` object to access its data
    public void displayResults(cache cacheInstance) {

        //Getting Replacement Policy
        String replacementPolicy="";
        switch(cacheInstance.input[5]) {
            case 0:
                replacementPolicy="LRU";
                break;
            case 1:
                replacementPolicy="FIFO";
                break;
            case 2:
                replacementPolicy="optimal";
                break;
            default:
                replacementPolicy="LRU";
                break;
        }

        //Getting Inclusion Property
        String inclusionProperty="";
        switch(cacheInstance.input[6]) {
            case 0:
                inclusionProperty="non-inclusive";
                break;
            case 1:
                inclusionProperty="inclusive";
                break;
        }

        //Setting Decimal Rounding
        DecimalFormat df=new DecimalFormat("#0.000000");
        
        //Input
        System.out.print("===== Simulator configuration =====\r\n");
        System.out.print(
                "input[0]:              "+cacheInstance.input[0]+"\r\n" + 
                "L1_SIZE:               "+cacheInstance.input[1]+"\r\n" + 
                "L1_ASSOC:              "+cacheInstance.input[2]+"\r\n" + 
                "L2_SIZE:               "+cacheInstance.input[3]+"\r\n" + 
                "L2_ASSOC:              "+cacheInstance.input[4]+"\r\n" + 
                "REPLACEMENT POLICY:    "+replacementPolicy+"\r\n" + 
                "INCLUSION PROPERTY:    "+inclusionProperty+"\r\n" + 
                "trace_file:            "+cacheInstance.traceFile+"\r\n");
                
        //L1 Cache
        System.out.print("===== L1 contents =====\r\n");
        for(int i=0;i<cacheInstance.cacheL1.length;i++){
            System.out.print("Set     "+i+":      ");
            for(int j=0;j<cacheInstance.cacheL1[i].length;j++){
                if(cacheInstance.dirtyBlocksL1[i][j])
                    System.out.print(cacheInstance.cacheL1[i][j]+" D  ");
                else
                    System.out.print(cacheInstance.cacheL1[i][j]+"    ");
            }
            System.out.print("\r\n");
        }
        //L2 cache
        if(cacheInstance.input[4]!=0){
        System.out.print("===== L2 contents =====\r\n");
        for(int i=0;i<cacheInstance.cacheL2.length;i++)
        {System.out.print("Set     "+i+":      ");
        for(int j=0;j<cacheInstance.cacheL2[i].length;j++){
            if(cacheInstance.dirtyBlocksL2[i][j])
                System.out.print(cacheInstance.cacheL2[i][j]+" D  ");
            else
                System.out.print(cacheInstance.cacheL2[i][j]+"    ");
        }
        System.out.print("\r\n");
        }
        }
        float missRateL1=(float)(cacheInstance.L1Tracker[1]+cacheInstance.L1Tracker[3])/(cacheInstance.L1Tracker[0]+cacheInstance.L1Tracker[2]);
        float missRateL2=((cacheInstance.input[4]==0) ? 0 : (float)(cacheInstance.L2Tracker[1])/(cacheInstance.L2Tracker[0]));
        int totalMemoryTraffic=((cacheInstance.input[4]==0) ? cacheInstance.L1Tracker[1]+cacheInstance.L1Tracker[3]+cacheInstance.L1Tracker[4] : cacheInstance.MemoryTraffic);

        //Printing raw simulation results
        System.out.print("===== Simulation results (raw) =====\r\n" + 
                "a. number of L1 reads:        "+ cacheInstance.L1Tracker[0]+ "\r\n" + 
                "b. number of L1 read misses:  "+ cacheInstance.L1Tracker[1] +"\r\n" + 
                "c. number of L1 writes:       "+ cacheInstance.L1Tracker[2] +"\r\n" + 
                "d. number of L1 write misses: "+ cacheInstance.L1Tracker[3] +"\r\n" + 
                "e. L1 miss rate:              "+ df.format(missRateL1) +"\r\n" + 
                "f. number of L1 writebacks:   "+ cacheInstance.L1Tracker[4] +"\r\n" + 
                "g. number of L2 reads:        "+ cacheInstance.L2Tracker[0] +"\r\n" + 
                "h. number of L2 read misses:  "+ cacheInstance.L2Tracker[1] +"\r\n" + 
                "i. number of L2 writes:       "+ cacheInstance.L2Tracker[2] +"\r\n" + 
                "j. number of L2 write misses: "+ cacheInstance.L2Tracker[3] +"\r\n" + 
                "k. L2 miss rate:              "+ df.format(missRateL2) +"\r\n" + 
                "l. number of L2 writebacks:   "+ cacheInstance.L2Tracker[4] +"\r\n" + 
                "m. total memory traffic:      "+ totalMemoryTraffic +"\r\n" +"");
        
       
    }
}