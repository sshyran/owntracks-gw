This documents the steps required to get started with your Choral Greenwich.
We assume you'll want to install the OwnTracks edition of the software onto it.

1. Connect your Greenwich device to a serial port on your PC/Mac via an appropriate D-SUB9 cable. Use Putty (Windows), Minicom (Linux) or Screen (Linux/Mac) to connect with 115200/8N1 (no flow control) to your serial device.

2. Hitting ENTER once or twice should cause the device to issue an `ACK_ERR`; this means you're not yet authenticated to the device, which is perfectly normal.

3. In order to install a new software version via the serial line, you'll require the MES toolchain (for Windows only). Depending on which serial port your device is connected to, use
```
mesport COM3
```
to set this up once and for all.

4. Reboot the device by unplugging the power.  After first `^SYSSTART` enter
```
AT<enter>
```
and quit putty to release the lock on COM3.

5. Copy the configuration you prepared as well as the `OwnTracks.ja[dr]` files to the device: (we recommend you create a small `inst.bat` with which to do this:

```
rem mescopy GWmqttSettings.txt 	mod:a:/file/
mescopy OwnTracks.properties	mod:a:/file/

mescopy OwnTracks.jad	mod:a:/app/
mescopy OwnTracks.jar	mod:a:/app/
```

Then connect to serial and configure the launch app:

```
AT^SCFG="Userware/Autostart/AppName","","a:/app/OwnTracks.jar"
AT^SCFG="Userware/Autostart","",1
AT^SJRA=a:/app/owntracks.jar
```

At the next reboot, you should see something like this, and you can 
begin entering commands:

```
^SYSSTART

^SYSSTART
Running OwnTracks Choral 0.3.78
>$gps
ACK: Aug 06 09:33:40
Latitude xx.034441
Longitude y.476541
Altitude 168.8m
Speed 0.718576kph
Course 0.0
Distance 141m

>
```
