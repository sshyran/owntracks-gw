package general;

import java.util.Vector;

public class Coda extends Vector implements GlobCost{ 
	
	/*
	 * local variables 
	 */
	private Object 	lastValidPOSQ= null;
	private boolean isLastValidQ = false;
	private String dsTypeQ;
	private int ptr_in;
	private int ptr_out;
	private Object element;
	
	/*
	 * constructors 
	 */
	public Coda(String type, int capacity) {
		dsTypeQ = type;
		this.setSize(capacity);
	}
	
	public void addElement(Object obj){
		ptr_in = InfoStato.getInstance().getInfoFileInt(TrkIN);
		removeElementAt(ptr_in);
		insertElementAt(obj, ptr_in);
		ptr_in++;
		if(ptr_in > 99){
			ptr_in = 0;
		}
		InfoStato.getInstance().setInfoFileInt(TrkIN, Integer.toString(ptr_in));
	}
	
	public Object returnElement(){
		ptr_out = InfoStato.getInstance().getInfoFileInt(TrkOUT);
		element = elementAt(ptr_out);
		ptr_out++;
		if(ptr_out > 99){
			ptr_out = 0;
		}
		InfoStato.getInstance().setInfoFileInt(TrkOUT, Integer.toString(ptr_out));
		return element;
	}
	
	/**
	 * Synchronized method to obtain last GPS valid position saved
	 * to DataStore, if not already done the first FIX return 'null'
	 * 
	 * @return	GPRMC string corresponding to the last valid GPS position
	 * 			or 'null' value if not already done the first FIX
	 * 
	 */
//	public synchronized Object LastValidElement() {
		/* 
		 * If in current session there was a FIX, then send last valid
		 * position of current session, OTHERWISE SEND THE POSITION RECOVERED
		 * FROM FILE
		 */
/*		if (lastValidPOSQ!=null) return lastValidPOSQ;
		else {
			if (dsTypeQ.equalsIgnoreCase(dsCHORAL)) {
				//System.out.println("Datastore *** return: "+InfoStato.getInstance().getInfoFileString(LastGPSValid));
				return InfoStato.getInstance().getInfoFileString(LastGPSValid);
			}
			else {
				//System.out.println("Datastore *** return: "+InfoStato.getInstance().getInfoFileString(LastGPRMCValid));
				return InfoStato.getInstance().getInfoFileString(LastGPRMCValid);
			}
		}
	}
*/	
	public synchronized Object readOnlyIfObjectIsValid() {
		// if last received GPS position is valid, return this
		if (isLastValidQ==true) {
			return this.lastElement();
		}
		// otherwise return 'null'
		else return null;
	}
}