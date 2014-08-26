# GPIO {#gpio}

Greenwich devices have up to five user-programmable [GPIO](http://en.wikipedia.org/wiki/General-purpose_input/output) pins, depending on the model, but at least three inputs and two outputs (open collectors) are available on the entry-level "Java" version:

This is the 14pin Molex connector seen from the back (i.e. plugged into `S1` on the Greenwich)

![Molex connector](art/molex.png)

* `GPIO1`, Pin S1-5, light-blue cable, topic name: `/gpio/1`
* `GPIO3`, Pin S1-12, grey cable, topic name: `/gpio/3`
* `GPIO7`, Pin S1-11, green _ignition_ cable (normally connected to fuse), topic name: `/gpio/7`

## Inputs

Each input (GPIO `1`, `3`, `7`) has an allowed voltage range of +- VIN (9-32V) with LOW being recognized at `<0.5V` and HIGH at `>3.0V`.

The OwnTracks-Edition of the software surfaces the state (HIGH/LOW) of these pins into MQTT publishes by publishing a payload of `0` or `1`, respectively, depending on the pin's state. One publish occurs per pin as soon as its state changes. Note, that the internal state is inverted compared to the external level; this is supposed to be typical for embedded devices. In other words, the published payload is `0` for HIGH and `1` for LOW.

The topic used for publishing is the general topic of the device with `/gpio/<pin>` appended to it:

```
owntracks/acme/van17/gpio/1 1
owntracks/acme/van17/gpio/3 1
owntracks/acme/van17/gpio/7 0
```

![Arduino](art/arduino.png)

In order to toggle these pins with, say, an Arduino, you can use a small sketch on a basic Arduino board. The following is a variation of the _blink_ sketch which flips PIN 10 connected to GPIO1 on the Greenwich every 5 seconds, and blinks LED13 on the Arduino accordingly.

```
#define LED        13
#define GREENWICH  10

void setup() {
    pinMode(LED, OUTPUT);
    pinMode(GREENWICH, OUTPUT);
}

void loop() {
    digitalWrite(LED, HIGH);
    digitalWrite(GREENWICH, HIGH);
    delay(5000);
    digitalWrite(LED, LOW);
    digitalWrite(GREENWICH, LOW);
    delay(5000);
}
```

## Outputs

The two outputs (open collectors) may draw a maximum current of 300mA (anything above that
may destroy your equipment!).

* `GPIO10`, Pin S1-13, brown (shown with white border), open collector, mapped onto command `out 1`
* `GPIO9`, Pin S1-6, green (shown with white border), open collector, mapped onto command `out 2`

To switch a GPIO output, send a command with a payload consisting of the verb (`out`)
followed by a space and the GPIO output number (`1` or `2`), followed by the desired
state (`0` or `1`). `1` means collector open, so load is switched on.

```
mosquitto_pub -t owntracks/acme/van17/cmd -m "out 1 0"
```

This would open pin S1-13 (brown).

\newpage
