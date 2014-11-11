# Configuration and settings {#allsettings}

The device can be configured in four distinct ways:

1. With an `owntracks.properties` file which is provisioned to the device via MES. This is
   a text file (newline or CR-NL line endings) which looks like an INI-type file.
   Lines which begin with a hash symbol (`#`) are comments, all other lines
   *must* be of type _key_`=`_value_, and where keys are case sensitive.
   

	```
# file:///a:/file/OwnTracks.properties written: Mon Aug 25 13:44:04 UTC 2014
fields=course,speed,altitude,distance,trip
apn=datamobile
host=ssl://mqtt.example.net
minSpeed=10
minDistance=100
loginTimeout=3600
otapURI=http://example.net/otap/otap.jad
tid=V7
publish=owntracks/acme/
notifyURI=http://example.net/otap/otap.jad?id=@
maxInterval=60
user=jjolie
password=<password>
port=8883
minInterval=3600
motion=63
secret=1234567890
# EOF
	```

2. Via SMS. The SIM card in the device has a mobile number. If your SIM card's
   data plan allows SMS, it can receive commands via SMS. **Note**: this can
   incurr additional charges.

3. Via MQTT commands sent to the device's `subscription` topic (typically `../cmd`)

4. Via a serial console attached to the RS-232C interface of the Greenwich, as long
   as the application is running. This is indicated on the console by a prompt (`> `).
   From here, commands can be entered. In the console.

The following commands are available. Those marked with a `+` symbol require
prior authentication to the device:


