# MQTT topics

The _Greenwich OwnTracks Edition_ publishes [MQTT](http://mqtt.org) payloads to
distinct topics, which are all prefixed by the configurable
[publish](README_Settings.md) and [clientID](README_Settings.md) settings. (Recall
that the latter defaults to the IMEI.) All publishes are _retained_, unless otherwise specified.

Assuming we've configured

```
publish=owntracks/gw/
clientID=C1
```

the _base topic_ will be `owntracks/gw/C1`

### base topic
The base topic is used for [JSON payloads](https://github.com/owntracks/owntracks/wiki/JSON).

### `../status`

The status subtopic indicates the MQTT connection status of the device:
* ' 1' = connected
* ' 0' = connection lost (determined by the MQTT broker using LWT)
* '-1' = disconnected

```
owntracks/gw/C1/status 1
```

### `../start`

Upon startup, the device publishes the IMEI, the application version and the time of the start:

```
owntracks/gw/C1/start 123456789012345 0.8.1 20140930T085913Z
```

### `../voltage`

Built-in battery (`batt`) and external (`ext`) voltages are published to `../voltage/` when voltage changes "significantly"
(defined in [Settings](README_Settings.md)).

```
owntracks/gw/C1/voltage/batt 4.4
owntracks/gw/C1/voltage/ext 12.2
```

### `../temperature`

Temperature sensors `0` and `1` readings in degrees Celsius with 2 decimals are published to `../voltage/\[01\]` when temperature changes "significantly"
(defined in [Settings](README_Settings.md)).

```
owntracks/gw/C1/temperature/0 4.41
owntracks/gw/C1/temperature/1 24.82
```

### `../operators` and `../cellinfo`

The current operator, allowed other operators, forbidden operators and unknown operators (all in MCC + MNC numeric format)
and the information of the current mobile cell are reported when they change.
[Definitions](http://de.wikipedia.org/wiki/Location_Area)

```
owntracks/gw/C1/operators 26207 +22801 +22802 +22803 -26201 -22602 -26203 ?33301
owntracks/gw/C1/cellinfo 262 7 1451 35
```

### `../gpio/`

The *inverted* status of the GPIO pins are published under the `gpio/` subtopic, [as documented](GPIO.md).

```
owntracks/gw/C1/gpio/1 0
owntracks/gw/C1/gpio/3 1
owntracks/gw/C1/gpio/7 0
```

### `../cmd/out`

Output of commands sent to the device (e.g. `login`, `set`, etc.) is published to the `cmd/out` topic.

```
owntracks/gw/C1/cmd login xxx
owntracks/gw/C1/cmd/out NACK: incorrect login
```
