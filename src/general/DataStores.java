/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package general;

/**
 *
 * @author christoph
 */
public class DataStores {

     /**
     * dataStore types
     */
    public final static int dsDRMC = 0;
    public final static int dsDGGA = 1;
    public final static int dsTRMC = 2;
    public final static int dsTGGA = 3;
    private final String[] dataStoreNames = {"dsDRMC", "dsDGGA", "dsTRMC", "dsTGGA"};
    private final int NUMBEROFDATASTORES = 4;

    DataStore[] dataStores;

    private DataStores() {
       dataStores = new DataStore[NUMBEROFDATASTORES];
        for (int i = 0; i < NUMBEROFDATASTORES; i++) {
            dataStores[i] = new DataStore(dataStoreNames[i]);
        }
    };

    public static DataStores getInstance() {
        return DataStoresHolder.INSTANCE;
    }

    public static DataStore getInstance(int index) {
        DataStores dataStores = DataStores.getInstance();
        if (index < dataStores.NUMBEROFDATASTORES) {
            return dataStores.dataStores[index];
        } else {
            return null;
        }
    }
    
    private static class DataStoresHolder {
        private static final DataStores INSTANCE = new DataStores();
    }
 }
