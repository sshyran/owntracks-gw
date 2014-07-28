Code Analysis and OwnTracks Enhancements
========================================

## Updated

* MQTTHandler.java
Auto reconnecting and subscribing
TLS works now with correct API authorizations

* Seriale.java - input from serial line and command processing "#"-commands
Added Commandprocessor for "$"-commands
TODO overlap with ATSender and Command Processor

* SocketGPRStask.java - process GPS to GPRS
Calling MQTTHandler and LocationHandler

## Added Classes
* DataStores.java - singleton wrapper for datastores
* Location.java - Location data structure
* LocationManager.java - Manages location updates, changes, minDistance, maxInterval
* Settings.java - singleton Class for boolean, int and string type settings on a .properties file backend
* StringSplitter.java - replacement for java String.split() method missing in JavaME
* CommandProcessor.java - shared user command interpretor and executor for different input media (SMS, CSD, SERIAL, MQTT-CMD)
* Mailboxes.java -singleton wrapper for datastores
* SemAT.java - singleton wrapper for SemAT

## Removed Files
* ATListenerCustom.java
was just necesseray to get references to singleton Objects

* TimerTaskCustom.java 
was just necesseray to get references to singleton Objects

* ThreadCustom.java
a) was just necesseray to get references to singleton Objects
b) getChecksum only used in GPS -> moved there

## Small Updates

* AppMain.java - start and setup
Removed global data setup (InfoStato, Mailbox, Flashfile, Datastore)

* ATListenerStd.java - processing of AT responses
Just removed the global references

* ATListenerEvents.java - processing of AT events
Just removed the global references

* ATsender.java
Just removed the global references
TODO overlap with CommandProcessor and Seriale

* BCListenerCustom.java
Just removed the global references
TODO duplicated use in more than one tasks???

* CheckSMS.java - reading and processing SMS commands
Just removed the global references
TODO uses common command processor

* CommGPStrasparent.java
Just removed the global references

* DataStore.java - stack datastructure save to be used in threaded environmend
Just removed the global references
TODO understand what it does, why there are 4 instances?

* FlashFile.java
Changed to singleton class, simple access from everywhere
TODO might be redundant, use Settings.java instead

* GlobCost.java - global Co(n)stants
Just the program version and debug indicators
TODO can we move some of the constants more locally?

* GoToPowerDown.java
Just removed the global references

* GPIOmanager.java
Just removed the global references

* InfoStato.java
Just removed the global references
TODO may need some overlap reduction with Settings and GlobCost

* LogError.java 
Just removed the global references

* Posusr.java
Just removed the global references
TODO seems to have overlap to LocationManager

* SaveData.java
Just removed the global references

* TestChiave.java - manage ingnition key 
Just removed the global references

* TimeoutTask.java - specialisation of java.TimerTask
Just removed the global references

* TrackingGPRS.java
Just removed the global references

* UDPSocketTask.java - manage and process commands via UDP
Just removed the global references

* UpdateCSD.java - manage and process commands via CSD
Just removed the global references

## Unchanged Files
* Coda.java - Low level Queue (Coda) implementation
* Monitor.java - Low level thread synchronisation
* PrioSem.java - Low level prioritized semaphore
* SemaforoEV.java - Low level semaphore
* Mailbox.java - Low level inter thread pipe communication
* CountSem.java - Low level counting semaphore
* BCListener.java - Bearer Control events processing
* InvalidThreadException.java - Low Level threading exception definition


## unused Classes
FlashRecordStore.java
