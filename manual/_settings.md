
### GPRS

These settings are related to the modem and GPRS, and include settings for the modem to be able to connect via the SIM card operator to the Internet.

+-----------------+----------------+-----------------+--------------+---------------------------+
| Setting         | Default        | Values          | Validity     | Meaning                        |
+=================+================+=================+==============+===========================+
| `pin`           |                | 4 digits        | reboot       | SIM PIN. If it is not set, no `AT CPIN` command will be issued to the modem. |
+-----------------+----------------+-----------------+--------------+---------------------------+
| `apn`           | internet       | string          | reboot       | GPRS access point name         |
+-----------------+----------------+-----------------+--------------+---------------------------+
| `apnUser`       |                | string          | reboot       | GPRS access point user-id, required by some providers |
+-----------------+----------------+-----------------+--------------+---------------------------+
| `apnPassword`   |                | string          | reboot       | GPRS access point password (for `apnUser`), required by some providers |
+-----------------+----------------+-----------------+--------------+---------------------------+
| `gprsTimeout`   | 600            | 1...            | next loop    | time in seconds after which the system no longer expects GPRS |
+-----------------+----------------+-----------------+--------------+---------------------------+

\newpage

### Device

The device-related settings lay down how the device operates in general. Particular attention should be paid to the _OTAP_ settings if OTAP is required.

+-----------------+----------------+-----------------+--------------+---------------------------+
| Setting         | Default        | Values          | Validity     | Meaning                        |
+=================+================+=================+==============+===========================+
| `sleep`         | 21600          | 0..             | reboot       | sleep <sleep> seconds after device off before device is woken up by clock (21600s ~ 6 hours) |
+-----------------+----------------+-----------------+--------------+---------------------------+
| `motion`        | 4              | 0,1..255        | reboot       | Sensitivity for motion sensor. 0=off, 1=highest sensitivity |
+-----------------+----------------+-----------------+--------------+---------------------------+
| `otapUser`      |                | string          | upgrade      | HTTP basic auth username for OTAP |
+-----------------+----------------+-----------------+--------------+---------------------------+
| `otapPassword`  |                | string          | upgrade      | HTTP basic auth password for OTAP, related to `otapUser`. |
+-----------------+----------------+-----------------+--------------+---------------------------+
| `otapURI`       |                | string          | upgrade      | URL to `OwnTracks.jad` for OTAP. |
+-----------------+----------------+-----------------+--------------+---------------------------+
| `notifyURI`     |                | string          | upgrade      | optional URL to POST result of an OTAP upgrade. If this is not set, no notification will be posted |
+-----------------+----------------+-----------------+--------------+---------------------------+
| `lowBattery`    | 3599           | integer         | immediate    | Threshold for low battery in mV (e.g. battery voltage below 3.599V will switch off the device) |
+-----------------+----------------+-----------------+--------------+---------------------------+
| `dExtVoltage`   | 500            | integer         | immediate    | controls the external voltage monitoring. Reports when voltage varies by more than the value given as mV |
+-----------------+----------------+-----------------+--------------+---------------------------+
| `dBattVoltage`  | 100            | integer         | immediate    | controls the battery voltage monitoring. Reports when voltage varies by more than the value given as mV |
+-----------------+----------------+-----------------+--------------+---------------------------+
| `battery`       | 0              | 0/1             | reboot       | if 0, device will shutdown when external power is disconnectd. if 1, device will keep running until lowBattery is detected |
+-----------------+----------------+-----------------+--------------+---------------------------+

\newpage

### MQTT

This section documents the MQTT-related parameters. Of particular importance are `host` and `port`, where the former also contains a _scheme_. For securely connecting to a TLSv1 broker, make sure to specify the `ssl://` scheme in the `host` setting.

MQTT topics are used as follows: `publish` specifies a top-level topic branch to which the device publishes. The configured `clientID` (which defaults to the device's IMEI) is appended to that. The device subscribes to the `subscription` topic (default's to `publish/clientID/cmd`) for commands.

