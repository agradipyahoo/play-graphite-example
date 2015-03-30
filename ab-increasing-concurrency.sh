#!/bin/bash
COUNTER=1
while [  $COUNTER -lt 11 ]; do
	echo concurrency is $COUNTER
	ab -c $COUNTER -n 1000 https://radiant-chamber-5841.herokuapp.com/
	let COUNTER=COUNTER+1
done

COUNTER=9
while [  $COUNTER -gt 0 ]; do
	echo concurrency is $COUNTER
	ab -c $COUNTER -n 1000 https://radiant-chamber-5841.herokuapp.com/
	let COUNTER=COUNTER-1
done
