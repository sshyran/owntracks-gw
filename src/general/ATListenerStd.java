/* Class 	ATListenerStd
 * 
 * This software is developed for Choral devices with Java.
 * Copyright Choral srl. All Rights reserved. 
 */

package general;

import java.io.IOException;

import choral.io.PowerManager;

import com.cinterion.io.*;

/**
 * This class is used to receive AT command answers.
 * To use this class, create before a class instance and pass it
 * as parameter of method 'ATCommand.addListener'.
 * <BR>
 * Please note:
 *      "^SYSSTART" and others 'system start URCs' are passed to 
 * 		Java application after first AT command sent to the module
 * 		after power up.
 * 
 * @version	1.03 <BR> <i>Last update</i>: 20-11-2007
 * @author 	alessioza
 * 
 */
public class ATListenerStd implements GlobCost, ATCommandResponseListener {
		
	/*
	 * local variables
	 */
	private int 	SGIOvalue;
	private double	supplyVoltage;
	private String	Vbatt;
	int 			numThread;
	boolean			isRicevitore;
	private String  temp, comandoGPRSCFG, dataGPRMC, oraGPRMC;
	int			countReg=0;
	boolean leaveAT = false;
	
	
	/* 
	 * constructors
	 */
	public ATListenerStd() {
	}
	
