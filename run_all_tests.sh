#!/usr/bin/env bash

sbt clean compile scalastyle coverage Test/test it/test coverageOff coverageReport