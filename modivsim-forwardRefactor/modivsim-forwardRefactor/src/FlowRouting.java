import javafx.util.Pair;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;


public class FlowRouting {
    private static ArrayList<Flow> flows = new ArrayList<>();
    private static int numFlows;
    private static List<Map> list = new ArrayList<Map>();  //template list to form synchronized list
    private static List<Map> forwardingTable = Collections.synchronizedList(list);
    private static Topology topology = new Topology();
    private static double time = 0;

    //** FLOWS */
    private static class Flow {
        public final int priority;
        public final int srcNode;
        public final int dstNode;
        public final double dataSize;
        public boolean done = false;
        public boolean queued = false;
        public double actionTime;
        public double queueEndTime;
        public double[] lastActParameters;  // [last proceed path's BW, travel time]
        public List<Integer> lastPath;      // last proceed path
        public ArrayList<Integer> path = new ArrayList<>();
        public Flow(int srcNode,int dstNode,int dataSize,int priority){
            this.srcNode=srcNode;
            this.dstNode=dstNode;
            this.dataSize=dataSize;
            this.priority=priority;
            actionTime=0;
            queueEndTime=999;
            lastActParameters= new double[]{999, 0};
        }
        public void toStr(){
            System.out.println("Source Node: "+srcNode+", Destination Node: "+dstNode+", Data Size: "+dataSize+", Priority: "+priority);
        }
    }
    private static void readFlows() throws IOException{ // reads flows from the text flows.txt
        int priorityCount = -1;
        String [] tokens;
        BufferedReader reader;
        reader = new BufferedReader(new FileReader("./src/FlowRoutingFolder/flows.txt"));
        String line = reader.readLine();
        System.out.println("Flows are read from \"flows.txt\": ");
        while (line != null) {
            priorityCount++;
            ArrayList<Integer> a = new ArrayList<>();
            tokens = line.split("[,()]+");
            for(String s: tokens){
                a.add(Integer.parseInt(s));
            }
            Flow f =  new Flow(a.get(0),a.get(1),a.get(2),priorityCount);
            flows.add(f);
            System.out.print("f"+f.priority+": "+line+" ->\t");
            f.toStr();
            line = reader.readLine();
        }
        numFlows=flows.size();
        reader.close();
    }

    //** TOPOLOGY */
    private static class Topology {
        protected  Map<String,ArrayList<Double>> topologyMap = new HashMap<>();
        public Topology(){
            constructTopology();
        }
        public ArrayList<Integer> neighbours(int node){ //returns the neighbour nodes of the given node
            ArrayList<Integer> arr = new ArrayList<>();
            for(int i=0;i<5;i++){
                if(node!=i){
                    if(topologyMap.containsKey(link(node,i))){
                        arr.add(i);
                    }
                }
            }
            return arr;
        }
        private void constructTopology(){
            topologyMap.put(link(0,1),linkProperties(10,1,0));
            topologyMap.put(link(0,2),linkProperties(15,1,0));
            topologyMap.put(link(1,2),linkProperties(5,1,0));
            topologyMap.put(link(1,4),linkProperties(15,1,0));
            topologyMap.put(link(2,3),linkProperties(10,1,0));
            topologyMap.put(link(4,3),linkProperties(5,1,0));
        }

