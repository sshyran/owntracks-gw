# AT-commands Cinterion

Some _AT_ commands we've found useful for debugging checking things.

### Show signals

```
AT+CMER=2,0,0,2,0

   +CIEV: battchg,5	-- Battery charge level (0-5)
   +CIEV: signal,7	-- Signal level (0-7)
   +CIEV: service,1	-- Service is available (true/false)
   +CIEV: sounder,0	-- When the phone is ringing (true/false)
   +CIEV: message,0	-- If unread message (true/false)
   +CIEV: call,0	-- If call is in progress (true/false)
   +CIEV: roam,0	-- If we in roaming (true/false)
   +CIEV: smsfull,0	-- If the memory is full of SMS (true/false)
   +CIEV: rssi,3	-- Signal strengh (0-5)
```

### Show internal configuration

```
AT^SCFG?
```

### Set autostart program

```
AT^SCFG=”Userware/Autostart/Delay”,”100”
AT^SCFG="Userware/Autostart/AppName","","a:/app/OwnTracks.jar"
AT^SCFG="Userware/Autostart","",1
```

### Launch app from AT-mode

```
AT^SJRA=a:/app/owntracks.jar
```

### Set watchdog to restart mode

```
AT^SCFG="Userware/Watchdog","1"
```
