#!/usr/bin/env bash

sbt clean compile coverage Test/test it/test coverageOff coverageReport
