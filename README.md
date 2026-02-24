# income-tax-view-change-frontend

[![Build Status](https://travis-ci.org/hmrc/income-tax-view-change-frontend.svg)](https://travis-ci.org/hmrc/income-tax-view-change-frontend) [ ![Download](https://api.bintray.com/packages/hmrc/releases/income-tax-view-change-frontend/images/download.svg) ](https://bintray.com/hmrc/releases/income-tax-view-change-frontend/_latestVersion)


This is the repository for the Income Tax View and Change frontend.

Backend: https://github.com/hmrc/income-tax-view-change

Stub: https://github.com/hmrc/income-tax-view-change-dynamic-stub

Feature switch info: https://confluence.tools.tax.service.gov.uk/pages/viewpage.action?spaceKey=MISUV&title=Feature+Switches+State

Requirements
------------

This service is written in [Scala](http://www.scala-lang.org/) and [Play](http://playframework.com/), so needs at least a [JRE] to run.

## Compiling the application
To simply compile application across all three code domains: code, unit tests and integration tests
run following command:

```
sbt compileAll
```

## Run the application


To start all Service Manager services from the latest RELEASE version instead of snapshot execute the following:

```
sm2 --start ITVC_TEST --appendArgs '{"CITIZEN_DETAILS":["-Dmongodb.cid-sautr-cache.enabled=false"]}'
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
sbt clean coverage test it/test coverageOff coverageReport
```

## How to run sbt-scoverage plugin for the application

To generate scoverage report for the unit tests execute:

```
sbt clean coverage test coverageOff coverageReport
```

To generate scoverage report for the integration tests execute:

```
sbt clean coverage it/test coverageOff coverageReport
```

To generate aggregated scoverage report for the unit and integration tests in one go execute:

```
sbt clean coverage test it/test coverageOff coverageReport
```

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")

### To access Opt Out Custom Data testOnly page to see current data, first log in as a whitelisted opt out user (e.g: OP000001A) and then go to:

Local:

Individual: http://localhost:9081/report-quarterly/income-and-expenses/view/test-only/showOptOutCurrentData

Agent: http://localhost:9081/report-quarterly/income-and-expenses/view/agents/test-only/showOptOutCurrentData


Staging:

Individual: https://www.staging.tax.service.gov.uk/report-quarterly/income-and-expenses/view/test-only/showOptOutCurrentData

Agent: https://www.staging.tax.service.gov.uk/report-quarterly/income-and-expenses/view/agents/test-only/showOptOutCurrentData


### To access income-tax-session-data testOnly page to test the income-tax-session-data service, first log in as an agent and then go to:

Local: http://localhost:9081/report-quarterly/income-and-expenses/view/agents/test-only/session-storage
Staging: https://www.staging.tax.service.gov.uk/report-quarterly/income-and-expenses/view/agents/test-only/session-storage

Functionality for getting this working with individuals will be added soon.