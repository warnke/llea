# -*- coding: utf-8 -*-
"""
    jQuery-Flask
    ~~~~~~~~~~~~~~~~~~~
    Resolves jQuery request and returns three arrays
    1) cumulative sum
    2) part of the cumulative data that has stabilized (converged)
    3) emergent rates that are determined from mixing in the expected rate.

    Edit line containing "xx" below to connect to Redis server.
"""
from app import app
from flask import Flask, jsonify, render_template, request, redirect, send_file
from redis import StrictRedis
import time, math

def weight(dt):
    """Returns the lognormal distribution function for dt as weight"""
    ## scale time to account for latency if necessary
    scaledTime = dt - 10.0
    if(scaledTime < 0):
        ## return small positive float, as math.erf fails at zero
        scaledTime = 0.01
    lmu = 2.5
    lsig = 1.0
    invsqrt2 = 1.0/math.sqrt(2.0)
    return 0.5*( 1 + math.erf( invsqrt2*(math.log(scaledTime) - lmu)/lsig ) )

@app.route('/_timeseries')
def timeseries():
    """Retrieve time series for currKey"""

    ## Time interval for measurement
    interval = 30
    ## Expectation for total count
    expectation = 5000.0

    ## Get parameter from javascript
    currKey = request.args.get('currKey', 0, type=int)

    ## Open Redis connection
                            
    #red = StrictRedis(host='ec2-52-26-112-23.us-west-2.compute.amazonaws.com', password=None) # SPARK
    red = StrictRedis(host='ec2-52-25-10-79.us-west-2.compute.amazonaws.com', password=None) # FLINK

    ## Compute average time at worker node when issuing messages from DB.
    dbTime = 0.0
    for ii in xrange(3):
      dbTime += float(red.get("currtime-" + str(ii)))
    dbTime /= 3.0
    #print("dbTime = {}".format(dbTime))

    ## prospective value to get from DB
    nvalue = None
    shiftInterval = 0
    ## round off to obtain interval
    nowinterval = int(dbTime)/interval*interval

    ## We don't know if current time interval in already in DB,
    ## so search for most recent interval.
    while (nvalue is None) and (shiftInterval < 100):
      currInterval = nowinterval - shiftInterval*interval
      key = "%s;%s" % (currKey, currInterval)
      # debugging
      #print("Attempting to get value of key {} from DB...".format(key))
      nvalue = red.get(key)
      #print("Value is {}".format(nvalue))
      # debugging
      
      shiftInterval += 1

    ## Get most recent points from DB and build result arrays.
    numPts = 20
    emergent = []
    cumulative = []
    stable = []
    for ii in xrange(1, numPts + 1):
      pastInterval = currInterval - (numPts - ii)*interval
      key = "%s;%s" % (currKey, pastInterval)
      nvalue = red.get(key)
      value = int(nvalue) if (not nvalue is None) else 0
      cumulative.append([pastInterval, value])
      if (ii < numPts - 4):
        stable.append([pastInterval, value])
      dt = dbTime - pastInterval
      emergent.append([
        pastInterval, value if (ii < numPts - 4) else
        value + (1.0 - weight(dt))*expectation
      ])
    return jsonify(cumulative = cumulative, emergent = emergent, stable = stable)

# returns slide deck as redirect for easy access
@app.route('/slides')
def slides():
    #return redirect("http://www.google.com")
    return redirect("https://docs.google.com/presentation/d/1up2ctpKWbFasaVBXw7OxhSEFBIpBT-ZM_CoFUewpWBI/edit?usp=sharing")

#@app.route('/team')
#def team():
#    #if request.args.get('type') == '1':
#    #   filename = 'data.png'
#    #else:
#    #   filename = 'data.png'
#    #return send_file(filename, mimetype='image/png')
#    return send_file('/home/ubuntu/Temp/john_kamps.JPG', mimetype='image/jpeg')

@app.route('/')
@app.route('/index.html')
def index():
    return render_template('index.html')
