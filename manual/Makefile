TARGET=gw-owntracks

SOURCES= header.yaml \
	 gw-owntracks.md \
	 apps.md \
	 infra.md \
	 mqtt.md \
	 installing.md \
	 settings-intro.md \
	 _settings.md \
	 GPIO.md \
	 OTAP.md \
	 notes.md \
	 credits.md \

$(TARGET).pdf: Makefile $(SOURCES)
	pandoc -s -V geometry:margin=1in --variable mainfont="Georgia" --variable sansfont="Arial" --variable monofont="Courier" --variable fontsize=11pt --variable version=1.10 $(SOURCES) -o $(TARGET).pdf

$(TARGET).html: Makefile $(SOURCES)
	pandoc -s -V geometry:margin=1in --variable mainfont="Georgia" --variable sansfont="Arial" --variable monofont="Courier" --variable fontsize=11pt --variable version=1.10 $(SOURCES) -o $(TARGET).html

_settings.md: ../settings.yml
	./s2m.py > _settings.md


# convert -units PixelsPerInch in.jpg  -density 900 out.jpg
