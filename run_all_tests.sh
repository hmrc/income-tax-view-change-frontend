#!/usr/bin/env bash

sbt clean cleanFiles compile coverage Test/test it/test coverageOff coverageReport -mem 5000