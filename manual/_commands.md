+-----------------+-------------------+----------+---------------------------+
| Command         | Params            | Auth     | Meaning                        |
+=================+===================+==========+===========================+
| `login`         | _secret_          | Y        | A successful login expires after `loginTimeout` seconds, and _secret_ is checked from settings, initially provisioned via a properties file |
|                 |                   |          |                           |
+-----------------+-------------------+----------+---------------------------+
| `logout`        |                   | N        | Logout from a logged-in session. |
|                 |                   |          |                           |
+-----------------+-------------------+----------+---------------------------+
| `set`           | [_key_[=[_value_]]] | Y        | Set a value ...                |
|                 |                   |          |                           |
+-----------------+-------------------+----------+---------------------------+
| `state`         |                   | N        | Shows information.             |
|                 |                   |          |                           |
+-----------------+-------------------+----------+---------------------------+
| `reconnect`     |                   | Y        | Disconnects from and re-connects to the MQTT broker |
|                 |                   |          |                           |
+-----------------+-------------------+----------+---------------------------+
| `device`        |                   | Y        | Shows device software and hardware characteristics |
|                 |                   |          |                           |
+-----------------+-------------------+----------+---------------------------+
| `log`           | _type_            | Y        | Shows or resets log files in persistent memory. `log` shows the current log file, `log old` the previous, and `log delete` resets the log file. |
|                 |                   |          |                           |
+-----------------+-------------------+----------+---------------------------+
| `destroy`       |                   | Y        | Switches back to non-Java AT-mode. (Used only during provisioning.) |
|                 |                   |          |                           |
+-----------------+-------------------+----------+---------------------------+
| `upgrade`       |                   | Y        | Performs an [OTA](#otap) upgrade. |
|                 |                   |          |                           |
+-----------------+-------------------+----------+---------------------------+
| `exec`          | _at-command_      | Y        | Passes a command to the modem. (Used only during development.) |
|                 |                   |          |                           |
+-----------------+-------------------+----------+---------------------------+
| `out`           | _switch_          | Y        | Swtich [GPIO](#gpio) outputs.  |
|                 |                   |          |                           |
+-----------------+-------------------+----------+---------------------------+
| `False`         | _minutes_         | Y        | Suspends location publishes for _minutes_ or re-enables if _minutes_ is `0`. |
|                 |                   |          |                           |
+-----------------+-------------------+----------+---------------------------+
| `gps`           |                   | N        | Requests location update which is also published over MQTT with trigger `m`. |
|                 |                   |          |                           |
+-----------------+-------------------+----------+---------------------------+
| `zero`          |                   | Y        | Resets trip counter to 0.      |
|                 |                   |          |                           |
+-----------------+-------------------+----------+---------------------------+

\newpage
