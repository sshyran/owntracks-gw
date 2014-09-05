# Notes

## SIM card

A SIM card with a data plan is required for Greenwich to communicate over the
Internet.  We've had good experience using affordable M2M data plans which
typically include roaming. For example in Europe, we're using M2M SIM cards
which are charged at EUR 0.20 per megabyte of data, and these cards can roam
through several European countries at that same rate.

## Data volume

How much data volume will the Greenwich actually produce? This depends on a
number of factors:

* _LocationManager_ settings which define how often location data will be
  published
* OTA updates (at approx 300kb per update)

Under the assumption that the device moves for a total of 8 hours per day,
6 of which it is actually activly moving, 5 days per week and 20 days
per month, we get a round 600 hours per month.

At a setting of `maxInterval=5`: `600 * 3600 / 5` results in 100.000 messages
at 350 bytes results in 35 MB.

However, with a setting of `maxInterval=300` we obtain `600 * 3600 / 300
` ~ 7.200 messages; at  350 Byte gives us 2.5 MB.


\newpage
