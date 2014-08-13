# GPIO

Greenwich devices have up to five user-programmable [GPIO](http://en.wikipedia.org/wiki/General-purpose_input/output) pins, depending on the model, but at least three are available on the entry-level "Java" version:

* Pin S1-5 (GPIO1), light-blue cable
* Pin S1-12 (GPIO3), grey cable
* Pin S1-11 (Ignition), green cable (normally connected to fuse)

Each input has an allowed volate range von +- VIN (9-32V) with Low being recognized at `<0.5V` and High at `>3.0V`.

The OwnTracks-Edition of the software surfaces the state (HIGH/LOW) of these pins into MQTT publishes by publishing a payload of `0` or `1`, respectively, depending on the pin's state. Note, that the internal state is inverted compared to the external level; this is supposed to be typical for embedded devices. In other words, the published payload is `0` for HIGH and `1` for LOW.

The topic used for publishing is the general topic of the device with `/gpio/<pin>` appended to it:

```
owntracks/gw/GWCAR-jp/gpio/1 1
owntracks/gw/GWCAR-jp/gpio/3 1
owntracks/gw/GWCAR-jp/gpio/7 0
```

![Arduino](assets/arduino.png)

In order to toggle these pins with, say, an Arduino, you can use a this small sketch on a basic Arduino board. The following is a variation of the _blink_ sketch which flips PIN 10 connected to GPIO1 on the Greenwich every 5 seconds, and blinks LED13 on the Arduino accordingly.

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
