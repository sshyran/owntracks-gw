/*	
 * Class 	ThreadCustom
 * 
 * This software is developed for Choral devices with Java.
 * Copyright Choral srl. All Rights reserved. 
 */

package general;

/**
 * Extension of Thread class, that contains some additional methods frequently
 * used in the thread for the MIDlets.
 * 
 * @version	1.01 <BR> <i>Last update</i>: 08-10-2007
 * @author 	alessioza
 * 
 */
public class ThreadCustom extends Thread implements GlobCost{
	
	/* 
	 * local variables
	 */
	protected String	checksum;
	
	
	/*
	 * methods
	 */
	
	/**
	 * For ckecksum calculation on a string
	 * 
	 * @param	sentence	string on which calculate checksum
	 * @return	checksum
	 */
	public String getChecksum(String sentence) {

		try{
			int[] intSentence = new int[sentence.length()];
			intSentence[0] = sentence.charAt(0);
			
			for (int i = 1; i < sentence.length() ; i++){
				intSentence[i] = intSentence[i-1] ^ sentence.charAt(i);
			}
			
			checksum = Integer.toHexString(intSentence[sentence.length()-1]);
			if(checksum.length() < 2) return "0" + checksum;
			else return checksum;
		}catch(IndexOutOfBoundsException e){
			return "00";
		}
	}
	
} //ThreadCustom

