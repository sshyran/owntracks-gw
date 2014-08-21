Settings.properties & Command Processor
=======================================

## Settings.file Format

* comment lines start with `#`
* other lines are `<key>=<value>`
* keys are case sensitive


```
# file:///a:/file/OwnTracks.properties written: Tue Jan 01 00:12:24 UTC 2002
maxInterval=90
user=ajuser
raw=0
password=apassword
minDistance=500
# EOF
```

## Command Processor user interface

Commands can be entered
* via serial line at the `>` prompt (starting with `$`, e.g. `$set`)
* via an MQTT publish to the base topic with `/cmd` appended to it.
* via SMS

Commands are:
* - login _secret_
* + logout
* + set `[<key>[=[<value>]]]`
* + reboot ([sic])
* - gps (requests location update)
* - state (shows state)
* + log `[old/delete]` (shows or resets log files)
* - close (closes CSD)
* - destroy (switches back to non-Java AT-mode)
* + upgrade (perform an [OTA upgrade](OTAP.md)
* - exec _at-command_ (pass command to modem)

commands marked with `+` require login

For example, assume your Greenwich is publishing to `owntracks/GWBUS-ak`, I can publish the following:

```
mosquitto_pub ... -t owntracks/GWBUS-ak/cmd -m "gps"
```

and the device will respond with:

```
owntracks/GWBUS-ak {"_type":"location","t":"m", ... }
owntracks/GWBUS-ak/cmd/out ACK: command: "gps" {"_type":"location","t":"m", ...}
```

login expires after "loginTimeout" (settings) seconds
"secret" is stored in settings, too (see below)

`gps` returns and publishes the last known location


## used keys

### GPRS

| Key         | Values | Default  | Validity | Description                |
|:------------|:------:|:--------:|---------:|:---------------------------|
| pin         |        | "" (none)| reboot   | SIM pin, if none given, no cpin command|
| apn         |        | internet | reboot   | GPRS access point name     |
| apnUser     |        |          | reboot   | GPRS access point user id  |
| apnPassword |        |          | reboot   | GPRS access point password |
| gprsTimeout | 1-...  | 600      | next loop| time in seconds after which the system does not expect gprs anymore |
| mqttTimeout | 1-...  | 600      | next loop| time in seconds after which the system does not expect mqtt anymore |

### Device

| Key         | Values   | Default  | Validity | Description                |
|:------------|:--------:|:--------:|---------:|:---------------------------|
| sleep       | 0...     | 21600(6h)| reboot   | sleep after device off     |
| motion      | 0,1..255 | 4        | reboot   | sensivity for motion sensor, 0=off, 1 highest sensitivity|
| otapUser         |  `""` | string   | upgrade    | HTTP basic auth username for [OTAP](OTAP.md) |
| otapPassword     |  `""` | string   | upgrade    | HTTP basic auth password for [OTAP](OTAP.md) |
| otapURI          |  `""` | string   | upgrade    | URL to `OwnTracks.jad` for [OTAP](OTAP.md) |
| notifyURI        |  `""` | string   | upgrade    | URL to POST result of [OTAP](OTAP.md) |
| lowBattery       |  `""` | 3599   | immediately    | Threshold to assume low Battery in mV |


### MQTT

| Key  | Values | Default | Validity | Description |
|:-----|:------:|:-------:|---------:|:------------|
| protocol         | TCP/UDP | TCP    | reboot    | publish via TCP/MQTT or UDP/CSV |
| host             |         | tcp://localhost  | reboot    | scheme and hostname/ip to connect to (valid schemes are `tcp:` and `ssl:`) |
| port             | 1-... | 1883        | reboot    | port to connect to |
| retained         | 0/1   | 1        | reboot    | publish locations with retained flag |
| qos              | 0-2   | 1        | reboot    | publish locations with Quality of Service |
| raw              | 0/1   | 1        | immediate | publish raw GPS string to .../raw topic |
| user             |       | null     | reboot    | MQTT connect user name, default means no authorization |
| password         |       | null     | reboot    | MQTT connect password |
| publish          |       | owntracks/gw | reboot    | base topic for MQTT location messages |
| willTopic        |       | =publish + / + clientID + /status | reboot    | topic for MQTT last will and testament |
| will             |       | 0    | reboot    | the message published on MQTT client error |
| willQos          |  0..2 | 1    | reboot    | NQTT willQos |
| willRetain       |  0/1  | 1    | reboot    | MQTT willRetain |
| keepAlive        |  1-.. | 60   | reboot    | MQTT keepAlive |
| cleanSession     |  0/1  | 1    | reboot    | MQTT cleansession |
| subscription     |       | =publish + /cmd | reboot    | client listens for commands here |
| subscriptionQos  |  0..2 | 1    | reboot    | MQTT subscriptionQos |
| clientID         |       | IMEI | reboot    | used as MQTT connect client id and is appended to base publish topic|


`$set clientID=<id>` sets the MQTT clientID for the publish (default is the device's IMEI number). This identifier is also appended to `$set publish=<basetopic>`, default owntracks/gw


### LocationManager

| Key  | Values | Default | Validity | Description |
|:-----|:------:|:-------:|---------:|:------------|
| minSpeed         | 0-... | 0        | immediate | minimum speed in km/h to switch to move move|
| minDistance      | 0-... | 0        | immediate | distance sincde last publis in meters to switch to move mode|
| maxInterval      | 0-... | 0        | immediate | maximum time before before next publish in move mode in seconds |
| minInterval      | 0-... | 0        | immediate | minimum time before before publish in stationary mode in seconds |
| fields           |       | course,speed,altitude,distance,battery,trip | immediate | comma separated list of optional fields in publish message |
| fixTimeout       | 1-... | 600      | next loop | time in seconds after which the system does not expect a fix anymore |


### Debugging

| Key  | Values | Default | Validity | Description |
|:-----|:------:|:-------:|---------:|:------------|
| generalDebug| 0/1 | 0 | immediate|controls the debug output for most of the code |
| locDebug    | 0/1 | 0 | immediate|controls the debug output of the LocationManager |
| timerDebug  | 0/1 | 0 | immediate|controls the debug output of the WatchDogs |
| usbDebug    | 0/1 | 0 | immediate|if set to 1, debug output flows to USB instead of ASC0 |
| gsmDebug    | 0/1 | 0 | immediate|controls the debug output for all AT commands and responses |
| mainDebug   | 0/1 | 0 | immediate|controls the debug output for main taks |
| keyDebug    | 0/1 | 0 | immediate|controls the debug output for iginition key management |

### Miscellaneous

| Key  | Values | Default | Validity | Description |
|:-----|:------:|:-------:|---------:|:------------|
| secret |  | 1234567890 | immediate | for login |
| loginTimeout    | 0-... | 30 | immediate| you will have to login again afer loginTimeout seconds, 0 = disable |

### Status monitoring

When the device comes alive, it will publish a retained `1` to the base `publish` topic with `/status` appended to it. Assuming you've configured

```
publish=owntracks/gw/
```

and the devices `clientID` is `dev1`, the following publish will be seen when the device comes
up:

```
owntracks/gw/dev1/status 1
```

Similarly, a retained `0` will be published when the device goes offline. When the device does an "intended shutdown", e.g. you disable tracking with

