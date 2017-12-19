/*
 * Copyright 2017 HM Revenue & Customs
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
      calcStatus = Some(Estimate)
    )
    val lastTaxCalcCrystallisedSuccess = LastTaxCalculation(
      calcID = testTaxCalculationId,
      calcTimestamp = "2017-07-06T12:34:56.789Z",
      calcAmount = 543.21,
      calcStatus = Some(Crystallised)
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

    val calculationDataSuccessModel = CalculationDataModel(incomeTaxYTD = 90500.00,
      incomeTaxThisPeriod = 2000.00,
      profitFromSelfEmployment = 200000.00,
      profitFromUkLandAndProperty = 10000.00,
      totalIncomeReceived = 230000.00,
      proportionAllowance = 11500.00,
      totalIncomeOnWhichTaxIsDue = 198500.00,
      payPensionsProfitAtBRT = 20000.00,
      incomeTaxOnPayPensionsProfitAtBRT = 4000.00,
      payPensionsProfitAtHRT = 100000.00,
      incomeTaxOnPayPensionsProfitAtHRT = 40000.00,
      payPensionsProfitAtART = 50000.00,
      incomeTaxOnPayPensionsProfitAtART = 22500.00,
      incomeTaxDue = 66500.00,
      nationalInsuranceClass2Amount = 10000.00,
      totalClass4Charge =14000.00,
      rateBRT = 20.00,
      rateHRT = 40.00,
      rateART = 45.00,
      eoyEstimate = Some(EoyEstimate(66000.00))
    )

    val noTaxOrNICalcDataModel =
      CalculationDataModel(
        profitFromSelfEmployment = 500.00,
        profitFromUkLandAndProperty = 500.00,
        proportionAllowance = 2868.00,
        totalIncomeOnWhichTaxIsDue = 0,

        payPensionsProfitAtBRT = 0.00,
        rateBRT = 20.00,
        incomeTaxOnPayPensionsProfitAtBRT = 0.00,

        payPensionsProfitAtHRT = 0.00,
        rateHRT = 40.00,
        incomeTaxOnPayPensionsProfitAtHRT = 0,

        payPensionsProfitAtART = 0,
        rateART = 45.00,
        incomeTaxOnPayPensionsProfitAtART = 0,

        nationalInsuranceClass2Amount = 0,
        totalClass4Charge =0,
        incomeTaxYTD = 0,

        //Don't need these
        incomeTaxThisPeriod = 2000.00,
        totalIncomeReceived = 9000.00,
        incomeTaxDue = 2072.05
      )

    val noTaxJustNICalcDataModel =
      CalculationDataModel(
        profitFromSelfEmployment = 1506.25,
        profitFromUkLandAndProperty = 0,
        proportionAllowance = 2868.00,
        totalIncomeOnWhichTaxIsDue = 0,

        payPensionsProfitAtBRT = 0,
        rateBRT = 20.00,
        incomeTaxOnPayPensionsProfitAtBRT = 0.00,

        payPensionsProfitAtHRT = 0,
        rateHRT = 40.00,
        incomeTaxOnPayPensionsProfitAtHRT = 0,

        payPensionsProfitAtART = 0,
        rateART = 45.00,
        incomeTaxOnPayPensionsProfitAtART = 0,

        nationalInsuranceClass2Amount=20.05,
        totalClass4Charge = 17.05,
        incomeTaxYTD = 37.05,

        //Don't need these
        incomeTaxThisPeriod = 2000.00,
        totalIncomeReceived = 9000.00,
        incomeTaxDue = 2072.05
      )

    val busPropBRTCalcDataModel =
      CalculationDataModel(
        profitFromSelfEmployment = 1500.00,
        profitFromUkLandAndProperty = 1500.00,
        proportionAllowance = 2868.00,
        totalIncomeOnWhichTaxIsDue = 132.00,

        payPensionsProfitAtBRT = 132.00,
        rateBRT = 20.00,
        incomeTaxOnPayPensionsProfitAtBRT = 26.00,

        payPensionsProfitAtHRT = 0,
        rateHRT = 40.00,
        incomeTaxOnPayPensionsProfitAtHRT = 0,

        payPensionsProfitAtART = 0,
        rateART = 45.00,
        incomeTaxOnPayPensionsProfitAtART = 0,

        nationalInsuranceClass2Amount=110,
        totalClass4Charge = 13.86,
        incomeTaxYTD = 149.86,

        //Don't need these
        incomeTaxThisPeriod = 2000.00,
        totalIncomeReceived = 9000.00,
        incomeTaxDue = 2072.05,

        eoyEstimate = Some(EoyEstimate(66000.00))
      )

    val busBropHRTCalcDataModel =
      CalculationDataModel(
        profitFromSelfEmployment = 30000.00,
        profitFromUkLandAndProperty = 7875.00,
        proportionAllowance = 2868.00,
        totalIncomeOnWhichTaxIsDue = 35007.00,

        payPensionsProfitAtBRT = 8352.00,
        rateBRT = 20.00,
        incomeTaxOnPayPensionsProfitAtBRT = 1670.00,

        payPensionsProfitAtHRT = 26654.00,
        rateHRT = 40.00,
        incomeTaxOnPayPensionsProfitAtHRT = 10661.00,

        payPensionsProfitAtART = 0,
        rateART = 45.00,
        incomeTaxOnPayPensionsProfitAtART = 0,

        nationalInsuranceClass2Amount=500.71,
        totalClass4Charge = 896.00,
        incomeTaxYTD = 13727.71,

        //Don't need these
        incomeTaxThisPeriod = 2000.00,
        totalIncomeReceived = 9000.00,
        incomeTaxDue = 2072.05
      )

    val busPropARTCalcDataModel =
      CalculationDataModel(
        profitFromSelfEmployment = 875.00,
        profitFromUkLandAndProperty = 40000.00,
        proportionAllowance = 2868.00,
        totalIncomeOnWhichTaxIsDue = 38007.00,

        payPensionsProfitAtBRT = 8352.00,
        rateBRT = 20.00,
        incomeTaxOnPayPensionsProfitAtBRT = 1670.00,

        payPensionsProfitAtHRT = 29044.00,
        rateHRT = 40.00,
        incomeTaxOnPayPensionsProfitAtHRT = 11617.00,

        payPensionsProfitAtART = 609.00,
        rateART = 45.00,
        incomeTaxOnPayPensionsProfitAtART = 274.00,

        nationalInsuranceClass2Amount=1000.00,
        totalClass4Charge = 456.71,
        incomeTaxYTD = 15017.71,

        //Don't need these
        incomeTaxThisPeriod = 2000.00,
        totalIncomeReceived = 9000.00,
        incomeTaxDue = 2072.05
      )

    val justBusinessCalcDataModel =
      CalculationDataModel(
        profitFromSelfEmployment = 3000.00,
        profitFromUkLandAndProperty = 0,
        proportionAllowance = 2868.00,
        totalIncomeOnWhichTaxIsDue = 132.00,

        payPensionsProfitAtBRT = 132.00,
        rateBRT = 20.00,
        incomeTaxOnPayPensionsProfitAtBRT = 26.00,

        payPensionsProfitAtHRT = 0,
        rateHRT = 40.00,
        incomeTaxOnPayPensionsProfitAtHRT = 0,

        payPensionsProfitAtART = 0,
        rateART = 45.00,
        incomeTaxOnPayPensionsProfitAtART = 0,

        nationalInsuranceClass2Amount=100.00,
        totalClass4Charge = 23.86,
        incomeTaxYTD = 149.86,

        //Don't need these
        incomeTaxThisPeriod = 2000.00,
        totalIncomeReceived = 9000.00,
        incomeTaxDue = 2072.05
      )

    val justPropertyCalcDataModel: CalculationDataModel =
      CalculationDataModel(
        profitFromSelfEmployment = 0,
        profitFromUkLandAndProperty = 3000.00,
        proportionAllowance = 2868.00,
        totalIncomeOnWhichTaxIsDue = 132.00,

        payPensionsProfitAtBRT = 132.00,
        rateBRT = 20.00,
        incomeTaxOnPayPensionsProfitAtBRT = 26.00,

        payPensionsProfitAtHRT = 0,
        rateHRT = 40.00,
        incomeTaxOnPayPensionsProfitAtHRT = 0,

        payPensionsProfitAtART = 0,
        rateART = 45.00,
        incomeTaxOnPayPensionsProfitAtART = 0,

        nationalInsuranceClass2Amount=100.00,
        totalClass4Charge = 23.86,
        incomeTaxYTD = 149.86,

        //Don't need these
        incomeTaxThisPeriod = 2000.00,
        totalIncomeReceived = 9000.00,
        incomeTaxDue = 2072.05
      )

    val mandatoryOnlyDataModel =
      CalculationDataModel(
        profitFromSelfEmployment = 0,
        profitFromUkLandAndProperty = 0,
        proportionAllowance = 0,
        totalIncomeOnWhichTaxIsDue = 0,
        payPensionsProfitAtBRT = 0,
        rateBRT = 0,
        incomeTaxOnPayPensionsProfitAtBRT = 0,
        payPensionsProfitAtHRT = 0,
        rateHRT = 0,
        incomeTaxOnPayPensionsProfitAtHRT = 0,
        payPensionsProfitAtART = 0,
        rateART = 0,
        incomeTaxOnPayPensionsProfitAtART = 0,
        nationalInsuranceClass2Amount = 0,
        totalClass4Charge = 0,
        incomeTaxYTD = 90500,
        incomeTaxThisPeriod = 2000,
        totalIncomeReceived = 0,
        incomeTaxDue = 0
      )

    val mandatoryCalculationDataSuccessString: String =
      """
        |{
        | "incomeTaxYTD": 90500,
        | "incomeTaxThisPeriod": 2000
        |}
      """.stripMargin

    val calculationDataSuccessString: String =
      """
        |{
        | "incomeTaxYTD": 90500,
        | "incomeTaxThisPeriod": 2000,
        | "profitFromSelfEmployment": 200000,
        | "profitFromUkLandAndProperty": 10000,
        | "totalIncomeReceived": 230000,
        | "proportionAllowance": 11500,
        | "totalIncomeOnWhichTaxIsDue": 198500,
        | "payPensionsProfitAtBRT": 20000,
        | "incomeTaxOnPayPensionsProfitAtBRT": 4000,
        | "payPensionsProfitAtHRT": 100000,
        | "incomeTaxOnPayPensionsProfitAtHRT": 40000,
        | "payPensionsProfitAtART": 50000,
        | "incomeTaxOnPayPensionsProfitAtART": 22500,
        | "incomeTaxDue": 66500,
        | "nationalInsuranceClass2Amount": 10000,
        | "totalClass4Charge": 14000,
        | "rateBRT": 20,
        | "rateHRT": 40,
        | "rateART": 45,
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
        | "payFromAllEmployments": 888,
        | "benefitsAndExpensesReceived": 888,
        | "allowableExpenses": 888,
        | "payFromAllEmploymentsAfterExpenses": 888,
        | "shareSchemes": 888,
        | "profitFromSelfEmployment": 200000,
        | "profitFromPartnerships": 888,
        | "profitFromUkLandAndProperty": 10000,
        | "dividendsFromForeignCompanies": 888,
        | "foreignIncome": 888,
        | "trustsAndEstates": 888,
        | "interestReceivedFromUkBanksAndBuildingSocieties": 888,
        | "dividendsFromUkCompanies": 888,
        | "ukPensionsAndStateBenefits": 888,
        | "gainsOnLifeInsurance": 888,
        | "otherIncome": 888,
        | "totalIncomeReceived": 230000,
        | "paymentsIntoARetirementAnnuity": 888,
        | "foreignTaxOnEstates": 888,
        | "incomeTaxRelief": 888,
        | "incomeTaxReliefReducedToMaximumAllowable": 888,
        | "annuities": 888,
        | "giftOfInvestmentsAndPropertyToCharity": 888,
        | "personalAllowance": 11500,
        | "marriageAllowanceTransfer": 888,
        | "blindPersonAllowance": 888,
        | "blindPersonSurplusAllowanceFromSpouse": 888,
        | "incomeExcluded": 888,
        | "totalIncomeAllowancesUsed": 888,
        | "totalIncomeOnWhichTaxIsDue": 198500,
        | "payPensionsExtender": 888,
        | "giftExtender": 888,
        | "extendedBR": 888,
        | "payPensionsProfitAtBRT": 20000,
        | "incomeTaxOnPayPensionsProfitAtBRT": 4000,
        | "payPensionsProfitAtHRT": 100000,
        | "incomeTaxOnPayPensionsProfitAtHRT": 40000,
        | "payPensionsProfitAtART": 50000,
        | "incomeTaxOnPayPensionsProfitAtART": 22500,
        | "netPropertyFinanceCosts": 888,
        | "interestReceivedAtStartingRate": 888,
        | "incomeTaxOnInterestReceivedAtStartingRate": 888,
        | "interestReceivedAtZeroRate": 888,
        | "incomeTaxOnInterestReceivedAtZeroRate": 888,
        | "interestReceivedAtBRT": 888,
        | "incomeTaxOnInterestReceivedAtBRT": 888,
        | "interestReceivedAtHRT": 888,
        | "incomeTaxOnInterestReceivedAtHRT": 888,
        | "interestReceivedAtART": 888,
        | "incomeTaxOnInterestReceivedAtART": 888,
        | "dividendsAtZeroRate": 888,
        | "incomeTaxOnDividendsAtZeroRate": 888,
        | "dividendsAtBRT": 888,
        | "incomeTaxOnDividendsAtBRT": 888,
        | "dividendsAtHRT": 888,
        | "incomeTaxOnDividendsAtHRT": 888,
        | "dividendsAtART": 888,
        | "incomeTaxOnDividendsAtART": 888,
        | "totalIncomeOnWhichTaxHasBeenCharged": 888,
        | "taxOnOtherIncome": 888,
        | "incomeTaxDue": 66500,
        | "incomeTaxCharged": 888,
        | "deficiencyRelief": 888,
        | "topSlicingRelief": 888,
        | "ventureCapitalTrustRelief": 888,
        | "enterpriseInvestmentSchemeRelief": 888,
        | "seedEnterpriseInvestmentSchemeRelief": 888,
        | "communityInvestmentTaxRelief": 888,
        | "socialInvestmentTaxRelief": 888,
        | "maintenanceAndAlimonyPaid": 888,
        | "marriedCouplesAllowance": 888,
        | "marriedCouplesAllowanceRelief": 888,
        | "surplusMarriedCouplesAllowance": 888,
        | "surplusMarriedCouplesAllowanceRelief": 888,
        | "notionalTaxFromLifePolicies": 888,
        | "notionalTaxFromDividendsAndOtherIncome": 888,
        | "foreignTaxCreditRelief": 888,
        | "incomeTaxDueAfterAllowancesAndReliefs": 888,
        | "giftAidPaymentsAmount": 888,
        | "giftAidTaxDue": 888,
        | "capitalGainsTaxDue": 888,
        | "remittanceForNonDomiciles": 888,
        | "highIncomeChildBenefitCharge": 888,
        | "totalGiftAidTaxReduced": 888,
        | "incomeTaxDueAfterGiftAidReduction": 888,
        | "annuityAmount": 888,
        | "taxDueOnAnnuity": 888,
        | "taxCreditsOnDividendsFromUkCompanies": 888,
        | "incomeTaxDueAfterDividendTaxCredits": 888,
        | "nationalInsuranceContributionAmount": 888,
        | "nationalInsuranceContributionCharge": 888,
        | "nationalInsuranceContributionSupAmount": 888,
        | "nationalInsuranceContributionSupCharge": 888,
        | "totalClass4Charge": 14000,
        | "nationalInsuranceClass1Amount": 888,
        | "nationalInsuranceClass2Amount": 10000,
        | "nicTotal": 24000,
        | "underpaidTaxForPreviousYears": 888,
        | "studentLoanRepayments": 888,
        | "pensionChargesGross": 888,
        | "pensionChargesTaxPaid": 888,
        | "totalPensionSavingCharges": 888,
        | "pensionLumpSumAmount": 888,
        | "pensionLumpSumRate": 888,
        | "statePensionLumpSumAmount": 888,
        | "remittanceBasisChargeForNonDomiciles": 888,
        | "additionalTaxDueOnPensions": 888,
        | "additionalTaxReliefDueOnPensions": 888,
        | "incomeTaxDueAfterPensionDeductions": 888,
        | "employmentsPensionsAndBenefits": 888,
        | "outstandingDebtCollectedThroughPaye": 888,
        | "payeTaxBalance": 888,
        | "cisAndTradingIncome": 888,
        | "partnerships": 888,
        | "ukLandAndPropertyTaxPaid": 888,
        | "foreignIncomeTaxPaid": 888,
        | "trustAndEstatesTaxPaid": 888,
        | "overseasIncomeTaxPaid": 888,
        | "interestReceivedTaxPaid": 888,
        | "voidISAs": 888,
        | "otherIncomeTaxPaid": 888,
        | "underpaidTaxForPriorYear": 888,
        | "totalTaxDeducted": 888,
        | "incomeTaxOverpaid": 888,
        | "incomeTaxDueAfterDeductions": 888,
        | "propertyFinanceTaxDeduction": 888,
        | "taxableCapitalGains": 888,
        | "capitalGainAtEntrepreneurRate": 888,
        | "incomeTaxOnCapitalGainAtEntrepreneurRate": 888,
        | "capitalGrainsAtLowerRate": 888,
        | "incomeTaxOnCapitalGainAtLowerRate": 888,
        | "capitalGainAtHigherRate": 888,
        | "incomeTaxOnCapitalGainAtHigherTax": 888,
        | "capitalGainsTaxAdjustment": 888,
        | "foreignTaxCreditReliefOnCapitalGains": 888,
        | "liabilityFromOffShoreTrusts": 888,
        | "taxOnGainsAlreadyCharged": 888,
        | "totalCapitalGainsTax": 888,
        | "incomeAndCapitalGainsTaxDue": 888,
        | "taxRefundedInYear": 888,
        | "unpaidTaxCalculatedForEarlierYears": 888,
        | "marriageAllowanceTransferAmount": 888,
        | "marriageAllowanceTransferRelief": 888,
        | "marriageAllowanceTransferMaximumAllowable": 888,
        | "nationalRegime": "888",
        | "allowance": 888,
        | "limitBRT": 888,
        | "limitHRT": 888,
        | "rateBRT": 20,
        | "rateHRT": 40,
        | "rateART": 45,
        | "limitAIA": 888,
        | "limitAIA": 888,
        | "allowanceBRT": 888,
        | "interestAllowanceHRT": 888,
        | "interestAllowanceBRT": 888,
        | "dividendAllowance": 888,
        | "dividendBRT": 888,
        | "dividendHRT": 888,
        | "dividendART": 888,
        | "class2NICsLimit": 888,
        | "class2NICsPerWeek": 888,
        | "class4NICsLimitBR": 888,
        | "class4NICsLimitHR": 888,
        | "class4NICsBRT": 888,
        | "class4NICsHRT": 888,
        | "proportionAllowance": 11500,
        | "proportionLimitBRT": 888,
        | "proportionLimitHRT": 888,
        | "proportionalTaxDue": 888,
        | "proportionInterestAllowanceBRT": 888,
        | "proportionInterestAllowanceHRT": 888,
        | "proportionDividendAllowance": 888,
        | "proportionPayPensionsProfitAtART": 888,
        | "proportionIncomeTaxOnPayPensionsProfitAtART": 888,
        | "proportionPayPensionsProfitAtBRT": 888,
        | "proportionIncomeTaxOnPayPensionsProfitAtBRT": 888,
        | "proportionPayPensionsProfitAtHRT": 888,
        | "proportionIncomeTaxOnPayPensionsProfitAtHRT": 888,
        | "proportionInterestReceivedAtZeroRate": 888,
        | "proportionIncomeTaxOnInterestReceivedAtZeroRate": 888,
        | "proportionInterestReceivedAtBRT": 888,
        | "proportionIncomeTaxOnInterestReceivedAtBRT": 888,
        | "proportionInterestReceivedAtHRT": 888,
        | "proportionIncomeTaxOnInterestReceivedAtHRT": 888,
        | "proportionInterestReceivedAtART": 888,
        | "proportionIncomeTaxOnInterestReceivedAtART": 888,
        | "proportionDividendsAtZeroRate": 888,
        | "proportionIncomeTaxOnDividendsAtZeroRate": 888,
        | "proportionDividendsAtBRT": 888,
        | "proportionIncomeTaxOnDividendsAtBRT": 888,
        | "proportionDividendsAtHRT": 888,
        | "proportionIncomeTaxOnDividendsAtHRT": 888,
        | "proportionDividendsAtART": 888,
        | "proportionIncomeTaxOnDividendsAtART": 888,
        | "proportionClass2NICsLimit": 888,
        | "proportionClass4NICsLimitBR": 888,
        | "proportionClass4NICsLimitHR": 888,
        | "proportionReducedAllowanceLimit": 888,
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