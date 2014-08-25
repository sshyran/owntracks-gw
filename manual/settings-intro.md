# Configuration and settings

The device can be configured in four distinct ways:

1. With an `owntracks.properties` file which is provisioned to the device. This is
   a text file (newline or CR-NL line endings) which looks like an INI-type file.
   Lines which begin with a hash symbol (`#`) are comments, all other lines
   *must* be of type _key_`=`_value_, and where keys are case sensitive.
   

	```
# file:///a:/file/OwnTracks.properties written: Mon Aug 25 13:44:04 UTC 2014
battery=0
sleep=3600
fields=course,speed,altitude,distance,trip
apn=datamobile
fixTimeout=800
host=ssl://mqtt.example.net
minSpeed=10
minDistance=100
loginTimeout=3600
otapURI=http://example.net/otap/otap.jad
tid=V7
mqttTimeout=600
keepAlive=30
publish=owntracks/acme/
notifyURI=http://example.net/otap/otap.jad?id=@
maxInterval=60
dbgComp=none
user=jjolie
gprsTimeout=600
sensitivity=1
password=<password>
closeMode=ResetHW
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
   From here, commands can be entered. In the console, all commands begin with a
   `$` character (e.g. `$login`, `$set`, etc.)

The following commands are available. Those marked with a `+` symbol require
prior authentication to the device:

* `-` `login` _secret_ (A successful login expires after `loginTimeout` seconds, and _secret_ is stored in settings, initially provisioned via a properties file)
* `+` `logout`
* `+` `set` `[<key>[=[<value>]]]`
* `+` `reboot`
* `-` `gps` (requests location update which is also published over MQTT)
* `-` `state` (shows state)
* `+` `log` `[old/delete]` (shows or resets log files)
* `-` `destroy` (switches back to non-Java AT-mode)
* `+` `upgrade` (perform an [OTA upgrade](OTAP.md)
* `-` `exec` _at-command_ (pass command to modem)


When configuring the device via SMS or MQTT, the operator must authenticate to
the device using its configured `secret`. Only then will the device allow settings
to be viewed or altered. As an example, consider these MQTT publishes which "talk"
to the device, assuming it is already operational and is currently connected to
the same MQTT broker we're talking to.

```bash
mosquitto_pub -u $user -P $pass -t owntracks/acme/van17/cmd -m 'login 123456'
mosquitto_pub -u $user -P $pass -t owntracks/acme/van17/cmd -m 'set raw=0'
```

We recommend subscribing to the output of the commands, as the device will inform
you whether the operation was completed, and in the case of commands which produce
output, will use that to communicate:

```bash
mosquitto_sub -u $user -P $pass -v -t owntracks/acme/van17/cmd/out

owntracks/acme/van17/cmd/out ACK: login accepted
owntracks/acme/van17/cmd/out ACK: raw=0
```

The following tables list all configurable settings for the device. Note that
most of these have rather sensible values and do not necessarily have to be
modified.
