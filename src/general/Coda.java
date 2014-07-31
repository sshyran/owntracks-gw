package general;

import java.util.Vector;

public class Coda extends Vector implements GlobCost{ 
	
	/*
	 * local variables 
	 */
	private Object 	lastValidPOSQ= null;
	private boolean isLastValidQ = false;
	private String dsTypeQ;
	private int ptr_in = 0;
	private int ptr_out = 0;
	private Object element;
	
	/*
	 * constructors 
	 */
	public Coda(String type, int capacity) {
		dsTypeQ = type;
		this.setSize(capacity);
	}
	
	public void addElement(Object obj){
		//ptr_in = InfoStato.getInstance().getInfoFileInt(TrkIN);
		removeElementAt(ptr_in);
		insertElementAt(obj, ptr_in);
		ptr_in++;
		if(ptr_in > 99){
			ptr_in = 0;
		}
		//InfoStato.getInstance().setInfoFileInt(TrkIN, Integer.toString(ptr_in));
	}
	
	public Object returnElement(){
		//ptr_out = InfoStato.getInstance().getInfoFileInt(TrkOUT);
		element = elementAt(ptr_out);
		ptr_out++;
		if(ptr_out > 99){
			ptr_out = 0;
		}
		//InfoStato.getInstance().setInfoFileInt(TrkOUT, Integer.toString(ptr_out));
		return element;
	}
	
}