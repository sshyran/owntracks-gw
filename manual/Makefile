TARGET=gw-owntracks

VARS=-V linkcolor=blue

SOURCES= header.yaml \
	 gw-owntracks.md \
	 story.md \
	 apps.md \
	 infra.md \
	 mqtt.md \
	 installing.md \
	 settings-intro.md \
	 _commands.md \
	 commands-continued.md \
	 _settings.md \
	 GPIO.md \
	 OTAP.md \
	 notes.md \
	 credits.md \

$(TARGET).pdf: Makefile $(SOURCES)
	pandoc -s $(VARS) -V geometry:margin=1in --variable mainfont="Georgia" --variable sansfont="Arial" --variable monofont="Courier" --variable fontsize=11pt --variable version=1.10 $(SOURCES) -o $(TARGET).pdf

$(TARGET).html: Makefile $(SOURCES)
	pandoc -s $(VARS) -V geometry:margin=1in --variable mainfont="Georgia" --variable sansfont="Arial" --variable monofont="Courier" --variable fontsize=11pt --variable version=1.10 $(SOURCES) -o $(TARGET).html

_settings.md: ../settings.yml s2m.py
	./s2m.py > _settings.md
_commands.md: commands.yml c2m.py
	./c2m.py > _commands.md


# convert -units PixelsPerInch in.jpg  -density 900 out.jpg
