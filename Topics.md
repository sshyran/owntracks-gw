# MQTT topics

The _Greenwich OwnTracks Edition_ publishes [MQTT](http://mqtt.org) payloads to
distinct topics, which are all prefixed by the configurable
[publish](README_Settings.md) and [clientID](README_Settings.md) settings. (Recall
that the latter defaults to the IMEI.) All publishes are _retained_, unless otherwise specified.

Assuming we've configured

```
publish=owntracks/gw/
clientID=GWCAR-jp
```

the _base topic_ will be `owntracks/gw/GWCAR-jp`.

## base topic

The base topic is used for [JSON payloads](https://github.com/owntracks/owntracks/wiki/JSON).

## `../hw`

Upon startup, the device publishes the hardware model of the chip. Example:

```
owntracks/gw/GWCAR-jp/hw Cinterion,EGS5-X,REVISION 02.004
```

## `../sw/`

Upon startup, the device publishes the Greenwich version as well as the application version:

```
owntracks/gw/GWCAR-jp/sw/gw 02.16B,02.01,02.16
owntracks/gw/GWCAR-jp/sw/midlet OwnTracks Choral 0.5.43
```

## `../voltage`

Built-in battery (`batt`) and external (`ext`) voltages are published to `../voltage/` when voltage changes "significantly" (this is currently when a change of 0.1V is detected).

```
owntracks/gw/GWCAR-jp/voltage/batt 4.4V
owntracks/gw/GWCAR-jp/voltage/ext 12.2V
```


## `../gpio/`

The *inverted* status of the GPIO pins are published under the `gpio/` subtopic, [as documented](GPIO.md).

```
owntracks/gw/GWCAR-jp/gpio/1 0
owntracks/gw/GWCAR-jp/gpio/3 1
owntracks/gw/GWCAR-jp/gpio/7 0
```

## `../cmd/out`

Output of commands sent to the device (e.g. `login`, `set`, etc.) is published to the `cmd/out` topic.

```
owntracks/gw/GWCAR-jp/cmd login xxx
owntracks/gw/GWCAR-jp/cmd/out NACK: incorrect login
```
