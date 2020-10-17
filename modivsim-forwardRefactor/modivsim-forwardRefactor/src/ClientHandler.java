import java.io.*;
import java.net.Socket;

public class ClientHandler extends Thread {
    ObjectOutputStream out;
    ObjectInputStream in;
    final Socket s;
    int clientID;
    int serverID;
    Node clientNode;

    public ClientHandler(Socket s,ObjectInputStream in, ObjectOutputStream out, int clientID, int serverID, Node clientNode) throws IOException {
        this.s = s;
        this.clientID = clientID;
        this.serverID = serverID;
        this.clientNode = clientNode;

        this.out = out;
        this.in = in;
    }

    public void sendMessage(Message msg) throws IOException {
        out.writeObject(msg);
        out.flush();
        out.reset();
    }

    public void receiveMessage() throws IOException, ClassNotFoundException {
        Message m = (Message) in.readObject();
        if (m.changed){
            clientNode.receiveUpdate(m);
        }
    }

}
