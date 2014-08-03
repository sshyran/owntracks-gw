# OTAP

Over the Air Provisioning is supported by the OwnTracks Edition, and it can be
triggered by an MQTT publish or via the console. The following prerequisites
must be met.

Configure the following either within the `.properties` file or on the console:

```
set otapURI=http://example.com/files/@/OwnTracks.jad
set notifyURI=http://example.com/otap.php?id=@
```

`otapURI` sets the base URI for the OTA upgrade. Any number of `@` characters
will be replaced by the device's `clientID`. (If the clientID contains an `@` character,
this conversion is not performed.) So, for a clientID of `dev1`, the
URI above will be expanded to

```
http://example.com/files/dev1/OwnTracks.jad
```

Internally, the following AT command is sent to the device to configure OTAP:

```
AT^SJOTAP=,<otapURI>,a:/app,jog,secret,gprs,<apn>,,,8.8.8.8,<notifyURI>
```

The actual upgrade is launched with an `$upgrade` command which may also be submitted via MQTT to the device.

Upon receiving the `$upgrade`, the device issues `AT^SJOTAP` to perform the actual HTTP GET request for the _jad_ file. This text file contains the URI to the _jar_ file:

```
...
MIDlet-Jar-URL: http://example.com/files/OwnTracks.jar
```

Only if the device can access the URL for the _jar_ will it actually perform a software upgrade. Of course this _jad_ could be generated on-the-fly with a pointer to the location of the _jar_.

As soon as the Greenwich has performed the upgrade, it will send an HTTP POST to the specified _NotifyURL_. This contains something like:

```
900 Success
```

(other codes currently unknown to us).
