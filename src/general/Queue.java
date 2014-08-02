/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package general;

import java.util.Vector;

/**
 *
 * @author christoph
 */
public class Queue {
    
    private final Vector vector;
    private final int capacity;
    private final String name;
    private boolean loaded;
    
    Queue(int capacity, String name) {
        this.name = name;
        this.capacity = capacity;
        if (capacity > 0) {
            vector = new Vector(capacity);
        } else {
            vector = new Vector();
        }
    }
    
    public synchronized Object get() {
        load();
        if (vector.isEmpty()) {
            return null;
        } else {
            Object o = vector.firstElement();
            vector.removeElementAt(0);
            return o;
        }
    }
    
    public synchronized boolean put(Object o) {
        load();
        boolean overflow = false;
        if (capacity > 0  && vector.size() >= capacity) {
            vector.removeElementAt(0);
            overflow = true;
        }
        vector.addElement(o);
        return overflow;
    }
    
    private int load() {
        if (!loaded && name != null) {
            loaded = true;
        }
        return vector.size();
    }
    
    public synchronized void store() {
        load();
        if (name != null) {
        }
    }
    
    public synchronized int size() {
        return vector.size();
    }
    
    public synchronized int capacity() {
        return capacity;
    }
}