        private String link(int a,int b){   // it takes the nodes of link (interchangeably), turns into String version which is the format used in topologyMap
            return String.valueOf(Integer.min(a,b))+"-"+String.valueOf(Integer.max(a,b));
        }
        private ArrayList<Double> linkProperties(double bandwidth, double availability,double reservedTime){
            ArrayList<Double> arr = new ArrayList<>();
            arr.add(bandwidth);
            arr.add(availability);
            arr.add(reservedTime);
            return arr;
        }
        public double getLinkBW(int a, int b){
            try{
                return topologyMap.get(link(a,b)).get(0);
            }catch(NullPointerException e){
                System.out.println("There is no link between "+a+" and "+b);
                return -1;
            }

        }
        public double getLinkAvail(int a, int b){
            try{
                if(a==b) return 1;
                else return topologyMap.get(link(a,b)).get(1);
            }catch(NullPointerException e){
                System.out.println("There is no link between "+a+" and "+b);
                return -1;
            }

        }
        public double getLinkRsdTime(int a, int b){
            try{
                if(a==b) return 0;
                else return topologyMap.get(link(a,b)).get(2);
            }catch(NullPointerException e){
                System.out.println("There is no link between "+a+" and "+b);
                return -1;
            }

        }
        public void setLinkAvail(int a, int b, double av){
            try{
                topologyMap.get(link(a,b)).set(1,av);
            }catch(NullPointerException e){
                System.out.println("There is no link between "+a+" and "+b);
            }
        }
        public void setLinkRsdTime(int a, int b, double rs){
            try{
                topologyMap.get(link(a,b)).set(2,rs);
            }catch(NullPointerException e){
                System.out.println("There is no link between "+a+" and "+b);
            }
        }
        public void update(double actionTime, List<Integer> subPath) throws ConcurrentModificationException {
            int e = subPath.get(0);
            int ePlus = 0;
            for(int i=1; i<subPath.size(); i++){
                ePlus=subPath.get(i);
                setLinkAvail(ePlus,e,0);
                setLinkRsdTime(ePlus,e,actionTime);
                e=ePlus;
            }
        }
        public void clear(){
            topologyMap.forEach((k, v) -> {
                if(v.get(2)<=time){
                    v.set(1,(double)1);
                    v.set(2,time);
                }
            });
        }
        public void printTopology(){
            topologyMap.forEach((k, v) -> {
                System.out.println("Link ("+k+") -> BW: "+v.get(0)+",\t Availability(0/1): "+v.get(1)+",\t Reserved Until Time: "+v.get(2));
            });
        }
    }

    //** FORWARDING */
    private static List<ArrayList<Integer>> readForwardingTable() throws IOException{
        String [] tokens = null;
        ArrayList<Integer> arr;
        List<ArrayList<Integer>> forTable = new ArrayList<>();
        BufferedReader reader;
        reader = new BufferedReader(new FileReader("./src/FlowRoutingFolder/forwardingTable.txt"));
        String line = reader.readLine();

        while (line != null) {
            arr = new ArrayList<>();
            tokens = line.split("[,{}= ]+");
            for(String a: tokens){
                try{ arr.add(Integer.parseInt(a));
                }catch(NumberFormatException e){}
            }
            forTable.add(arr);
            line = reader.readLine();
        }
        reader.close();
        System.out.println("Forwarding Table is read from \"forwardingTable.txt\": ");
        return forTable;
    }
    private static void implementForwardingTable() throws IOException{
        List<ArrayList<Integer>> forTableArray = readForwardingTable();
        int size = forTableArray.size();
        int trio = 0;
        for(ArrayList<Integer> ar : forTableArray){
            Map<Integer, Pair<Integer, Integer>> forwardingEntry = new HashMap<>();
            for(int i=0; i<(size-1);i++){
                trio = i*3;
                forwardingEntry.put(ar.get(trio), new Pair(ar.get(trio+1) , ar.get(trio+2)) );
            }
            forwardingTable.add(forwardingEntry);
        }
        printForwardingTable();
    }
    private static void printForwardingTable(){
        for(Map m :forwardingTable){
            System.out.println(m);
        }
    }
    private static double nextHop(int src, int dst){
        return ((Pair<Integer, Integer>) forwardingTable.get(src).get(dst)).getKey();
    }
    private static double alternativeHop(int src, int dst){
        return ((Pair<Integer, Integer>) forwardingTable.get(src).get(dst)).getValue();
    }
    private static double [] suggestedDst(int src,int dst){
        double[] sug = {0,0};  // [suggested next node, action if it turns out to be queued]
        double next = nextHop(src,dst);
        double altNext = alternativeHop(src,dst);
        if(topology.getLinkAvail(src,(int)next)==1){
            sug[0] = next;
        }else if(topology.getLinkAvail(src,(int)altNext)==1){
            sug[0] = altNext;
        }else{
            sug[1] = Double.min(topology.getLinkRsdTime(src,(int)next),topology.getLinkRsdTime(src, (int)altNext));
            sug[0] = 999; // queue
        }
        return sug;
    }