	/* 
	 * methods
	 */
	/**
	 * AT commands answer manager.
	 */
	public void ATResponse(String response) {
		/* 
		 * callback method for passing the response to a call
		 * of the NON-blocking version of the ATCommand.send()
		 */
		if(debugGSM){
			System.out.println("ATResponse: " + response);
		}
		leaveAT = false;
		/* Release XT65
		 * 
		 */
		if (response.indexOf("REVISION ") >=0) {
			//InfoStato.getInstance().setATexec(false);		// AT resource is free, no one AT command executing
			response = response.substring(response.indexOf("REVISION ")+"REVISION ".length());
			InfoStato.getInstance().setREV(response.substring(0,response.indexOf("\r")));
		} 
		
		/*
		 * CSQ
		 */
		if (response.indexOf("+CSQ") >=0) {
			//System.out.println("ATListenerStd: AT+CSQ");
			//InfoStato.getInstance().setATexec(false);		// AT resource is free, no one AT command executing
			InfoStato.getInstance().setCSQ(response.substring(response.indexOf("+CSQ: ")+"+CSQ: ".length(), response.indexOf(",")));
		} //+CSQ
		
		if(response.indexOf("^SCFG: \"MEopMode/Airplane\",\"off\"") >= 0){
			//InfoStato.getInstance().setATexec(false);		// AT resource is free, no one AT command executing
			//System.out.println("^SCFG: \"MEopMode/Airplane\",\"off\"");
			FlashFile.getInstance().setImpostazione(CloseMode, closeAIR);
			// Write to FlashFile.getInstance()
			InfoStato.getFile();
			FlashFile.getInstance().writeSettings();
			InfoStato.freeFile();
			Mailboxes.getInstance(0).write(msgREBOOT);
			
		}

		/*			 
		 * Answer to CSD call
		 */ 
		if (response.indexOf("CONNECT 9600/RLP") >=0) {
			//System.out.println("ATListenerStd: CSD connection established!");
			//InfoStato.getInstance().setATexec(false);
			InfoStato.getInstance().setCSDconnect(true);
		} //CONNECT
		
		
		/*
		 * Answer to IMEI command
		 */ 
		if (response.indexOf("AT+CGSN") >=0) {
			//InfoStato.getInstance().setATexec(false);
			InfoStato.getInstance().setIMEI(response.substring(response.indexOf("+CGSN\r\r\n")+"+CGSN\r\r\n".length(), response.indexOf("OK")-4));
		} //IMEI
		
		/*
		 * Answer to read of GPIO key
		 */
		if (response.indexOf("^SGIO") >=0) {
			//InfoStato.getInstance().setATexec(false);		// AT resource is free, no one AT command executing
			SGIOvalue = Integer.parseInt(response.substring(response.indexOf("^SGIO")+7,response.indexOf("^SGIO")+8));
			
			/*
			 * GPIO key
			 */
			
			// GPIO n.7
			if (InfoStato.getInstance().getGPIOnumberTEST()==7) {
			
				// if SGIOvalue = "0" -> key active -> set value
				if (SGIOvalue==0) {
					InfoStato.getInstance().setGPIOchiave(0);
					InfoStato.getInstance().setDigitalIN(0,0);
					InfoStato.getInstance().setTipoRisveglio(risveglioChiave);
					//System.out.println("ATListenerStd: power up due to key activation!!");
				}
				// if SGIOvalue = "1" -> key not active -> no set value -> set '-1'
				else {
					InfoStato.getInstance().setGPIOchiave(1);
					InfoStato.getInstance().setDigitalIN(1,0);
				}
			
			} //GPIO7

			/*
			 * Digital input
			 */
			
			// Input 1 = GPIO n.1
			if (InfoStato.getInstance().getGPIOnumberTEST()==1) {
				InfoStato.getInstance().setDigitalIN(SGIOvalue,1);			
			} //Input 1
			
			// Input 2 = GPIO n.3
			else if (InfoStato.getInstance().getGPIOnumberTEST()==3) {
				InfoStato.getInstance().setDigitalIN(SGIOvalue,2);			
			} //Input 2
			
		} //^SGIO
		
		
		/* 
		 * Operation on ^SBV (battery control)
		 */
		if (response.indexOf("^SBV") >=0) {
			//InfoStato.getInstance().setATexec(false);		// AT resource is free, no one AT command executing
			//System.out.print("ATListenerStd: check battery voltage...");
			// extract info about battery voltage
			response = response.substring(response.indexOf("^SBV: "));
			//System.out.println("response:" + response);
			Vbatt = response.substring("^SBV: ".length(),response.indexOf("\r\n"));
			if(debugGSM){
				System.out.println("Vbatt:" + Vbatt);
			}
			//new LogError("Vbatt:" + Vbatt);
			supplyVoltage = Double.parseDouble(Vbatt);
			//System.out.println("SupplyVoltage:"+supplyVoltage+"mV");
			// check battery voltage
			if (supplyVoltage <= VbattSoglia) {
				// send msg to AppMain about battery undervoltage
				Mailboxes.getInstance(0).write(msgBattScarica);	
				if(debugGSM){
					System.out.println("^SBC: UnderVoltage: " + Vbatt);
				}
			}
			// insert battery info into FlashFile.getInstance()
			Vbatt = Vbatt.substring(0,1) + "." + Vbatt.substring(1,2) + "V";
			InfoStato.getInstance().setBatteryVoltage(Vbatt);
			//System.out.println("ATListenerStd, Battery Voltage: " + Vbatt);
		} //^SBV
		
		
		/* 
		 * Operations on +CPMS (SMS memory status)
		 */
		if (response.indexOf("+CPMS: \"MT\"") >=0) {
			
			//InfoStato.getInstance().setATexec(false);		// AT resource is free, no one AT command executing
			if(debugGSM){
				System.out.println(response);
			}
			try{
				temp = response.substring(response.indexOf("\"MT\",") + "\"MT\",".length());
				//System.out.println("ATListenerStd, +CPMS: " + temp);
				
				String temp2 = temp;
				
				temp = temp.substring(0, temp.indexOf(","));
				//System.out.println("ATListenerStd, +CPMS: " + temp);
				
				InfoStato.getInstance().setNumSMS(Integer.parseInt(temp));
				//System.out.println("ATListenerStd, SMS number: " + InfoStato.getInstance().getNumSMS());
				
				temp2 = temp2.substring(temp2.indexOf(",")+1);
				
				temp2 = temp2.substring(0, temp2.indexOf(","));
				
				InfoStato.getInstance().setMaxNumSms(Integer.parseInt(temp2));
				
			}catch(NumberFormatException  e){
				
			}catch(NullPointerException e){
				
			} catch (StringIndexOutOfBoundsException ex) {
				
			}
		} //+CPMS
		
		
		/* 
		 * Operations on +CMGL (SMS list)
		 */
		if (response.indexOf("+CMGL") >=0) {
			
			//InfoStato.getInstance().setATexec(false);		// AT resource is free, no one AT command executing
			try {
				// Extract strng '+CMGL.....'
				temp = response.substring(response.indexOf("+CMGL: "));
				//System.out.println(temp);
				// Extract string '**,*'
				temp = temp.substring(temp.indexOf("+CMGL: ") + "+CMGL: ".length(), temp.indexOf(","));
				//System.out.println(temp);
				InfoStato.getInstance().setCodSMS(Integer.parseInt(temp));
			} catch (StringIndexOutOfBoundsException ex) {
				if(debugGSM){
					System.out.println("ATListenerStd, +CMGL: StringIndexOutOfBoundsException");
				}
				InfoStato.getInstance().setCodSMS(-2);
			} //catch
			
			//System.out.println("ATListenerStd, SMS code: " + InfoStato.getInstance().getCodSMS());
			
		} //+CMGL
		
		if (response.indexOf("+CMGD") >=0) {
			//System.out.println(response);
		}
		
		/* 
		 * Operations on +CMGR
		 */
		if (response.indexOf("+CMGR") >=0) {
			/*
			 * Check if present a particular string into SMS text
			 */
			if(response.indexOf("+CMGR: 0,,0") >= 0){
				InfoStato.getInstance().setSMSCommand("+CMGR: 0,,0");
				InfoStato.getInstance().setValidSMS(false);
			}
			else{
				try {
					//System.out.println("ATListenerStd, response: " + response);
					comandoGPRSCFG = response.substring(response.indexOf("GPRSCFG "));
					//System.out.println("ATListenerStd, comando1: " + comandoGPRSCFG);
					comandoGPRSCFG = comandoGPRSCFG.substring(comandoGPRSCFG.indexOf("GPRSCFG ")+"GPRSCFG ".length(), comandoGPRSCFG.indexOf(","));
					//System.out.println("ATListenerStd, APN: " + comandoGPRSCFG);
					
					FlashFile.getInstance().setImpostazione(ConnProfileGPRS, "bearer_type=GPRS;access_point="+comandoGPRSCFG);
					FlashFile.getInstance().setImpostazione(apn, comandoGPRSCFG);		    						
					InfoStato.getFile();
					FlashFile.getInstance().writeSettings();
					InfoStato.freeFile();
					
				} catch (StringIndexOutOfBoundsException ex) {
					if(debug){
						System.out.println("ATListenerStd, +CMGR: StringIndexOutOfBoundsException");
					}
				} catch (NullPointerException npe) {
					if(debug){
						System.out.println("ATListenerStd, +CMGR: NullPointerException");
					}
				} //catch
				
				/*
				 * Extract telephone number
				 */
				try {
					
					temp = InfoStato.getInstance().campo(response, 1, false);
					//System.out.println("ATListenerStd, extract number: " + temp);
					
					// Telephone number of sender
					InfoStato.getInstance().setNumTelSMS(temp);
					//System.out.println("ATListenerStd, SMS sender number: " + InfoStato.getInstance().getNumTelSMS());
					
				} catch (StringIndexOutOfBoundsException ex) {
					if(debug){
						System.out.println("ATListenerStd, +CMGR: StringIndexOutOfBoundsException (Number)");
					}
				} catch (NullPointerException npe) {
					if(debug){
						System.out.println("ATListenerStd, +CMGR: NullPointerException (Number)");
					}
				} //catch
				
				/*
				 * Check validity of command SMS
				 */
				if (response.indexOf(keySMS) >=0){
					InfoStato.getInstance().setSMSCommand(keySMS);
					InfoStato.getInstance().setValidSMS(true);
				}
				else{
					if(response.indexOf(keySMS1) >=0){
						InfoStato.getInstance().setSMSCommand(keySMS1);
						InfoStato.getInstance().setValidSMS(true);
					}
					else{
						if(response.indexOf(keySMS2) >=0){
							InfoStato.getInstance().setSMSCommand(keySMS2);
							InfoStato.getInstance().setValidSMS(true);
						}
						else
						InfoStato.getInstance().setValidSMS(false);
					}
				}
			}
		} //+CMGR
		
		
		/*
		 * Send SMS
		 */
		if (response.indexOf(">") >=0) {
			leaveAT = true;
		} //>
		
		if (response.indexOf("+CMGS") >=0) {
			//InfoStato.getInstance().setATexec(false);		// AT resource is free, no one AT command executing
			//System.out.println(response);
		}
		
		/* 
		 * Operations on +CCLK
		 */
		if (response.indexOf("+CCLK") >=0) {
			//InfoStato.getInstance().setATexec(false);		// AT resource is free, no one AT command executing
			/* 
			 * Extract strings oraGPRMC and dataGPRMC
			 */
			//System.out.println("ATListenerStd, +CCLK:: received answer is " + response);
			dataGPRMC = response.substring(response.indexOf("\"")+1, response.indexOf(","));
			response = response.substring(response.indexOf(","));
			oraGPRMC= response.substring(response.indexOf(",")+1, response.indexOf("\""));
			InfoStato.getInstance().setDataOraGPRMC(dataGPRMC, oraGPRMC);
			
		} //+CCLK
		
		/* 
		 * Operations on +COPS (SIM network registration)
		 */
		if (response.indexOf("^SMONG") >=0 || response.indexOf("^smong") >=0) {
			//InfoStato.getInstance().setATexec(false);		// AT resource is free, no one AT command executing
			//System.out.println(response);
			//new LogError(response);
		} //^SMONG
		
		/* 
		 * Operations on +COPS (SIM network registration)
		 */
		if (response.indexOf("+COPS:") >=0) {
			//InfoStato.getInstance().setATexec(false);		// AT resource is free, no one AT command executing
			if(response.indexOf(",") >= 0)
				countReg = 0;
			else
				countReg++;
			if(countReg>10){	
				new LogError("NO NETWORK");
				//System.out.println("NO NETWORK");
				InfoStato.getInstance().setReboot();
			}
		} //+COPS
		
		
		
		/* 
		 * I wait for AT command answer before free AT resource for a new operation
		 */
		
		// Execution OK
		if (response.indexOf("OK") >=0) {
			//System.out.println("ATListenerStd, AT command result 'OK'");
			//InfoStato.getInstance().setATexec(false);		// AT resource is free, no one AT command executing
		} //OK
		
		// Execution ERROR
		if (response.indexOf("ERROR") >=0) {
			if(debugGSM){
				System.out.println("ATListenerStd, AT command result 'ERROR'");
			}
			//InfoStato.getInstance().setATexec(false);		// AT resource is free, no one AT command executing
		} //ERROR
		// Execution NO CARRIER
		if (response.indexOf("NO CARRIER") >=0) {
			
			//InfoStato.getInstance().setATexec(false);		// AT resource is free, no one AT command executing

		} //NO CARRIER

		// Execution BUSY
		if (response.indexOf("BUSY") >=0) {
			
			//InfoStato.getInstance().setATexec(false);		// AT resource is free, no one AT command executing
		} //BUSY

		// Execution NO DIALTONE
		if (response.indexOf("NO DIALTONE") >=0) {
			
			//InfoStato.getInstance().setATexec(false);		// AT resource is free, no one AT command executing
		} //NO DIALTONE
		
		//if(!leaveAT)
			InfoStato.getInstance().setATexec(false);		// AT resource is free, no one AT command executing
		//System.out.println("EXIT LISTENER");
			
	} //ATResponse	
} //ATListenerStd

