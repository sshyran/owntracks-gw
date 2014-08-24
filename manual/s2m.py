#!/usr/bin/env python

import os
import sys
import yaml
import datetime
import codecs

f = codecs.open('../settings.yml', 'r', 'utf-8')
str = f.read()
doc = yaml.load(str)
f.close()

paragraphs = {}

for section in doc:
    for sec in section:
        for param in section[sec]:
            for setting in param.keys():
                if setting == 'block':
                    paragraphs[sec] = param[setting]

for section in doc:
    for sec in section:
        print ""
        print "###", sec
        print ""

        if sec in paragraphs:
            print paragraphs[sec]   # Section description (block)

        # print "|Setting|Default|Values|Validity|Meaning|"
        # print "|:------|:------|:-----|:-------|:------|"

        fmt = "| %-15s | %-14s | %-15s | %-12s | %-30s |"
        header = fmt % ('Setting', 'Default', 'Values', 'Validity', 'Meaning')

        print "+-----------------+----------------+-----------------+--------------+---------------------------+"
        print header
        print "+=================+================+=================+==============+===========================+"

        for param in section[sec]:
            for setting in param.keys():
                if setting != 'block':
                    desc    = param[setting].get('desc', "")
                    default = param[setting].get('default', "")
                    valid    = param[setting].get('validity', "")
                    values   = param[setting].get('values', "")

                    pp = "`%s`" % setting

                    print fmt % (pp, default, values, valid, desc)
                    print "+-----------------+----------------+-----------------+--------------+---------------------------+"

        print ""
        print "\\newpage"  # LaTeX
