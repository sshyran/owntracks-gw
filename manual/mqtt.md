# MQTT payloads

Greenwich publishes messages containing _payloads_ to particular topics. The topic branch
is user-configurable by setting the `publish` and `clientID` settings. Assuming the former
is set to `owntracks/acme` and the latter to `van17`, the _base topic_ becomes `owntracks/acme/van17`, and that is where location messages (in JSON) are published to. (All publishes are typically
retained, unless you configure `retain=0`.)


A JSON message as published by Greenwich could look like this (the in-line comments are not valid JSON; we've added them here as explanation):

```json
{
  "_type": "location",         // type
  "t": "t",                    // trigger type (see table)
  "tst": "1408810440",         // UNIX epoch timestamp
  "tid": "V7",                 // tracker-ID (configurable)
  "lat": "48.858334",          // latitude
  "lon": "2.295134",           // longitude
  "alt": "38.0",               // altitude (*)
  "vel": "46.5315",            // velocity (speed) (*)
  "batt": "12.4",              // external battery level (*)
  "cog": "283.35",             // course over ground (*)
  "dist": "569",               // distance (*)
  "trip": "8441"               // trip covered in meters (*)
}
```
JSON elements marked with an asterisk `(*)` may or may not be present, depending on the
configured `fields`.

### Triggers

In the JSON above, we mention the word _trigger_. This describes why a particular
location publish was issued. The following is a list of triggers:

--------- ---------------------------------------------------------
 Trigger  Reason
--------- ---------------------------------------------------------
    f     First publish after reboot
    s     Device started by motions (s)ensor without ignition on - theft alarm. Send instead of f.
          No need for external power.
    a     Device started by (a)larm clock. Alive signal without ignition. Send instead of f.
          No need for external power.

    k     When transitioning from _move_ to _stationary_ mode an additional publish is sent marked with trigger `k` (park)

    L     Last recorded position upon graceful shutdown 

    l     GPS signal lost. Even though GPS signal has gone (e.g. driven into tunnel)
          we may still have a GPRS signal, so we can publish the `l`ast known position

    m     For manually requested locations (e.g. by publishing to `/cmd`)

    t     (time) for location published because device is movi

    T     (Time) Vehicle is immobile and `maxInterval` has elapsed.

    v     Move. One `v` trigger is sent on transition from park or `t` to move
--------- ---------------------------------------------------------

Table: Triggers emitted in location publishes



In addition to location messages as shown above, the Greenwich will also publish additional
messages as follows:

### Hardware information

Upon startup, the device publishes the hardware model of the chip and the IMEI. Example:

```
owntracks/acme/van17/hw Cinterion,EGS5-X,REVISION 02.004
owntracks/acme/van17/hw/imei 012345678901234
```

### Software versions

Upon startup, the device publishes the Greenwich version as well as the application version:

```
owntracks/acme/van17/sw/gw 02.16B,02.01,02.16
owntracks/acme/van17/sw/midlet 0.5.43
```

### Voltages

Built-in battery (`batt`) and external (`ext`) voltages are published to `../voltage/` when voltage changes "significantly", as configured with `dExtVoltage` and `dBattVoltage`:

```
owntracks/acme/van17/voltage/batt 4.4
owntracks/acme/van17/voltage/ext 12.2
```


### GPIO

The *inverted* status of the GPIO pins are published under the `gpio/` subtopic, as documented in the _GPIO_ section.

```
owntracks/acme/van17/gpio/1 0
owntracks/acme/van17/gpio/3 1
owntracks/acme/van17/gpio/7 0
```

### Command output

Output of commands sent to the device (e.g. `login`, `set`, etc.) is published to the `../cmd/out` topic.

```
owntracks/acme/van17/cmd login xxx
owntracks/acme/van17/cmd/out NACK: incorrect login
owntracks/acme/van17/cmd login 1234567890
owntracks/acme/van17/cmd/out login accepted
```

\newpage