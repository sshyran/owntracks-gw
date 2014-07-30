Settings.properties file
========================

## Format

* comment lines start with #
* other lines are <key>=<value>
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

## user interface

Commands can be entered
a) via serial line (starting with $, e.g. $set)
b) via publish to .../cmd

Commands are:
- login <secret>
+ logout
+ set [<key>[=[<value>]]]
+ reboot
- gps

commands marked with + require login

login expires after "loginTimeout" (settings) seconds
"secret" is stored in settings, too (see below)

gps returns and publishes the last known location


## used keys

* minDistance minimum distance travelled before next location is published in meters, default 0

* maxInterval maximum time interval before next location is published in seconds, default 0

* retained publish locations with retained flag, 0/1, default 1

* qos publish locations with quality of service 0..2, default 1

* raw publishes raw GPS string to .../raw topic, 0/1, default 1

* user MQTT connect user name, default no authorization

* password MQTT connect user password, default no password

* willTopic" the topic for the last will and testament, default = publish topic

* will" the will to be published on client error, default = {"type":"lwt","tst":""}

* willQos default 1

* willRetain default false

* keepAlive in seconds default 60

* cleanSession 0/1, default 1

* subscription default publish topic + /cmd

* subscriptionQos default 1

* fields comma separated list of optional fields in publish message , default = "course,speed,altitude,distance,battery"

* publish the base topic for publications, default "owntracks/gw/"

* secret for login, default 1234567890

* loginTimeout login expires after <loginTimeout> seconds, default 30

* clientID, used as MQTT connect client id and is appended to base publish topic, default IMEI of device


`$set clientID=<id>` sets the MQTT clientID for the publish (default is the device's IMEI number). This identifier is also appended to `$set publish=<basetopic>`, default owntracks/gw