    //** ROUTING */
    private static void arrangeFlows(){
        List<Boolean> flowsStatus = new ArrayList<>();
        for(int i=0; i<numFlows;i++){
            flowsStatus.add(false);
        }
        System.out.println();
        System.out.println("****************************** FLOW ROUTING **********************************");

        while(true){
            System.out.println();
            System.out.println("TIME is "+time);
            System.out.println("--------------------------------------------------------------");
            System.out.println("-> Completion Status of Flows:  "+flowsStatus.toString());

            for(int i=0; i<numFlows; i++){              // SEQUENTIAL PROCESS FOR EACH FLOW
                if(flows.get(i).actionTime<= time){
                    if(flows.get(i).done){                                  // 1) DONE
                        System.out.println("Done, not proceeded. f"+i);
                        topology.clear();
                        flowsStatus.set(i,true);
                    }else{                                                  // 2) PROCESS REQUIRED
                        System.out.println("Process For f"+i+": ");
                        proceedFlow(flows.get(i));
                    }
                }else{                                                      // 3) PROCESS LATER ON
                    System.out.println("Action time is waited. f"+i);
                }

            }
            proceedToClosestActionTime();                  // UPDATE TIME TO CONTINUE NEXT STEP
            if(!flowsStatus.contains(false)) break;        // BREAK WHEN ALL FLOWS DONE
        }
        System.out.println();
        System.out.println("*** END: TIME IS "+time);
        System.out.println("--------------------------------------------------------------");
        System.out.println("Completion Status of Flows:  "+flowsStatus.toString());
        System.out.println("Flow Routing has done successfully at time = "+time+".");
        for(Flow f: flows){
            System.out.println("\tf"+f.priority+"'s final path: "+f.path+", done at time = "+f.actionTime+".");
        }


    }
    private static void proceedFlow(Flow f){
        f.queued=false;
        int pathSize = f.path.size();
        int src = f.srcNode;
        int dst = f.dstNode;
        if(pathSize==0) f.path.add(f.srcNode);  // source node added to path at the beginning
        else src = f.path.get(pathSize-1);      // or last node is taken into consideration to determine next node

        double [] suggestion = new double[]{src,time};
        if(src!=dst){   // CASE: where flow has not source=destination as default

            suggestion = suggestedDst(src,dst);  // [suggested next node, action if it turns out to be queued]
            while(suggestion[0]!=dst){ // while path is not done!

                if(suggestion[0]!=999) {  // Suggestion not seemed queued
                    int lastReachedHop = f.path.get(f.path.size()-1);
                    double alterNext = alternativeHop(lastReachedHop,dst);
                    double next = nextHop(lastReachedHop,dst);
                    if (f.path.contains((int)suggestion[0]) || isLoopPrisoner(suggestion[0],f)){  // checks for potential loops

                        if(suggestion[0]==alterNext){   // 1) Looped and no alternative available
                            System.out.println(" *f"+f.priority+": prevented from a loop.");
                            suggestion[1] = topology.getLinkRsdTime(lastReachedHop,(int)next); // set suggested action time as queue time
                            f.queued = true;
                            break;
                        }else if(topology.getLinkAvail(lastReachedHop,(int)alterNext)!=1){  // 2) Looped and no alternative is busy
                            suggestion[1] = topology.getLinkRsdTime(lastReachedHop,(int)alterNext); // set suggested action time as queue time
                            System.out.println(" *f"+f.priority+": queued.");
                            f.queued=true;
                            break;
                        }else{  // 3) Looped but alternative is available
                            if(!f.path.contains(alterNext)) System.out.println(" *f"+f.priority+": is looped since there is no other alternative!");
                            suggestion[0] = alterNext;
                            f.path.add((int)suggestion[0]);
                        }

                    }else{

                        f.path.add((int)suggestion[0]);

                    }
                }else{  // Directly Queued
                    System.out.println(" *f"+f.priority+": queued.");
                    f.queued=true;
                    break;
                }
                // a node added, looking for another next node
                suggestion = suggestedDst((int)suggestion[0],dst);
            }

            if (!f.queued){     // If all nodes are added until destination: Done
                f.path.add(dst);
                f.queueEndTime=999;
            }else{              // Set queue time
                f.queueEndTime = suggestion[1];
            }

            int addedPath = f.path.size()-pathSize;
            if(addedPath!=0) findPathBottleNeck(f,addedPath);       // 1) If path added, go for doing related updates
            else if(f.queued) {                                     // 2) If no added path but queued, do updates here.
                if(f.lastActParameters[0]!=999){
                    System.out.println(" *f"+f.priority+": waiting queue has been got delayed.");
                    double passedData = f.lastActParameters[1]*f.lastActParameters[0];
                    double passingTime= (f.dataSize-passedData)/f.lastActParameters[0];
                    if(passingTime+time<=f.queueEndTime){
                        f.actionTime=passingTime+time;
                        f.lastActParameters = new double[]{999, 999};
                    }else{
                        f.lastActParameters[1] = f.lastActParameters[1]+(f.queueEndTime-time);
                        f.actionTime = f.queueEndTime;
                    }
                    topology.update(f.actionTime,f.lastPath);
                }else{
                    System.out.println(" *f"+f.priority+": finished the path up-to where the queue begins.");
                    topology.clear();
                    f.actionTime=f.queueEndTime;
                }

            }

        }else{ // CASE: where flow has not source=destination as default
            f.actionTime=time;
            f.done=true;
            System.out.println(" *f"+f.priority+": done at very time!   (source = destination)");
        }

        System.out.println(" *f"+f.priority+"'s action time is "+f.actionTime);
        System.out.println(" *f"+f.priority+"'s so-far total path: "+f.path);
    }
    private static void findPathBottleNeck(Flow f, int addedPathSize){   // returns the bottleneck of added any dsub-path

        int size = f.path.size();
        int startOfSubPath = size - addedPathSize;
        if(size!=addedPathSize) startOfSubPath--;
        List<Integer> subPath = f.path.subList(startOfSubPath,size);
        if(!(size==1 && time==0)){  // If sub-path does not consist only of initial (source) node
            System.out.println(" *f"+f.priority+": path added.");
            double minBW=999;
            for(int i=startOfSubPath; i<size-1; i++){
                minBW = Double.min(minBW,topology.getLinkBW(f.path.get(i),f.path.get(i+1)));
            }
            proceedAddedPath(f,subPath,minBW,addedPathSize);  // more updates related added sub-path
            System.out.println(" *f"+f.priority+"'s added sub-path: "+subPath);
        }else{
            System.out.println(" *f"+f.priority+": queued at the initial.");
            f.actionTime=f.queueEndTime;
        }

    }
    public static void proceedAddedPath(Flow f,List<Integer> subPath,double bottleneckBW,int subPathSize ){

        double passingTime = 0;
        f.lastPath = subPath.subList(0,subPathSize);

        // CHECKING EFFECT/STATUS OF LAST PATH
        if(f.lastActParameters[0]!=999){  // The effect of last path still remains -> while transmission occurs, queue opened (new sub-path added)
            if(f.lastActParameters[0]<bottleneckBW){ // comparison of bottlenecks of last and new paths
                double passedData = f.lastActParameters[1] * f.lastActParameters[0];
                passingTime= (f.dataSize-passedData)/f.lastActParameters[0];
            }else{
                passingTime = f.dataSize/bottleneckBW;
            }
            topology.update(passingTime+time, f.lastPath);
            f.lastActParameters = new double[]{999, 999};   // The effect of the last path is eliminated
        }else{
            passingTime = f.dataSize/bottleneckBW;
        }

        f.actionTime = time + passingTime;  // Setting action time where arrangeFlows() will be able to proceed on the relevant flow


        // CHECKING THE STATUS OF THE FLOW AFTER NEWLY ADDED PATH
        if (subPath.get(subPath.size()-1)==f.dstNode){  // 1)Done
            System.out.println(" *f"+f.priority+"'s path is done. Transmission will be completed at action time.");
            f.done=true;
        }else{                                          // 2)Still queued
            System.out.println(" *f"+f.priority+" will be queued after having a sub-path.");
            if(f.actionTime>f.queueEndTime){  // if needed, save the required info of newly added path
                System.out.println(" *f"+f.priority+"'s sub-path transmission lasts longer than the queue time. Action time is considered as queue time.");
                f.lastPath = subPath;
                f.lastActParameters[0]=bottleneckBW;
                f.lastActParameters[1]=f.queueEndTime-time;
                f.actionTime=f.queueEndTime;
            }
        }

        topology.update(f.actionTime,subPath);
    }
    private static void proceedToClosestActionTime(){
        double tempTime = time;
        topology.topologyMap.forEach((k, v) -> {
            if (v.get(2)>tempTime){
                if(time==tempTime) time=v.get(2);
                time = Double.min(time,v.get(2));
            }
        });
        topology.clear();

    }
    private static boolean isLoopPrisoner(double suggestion, Flow f) {  // Looks for cases where secret potential loop traps might be hided. (even if it is not seemed so)
        boolean isPrisoner = true ;
        for(int n : topology.neighbours((int)suggestion)){
            if(!f.path.contains(n)) isPrisoner = false;
        }
        return isPrisoner;
    }

    //** MAIN METHOD*/
    public static void main(String[] args) throws IOException {
        System.out.println();
        System.out.println("*************************** PROGRAM INITIALIZATION ***************************");
        readFlows();
        System.out.println();
        implementForwardingTable();
        System.out.println();
        System.out.println("Topology is constructed by using bandwidth and forwarding table info.");
        topology.printTopology();
        System.out.println();
        arrangeFlows();

    }

}
