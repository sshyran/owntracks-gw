/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package general;

import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordStoreFullException;
import javax.microedition.rms.RecordStoreNotFoundException;
import javax.microedition.rms.RecordStoreNotOpenException;
import javax.microedition.rms.InvalidRecordIDException;

/**
 *
 * @author christoph
 */
public class Queue {
    private final String name;
    
    private RecordStore recordStore;
    private int recordID = 1;

    Queue(String name) {
        this.name = name;
        try {
            this.recordStore = RecordStore.openRecordStore(name, false);
            SLog.log(SLog.Informational, "Queue", "openRecordStore " + name);
            if (this.recordStore.getNumRecords() == 0) {
                this.recordStore.closeRecordStore();
                RecordStore.deleteRecordStore(name);
                SLog.log(SLog.Informational, "Queue", "deleteRecordStore " + name);
                this.recordStore = RecordStore.openRecordStore(name, true);
                SLog.log(SLog.Informational, "Queue", "openRecordStore (create)" + name);
            }
        } catch (RecordStoreNotFoundException rsnfe) {
            try {
                this.recordStore = RecordStore.openRecordStore(name, true);
                SLog.log(SLog.Informational, "Queue", "openRecordStore (create)" + name);
            } catch (RecordStoreFullException rsfe) {
                SLog.log(SLog.Error, "Queue", "RecordStoreFullException " + name);
            } catch (RecordStoreException rse) {
                SLog.log(SLog.Error, "Queue", "RecordStoreException " + name);

            }
        } catch (RecordStoreFullException rsfe) {
            SLog.log(SLog.Error, "Queue", "RecordStoreFullException " + name);

        } catch (RecordStoreException rse) {
            SLog.log(SLog.Error, "Queue", "RecordStoreException " + name);

        }
    }

    public synchronized byte[] get() {
        byte[] bytes = null;
        try {
            SLog.log(SLog.Debug, "Queue", "getNumRechords " + recordStore.getNumRecords());            
            if (recordStore.getNumRecords() == 0) {
                return null;
            } else {
                boolean gotRecord = false;
                do {
                    try {
                        SLog.log(SLog.Debug, "Queue", "getRecord " + recordID);            
                        bytes = recordStore.getRecord(recordID);
                        gotRecord = true;
                    } catch (InvalidRecordIDException irie) {
                        SLog.log(SLog.Informational, "Queue", "InvalidRecordIDException " + recordID);            
                        recordID++;
                    }
                } while (!gotRecord);
            }
        } catch (RecordStoreNotOpenException rsnoe) {
            SLog.log(SLog.Error, "Queue", "RecordStoreNotOpenException getRecord " + recordID);
        } catch (RecordStoreException rse) {
            SLog.log(SLog.Error, "Queue", "RecordStoreException getRecord " + recordID);
        }
        return bytes;
    }

    public synchronized void consume() {
        try {
            SLog.log(SLog.Debug, "Queue", "deleteRecord " + recordID);            
            recordStore.deleteRecord(recordID);
            recordID++;
        } catch (RecordStoreNotOpenException rsnoe) {
            SLog.log(SLog.Error, "Queue", "RecordStoreNotOpenException deleteRecord " + recordID);
        } catch (RecordStoreException rse) {
            SLog.log(SLog.Error, "Queue", "RecordStoreException deleteRecord " + recordID);
        }
    }

    public synchronized boolean put(byte[] bytes) {
        try {
            int newRecordId =  recordStore.addRecord(bytes, 0, bytes.length);
            SLog.log(SLog.Debug, "Queue", "addRecord " + newRecordId);
        } catch (RecordStoreNotOpenException rsnoe) {
            SLog.log(SLog.Error, "Queue", "RecordStoreNotOpenException addRecord");
            return false;
        } catch (RecordStoreException rse) {
            SLog.log(SLog.Error, "Queue", "RecordStoreException addRecord");
            return false;
        }
        return true;
    }

    public synchronized int size() {
        try {
            return recordStore.getNumRecords();
        } catch (RecordStoreNotOpenException rsnoe) {
            SLog.log(SLog.Error, "Queue", "RecordStoreNotOpenException getNumRecords");
            return 0;
        }
    }
}
