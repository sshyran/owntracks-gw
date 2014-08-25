#!/usr/bin/env python
# -*- coding: utf-8 -*-

# JPMens. 
# Publish retained messages for screenshots

import paho.mqtt.publish as mqtt
import json
import time


host = '172.16.153.110'
port = 1883

branch = 'tt/simu'

list = [
    # topic      tid     lat            lon             cog         vel
    ('mannheim', 'V7',  '49.508675',    '8.561470',     '340.94',  '98' ),
    ('choral',  'CH',   '44.424137',    '11.612695',    '110.94',   '8'),
    ('ey',      'EY',   '46.625819',    '8.023974',     '102.94',   '23'),
    ('annie',   '**',   '48.5738656',   '13.4',         '0',        '0'),
]

for p in list:
    data = {
        '_type'     : 'location',
        'tst'       : int(time.time()),
        'tid'       : p[1],
        'lat'       : p[2],
        'lon'       : p[3],
        'cog'       : p[4],
        'vel'       : p[5],
    }

    payload = json.dumps(data)

    topic = '%s/%s' % (branch, p[0])

    mqtt.single(topic, payload, qos=0, retain=True, hostname=host, port=port)