+-----------------+----------------+-----------------+--------------+---------------------------+
| Setting         | Default        | Values          | Validity     | Meaning                        |
+=================+================+=================+==============+===========================+
| `cleanSession`  | 1              | 0/1             | reboot       | Whether or not to set the MQTT _cleanSession_ flag. |
+-----------------+----------------+-----------------+--------------+---------------------------+
| `clientID`      | IMEI           | string          | reboot       | Used in MQTT connect and appended to `publish` topic. |
+-----------------+----------------+-----------------+--------------+---------------------------+
| `fields`        |                |                 | immediate    | Comma separated list of optional fields in published message. May include any of `course`, `speed`, `altitude`, `distance`, `battery`, `trip` |
+-----------------+----------------+-----------------+--------------+---------------------------+
| `host`          |                | string          | reboot       | Scheme and hostname or IP address to connect to. Valid schemes are `tcp:` and `ssl:` |
+-----------------+----------------+-----------------+--------------+---------------------------+
| `keepAlive`     | 60             | 1..             | reboot       | MQTT keepalive in seconds      |
+-----------------+----------------+-----------------+--------------+---------------------------+
| `mqttTimeout`   | 600            | 1-..            | next loop    | time in seconds after which device no longer expects establishing an MQTT connection. System will reboot. |
+-----------------+----------------+-----------------+--------------+---------------------------+
| `port`          | 1883           | 1-..            | reboot       | TCP/UDP port to connect to. When using `ssl://` in `host`, this should probably be changed to, say, 8883. |
+-----------------+----------------+-----------------+--------------+---------------------------+
| `qos`           | 1              | 0-2             | reboot       | Quality of Service (QoS) for MQTT publishes |
+-----------------+----------------+-----------------+--------------+---------------------------+
| `raw`           | 1              | 0/1             | immediate    | Publish raw GPS string to the `../raw` topic |
+-----------------+----------------+-----------------+--------------+---------------------------+
| `retained`      | 1              | 0/1             | reboot       | Publish locations with a retained flag |
+-----------------+----------------+-----------------+--------------+---------------------------+
| `user`          |                |                 | reboot       | Username for MQTT connect. Default means no authentication |
+-----------------+----------------+-----------------+--------------+---------------------------+
| `password`      |                |                 | reboot       | Password for MQTT connect.     |
+-----------------+----------------+-----------------+--------------+---------------------------+
| `publish`       | owntracks/gw   |                 | reboot       | Base topic for MQTT publishes  |
+-----------------+----------------+-----------------+--------------+---------------------------+
| `subscription`  | ../cmd         |                 | reboot       | Device listens for commands at this topic |
+-----------------+----------------+-----------------+--------------+---------------------------+
| `subscriptionQos` | 1              | 0..2            | reboot       | QoS for `subscription`         |
+-----------------+----------------+-----------------+--------------+---------------------------+
| `tid`           | <xx>           | alnum           | next pub     | Short tracker-ID, typically two letters, which defaults to last two characters of `clientID` |
+-----------------+----------------+-----------------+--------------+---------------------------+
| `will`          | 0              |                 | reboot       | The message published as LwT   |
+-----------------+----------------+-----------------+--------------+---------------------------+
| `willQos`       | 1              | 0..2            | reboot       | QoS for `willTopic` publish    |
+-----------------+----------------+-----------------+--------------+---------------------------+
| `willRetain`    | 1              | 0/1             | reboot       | Wether or not to retain the message published to `willTopic` |
+-----------------+----------------+-----------------+--------------+---------------------------+
| `willTopic`     | see above      |                 | reboot       | Topic for MQTT Last Will and Testament |
+-----------------+----------------+-----------------+--------------+---------------------------+

\newpage

### LocationManager

The LocationManager is responsible for determining when to publish
a location. These settings can be used to alter its behaviour and
they influence _how often_ data is published.

`minSpeed` should probably be set to about 5 km/h when in a vehicle; it
means whenever the vehicle slows down to below this value it transmits
a `k` message (park) and transmits and enters _drive mode_ when it
accelerates over 5 km/h.

The two parameters influencing the precision/data volume from a user point of view are
`minInterval` and `maxInterval`. The following scenarios show examples:

* For low data volume (few publishes): `maxInterval=300` (5min),
  `minInterval=18000` (5h).  While driving, report every 5 minutes
  when actually moving and everytime the car stops.  Autobahn 5min at
  6-20 km, small roads 6 km, towns probably only when stopped. While
  parked, typically overnight, will give 2 positions before started
  again next morning.  Useful when general information is necessary.

* medium publishes: `maxInterval=60`, `minInterval=9000` (2,5h)
  Reports on Autobahn (at 80-240kmh) every 1.3-4km, on small roads
  (at 75kmh) every 1,2 km, in town (30 kmh) every 500 m. This
  setting would allow to actually follow a vehicle whithout missing
  significant crossroads or landmarks.

* frequent publishes (high precision): `maxInterval=5`, `minInterval=3600` (1h)
  While driving, report every 5 seconds when actually moving and everytime the car stops.
  Autobahn every 100 m - 300 m, small roads every 80m, towns every
  40m. This would allow to follow the vehicle as it moves on screen.
  It would allow for speed monitoring.

Settings for pedestrians, runners, bicycle riders, race cars would be different.
      

