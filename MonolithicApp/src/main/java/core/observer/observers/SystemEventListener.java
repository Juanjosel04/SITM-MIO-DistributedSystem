package core.observer.observers;

import core.observer.events.SystemEvent;

public interface SystemEventListener {
    void onSystemEvent(SystemEvent event);
}
