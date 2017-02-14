#!/usr/bin/env python
## printing to stderr in python3 style
from __future__ import print_function
import redis
import sys

import logging, time
#import numpy as np

def eprint(*args, **kwargs):
    print(*args, file=sys.stderr, **kwargs)

redis_host = '52.25.10.79'
db_client = redis.StrictRedis(host=redis_host, password=None)

count = 0
while True:
    db_client.set("currtime-{}".format(count), '{:.2f}'.format(time.time()))
    print("Connect to redis on {}. SET: ".format(redis_host), "currtime-{}".format(count), '{:.2f}'.format(time.time()))

    count = count + 1 if count < 2 else 0
    time.sleep(1)
