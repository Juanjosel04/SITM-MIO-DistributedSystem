package core.observer.subject;

import core.observer.events.SystemEvent;
import core.observer.observers.SystemEventListener;

import java.util.ArrayList;
import java.util.List;

public class SystemEventPublisher {
    private final List<SystemEventListener> listeners = new ArrayList<SystemEventListener>();

    public synchronized void addListener(SystemEventListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public synchronized void removeListener(SystemEventListener listener) {
        listeners.remove(listener);
    }

    public void publish(SystemEvent event) {
        List<SystemEventListener> snapshot;
        synchronized (this) {
            snapshot = new ArrayList<SystemEventListener>(listeners);
        }
        for (SystemEventListener listener : snapshot) {
            listener.onSystemEvent(event);
        }
    }
}
