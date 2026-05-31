package core.queue;

import core.model.Datagram;

import java.util.LinkedList;
import java.util.Queue;

public class DatagramQueue {
    private final Queue<Datagram> queue = new LinkedList<Datagram>();

    public void add(Datagram datagram) {
        if (datagram != null) {
            queue.add(datagram);
        }
    }

    public Datagram poll() {
        return queue.poll();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public int size() {
        return queue.size();
    }
}
