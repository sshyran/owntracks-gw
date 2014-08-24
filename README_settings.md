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
* - destroy (switches back to non-Java AT-mode)
* + upgrade (perform an [OTA upgrade](OTAP.md)
* - exec _at-command_ (pass command to modem)
* + reconnect (disconnects and establishes new MQTT connection)

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

