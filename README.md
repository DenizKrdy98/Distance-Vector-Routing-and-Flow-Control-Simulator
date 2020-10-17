# Distance-Vector-Routing-and-Flow-Control-Simulator

The project simulates a network traffic in a given topology where the distance vector algorithm is used for path computations and routing. The topology of the network is given and cannot be changed by the user. User can change the path costs between each router, and specify any number of flows from any router to the other. Package size also can be changed. After the user specifies, the program can run the network traffic.  

The program basically consists of two major parts: 
     - Distance Vector Computation
     - Forwarding and Routing Algorithm
     
  Distance Vector Computation:
  In this part, the user enters the path costs, and the distance vectors are computed by the program, and exported to a text file.
  
  Flow Routing Algorithm:
  In this part, the program takes the output of first part, the distance vectors, as an input to construct the topology. The user enters the each packet flow to be executed, i.e., specifies the packages to be transmitted from which router to which router with the sizes of packages. Then, the program similates the traffic at each steps of each packages including visited routers, queues, delays etc. 
  
  Used skills:
    * Socket programming in router communications
    * Multi-Threading
    * De-centralized programming
    * Object-oriented design
    * I/O Management, File Reading/Writing
    * Implementation of Distance Vector, Routing/Forwarding, and Flow Control Algorithms
   
