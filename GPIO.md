# GPIO

Greenwich devices have up to five user-programmable [GPIO](http://en.wikipedia.org/wiki/General-purpose_input/output) pins, depending on the model, but at least three are available on the entry-level "Java" version:

* Pin S1-5 (GPIO1)
* Pin S1-12 (GPIO3)
* Pin S1-11 (Ignition)

The OwnTracks-Edition of the software surfaces the state (HIGH/LOW) of these pins into MQTT publishes by publishing a payload of `1` or `0` depending on the pin's state. The topic used for publishing is the general topic of the device with `/gpio/<pin>` appended to it:

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