+-----------------+----------------+-----------------+--------------+---------------------------+
| Setting         | Default        | Values          | Validity     | Meaning                        |
+=================+================+=================+==============+===========================+
| `sensitivity`   | 1              | 0-...           | immediate    | Threshold in meters to exclude movement artifacts from trip calculation |
+-----------------+----------------+-----------------+--------------+---------------------------+
| `minSpeed`      | 5              | 0-...           | immediate    | Mininum speed in Km/h to switch to _move_ mode |
+-----------------+----------------+-----------------+--------------+---------------------------+
| `minDistance`   | 100            | 0-...           | immediate    | Distance since last publish in meters to switch to _move_ mode |
+-----------------+----------------+-----------------+--------------+---------------------------+
| `maxInterval`   | 60             | 0-...           | immediate    | Maximum time in seconds before next publish in _move_ mode |
+-----------------+----------------+-----------------+--------------+---------------------------+
| `minInterval`   | 3600           | 0-...           | immediate    | Mininum time in seconds before publish in _stationary_ (park) mode |
+-----------------+----------------+-----------------+--------------+---------------------------+
| `fixTimeout`    | 600            | 1-...           | immediate    | Time in seconds after which the device no longer expects a GPS fix. Device will reboot. |
+-----------------+----------------+-----------------+--------------+---------------------------+

\newpage

### Debugging and Logging

The app provides debug and logging information prioritised and categorised by component.

The used priorities are specified by a character (similar to syslog) with "P" for emergency beeing the highest priority:

* `P` Emergency (Panic)
* `A` Alert
* `C` Critical
* `E` Error
* `W` Warning
* `N` Notice
* `I` Informational
* `D` Debug

Debug and logging output can be written to the standard error stream
(typically a connected terminal), a file on the device (`log.txt`), and it can be
published via MQTT to the `../log` topic.

The level of output is specified by the settings

* `stderrLogLevel`
* `fileLogLevel`
* `topicLogLevel`

The specified character assigned to each of the output media limits the
output to priorities higher or equal to the priority given. Examples:

`stderrLogLevel=D`, all logging levels and debug is written to standard error.
`topicLogLevel=C`, only critical, alert and emergency log messages are sent via MQTT.
`fileLogLevel=E`, error, critical, alert, and emergency log messages are written to the local file. Warning, notice, informational anddebug messages are ignored

To limit the amount of debug messages, the set of components for which the messages are written can be controlled by the `dbgComp` parameter. If set to `"all"`, all debug messages are shown. When `"none"` is specified, no debug messages will be shown. `"none" is the default value. 

Otherwise a comma separated list of components allows granular control of the output (e.g. `"AppMain,LocationManager"`). Currently the following components exist:

* AppMain
* ATManager
    * BatteryManager
    * CommASCThread
    * CommGPSThread
* CommandProcessor
    * DateFormatter
    * GPIO6WatchDogTask
    * GPIOInputManager
* LocationManager
* MQTTHandler
    * MicroManager
    * ProcessSMSThread
    * Settings
* SocketGPRSThread
* SSLSocketFactory
* UserwareWatchDogTask

+-----------------+----------------+-----------------+--------------+---------------------------+
| Setting         | Default        | Values          | Validity     | Meaning                        |
+=================+================+=================+==============+===========================+
| `dbgComp`       | none           | list            | immediate    | comma-separated list of components to debug; controls the debug output e.g. `"AppMain,LocationManager"` |
+-----------------+----------------+-----------------+--------------+---------------------------+
| `stderrLogLevel` | D              | string          | immediate    | controls the logging to the device's standard error device (terminal) |
+-----------------+----------------+-----------------+--------------+---------------------------+
| `fileLogLevel`  | E              | string          | immediate    | controls the logging into the device's log file |
+-----------------+----------------+-----------------+--------------+---------------------------+
| `topicLogLevel` | E              | string          | immediate    | controls the logging to the MQTT broker on `../log/<priority>/<log-message>` |
+-----------------+----------------+-----------------+--------------+---------------------------+
| `usbDebug`      | 0              | 0/1             | immediate    | if set to 1, debug output flows to USB instead of ASC0 via standard error |
+-----------------+----------------+-----------------+--------------+---------------------------+

\newpage

### Miscellaneous


+-----------------+----------------+-----------------+--------------+---------------------------+
| Setting         | Default        | Values          | Validity     | Meaning                        |
+=================+================+=================+==============+===========================+
| `secret`        | 1234567890     | digits          | immediate    | The secret required to issue a `login` command to the device |
+-----------------+----------------+-----------------+--------------+---------------------------+
| `loginTimeout`  | 30             | 0-...           | immediate    | Successful logins expire after this number of seconds |
+-----------------+----------------+-----------------+--------------+---------------------------+

\newpage
