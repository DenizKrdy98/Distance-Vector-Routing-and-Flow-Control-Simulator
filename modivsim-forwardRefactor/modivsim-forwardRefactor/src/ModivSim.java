import javafx.util.Pair;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.MemoryHandler;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.concurrent.TimeUnit.*;

public class ModivSim 
{ 
    private final static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    static HashMap<Pair<Integer,Integer>, ArrayList<PathStruct>> paths=new HashMap<>();
    static ArrayList<Node> nodes = new ArrayList<>();
    private static int numNodes;
    private static boolean[] visited=new boolean[numNodes];
    private static Stack<Integer> path= new Stack<>();
    static int round = 0;

    public static Node readNode(File filePath, int nodeCount) throws IOException {
        String content;
        HashMap<Integer, Integer> linkCost = new HashMap<>();
        HashMap<Integer, Integer> linkBandwidth = new HashMap<>();
        ArrayList<Integer> dynamicLinks= new ArrayList<>();

        content = new String(Files.readAllBytes(Paths.get(filePath.toURI())));
        String[] tokens = content.split(",[(]");

        int nodeId = Integer.parseInt(tokens[0]);

        for (int i = 1; i < tokens.length; i++) {
            // reading neighbour
            String neighbour = tokens[i];
            //System.out.println(neighbour);

            neighbour = neighbour.replaceAll("[()]", "");

            String[] ntokens = neighbour.split(",");
            // System.out.println(Arrays.toString(ntokens));
            int nId = Integer.parseInt(ntokens[0]);
            if(ntokens[1].equals("x")){
                dynamicLinks.add(nId);
                linkCost.put(nId, getRandomNumberInRange(1,10));
            }else{
                linkCost.put(nId, Integer.parseInt(ntokens[1]));
            }


            linkBandwidth.put(nId, Integer.parseInt(ntokens[2]));
        }
        Node ans = new Node(nodeId, linkCost, linkBandwidth, nodeCount,dynamicLinks);
        //System.out.println("node with nodeid " + nodeId);
        nodes.add(ans);
        return ans;
    }

    public static synchronized void scheduleOperation() {
        final Runnable updater = new Runnable() {
            public void run() {
                checkConvergence();
            }
        };
        final ScheduledFuture<?> updaterHandle = scheduler.scheduleAtFixedRate(updater, 5, 1, SECONDS);
    }

    private static void checkConvergence() {
        boolean converged = true;
        for (Node node: nodes){
            if(node.changed==true){
                converged=false;
            }
        }
        if (converged){
            System.out.println("Simulation converged.");

            for (int i = 0; i < numNodes; i++) {
                nodes.get(i).setConverged(true);
                for (int j = 0; j < numNodes; j++) {
                    visited= new boolean[numNodes];
                    path= new Stack<>();

                    System.out.println("PRINTING PATHS FROM "+i+ " TO "+j);
                    ArrayList<PathStruct> pathStructs= new ArrayList<>();
                    printAllPaths(i,j,0,pathStructs);
                    paths.put(new Pair<>(i,j),pathStructs);
                }
            }
            populateForwardingTables();

            for (Node node: nodes){
                System.out.println(node.forwardingTable);
            }

            try {
                File file = new File("./src/FlowRoutingFolder/forwardingTable.txt");
                FileWriter writer = new FileWriter(file);
                BufferedWriter bufferedWriter = new BufferedWriter(writer);
                int size=nodes.size();
                for (Node node: nodes){
                    bufferedWriter.write(node.forwardingTable.toString());
                    if (!(node.nodeID==size-1)) bufferedWriter.newLine();
                }
                bufferedWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            scheduler.shutdown();
        }else{
            round++;
        }
    }

    private static void populateForwardingTables() {
        for (Node node: nodes){
            HashMap<String, Pair<Integer, Integer>> forwardingTable= new HashMap<>();

            for (int i=0; i< numNodes; i++){
                if (node.nodeID != i){
                    ArrayList<PathStruct> allPaths = paths.get(new Pair<>(node.nodeID,i));

                    Collections.sort(allPaths,Comparator.comparing(PathStruct::getCost));
                    PathStruct minPath = allPaths.get(0);

                    int firstHop;
                    int secondHop = 0;
                    if (minPath.path.size() == 1){
                        firstHop= i;
                    }else{
                        firstHop= (int) minPath.path.toArray()[1];
                    }
                    for (int j = 1; j < allPaths.size(); j++) {
                        PathStruct secondMinPath = allPaths.get(j);
                        if (secondMinPath.path.size() == 1){
                            secondHop= i;
                        }else{
                            secondHop= (int) secondMinPath.path.toArray()[1];
                        }
                        if (firstHop != secondHop){
                            break;
                        }
                    }


                    forwardingTable.put(Integer.toString(i), new Pair<>(firstHop,secondHop));
                }
            }
            node.forwardingTable=forwardingTable;
        }
    }

    private static void  printAllPaths(int src, int dest, int currentCost, ArrayList<PathStruct> pathStructs){
        if (src == dest){
            pathStructs.add(new PathStruct((Stack<Integer>) path.clone(),currentCost));
            System.out.println(Arrays.toString(path.toArray())+ " cost = "+ currentCost);

        }else{
            visited[src] = true;
            path.push(src);
            for (int adjacentNode: nodes.get(src).neighbourIds){
                if (visited[adjacentNode] == false){
                    printAllPaths(adjacentNode,dest, nodes.get(src).distanceTable[adjacentNode][adjacentNode]+currentCost,pathStructs);
                }
            }
            visited[src] = false;
            path.pop();
        }
    }

    private static int getRandomNumberInRange(int min, int max) {

        if (min >= max) {
            throw new IllegalArgumentException("max must be greater than min");
        }

        Random r = new Random();
        return r.nextInt((max - min) + 1) + min;
    }



    public static void main(String[] args) throws IOException {

        // get node count
        File nodeFolder = new File("./src/nodeFolder/");

        int nodeCount = nodeFolder.listFiles().length;
        numNodes= nodeCount;

        for (int i = 0; i < nodeCount; i++) {
            Thread t = readNode(nodeFolder.listFiles()[i], nodeCount);
            t.start();
        }

        nodes.parallelStream().forEach((node) -> {
            try {
                node.establishConnections();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        scheduleOperation();
        
    } 
} 