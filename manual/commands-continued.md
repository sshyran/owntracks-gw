
`gps` sends the current or - if temporarily not available - last known
location. If switched `off`, no new GPS messages are processed. In this case
`gps` will report back the last known location
before the `off` command.

The same applies to the `"t":"L"` last-known location publish sent when the Greenwich shuts
down intentionally.  If the Greenwich was (re-)started while in `off` mode,
there is no last known location. `"t":"L"` will not be sent and `gps` will
return "no location available".

When configuring the device via SMS or MQTT, the operator must authenticate to
the device using its configured `secret`. Only then will the device allow settings
to be viewed or altered. As an example, consider these MQTT publishes which "talk"
to the device, assuming it is already operational and is currently connected to
the same MQTT broker we're talking to.

```bash
mosquitto_pub -u $user -P $pass -t owntracks/acme/van17/cmd -m 'login 123456'
mosquitto_pub -u $user -P $pass -t owntracks/acme/van17/cmd -m 'state'
```

We recommend subscribing to the output of the commands, as the device will inform
you of errors, and in the case of commands which produce
output, will use that topic to publish output:

```bash
mosquitto_sub -u $user -P $pass -v -t owntracks/acme/van17/cmd/out
```

Sample output for `state` command
```
owntracks/acme/van17/cmd/out NUMSAT=10
owntracks/acme/van17/cmd/out BEARER=3
owntracks/acme/van17/cmd/out GPRS=1
owntracks/acme/van17/cmd/out CREG=5
owntracks/acme/van17/cmd/out CGREG=5
owntracks/acme/van17/cmd/out BATT=4.4
owntracks/acme/van17/cmd/out EXTV=12.2
owntracks/acme/van17/cmd/out Q=0
owntracks/acme/van17/cmd/out CONN=1
owntracks/acme/van17/cmd/out NETW=1
owntracks/acme/van17/cmd/out OPER="Provider"
owntracks/acme/van17/cmd/out WAKEUP=IgnitionWakeup
owntracks/acme/van17/cmd/out DATE=2014-08-28 15:02:40
```

Sample out for `device` command
```
uFW=02.18,02.01,02.16
SW=0.7.32
EG5=Cinterion,EGS5-X,REVISION 02.004
IMEI=123456789012345
```

Sample out for `gps` command
```
2014-08-28 15:02:51
Latitude xx.195715
Longitude x.688207
Altitude 57m
Speed 0kph
Course 0
Trip 0m

```


The following tables list all configurable settings for the device. Note that
most of these have rather sensible values and do not necessarily have to be
modified.
