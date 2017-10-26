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
```
Where:
* **id** is an identifier for the self-employment business, unique to the customer
* **start**, **end**, **commencementDate** and **cessationDate** are dates in the format ```YYYY-MM-DD```
* **accountingType** is either ```CASH``` or ```ACCRUAL```
* **tradingName** is the Business trading name
* **businessDescription** is a description that conforms to the SIC 2007 standard trade classifications
* **businessAddressLineOne**, **Two**, **Three** and **Four** are the lines of the Business address
* **businessPostcode** is the Business postcode. It must conform to regex: ```^[A-Z]{1,2}[0-9][0-9A-Z]?\s?[0-9][A-Z]{2}|BFPO\s?[0-9]{1,10}$```


### GET /self-assessment/ni/:nino/self-employments/:selfEmploymentId/obligations

Where:
* **:nino** is a valid NINO in format XX999999X, for example: ```QQ123456C```
* **:selfEmploymentId** is an identifier for the self-employment business, unique to the customer

#### Success Response

**HTTP Status**: 200

**Example HTTP Response Body**:
```
{
  "obligations": [
    {
      "start": "2017-04-06",
      "end": "2017-07-05",
      "due": "2017-08-05",
      "met": true
    },
    {
      "start": "2017-07-06",
      "end": "2017-10-05",
      "due": "2017-11-05",
      "met": true
    },
    {
      "start": "2017-10-06",
      "end": "2018-01-05",
      "due": "2018-02-05",
      "met": false
    },
    {
      "start": "2018-01-06",
      "end": "2018-04-05",
      "due": "2018-05-06",
      "met": false
    }
  ]
}
```
Where:
* **start**, **end** and **due** are dates in the format ```YYYY-MM-DD```
* **met** is ```true``` if the obligation period has been met and ```false``` otherwise


### GET /ni/:nino/calculations/:taxCalculationId

Where:
* **:nino** is a valid NINO in format XX999999X, for example: ```QQ123456C```
* **:taxCalculationId** is the ID of the tax calculation

#### Success Response

**HTTP Status**: 200

**Example HTTP Response Body**:
```
{
  "incomeTaxYTD":1000.25,
  "incomeTaxThisPeriod":1000.25,
  "profitFromSelfEmployment":200.22,
  "profitFromUkLandAndProperty":200.22,
  "totalIncomeReceived":200.22,
  "proportionAllowance":200,
  "totalIncomeOnWhichTaxIsDue":200.22,
  "payPensionsProfitAtBRT":200.22,
  "incomeTaxOnPayPensionsProfitAtBRT":200.22,
  "payPensionsProfitAtHRT":200.22,
  "incomeTaxOnPayPensionsProfitAtHRT":200.22,
  "payPensionsProfitAtART":200.22,
  "incomeTaxOnPayPensionsProfitAtART":200.22,
  "incomeTaxDue":200.22,
  "totalClass4Charge":200.22,
  "nationalInsuranceClass2Amount":200.22,
  "rateBRT":20.00,
  "rateHRT":40.00,
  "rateART":50.00,
  ... (excluded elements our app doesn't use)
}
```
Where:
* **incomeTaxYTD** details the amount of income tax is due based on the year to date figures supplied
* **incomeTaxThisPeriod** details the amount of income tax is due this period
* **profitFromSelfEmployment** is the profit from self-employment
* **profitFromUkLandAndProperty** is the profit from UK land and property
* **totalIncomeReceived** is the total income received
* **proportionAllowance** is the proportional personal allowance
* **totalIncomeOnWhichTaxIsDue** is the total income on which tax is due
* **payPensionsProfitAtBRT** is the amount of pay, pensions and profit at the basic rate of tax
* **incomeTaxOnPayPensionsProfitAtBRT** is the tax on pay, pensions and profit at the basic rate of tax (BRT)
* **payPensionsProfitAtHRT** is the amount of pay, pensions and profit at the higher rate of tax
* **incomeTaxOnPayPensionsProfitAtHRT** is the tax on pay, pensions and profit at the higher rate of tax (HRT)
* **payPensionsProfitAtART** is the amount of pay, pensions and profit at the additional higher rate of tax (AHRT)
* **incomeTaxOnPayPensionsProfitAtART** is the tax on pay, pensions and profit at the additional higher rate of tax (AHRT)
* **incomeTaxDue** is the income tax due
* **totalClass4Charge** is the total charge on class 4 national insurance contributions
* **nationalInsuranceClass2Amount** is the charge on class 2 national insurance contributions
* **rateBRT**, **rateHRT** and **rateART** are the basic, higher and additional higher rates of tax respectively


### GET /self-assessment/ni/:nino/uk-properties

Where **:nino** is a valid NINO in format XX999999X, for example: ```QQ123456C```

#### Success Response

**HTTP Status**: 200

**Example HTTP Response Body**:
```
{
}
```


### GET /self-assessment/ni/:nino/uk-properties/obligations

Where **:nino** is a valid NINO in format XX999999X, for example: ```QQ123456C```

#### Success Response

**HTTP Status**: 200

**Example HTTP Response Body**:
```
{
  "obligations": [
    {
      "start": "2017-04-06",
      "end": "2017-07-05",
      "due": "2017-08-05",
      "met": true
    },
    {
      "start": "2017-07-06",
      "end": "2017-10-05",
      "due": "2017-11-05",
      "met": true
    },
    {
      "start": "2017-10-06",
      "end": "2018-01-05",
      "due": "2018-02-05",
      "met": false
    },
    {
      "start": "2018-01-06",
      "end": "2018-04-05",
      "due": "2018-05-06",
      "met": false
    }
  ]
}
```
Where:
* **start**, **end** and **due** are dates in the format ```YYYY-MM-DD```
* **met** is ```true``` if the obligation period has been met and ```false``` otherwise


### API Error Responses

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

