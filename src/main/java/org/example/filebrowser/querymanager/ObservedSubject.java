package org.example.filebrowser.querymanager;

public interface ObservedSubject {
    void addObserver(Observer o);
    void removeObserver(Observer o);
    void notifyObservers(Observation observation);
}
