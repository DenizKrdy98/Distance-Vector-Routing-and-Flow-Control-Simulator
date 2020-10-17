import java.util.ArrayList;
import java.util.Stack;

public class PathStruct {
    public Stack<Integer> getPath() {
        return path;
    }

    public int getCost() {
        return cost;
    }

    Stack<Integer> path;
    int cost;

    PathStruct(Stack<Integer> path, int cost){
        this.cost=cost;
        this.path=path;
    }

}
