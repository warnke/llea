#!/usr/bin/env python
## printing to stderr in python3 style
from __future__ import print_function
import sys

import threading, logging, time

from pykafka import KafkaClient
from pykafka import partitioners
import numpy as np

def eprint(*args, **kwargs):
    print(*args, file=sys.stderr, **kwargs)

class ModPartitioner(partitioners.BasePartitioner):
    """
    Returns int value of the key mod the number of partitions
    """
    def __call__(self, partitions, key):
        """
        :param partitions: The partitions from which to choose
        :type partitions: sequence of :class:`pykafka.base.BasePartition`
        :param key: Key used for routing
        :type key: int
        :returns: A partition
        :rtype: :class:`pykafka.base.BasePartition`
        """
        if key is None:
            raise ValueError(
                'key cannot be `None` when using int partitioner'
            )
        partitions = sorted(partitions)  # sorting is VERY important
        return partitions[abs(int(key)) % len(partitions)]

mod_partitioner = ModPartitioner()

class Producer(threading.Thread):
    daemon = True
  
    def run(self):
        client = KafkaClient("localhost:9092")
        topic = client.topics["pipeline"]
        producer = topic.get_producer(partitioner=mod_partitioner, linger_ms = 200)
  
        keyNum = 30
        key = list(np.arange(keyNum))
  
        idNum = 10000
        id = list(np.arange(idNum))
  
        ratepersecond = 5000
        burstFraction=0.1
        burstnum = ratepersecond*burstFraction
  
        ## Gaussian parameters for variation of message rate
        gmu = 0.0
        gsig = 0.1
  
        ## Lognormal parameters for delay of message delivery
        lmu = 2.5
        lsig = 1.0
  
        global totalSamples
  
        while True:
            ## emit data in bursts, compute number of messages in this burst
            ## from target rate
            currNum = int(round( burstnum*(1 + np.random.normal(gmu, gsig)) ))
            tt = time.time()
            totalSamples += currNum
            for sample in xrange(currNum):
                delay = np.random.lognormal(lmu, lsig)
                currKey = key[np.random.randint(keyNum)]
                currID = id[np.random.randint(idNum)]
                ## Emulate delayed message delivery by back dating event time
                eventTime = tt - delay
                outputStr = "%s;%s;%s" % (currKey, currID, eventTime)
                #print(outputStr)
                producer.produce(outputStr, partition_key=str(currKey))
                ## Every ten minutes, bump up rate for two minutes to trigger alert.
                ## Trigger if event time falls into the (key%5)-th 2 min interval of 10 min rolling cycle.
                ## To get rate increase by 1/n, trigger if `not np.random.randint(n)`.
                if ( not np.random.randint(3) and 120*(currKey%5) <= (eventTime % 600) < 120*(currKey%5 + 1) ):
                    outputStr = "%s;%s;%s" % (currKey, currID, eventTime + np.random.normal(0.0, 5.0))
                    producer.produce(outputStr, partition_key=str(currKey))
  
            timeSpent = time.time() - tt
            sleepTime = burstFraction - timeSpent
            if sleepTime < 0:
                print("Rate too high! Launch more producers with smaller rate each.")
            else:
                time.sleep(sleepTime)


startTime = time.time()
totalSamples = 0
Producer().start()
while True:
    time.sleep(3)
endTime = time.time()
eprint(totalSamples, " samples produced in ", endTime - startTime, " seconds.")
