import java.io.Serializable;
import java.util.Arrays;

public class Message implements Serializable{
    int senderNodeID;
    int receiverNodeID;
    int senderDistanceVector[];
    boolean changed;

    public Message(int senderNodeID, int receiverNodeID, int senderDistanceVector[], boolean changed){
        this.senderNodeID = senderNodeID;
        this.receiverNodeID = receiverNodeID;
        this.senderDistanceVector = senderDistanceVector;
        this.changed=changed;
    }

    @Override
    public String toString() {
        return "Message{" +
                "senderNodeID=" + senderNodeID +
                ", receiverNodeID=" + receiverNodeID +
                ", senderDistanceVector=" + Arrays.toString(senderDistanceVector) +
                '}';
    }

    private static final long serialVersionUID = 1L;

}
