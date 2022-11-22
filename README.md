# income-tax-view-change-frontend

[![Build Status](https://travis-ci.org/hmrc/income-tax-view-change-frontend.svg)](https://travis-ci.org/hmrc/income-tax-view-change-frontend) [ ![Download](https://api.bintray.com/packages/hmrc/releases/income-tax-view-change-frontend/images/download.svg) ](https://bintray.com/hmrc/releases/income-tax-view-change-frontend/_latestVersion)


This is the repository for the Income Tax View and Change frontend.

Backend: https://github.com/hmrc/income-tax-view-change

Stub: https://github.com/hmrc/income-tax-view-change-dynamic-stub

Requirements
------------

This service is written in [Scala](http://www.scala-lang.org/) and [Play](http://playframework.com/), so needs at least a [JRE] to run.


## Run the application


To start all Service Manager services from the latest RELEASE version instead of snapshot execute the following:

```
sm --start ITVC_ALL --appendArgs '{"CITIZEN_DETAILS":["-Dmongodb.cid-sautr-cache.enabled=false"]}' -r
```


### To run the application locally execute the following:

```
sbt 'run 9081'
```
### To run the application locally execute in test mode the following:

```
sbt "run 9081 -Dplay.http.router=testOnlyDoNotUseInAppConf.Routes"
```

## Test the application

To test the application execute:

```
sbt clean scalastyle coverage test it:test coverageOff coverageReport
```

## How to run sbt-scoverage plugin for the application

To generate scoverage report for the unit tests execute:

```
sbt clean coverage test coverageOff coverageReport
```

To generate scoverage report for the integration tests execute:

```
sbt clean coverage it:test coverageOff coverageReport
```

To generate aggregated scoverage report for the unit and integration tests in one go execute:

```
sbt clean coverage test it:test coverageOff coverageReport
```

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")