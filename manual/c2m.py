#!/usr/bin/env python

import os
import sys
import yaml
import datetime
import codecs

f = codecs.open('commands.yml', 'r', 'utf-8')
str = f.read()
doc = yaml.load(str)
f.close()

fmt = "| %-15s | %-17s | %-8s | %-30s |"
header = fmt % ('Command', 'Params', 'Auth', 'Meaning')

#for command in doc:
#    for name in command:
#        print name, " = ", command[name].get('params')
#sys.exit(0)


print "+-----------------+-------------------+----------+---------------------------+"
print header
print "+=================+===================+==========+===========================+"

for command in doc:
    for name in command:
        # for setting in command[name].keys():
        params    = command[name].get('params', "")
        desc    = command[name].get('desc', "").rstrip()
        auth = command[name].get('auth', "")

        auth = "Y" if auth is True else "N"

        desc = desc.replace("\n", " ")

        pp = "`%s`" % name
                

        print fmt % (pp, params, auth, desc)
        print "|                 |                   |          |                           |"
        print "+-----------------+-------------------+----------+---------------------------+"

print ""

print "\\newpage"  # LaTeX
