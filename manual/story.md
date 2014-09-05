## A bit of background {#background}

Many tracking devices use either a "naked" GPRS connection to a fixed address on
the Internet or only SMS for communication. We chose MQTT, a TCP/IP-based protocol
for the _Internet of Things_, to power communications between Greenwich devices on
vehicles and your MQTT server, because of its flexibility as a _publish/subscribe_
system, the low protocol overhead it requires, and its resilience to bad networks.
(See the [section on MQTT](#mqttintro).) (Even so, you can remotely configure and
query a Greenwich via SMS if you need to.)

A very important aspect of Greenwich OwnTracks is our use of secure TLS
(formerly SSL) for ensuring encrypted connections between the devices and your
private infrastructure.

Greenwich devices thus utilize MQTT for communications with your infrastructure.
This communication is bi-directional: Greenwich devices talk to your server, and
your server can talk to Greenwich devices, for example to request an upgrade, set
specific parameters, turn on a [GPIO](#gpio) output, etc.

Server-side, i.e. within your premises, you can quickly and easily add _subscribers_
to the data published by Greenwich devices. Let's assume you want an application which
will check for alarms published by the device as soon as it is moved even though the
ignition is off. A simple program can _subscribe_ to the stream of messages arriving
at your MQTT server (called a _broker_) and react to those, e.g. by sending notifications
to a smartphone. (See the [Applications](#applications) and [MQTT payloads](#json) sections.)

Greenwich OwnTracks Edition has support for [Over The Air](#otap) upgrades and
is [highly configurable](#allsettings) with sensible defaults for most
settings. Nevertheless, there are settings you must configure, such as the
_hostname_ and _port_ of your MQTT broker and authentication data. 

Connecting a Greenwich to your vehicle is easy. The device requires power, which can be
taken from the vehicle's battery (or via a cigarette lighter), and it requires
a GPS and GSM antenna, both of which are available as a combination-antenna. If the device
is connected to the vehicle's _ignition_, then it can shutdown cleanly and report
vehicle movement (e.g. theft) even when in this low-power consumption mode.



\newpage
