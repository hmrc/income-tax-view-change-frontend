/*
 * Copyright 2018 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package assets

import auth.MtdItUser
import enums.{Crystallised, Estimate}
import models._
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.twirl.api.Html
import uk.gov.hmrc.auth.core._
import utils.ImplicitDateFormatter
import uk.gov.hmrc.auth.core.retrieve._

object TestConstants extends ImplicitDateFormatter {

  val testMtditid = "XAIT0000123456"
  val testNino = "AB123456C"
  val testUserName = "Albert Einstein"
  val testUserDetails = UserDetailsModel(testUserName, None, "n/a", "n/a")
  val testUserDetailsError = UserDetailsError
  val testUserDetailsUrl = "/user/oid/potato"
  lazy val testMtdItUser: MtdItUser[_] = MtdItUser(testMtditid, testNino, Some(testUserDetails), IncomeSourceDetails.bothIncomeSourceSuccessMisalignedTaxYear)(FakeRequest())
  lazy val testMtdItUserNoUserDetails: MtdItUser[_] = MtdItUser(testMtditid, testNino, None, IncomeSourceDetails.bothIncomeSourceSuccessMisalignedTaxYear)(FakeRequest())
  val testSelfEmploymentId  = "XA00001234"
  val testSelfEmploymentId2 = "XA00001235"
  val testTaxCalculationId = "CALCID"
  val testErrorStatus = Status.INTERNAL_SERVER_ERROR
  val testErrorMessage = "Dummy Error Message"
  val testAuthSuccessResponse = new ~(Enrolments(Set(
    Enrolment("HMRC-MTD-IT", Seq(EnrolmentIdentifier("MTDITID", testMtditid)), "activated"),
    Enrolment("HMRC-NI", Seq(EnrolmentIdentifier("NINO", testNino)), "activated")
  )),Option(testUserDetailsUrl))

  object ServiceInfoPartial {
    val serviceInfoPartialSuccess =
      Html("""
    <a id="service-info-home-link"
       class="service-info__item service-info__left font-xsmall button button--link button--link-table button--small soft-half--sides"
       data-journey-click="Header:Click:Home"
       href="/business-account">
      Business tax home
    </a>
    <ul id="service-info-list"
      class="service-info__item service-info__right list--collapse">
      <li class="list__item">
        <span id="service-info-user-name" class="bold-xsmall">Test User</span>
      </li>

      <li class="list__item soft--left">
        <a id="service-info-manage-account-link"
           href="/business-account/manage-account"
          data-journey-click="Header:Click:ManageAccount">
          Manage account
        </a>
      </li>
      <li class="list__item soft--left">
        <a id="service-info-messages-link"
           href="/business-account/messages"
          data-journey-click="Header:Click:Messages">
          Messages
        </a>
      </li>
    </ul>
  """.stripMargin.trim)
  }

  object ReportDeadlines {

    def fakeReportDeadlinesModel(m: ReportDeadlineModel): ReportDeadlineModel = new ReportDeadlineModel(m.start,m.end,m.due,m.met) {
      override def currentTime() = "2017-10-31"
    }

    val receivedObligation = fakeReportDeadlinesModel(ReportDeadlineModel(
      start = "2017-04-01",
      end = "2017-6-30",
      due = "2017-7-31",
      met = true
    ))

    val overdueObligation = fakeReportDeadlinesModel(ReportDeadlineModel(
      start = "2017-7-1",
      end = "2017-9-30",
      due = "2017-10-30",
      met = false
    ))

    val openObligation = fakeReportDeadlinesModel(ReportDeadlineModel(
      start = "2017-7-1",
      end = "2017-9-30",
      due = "2017-10-31",
      met = false
    ))

    val obligationsDataSuccessModel: ReportDeadlinesModel = ReportDeadlinesModel(List(receivedObligation, overdueObligation, openObligation))
    val obligationsDataSuccessString: String =
      """
        |{
        |  "obligations": [
        |    {
        |      "start": "2017-04-01",
        |      "end": "2017-06-30",
        |      "due": "2017-07-31",
        |      "met": true
        |    },
        |    {
        |      "start": "2017-07-01",
        |      "end": "2017-09-30",
        |      "due": "2017-10-30",
        |      "met": false
        |    },
        |    {
        |      "start": "2017-07-01",
        |      "end": "2017-09-30",
        |      "due": "2017-10-31",
        |      "met": false
        |    }
        |  ]
        |}
      """.stripMargin
    val obligationsDataSuccessJson = Json.parse(obligationsDataSuccessString)

    val obligationsDataErrorModel = ReportDeadlinesErrorModel(testErrorStatus, testErrorMessage)
    val obligationsDataErrorString =
      s"""
         |{
         |  "code":$testErrorStatus,
         |  "message":"$testErrorMessage"
         |}
      """.stripMargin
    val obligationsDataErrorJson = Json.parse(obligationsDataErrorString)

    val reportDeadlineReceived =
      """{
        |      "start": "2017-04-01",
        |      "end": "2017-06-30",
        |      "due": "2017-07-31",
        |      "met": true
        |    }"""
      .stripMargin
    val reportDeadlineReceivedJson = Json.parse(reportDeadlineReceived)

  }

  object BusinessDetails {

    val receivedObligation = ReportDeadlineModel(
      start = "2017-04-01",
      end = "2017-6-30",
      due = "2017-7-31",
      met = true
    )

    val overdueObligation = ReportDeadlineModel(
      start = "2017-7-1",
      end = "2017-9-30",
      due = "2017-10-30",
      met = false
    )

    val openObligation = ReportDeadlineModel(
      start = "2017-7-1",
      end = "2017-9-30",
      due = "2017-10-31",
      met = false
    )

    val obligationsDataSuccessModel: ReportDeadlinesModel = ReportDeadlinesModel(List(receivedObligation, overdueObligation, openObligation))
    val testBusinessAccountingPeriod = AccountingPeriodModel(start = "2017-6-1", end = "2018-5-30")
    val test2018BusinessAccountingPeriod = AccountingPeriodModel(start = "2017-3-5", end = "2018-3-6")
    val testTradeName = "business"
    val testTradeName2 = "business"

    val business1 = BusinessModel(
      id = testSelfEmploymentId,
      accountingPeriod = testBusinessAccountingPeriod,
      accountingType = "CASH",
      commencementDate = Some("2017-1-1"),
      cessationDate = None,
      tradingName = testTradeName,
      businessDescription = Some("a business"),
      businessAddressLineOne = Some("64 Zoo Lane"),
      businessAddressLineTwo = Some("Happy Place"),
      businessAddressLineThree = Some("Magical Land"),
      businessAddressLineFour = Some("England"),
      businessPostcode = Some("ZL1 064")
    )
    val business2 = BusinessModel(
      id = testSelfEmploymentId2,
      accountingPeriod = testBusinessAccountingPeriod,
      accountingType = "CASH",
      commencementDate = Some("2017-1-1"),
      cessationDate = None,
      tradingName = testTradeName2,
      businessDescription = Some("some business"),
      businessAddressLineOne = Some("65 Zoo Lane"),
      businessAddressLineTwo = Some("Happy Place"),
      businessAddressLineThree = Some("Magical Land"),
      businessAddressLineFour = Some("England"),
      businessPostcode = Some("ZL1 064")
    )

    val businessesSuccessResponse = List(business1)
    val multipleBusinessSuccessResponse = List(business1, business2)
    val noBusinessDetails = BusinessDetailsModel(List())
    val businessSuccessEmptyResponse = "[]"
    val businessesSuccessModel = BusinessDetailsModel(businessesSuccessResponse)
    val multipleBusinessesSuccessModel = BusinessDetailsModel(multipleBusinessSuccessResponse)
    val businessSuccessString: String =
      s"""
          {
             "businesses":[
                {
                   "id":"$testSelfEmploymentId",
                   "accountingPeriod":{
                     "start":"${testBusinessAccountingPeriod.start}",
                     "end":"${testBusinessAccountingPeriod.end}"
                   },
                   "accountingType":"CASH",
                   "commencementDate":"2017-01-01",
                   "tradingName":"$testTradeName",
                   "businessDescription":"a business",
                   "businessAddressLineOne":"64 Zoo Lane",
                   "businessAddressLineTwo":"Happy Place",
                   "businessAddressLineThree":"Magical Land",
                   "businessAddressLineFour":"England",
                   "businessPostcode":"ZL1 064"
                },
                {
                   "id":"$testSelfEmploymentId2",
                      "accountingPeriod":{
                        "start":"${testBusinessAccountingPeriod.start}",
                        "end":"${testBusinessAccountingPeriod.end}"
                      },
                   "accountingType":"CASH",
                   "commencementDate":"2017-01-01",
                   "tradingName":"$testTradeName2",
                   "businessDescription":"some business",
                   "businessAddressLineOne":"65 Zoo Lane",
                   "businessAddressLineTwo":"Happy Place",
                   "businessAddressLineThree":"Magical Land",
                   "businessAddressLineFour":"England",
                   "businessPostcode":"ZL1 064"
                }
             ]
          }
      """.stripMargin
    val businessSuccessJson: JsValue = Json.parse(businessSuccessString)


    val businessErrorModel = BusinessDetailsErrorModel(testErrorStatus, testErrorMessage)
    val businessErrorString: String =
      s"""
        |{
        |  "code":$testErrorStatus,
        |  "message":"$testErrorMessage"
        |}
      """.stripMargin
    val businessListErrorJson: JsValue = Json.parse(businessErrorString)

    val businessIncomeModel =
      BusinessIncomeModel(
        testSelfEmploymentId,
        testTradeName,
        None,
        testBusinessAccountingPeriod,
        obligationsDataSuccessModel
      )

    val businessIncomeModel2 =
      BusinessIncomeModel(
        testSelfEmploymentId2,
        testTradeName2,
        None,
        testBusinessAccountingPeriod,
        obligationsDataSuccessModel
      )

    val business2018IncomeModel =
      BusinessIncomeModel(
        testSelfEmploymentId,
        testTradeName,
        None,
        test2018BusinessAccountingPeriod,
        obligationsDataSuccessModel
      )

    val businessIncomeModelAlignedTaxYear =
      BusinessIncomeModel(
        testSelfEmploymentId,
        testTradeName,
        None,
        AccountingPeriodModel(start = "2017-4-6", end = "2018-4-5"),
        obligationsDataSuccessModel
      )

    val business2019IncomeModel =
      BusinessIncomeModel(
        testSelfEmploymentId,
        testTradeName,
        None,
        AccountingPeriodModel(start = "2018-3-5", end = "2019-3-6"),
        obligationsDataSuccessModel
      )
  }

  object PropertyIncome {
    val propertyIncomeModel = PropertyIncomeModel(
      accountingPeriod = AccountingPeriodModel("2017-04-06", "2018-04-05"),
      ReportDeadlines.obligationsDataSuccessModel
    )
  }

  object PropertyDetails {
    val testPropertyAccountingPeriod = AccountingPeriodModel(start = "2017-4-6", end = "2018-4-5")
    val propertySuccessModel = PropertyDetailsModel(testPropertyAccountingPeriod)
    val propertyErrorModel = PropertyDetailsErrorModel(testErrorStatus, testErrorMessage)
  }

  object Estimates {

    val testYear = 2018
    val testYearPlusOne = 2019
    val testYearPlusTwo = 2020
    val testCalcType = "it"

    //Last Tax Calculations
    val lastTaxCalcSuccess = LastTaxCalculation(
      calcID = testTaxCalculationId,
      calcTimestamp = "2017-07-06T12:34:56.789Z",
      calcAmount = 543.21,
      calcStatus = Estimate
    )
    val lastTaxCalcCrystallisedSuccess = LastTaxCalculation(
      calcID = testTaxCalculationId,
      calcTimestamp = "2017-07-06T12:34:56.789Z",
      calcAmount = 543.21,
      calcStatus = Crystallised
    )
    val lastTaxCalcError = LastTaxCalculationError(testErrorStatus, testErrorMessage)
    val lastTaxCalcNotFound: LastTaxCalculationResponseModel = NoLastTaxCalculation

    //Last Tax Calculation With Years (for sub pages)
    val lastTaxCalcSuccessWithYear = LastTaxCalculationWithYear(lastTaxCalcSuccess, testYear)
    val lastTaxCalcWithYearList = List(
      LastTaxCalculationWithYear(lastTaxCalcSuccess, testYear),
      LastTaxCalculationWithYear(lastTaxCalcSuccess, testYearPlusOne))
    val lastTaxCalcWithYearCrystallisedList = List(
      LastTaxCalculationWithYear(lastTaxCalcCrystallisedSuccess, testYear),
      LastTaxCalculationWithYear(lastTaxCalcCrystallisedSuccess, testYearPlusOne)
    )
    val lastTaxCalcWithYearListWithError = List(
      LastTaxCalculationWithYear(lastTaxCalcCrystallisedSuccess, testYear),
      LastTaxCalculationWithYear(lastTaxCalcError, testYearPlusOne)
    )
    val lastTaxCalcWithYearListWithCalcNotFound = List(
      LastTaxCalculationWithYear(lastTaxCalcCrystallisedSuccess, testYear),
      LastTaxCalculationWithYear(lastTaxCalcNotFound, testYearPlusOne)
    )
    val lastTaxCalcErrorWithYear = LastTaxCalculationWithYear(lastTaxCalcError, testYear)
  }

  object IncomeSourceDetails {

    //Outputs
    val bothIncomeSourceSuccessMisalignedTaxYear = IncomeSourcesModel(List(BusinessDetails.businessIncomeModel, BusinessDetails.businessIncomeModel2), Some(PropertyIncome.propertyIncomeModel))
    val businessIncomeSourceSuccess = IncomeSourcesModel(List(BusinessDetails.businessIncomeModel), None)
    val business2018IncomeSourceSuccess = IncomeSourcesModel(List(BusinessDetails.business2018IncomeModel), None)
    val business2018And19IncomeSourceSuccess = IncomeSourcesModel(List(BusinessDetails.business2018IncomeModel, BusinessDetails.business2019IncomeModel), None)
    val propertyIncomeSourceSuccess = IncomeSourcesModel(List.empty, Some(PropertyIncome.propertyIncomeModel))
    val noIncomeSourceSuccess = IncomeSourcesModel(List.empty, None)
    val bothIncomeSourcesSuccessBusinessAligned =
      IncomeSourcesModel(List(BusinessDetails.businessIncomeModelAlignedTaxYear), Some(PropertyIncome.propertyIncomeModel))
  }

  object CalcBreakdown {

    val calculationDataSuccessModel = CalculationDataModel(
      totalIncomeTaxNicYtd = 90500.00,
      totalTaxableIncome = 198500.00,
      personalAllowance = 11500.00,
      taxReliefs = 0,
      additionalAllowances = 505.00,
      incomeReceived = IncomeReceivedModel(
        selfEmployment = 200000.00,
        ukProperty = 10000.00,
        bankBuildingSocietyInterest = 1999.00,
        ukDividends = 0.0
      ),
      payPensionsProfit = PayPensionsProfitModel(
        basicBand = BandModel(
          taxableIncome = 20000.00,
          taxRate = 20.0,
          taxAmount = 4000.00
        ),
        higherBand = BandModel(
          taxableIncome = 100000.00,
          taxRate = 40.0,
          taxAmount = 40000.00
        ),
        additionalBand = BandModel(
          taxableIncome = 50000.00,
          taxRate = 45.0,
          taxAmount = 22500.00
        )
      ),
      savingsAndGains = SavingsAndGainsModel(
        startBand = BandModel(
          taxableIncome = 1.00,
          taxRate = 0.0,
          taxAmount = 0.0
        ),
        zeroBand = BandModel(
          taxableIncome = 20.00,
          taxRate = 0.0,
          taxAmount = 0.0
        ),
        basicBand = BandModel(
          taxableIncome = 0.0,
          taxRate = 20.0,
          taxAmount = 0.0
        ),
        higherBand = BandModel(
          taxableIncome = 0.0,
          taxRate = 40.0,
          taxAmount = 0.0
        ),
        additionalBand = BandModel(
          taxableIncome = 0.0,
          taxRate = 45.0,
          taxAmount = 0.0
        )
      ),
      dividends = DividendsModel(
        allowance = 0.0,
        basicBand = BandModel(
          taxableIncome = 0.0,
          taxRate = 0.0,
          taxAmount = 0.0
        ),
        higherBand = BandModel(
          taxableIncome = 0.0,
          taxRate = 0.0,
          taxAmount = 0.0
        ),
        additionalBand = BandModel(
          taxableIncome = 0.0,
          taxRate = 0.0,
          taxAmount = 0.0
        )
      ),
      nic = NicModel(
        class2 = 10000.00,
        class4 = 14000.00
      ),
      eoyEstimate = Some(EoyEstimate(66000.00))
    )

    val noTaxOrNICalcDataModel = CalculationDataModel(
      totalIncomeTaxNicYtd = 0.00,
      totalTaxableIncome = 0.00,
      personalAllowance = 2868.00,
      taxReliefs = 0,
      additionalAllowances = 0.00,
      incomeReceived = IncomeReceivedModel(
        selfEmployment = 500.00,
        ukProperty = 500.00,
        bankBuildingSocietyInterest = 0.00,
        ukDividends = 0.0
      ),
      payPensionsProfit = PayPensionsProfitModel(
        basicBand = BandModel(
          taxableIncome = 0.00,
          taxRate = 20.0,
          taxAmount = 0.00
        ),
        higherBand = BandModel(
          taxableIncome = 0.00,
          taxRate = 40.0,
          taxAmount = 0.00
        ),
        additionalBand = BandModel(
          taxableIncome = 0.00,
          taxRate = 45.0,
          taxAmount = 0.00
        )
      ),
      savingsAndGains = SavingsAndGainsModel(
        startBand = BandModel(
          taxableIncome = 0.00,
          taxRate = 0.0,
          taxAmount = 0.0
        ),
        zeroBand = BandModel(
          taxableIncome = 0.00,
          taxRate = 0.0,
          taxAmount = 0.0
        ),
        basicBand = BandModel(
          taxableIncome = 0.0,
          taxRate = 20.0,
          taxAmount = 0.0
        ),
        higherBand = BandModel(
          taxableIncome = 0.0,
          taxRate = 40.0,
          taxAmount = 0.0
        ),
        additionalBand = BandModel(
          taxableIncome = 0.0,
          taxRate = 45.0,
          taxAmount = 0.0
        )
      ),
      dividends = DividendsModel(
        allowance = 0.0,
        basicBand = BandModel(
          taxableIncome = 0.0,
          taxRate = 0.0,
          taxAmount = 0.0
        ),
        higherBand = BandModel(
          taxableIncome = 0.0,
          taxRate = 0.0,
          taxAmount = 0.0
        ),
        additionalBand = BandModel(
          taxableIncome = 0.0,
          taxRate = 0.0,
          taxAmount = 0.0
        )
      ),
      nic = NicModel(
        class2 = 0.00,
        class4 = 0.00
      )
    )

    val noTaxJustNICalcDataModel = CalculationDataModel(
      totalIncomeTaxNicYtd = 37.05,
      totalTaxableIncome = 0.00,
      personalAllowance = 2868.00,
      taxReliefs = 10.05,
      additionalAllowances = 0.00,
      incomeReceived = IncomeReceivedModel(
        selfEmployment = 1506.25,
        ukProperty = 0.00,
        bankBuildingSocietyInterest = 0.00,
        ukDividends = 0.0
      ),
      payPensionsProfit = PayPensionsProfitModel(
        basicBand = BandModel(
          taxableIncome = 0.00,
          taxRate = 20.0,
          taxAmount = 0.00
        ),
        higherBand = BandModel(
          taxableIncome = 0.00,
          taxRate = 40.0,
          taxAmount = 0.00
        ),
        additionalBand = BandModel(
          taxableIncome = 0.00,
          taxRate = 45.0,
          taxAmount = 0.00
        )
      ),
      savingsAndGains = SavingsAndGainsModel(
        startBand = BandModel(
          taxableIncome = 0.00,
          taxRate = 0.0,
          taxAmount = 0.0
        ),
        zeroBand = BandModel(
          taxableIncome = 0.00,
          taxRate = 0.0,
          taxAmount = 0.0
        ),
        basicBand = BandModel(
          taxableIncome = 0.0,
          taxRate = 20.0,
          taxAmount = 0.0
        ),
        higherBand = BandModel(
          taxableIncome = 0.0,
          taxRate = 40.0,
          taxAmount = 0.0
        ),
        additionalBand = BandModel(
          taxableIncome = 0.0,
          taxRate = 45.0,
          taxAmount = 0.0
        )
      ),
      dividends = DividendsModel(
        allowance = 0.0,
        basicBand = BandModel(
          taxableIncome = 0.0,
          taxRate = 0.0,
          taxAmount = 0.0
        ),
        higherBand = BandModel(
          taxableIncome = 0.0,
          taxRate = 0.0,
          taxAmount = 0.0
        ),
        additionalBand = BandModel(
          taxableIncome = 0.0,
          taxRate = 0.0,
          taxAmount = 0.0
        )
      ),
      nic = NicModel(
        class2 = 20.05,
        class4 = 17.05
      )
    )


    val busPropBRTCalcDataModel = CalculationDataModel(
      totalIncomeTaxNicYtd = 149.86,
      totalTaxableIncome = 132.00,
      personalAllowance = 2868.00,
      taxReliefs=24.90,
      additionalAllowances = 0.00,
      incomeReceived = IncomeReceivedModel(
        selfEmployment = 1500.00,
        ukProperty = 1500.00,
        bankBuildingSocietyInterest = 0.00,
        ukDividends = 0.0
      ),
      payPensionsProfit = PayPensionsProfitModel(
        basicBand = BandModel(
          taxableIncome = 132.00,
          taxRate = 20.0,
          taxAmount = 26.00
        ),
        higherBand = BandModel(
          taxableIncome = 0.00,
          taxRate = 40.0,
          taxAmount = 0.00
        ),
        additionalBand = BandModel(
          taxableIncome = 0.00,
          taxRate = 45.0,
          taxAmount = 0.00
        )
      ),
      savingsAndGains = SavingsAndGainsModel(
        startBand = BandModel(
          taxableIncome = 0.00,
          taxRate = 0.0,
          taxAmount = 0.0
        ),
        zeroBand = BandModel(
          taxableIncome = 0.00,
          taxRate = 0.0,
          taxAmount = 0.0
        ),
        basicBand = BandModel(
          taxableIncome = 0.0,
          taxRate = 20.0,
          taxAmount = 0.0
        ),
        higherBand = BandModel(
          taxableIncome = 0.0,
          taxRate = 40.0,
          taxAmount = 0.0
        ),
        additionalBand = BandModel(
          taxableIncome = 0.0,
          taxRate = 45.0,
          taxAmount = 0.0
        )
      ),
      dividends = DividendsModel(
        allowance = 0.0,
        basicBand = BandModel(
          taxableIncome = 0.0,
          taxRate = 0.0,
          taxAmount = 0.0
        ),
        higherBand = BandModel(
          taxableIncome = 0.0,
          taxRate = 0.0,
          taxAmount = 0.0
        ),
        additionalBand = BandModel(
          taxableIncome = 0.0,
          taxRate = 0.0,
          taxAmount = 0.0
        )
      ),
      nic = NicModel(
        class2 = 110,
        class4 = 13.86
      ),
      eoyEstimate = Some(EoyEstimate(66000.00))
    )

    val busBropHRTCalcDataModel = CalculationDataModel(
      totalIncomeTaxNicYtd = 13727.71,
      totalTaxableIncome = 35007.00,
      personalAllowance = 2868.00,
      taxReliefs = 0,
      additionalAllowances = 0.00,
      incomeReceived = IncomeReceivedModel(
        selfEmployment = 30000.00,
        ukProperty = 7875.00,
        bankBuildingSocietyInterest = 0.00,
        ukDividends = 0.0
      ),
      payPensionsProfit = PayPensionsProfitModel(
        basicBand = BandModel(
          taxableIncome = 8352.00,
          taxRate = 20.0,
          taxAmount = 1670.00
        ),
        higherBand = BandModel(
          taxableIncome = 26654.00,
          taxRate = 40.0,
          taxAmount = 10661.00
        ),
        additionalBand = BandModel(
          taxableIncome = 0.00,
          taxRate = 45.0,
          taxAmount = 0.00
        )
      ),
      savingsAndGains = SavingsAndGainsModel(
        startBand = BandModel(
          taxableIncome = 0.00,
          taxRate = 0.0,
          taxAmount = 0.0
        ),
        zeroBand = BandModel(
          taxableIncome = 0.00,
          taxRate = 0.0,
          taxAmount = 0.0
        ),
        basicBand = BandModel(
          taxableIncome = 0.0,
          taxRate = 20.0,
          taxAmount = 0.0
        ),
        higherBand = BandModel(
          taxableIncome = 0.0,
          taxRate = 40.0,
          taxAmount = 0.0
        ),
        additionalBand = BandModel(
          taxableIncome = 0.0,
          taxRate = 45.0,
          taxAmount = 0.0
        )
      ),
      dividends = DividendsModel(
        allowance = 0.0,
        basicBand = BandModel(
          taxableIncome = 0.0,
          taxRate = 0.0,
          taxAmount = 0.0
        ),
        higherBand = BandModel(
          taxableIncome = 0.0,
          taxRate = 0.0,
          taxAmount = 0.0
        ),
        additionalBand = BandModel(
          taxableIncome = 0.0,
          taxRate = 0.0,
          taxAmount = 0.0
        )
      ),
      nic = NicModel(
        class2 = 500.71,
        class4 = 896.00
      )
    )

    val busPropARTCalcDataModel = CalculationDataModel(
      totalIncomeTaxNicYtd = 15017.71,
      totalTaxableIncome = 38007.00,
      personalAllowance = 2868.00,
      taxReliefs = 0,
      additionalAllowances = 0.00,
      incomeReceived = IncomeReceivedModel(
        selfEmployment = 875.00,
        ukProperty = 40000.00,
        bankBuildingSocietyInterest = 0.00,
        ukDividends = 0.0
      ),
      payPensionsProfit = PayPensionsProfitModel(
        basicBand = BandModel(
          taxableIncome = 8352.00,
          taxRate = 20.0,
          taxAmount = 1670.00
        ),
        higherBand = BandModel(
          taxableIncome = 29044.00,
          taxRate = 40.0,
          taxAmount = 11617.00
        ),
        additionalBand = BandModel(
          taxableIncome = 609.00,
          taxRate = 45.0,
          taxAmount = 274.00
        )
      ),
      savingsAndGains = SavingsAndGainsModel(
        startBand = BandModel(
          taxableIncome = 0.00,
          taxRate = 0.0,
          taxAmount = 0.0
        ),
        zeroBand = BandModel(
          taxableIncome = 0.00,
          taxRate = 0.0,
          taxAmount = 0.0
        ),
        basicBand = BandModel(
          taxableIncome = 0.0,
          taxRate = 20.0,
          taxAmount = 0.0
        ),
        higherBand = BandModel(
          taxableIncome = 0.0,
          taxRate = 40.0,
          taxAmount = 0.0
        ),
        additionalBand = BandModel(
          taxableIncome = 0.0,
          taxRate = 45.0,
          taxAmount = 0.0
        )
      ),
      dividends = DividendsModel(
        allowance = 0.0,
        basicBand = BandModel(
          taxableIncome = 0.0,
          taxRate = 0.0,
          taxAmount = 0.0
        ),
        higherBand = BandModel(
          taxableIncome = 0.0,
          taxRate = 0.0,
          taxAmount = 0.0
        ),
        additionalBand = BandModel(
          taxableIncome = 0.0,
          taxRate = 0.0,
          taxAmount = 0.0
        )
      ),
      nic = NicModel(
        class2 = 1000,
        class4 = 456.71
      )
    )

    val justBusinessCalcDataModel = CalculationDataModel(
      totalIncomeTaxNicYtd = 149.86,
      totalTaxableIncome = 132.00,
      personalAllowance = 2868.00,
      taxReliefs = 0,
      additionalAllowances = 0.00,
      incomeReceived = IncomeReceivedModel(
        selfEmployment = 3000.00,
        ukProperty = 0.00,
        bankBuildingSocietyInterest = 0.00,
        ukDividends = 0.0
      ),
      payPensionsProfit = PayPensionsProfitModel(
        basicBand = BandModel(
          taxableIncome = 132.00,
          taxRate = 20.0,
          taxAmount = 26.00
        ),
        higherBand = BandModel(
          taxableIncome = 0.00,
          taxRate = 40.0,
          taxAmount = 0.00
        ),
        additionalBand = BandModel(
          taxableIncome = 0.00,
          taxRate = 45.0,
          taxAmount = 0.00
        )
      ),
      savingsAndGains = SavingsAndGainsModel(
        startBand = BandModel(
          taxableIncome = 0.00,
          taxRate = 0.0,
          taxAmount = 0.0
        ),
        zeroBand = BandModel(
          taxableIncome = 0.00,
          taxRate = 0.0,
          taxAmount = 0.0
        ),
        basicBand = BandModel(
          taxableIncome = 0.0,
          taxRate = 20.0,
          taxAmount = 0.0
        ),
        higherBand = BandModel(
          taxableIncome = 0.0,
          taxRate = 40.0,
          taxAmount = 0.0
        ),
        additionalBand = BandModel(
          taxableIncome = 0.0,
          taxRate = 45.0,
          taxAmount = 0.0
        )
      ),
      dividends = DividendsModel(
        allowance = 0.0,
        basicBand = BandModel(
          taxableIncome = 0.0,
          taxRate = 0.0,
          taxAmount = 0.0
        ),
        higherBand = BandModel(
          taxableIncome = 0.0,
          taxRate = 0.0,
          taxAmount = 0.0
        ),
        additionalBand = BandModel(
          taxableIncome = 0.0,
          taxRate = 0.0,
          taxAmount = 0.0
        )
      ),
      nic = NicModel(
        class2 = 100.0,
        class4 = 23.86
      )
    )

    val justPropertyCalcDataModel = CalculationDataModel(
      totalIncomeTaxNicYtd = 149.86,
      totalTaxableIncome = 132.00,
      personalAllowance = 2868.00,
      taxReliefs = 0,
      additionalAllowances = 0.00,
      incomeReceived = IncomeReceivedModel(
        selfEmployment = 0.00,
        ukProperty = 3000.00,
        bankBuildingSocietyInterest = 0.00,
        ukDividends = 0.0
      ),
      payPensionsProfit = PayPensionsProfitModel(
        basicBand = BandModel(
          taxableIncome = 132.00,
          taxRate = 20.0,
          taxAmount = 26.00
        ),
        higherBand = BandModel(
          taxableIncome = 0.00,
          taxRate = 40.0,
          taxAmount = 0.00
        ),
        additionalBand = BandModel(
          taxableIncome = 0.00,
          taxRate = 45.0,
          taxAmount = 0.00
        )
      ),
      savingsAndGains = SavingsAndGainsModel(
        startBand = BandModel(
          taxableIncome = 0.00,
          taxRate = 0.0,
          taxAmount = 0.0
        ),
        zeroBand = BandModel(
          taxableIncome = 0.00,
          taxRate = 0.0,
          taxAmount = 0.0
        ),
        basicBand = BandModel(
          taxableIncome = 0.0,
          taxRate = 20.0,
          taxAmount = 0.0
        ),
        higherBand = BandModel(
          taxableIncome = 0.0,
          taxRate = 40.0,
          taxAmount = 0.0
        ),
        additionalBand = BandModel(
          taxableIncome = 0.0,
          taxRate = 45.0,
          taxAmount = 0.0
        )
      ),
      dividends = DividendsModel(
        allowance = 0.0,
        basicBand = BandModel(
          taxableIncome = 0.0,
          taxRate = 0.0,
          taxAmount = 0.0
        ),
        higherBand = BandModel(
          taxableIncome = 0.0,
          taxRate = 0.0,
          taxAmount = 0.0
        ),
        additionalBand = BandModel(
          taxableIncome = 0.0,
          taxRate = 0.0,
          taxAmount = 0.0
        )
      ),
      nic = NicModel(
        class2 = 100.0,
        class4 = 23.86
      )
    )

    val justPropertyWithSavingsCalcDataModel = CalculationDataModel(
      totalIncomeTaxNicYtd = 149.86,
      totalTaxableIncome = 132.00,
      personalAllowance = 2868.00,
      taxReliefs = 0,
      additionalAllowances = 0.00,
      incomeReceived = IncomeReceivedModel(
        selfEmployment = 0.00,
        ukProperty = 3000.00,
        bankBuildingSocietyInterest = 2500.00,
        ukDividends = 0.0
      ),
      payPensionsProfit = PayPensionsProfitModel(
        basicBand = BandModel(
          taxableIncome = 132.00,
          taxRate = 20.0,
          taxAmount = 26.00
        ),
        higherBand = BandModel(
          taxableIncome = 0.00,
          taxRate = 40.0,
          taxAmount = 0.00
        ),
        additionalBand = BandModel(
          taxableIncome = 0.00,
          taxRate = 45.0,
          taxAmount = 0.00
        )
      ),
      savingsAndGains = SavingsAndGainsModel(
        startBand = BandModel(
          taxableIncome = 2500.00,
          taxRate = 0.0,
          taxAmount = 0.0
        ),
        zeroBand = BandModel(
          taxableIncome = 0.00,
          taxRate = 0.0,
          taxAmount = 0.0
        ),
        basicBand = BandModel(
          taxableIncome = 0.0,
          taxRate = 20.0,
          taxAmount = 0.0
        ),
        higherBand = BandModel(
          taxableIncome = 0.0,
          taxRate = 40.0,
          taxAmount = 0.0
        ),
        additionalBand = BandModel(
          taxableIncome = 0.0,
          taxRate = 45.0,
          taxAmount = 0.0
        )
      ),
      dividends = DividendsModel(
        allowance = 0.0,
        basicBand = BandModel(
          taxableIncome = 0.0,
          taxRate = 0.0,
          taxAmount = 0.0
        ),
        higherBand = BandModel(
          taxableIncome = 0.0,
          taxRate = 0.0,
          taxAmount = 0.0
        ),
        additionalBand = BandModel(
          taxableIncome = 0.0,
          taxRate = 0.0,
          taxAmount = 0.0
        )
      ),
      nic = NicModel(
        class2 = 100.0,
        class4 = 23.86
      )
    )

    val mandatoryOnlyDataModel = CalculationDataModel(
      totalIncomeTaxNicYtd = 90500.0,
      totalTaxableIncome = 0.00,
      personalAllowance = 0.00,
      taxReliefs = 0,
      additionalAllowances = 0.00,
      incomeReceived = IncomeReceivedModel(
        selfEmployment = 0.00,
        ukProperty = 0.00,
        bankBuildingSocietyInterest = 0.00,
        ukDividends = 0.0
      ),
      payPensionsProfit = PayPensionsProfitModel(
        basicBand = BandModel(
          taxableIncome = 0.00,
          taxRate = 0.0,
          taxAmount = 0.00
        ),
        higherBand = BandModel(
          taxableIncome = 0.00,
          taxRate = 0.0,
          taxAmount = 0.00
        ),
        additionalBand = BandModel(
          taxableIncome = 0.00,
          taxRate = 0.0,
          taxAmount = 0.00
        )
      ),
      savingsAndGains = SavingsAndGainsModel(
        startBand = BandModel(
          taxableIncome = 0.00,
          taxRate = 0.0,
          taxAmount = 0.0
        ),
        zeroBand = BandModel(
          taxableIncome = 0.00,
          taxRate = 0.0,
          taxAmount = 0.0
        ),
        basicBand = BandModel(
          taxableIncome = 0.0,
          taxRate = 0.0,
          taxAmount = 0.0
        ),
        higherBand = BandModel(
          taxableIncome = 0.0,
          taxRate = 0.0,
          taxAmount = 0.0
        ),
        additionalBand = BandModel(
          taxableIncome = 0.0,
          taxRate = 0.0,
          taxAmount = 0.0
        )
      ),
      dividends = DividendsModel(
        allowance = 0.0,
        basicBand = BandModel(
          taxableIncome = 0.0,
          taxRate = 0.0,
          taxAmount = 0.0
        ),
        higherBand = BandModel(
          taxableIncome = 0.0,
          taxRate = 0.0,
          taxAmount = 0.0
        ),
        additionalBand = BandModel(
          taxableIncome = 0.0,
          taxRate = 0.0,
          taxAmount = 0.0
        )
      ),
      nic = NicModel(
        class2 = 0.0,
        class4 = 0.0
      )
    )

    val mandatoryCalculationDataSuccessString: String =
      """{"incomeTaxYTD": 90500,"incomeTaxThisPeriod": 2000}"""

    val calculationDataSuccessString: String =
      """
        |{
        | "totalTaxableIncome": 198500,
        | "totalIncomeTaxNicYtd": 90500,
        | "personalAllowance": 11500,
        | "taxReliefs": 0,
        | "additionalAllowances" : 505,
        | "incomeReceived": {
        |    "selfEmployment": 200000,
        |    "ukProperty": 10000,
        |    "bankBuildingSocietyInterest": 1999,
        |    "ukDividends": 0
        | },
        | "payPensionsProfit": {
        |   "basicBand": {
        |     "taxableIncome": 20000,
        |     "taxRate": 20,
        |     "taxAmount": 4000
        |   },
        |   "higherBand": {
        |     "taxableIncome": 100000,
        |     "taxRate": 40,
        |     "taxAmount": 40000
        |   },
        |   "additionalBand": {
        |     "taxableIncome": 50000,
        |     "taxRate": 45,
        |     "taxAmount": 22500
        |   }
        | },
        | "savingsAndGains": {
        |   "startBand": {
        |     "taxableIncome": 1.00,
        |     "taxRate": 0,
        |     "taxAmount": 0
        |   },
        |   "zeroBand": {
        |     "taxableIncome": 20.00,
        |     "taxRate": 0,
        |     "taxAmount": 0
        |   },
        |   "basicBand": {
        |     "taxableIncome": 0,
        |     "taxRate": 20,
        |     "taxAmount": 0
        |   },
        |   "higherBand": {
        |     "taxableIncome": 0,
        |     "taxRate": 40,
        |     "taxAmount": 0
        |   },
        |   "additionalBand": {
        |     "taxableIncome": 0,
        |     "taxRate": 45,
        |     "taxAmount": 0
        |   }
        | },
        | "dividends": {
        |   "allowance": 0,
        |   "basicBand": {
        |     "taxableIncome": 0,
        |     "taxRate": 0,
        |     "taxAmount": 0
        |   },
        |   "higherBand": {
        |     "taxableIncome": 0,
        |     "taxRate": 0,
        |     "taxAmount": 0
        |   },
        |   "additionalBand": {
        |     "taxableIncome": 0,
        |     "taxRate": 0,
        |     "taxAmount": 0
        |   }
        | },
        | "nic": {
        |   "class2": 10000,
        |   "class4": 14000
        | },
        | "eoyEstimate": {
        |   "incomeTaxNicAmount": 66000
        | }
        |}
      """.stripMargin


    val calculationDataFullString =
      """
        |{
        | "incomeTaxYTD": 90500,
        | "incomeTaxThisPeriod": 2000,
        | "payFromAllEmployments": 0,
        | "totalAllowancesAndDeductions": 505,
        | "benefitsAndExpensesReceived": 0,
        | "allowableExpenses": 0,
        | "payFromAllEmploymentsAfterExpenses": 0,
        | "shareSchemes": 0,
        | "profitFromSelfEmployment": 200000,
        | "profitFromPartnerships": 0,
        | "profitFromUkLandAndProperty": 10000,
        | "dividendsFromForeignCompanies": 0,
        | "foreignIncome": 0,
        | "trustsAndEstates": 0,
        | "interestReceivedFromUkBanksAndBuildingSocieties": 1999,
        | "dividendsFromUkCompanies": 0,
        | "ukPensionsAndStateBenefits": 0,
        | "gainsOnLifeInsurance": 0,
        | "otherIncome": 0,
        | "totalIncomeReceived": 230000,
        | "paymentsIntoARetirementAnnuity": 0,
        | "foreignTaxOnEstates": 0,
        | "incomeTaxRelief": 0,
        | "incomeTaxReliefReducedToMaximumAllowable": 0,
        | "annuities": 0,
        | "giftOfInvestmentsAndPropertyToCharity": 0,
        | "personalAllowance": 11500,
        | "marriageAllowanceTransfer": 0,
        | "blindPersonAllowance": 0,
        | "blindPersonSurplusAllowanceFromSpouse": 0,
        | "incomeExcluded": 0,
        | "totalIncomeAllowancesUsed": 0,
        | "totalIncomeOnWhichTaxIsDue": 198500,
        | "payPensionsExtender": 0,
        | "giftExtender": 0,
        | "extendedBR": 0,
        | "payPensionsProfitAtBRT": 20000,
        | "incomeTaxOnPayPensionsProfitAtBRT": 4000,
        | "payPensionsProfitAtHRT": 100000,
        | "incomeTaxOnPayPensionsProfitAtHRT": 40000,
        | "payPensionsProfitAtART": 50000,
        | "incomeTaxOnPayPensionsProfitAtART": 22500,
        | "netPropertyFinanceCosts": 0,
        | "interestReceivedAtStartingRate": 1,
        | "incomeTaxOnInterestReceivedAtStartingRate": 0,
        | "interestReceivedAtZeroRate": 20,
        | "incomeTaxOnInterestReceivedAtZeroRate": 0,
        | "interestReceivedAtBRT": 0,
        | "incomeTaxOnInterestReceivedAtBRT": 0,
        | "interestReceivedAtHRT": 0,
        | "incomeTaxOnInterestReceivedAtHRT": 0,
        | "interestReceivedAtART": 0,
        | "incomeTaxOnInterestReceivedAtART": 0,
        | "dividendsAtZeroRate": 0,
        | "incomeTaxOnDividendsAtZeroRate": 0,
        | "dividendsAtBRT": 0,
        | "incomeTaxOnDividendsAtBRT": 0,
        | "dividendsAtHRT": 0,
        | "incomeTaxOnDividendsAtHRT": 0,
        | "dividendsAtART": 0,
        | "incomeTaxOnDividendsAtART": 0,
        | "totalIncomeOnWhichTaxHasBeenCharged": 0,
        | "taxOnOtherIncome": 0,
        | "incomeTaxDue": 66500,
        | "incomeTaxCharged": 0,
        | "deficiencyRelief": 0,
        | "topSlicingRelief": 0,
        | "ventureCapitalTrustRelief": 0,
        | "enterpriseInvestmentSchemeRelief": 0,
        | "seedEnterpriseInvestmentSchemeRelief": 0,
        | "communityInvestmentTaxRelief": 0,
        | "socialInvestmentTaxRelief": 0,
        | "maintenanceAndAlimonyPaid": 0,
        | "marriedCouplesAllowance": 0,
        | "marriedCouplesAllowanceRelief": 0,
        | "surplusMarriedCouplesAllowance": 0,
        | "surplusMarriedCouplesAllowanceRelief": 0,
        | "notionalTaxFromLifePolicies": 0,
        | "notionalTaxFromDividendsAndOtherIncome": 0,
        | "foreignTaxCreditRelief": 0,
        | "incomeTaxDueAfterAllowancesAndReliefs": 0,
        | "giftAidPaymentsAmount": 0,
        | "giftAidTaxDue": 0,
        | "capitalGainsTaxDue": 0,
        | "remittanceForNonDomiciles": 0,
        | "highIncomeChildBenefitCharge": 0,
        | "totalGiftAidTaxReduced": 0,
        | "incomeTaxDueAfterGiftAidReduction": 0,
        | "annuityAmount": 0,
        | "taxDueOnAnnuity": 0,
        | "taxCreditsOnDividendsFromUkCompanies": 0,
        | "incomeTaxDueAfterDividendTaxCredits": 0,
        | "nationalInsuranceContributionAmount": 0,
        | "nationalInsuranceContributionCharge": 0,
        | "nationalInsuranceContributionSupAmount": 0,
        | "nationalInsuranceContributionSupCharge": 0,
        | "totalClass4Charge": 14000,
        | "nationalInsuranceClass1Amount": 0,
        | "nationalInsuranceClass2Amount": 10000,
        | "nicTotal": 24000,
        | "underpaidTaxForPreviousYears": 0,
        | "studentLoanRepayments": 0,
        | "pensionChargesGross": 0,
        | "pensionChargesTaxPaid": 0,
        | "totalPensionSavingCharges": 0,
        | "pensionLumpSumAmount": 0,
        | "pensionLumpSumRate": 0,
        | "statePensionLumpSumAmount": 0,
        | "remittanceBasisChargeForNonDomiciles": 0,
        | "additionalTaxDueOnPensions": 0,
        | "additionalTaxReliefDueOnPensions": 0,
        | "incomeTaxDueAfterPensionDeductions": 0,
        | "employmentsPensionsAndBenefits": 0,
        | "outstandingDebtCollectedThroughPaye": 0,
        | "payeTaxBalance": 0,
        | "cisAndTradingIncome": 0,
        | "partnerships": 0,
        | "ukLandAndPropertyTaxPaid": 0,
        | "foreignIncomeTaxPaid": 0,
        | "trustAndEstatesTaxPaid": 0,
        | "overseasIncomeTaxPaid": 0,
        | "interestReceivedTaxPaid": 0,
        | "voidISAs": 0,
        | "otherIncomeTaxPaid": 0,
        | "underpaidTaxForPriorYear": 0,
        | "totalTaxDeducted": 0,
        | "incomeTaxOverpaid": 0,
        | "incomeTaxDueAfterDeductions": 0,
        | "propertyFinanceTaxDeduction": 0,
        | "taxableCapitalGains": 0,
        | "capitalGainAtEntrepreneurRate": 0,
        | "incomeTaxOnCapitalGainAtEntrepreneurRate": 0,
        | "capitalGrainsAtLowerRate": 0,
        | "incomeTaxOnCapitalGainAtLowerRate": 0,
        | "capitalGainAtHigherRate": 0,
        | "incomeTaxOnCapitalGainAtHigherTax": 0,
        | "capitalGainsTaxAdjustment": 0,
        | "foreignTaxCreditReliefOnCapitalGains": 0,
        | "liabilityFromOffShoreTrusts": 0,
        | "taxOnGainsAlreadyCharged": 0,
        | "totalCapitalGainsTax": 0,
        | "incomeAndCapitalGainsTaxDue": 0,
        | "taxRefundedInYear": 0,
        | "unpaidTaxCalculatedForEarlierYears": 0,
        | "marriageAllowanceTransferAmount": 0,
        | "marriageAllowanceTransferRelief": 0,
        | "marriageAllowanceTransferMaximumAllowable": 0,
        | "nationalRegime": "0",
        | "allowance": 0,
        | "limitBRT": 0,
        | "limitHRT": 0,
        | "rateBRT": 20,
        | "rateHRT": 40,
        | "rateART": 45,
        | "limitAIA": 0,
        | "limitAIA": 0,
        | "allowanceBRT": 0,
        | "interestAllowanceHRT": 0,
        | "interestAllowanceBRT": 0,
        | "dividendAllowance": 0,
        | "dividendBRT": 0,
        | "dividendHRT": 0,
        | "dividendART": 0,
        | "class2NICsLimit": 0,
        | "class2NICsPerWeek": 0,
        | "class4NICsLimitBR": 0,
        | "class4NICsLimitHR": 0,
        | "class4NICsBRT": 0,
        | "class4NICsHRT": 0,
        | "proportionAllowance": 11500,
        | "proportionLimitBRT": 0,
        | "proportionLimitHRT": 0,
        | "proportionalTaxDue": 0,
        | "proportionInterestAllowanceBRT": 0,
        | "proportionInterestAllowanceHRT": 0,
        | "proportionDividendAllowance": 0,
        | "proportionPayPensionsProfitAtART": 0,
        | "proportionIncomeTaxOnPayPensionsProfitAtART": 0,
        | "proportionPayPensionsProfitAtBRT": 0,
        | "proportionIncomeTaxOnPayPensionsProfitAtBRT": 0,
        | "proportionPayPensionsProfitAtHRT": 0,
        | "proportionIncomeTaxOnPayPensionsProfitAtHRT": 0,
        | "proportionInterestReceivedAtZeroRate": 0,
        | "proportionIncomeTaxOnInterestReceivedAtZeroRate": 0,
        | "proportionInterestReceivedAtBRT": 0,
        | "proportionIncomeTaxOnInterestReceivedAtBRT": 0,
        | "proportionInterestReceivedAtHRT": 0,
        | "proportionIncomeTaxOnInterestReceivedAtHRT": 0,
        | "proportionInterestReceivedAtART": 0,
        | "proportionIncomeTaxOnInterestReceivedAtART": 0,
        | "proportionDividendsAtZeroRate": 0,
        | "proportionIncomeTaxOnDividendsAtZeroRate": 0,
        | "proportionDividendsAtBRT": 0,
        | "proportionIncomeTaxOnDividendsAtBRT": 0,
        | "proportionDividendsAtHRT": 0,
        | "proportionIncomeTaxOnDividendsAtHRT": 0,
        | "proportionDividendsAtART": 0,
        | "proportionIncomeTaxOnDividendsAtART": 0,
        | "proportionClass2NICsLimit": 0,
        | "proportionClass4NICsLimitBR": 0,
        | "proportionClass4NICsLimitHR": 0,
        | "proportionReducedAllowanceLimit": 0,
        | "eoyEstimate": {
        |        "selfEmployment": [
        |            {
        |                "id": "selfEmploymentId1",
        |                "taxableIncome": 89999999.99,
        |                "supplied": true,
        |                "finalised": true
        |            },
        |            {
        |                "id": "selfEmploymentId2",
        |                "taxableIncome": 89999999.99,
        |                "supplied": true,
        |                "finalised": true
        |            }
        |        ],
        |        "ukProperty": [
        |            {
        |                "taxableIncome": 89999999.99,
        |                "supplied": true,
        |                "finalised": true
        |            }
        |        ],
        |        "totalTaxableIncome": 89999999.99,
        |        "incomeTaxAmount": 89999999.99,
        |        "nic2": 89999999.99,
        |        "nic4": 89999999.99,
        |        "totalNicAmount": 9999999.99,
        |        "incomeTaxNicAmount": 66000.00
        |    }
        |}
      """.stripMargin

    val calculationDataSuccessMinString: String = "{}"

    val calculationDataSuccessJson: JsValue = Json.parse(calculationDataSuccessString)

    val calculationDataErrorModel: CalculationDataErrorModel = CalculationDataErrorModel(testErrorStatus, testErrorMessage)
    val calculationDataErrorString: String =
      s"""
         |{
         |  "code":$testErrorStatus,
         |  "message":"$testErrorMessage"
         |}
       """.stripMargin
    val calculationDataErrorJson: JsValue = Json.parse(calculationDataErrorString)

    val calculationDisplaySuccessModel: CalculationDataModel => CalcDisplayModel = calcModel =>
      CalcDisplayModel(
        Estimates.lastTaxCalcSuccess.calcTimestamp,
        Estimates.lastTaxCalcSuccess.calcAmount,
        Some(calcModel),
        Estimate
      )

    val calculationDisplaySuccessCrystalisationModel: CalculationDataModel => CalcDisplayModel = calcModel =>
      CalcDisplayModel(
        Estimates.lastTaxCalcSuccess.calcTimestamp,
        Estimates.lastTaxCalcSuccess.calcAmount,
        Some(calcModel),
        Crystallised
      )

    val calculationDisplayNoBreakdownModel =
      CalcDisplayModel(
        Estimates.lastTaxCalcSuccess.calcTimestamp,
        Estimates.lastTaxCalcSuccess.calcAmount,
        None,
        Estimate
      )
  }

  object NinoLookup {
    val testNinoModel: Nino = Nino(nino = testNino)
    val testNinoModelJson: JsValue = Json.parse(
      s"""{
         |"nino":"$testNino"
         |}
       """.stripMargin)

    val testNinoErrorModel: NinoResponseError = NinoResponseError(testErrorStatus, testErrorMessage)
    val testNinoErrorModelJson: JsValue = Json.parse(
      s"""
         |{
         |  "status":$testErrorStatus,
         |  "reason":"$testErrorMessage"
         |}
       """.stripMargin
    )
  }
}