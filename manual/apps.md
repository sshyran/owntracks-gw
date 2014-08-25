# OwnTracks Apps

Greenwich OwnTracks integrates with the OwnTracks iOS and Android apps because the
MQTT payloads it publishes are compatible with the JSON payloads understood by the apps.
This in turn means, that you can use one of the apps to view the location of any number
of Greenwich devices which use OwnTracks edition software.

![OwnTracks for iPhone showing vehicle positions & movement](art/iphone-4people.png)

In addition to the smartphone apps, we also provide a Web-based map which,
like the apps, updates in real-time. This is made possible by utilizing
Websockets which "talk" to a Websocket-enabled MQTT broker.

\newpage

_Live-Table_ is a Websockets-based Web application which updates in real-time
as soon as an MQTT publish from one of the devices is received at the broker.
Aside from showing device status, it also displays the device's location so
you can see where your vehicles are at at a glance.

![OwnTracks Live-Table](art/livetable.png)

If you don't have access to one of the OwnTracks apps, you can query a particular
vehicle's location by sending it an SMS; the vehicle responds with information
on its location via SMS.

![OwnTracks queried via SMS](art/sms.png)

OwnTracks' _m2s_ subscribes directly to your MQTT broker and stores all
data it receives in a back-end database (e.g. MySQL or PostgreSQL) from
which you can, at any time, reconstruct where your vehicles have been.
_m2s_ stores data for historical purposes, and you can subsequently query
that data and process it. In addition to individual columns that are 
populated, you also have access to the raw JSON that was published by your
devices. Important to remember: it is _your_ data and you own it!

```
mysql> SELECT id, tst, lat, lon FROM location WHERE device = 'van17';
+--------+---------------------+-----------+----------+
| id     | tst                 | lat       | lon      |
+--------+---------------------+-----------+----------+
| 186477 | 2014-08-25 08:06:10 | 46.607067 | 7.90527  |
| 186478 | 2014-08-25 08:07:11 | 46.602936 | 7.906514 |
| 186480 | 2014-08-25 08:08:12 | 46.59976  | 7.907099 |
```

```
SELECT json FROM location WHERE id = 186480;
{"username": "acme", "_type": "location", "dist": "355", "topic": "owntracks/acme/van17",
  "device": "van17", "lat": "46.59976", "alt": "794.9", "lon": "7.907099", "t": "t",
  "vel": "17.338424", "cog": "144.18", "tst": "1408954092"}
```

Using _gpxexporter_, which is part of _m2s_, create GPX tracks which can be
viewed in standard GPX viewers such as with
[gpsvisualizer.com](http://www.gpsvisualizer.com) or
with Google Earth.

![OwnTracks GPX export](art/ey-belgium.png)

\newpage
