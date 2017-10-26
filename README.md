# income-tax-view-change-frontend

[![Build Status](https://travis-ci.org/hmrc/income-tax-view-change-frontend.svg)](https://travis-ci.org/hmrc/income-tax-view-change-frontend) [ ![Download](https://api.bintray.com/packages/hmrc/releases/income-tax-view-change-frontend/images/download.svg) ](https://bintray.com/hmrc/releases/income-tax-view-change-frontend/_latestVersion)


This is the repository for the Income Tax View and Change frontend.

Backend: https://github.com/hmrc/income-tax-view-change

Stub: https://github.tools.tax.service.gov.uk/hmrc/itvc-dynamic-stub


## APIs

### **GET** /self-assessment/ni/:nino/self-employments

Where **:nino** is a valid NINO in format XX999999X, for example: ```QQ123456C```

#### Success Response

**HTTP Status**: 200

**Example HTTP Response Body**:
```
{
    [
      {
        "id": "12334567890",
        "accountingPeriod": {
          "start": "2017-04-06",
          "end": "2018-04-05"
        },
        "accountingType": "CASH",
        "commencementDate": "2016-01-01",
        "cessationDate": "2017-12-31",
        "tradingName": "Acme Ltd.",
        "businessDescription": "Accountancy services",
        "businessAddressLineOne": "1 Acme Rd.",
        "businessAddressLineTwo": "London",
        "businessAddressLineThree": "Greater London",
        "businessAddressLineFour": "United Kingdom",
        "businessPostcode": "A9 9AA"
      }
    ]
}
```
Where:
* **id** is an identifier for the self-employment business, unique to the customer
* **start**, **end**, **commencementDate** and **cessationDate** are dates in the format ```YYYY-MM-DD```
* **accountingType** is either ```CASH``` or ```ACCRUAL```
* **tradingName** is the Business trading name
* **businessDescription** is a description that conforms to the SIC 2007 standard trade classifications
* **businessAddressLineOne**, **Two**, **Three** and **Four** are the lines of the Business address
* **businessPostcode** is the Business postcode. It must conform to regex: ```^[A-Z]{1,2}[0-9][0-9A-Z]?\s?[0-9][A-Z]{2}|BFPO\s?[0-9]{1,10}$```

#### Error Responses

##### INVALID NINO
* **Status**: 400
* **Code**: NINO_INVALID

##### Invalid POST/PUT by unauthorized agent
* **Status**: 400
* **Code**: INVALID_REQUEST

##### Agent not subscribed to Agent Services
* **Status**: 403
* **Code**: AGENT_NOT_SUBSCRIBED

##### The agent is not authorised to perform this action
* **Status**: 403
* **Code**: AGENT_NOT_AUTHORIZED

##### The client is not subscribed to MTD
* **Status**: 403
* **Code**: CLIENT_NOT_SUBSCRIBED


Requirements
------------

This service is written in [Scala](http://www.scala-lang.org/) and [Play](http://playframework.com/), so needs at least a [JRE] to run.


## Run the application


To update from Nexus and start all services from the RELEASE version instead of snapshot

```
sm --start ITVC_ALL -f
```


### To run the application locally execute the following:

Kill the service ```sm --stop INCOME_TAX_VIEW_CHANGE``` and run:
```
sbt 'run 9081'
```



## Test the application

To test the application execute

```
sbt clean scalastyle coverage test it:test coverageOff coverageReport
```


### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")

