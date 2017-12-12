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

package helpers

import java.time.LocalDate

import models._
import play.api.libs.json.{JsArray, JsValue, Json}
import utils.ImplicitDateFormatter

object IntegrationTestConstants extends ImplicitDateFormatter {

  val testDate = "2018-05-05".toLocalDate

  val testMtditidEnrolmentKey = "HMRC-MTD-IT"
  val testMtditidEnrolmentIdentifier = "MTDITID"
  val testMtditid = "XAITSA123456"
  val testUserName = "Albert Einstein"

  val testNinoEnrolmentKey = "HMRC-NI"
  val testNinoEnrolmentIdentifier = "NINO"
  val testNino = "AA123456A"
  val testCalcId = "01234567"

  val testYear = "2018"
  val testCalcType = "it"

  val testSelfEmploymentId = "ABC123456789"
  val otherTestSelfEmploymentId = "ABC123456780"

  object GetCalculationData {

    val calculationDataSuccessWithEoYModel = CalculationDataModel(
      incomeTaxYTD = 90500,
      incomeTaxThisPeriod = 2000,
      profitFromSelfEmployment = 200000,
      profitFromUkLandAndProperty = 10000,
      totalIncomeReceived = 230000,
      proportionAllowance = 11500,
      totalIncomeOnWhichTaxIsDue = 198500,
      payPensionsProfitAtBRT = 20000.00,
      incomeTaxOnPayPensionsProfitAtBRT = 4000,
      payPensionsProfitAtHRT = 100000.00,
      incomeTaxOnPayPensionsProfitAtHRT = 40000,
      payPensionsProfitAtART = 50000.00,
      incomeTaxOnPayPensionsProfitAtART = 22500,
      incomeTaxDue = 66500,
      nationalInsuranceClass2Amount = 14000,
      totalClass4Charge = 10000,
      rateBRT = 20,
      rateHRT = 40,
      rateART = 45,
      eoyEstimate = Some(EoyEstimate(25000))
    )

    val calculationDataSuccessModel = CalculationDataModel(
      incomeTaxYTD = 90500,
      incomeTaxThisPeriod = 2000,
      profitFromSelfEmployment = 200000,
      profitFromUkLandAndProperty = 10000,
      totalIncomeReceived = 230000,
      proportionAllowance = 11500,
      totalIncomeOnWhichTaxIsDue = 198500,
      payPensionsProfitAtBRT = 20000.00,
      incomeTaxOnPayPensionsProfitAtBRT = 4000,
      payPensionsProfitAtHRT = 100000.00,
      incomeTaxOnPayPensionsProfitAtHRT = 40000,
      payPensionsProfitAtART = 50000.00,
      incomeTaxOnPayPensionsProfitAtART = 22500,
      incomeTaxDue = 66500,
      nationalInsuranceClass2Amount = 14000,
      totalClass4Charge = 10000,
      rateBRT = 20,
      rateHRT = 40,
      rateART = 45,
      eoyEstimate = None
    )

    val calculationDataErrorModel = CalculationDataErrorModel(code = 500, message = "Calculation Error Model Response")

  }

  object GetBusinessDetails {
    def successResponse(selfEmploymentId: String): JsValue =
      Json.parse(
        s"""
          [
            {
              "id": "$selfEmploymentId",
              "accountingPeriod":{"start":"2017-01-01","end":"2017-12-31"},
              "accountingType":"CASH",
              "commencementDate":"2017-01-01",
              "cessationDate":"2017-12-31",
              "tradingName":"business",
              "businessDescription":"a business",
              "businessAddressLineOne":"64 Zoo Lane",
              "businessAddressLineTwo":"Happy Place",
              "businessAddressLineThree":"Magical Land",
              "businessAddressLineFour":"England",
              "businessPostcode":"ZL1 064"
            }
          ]
          """.stripMargin)

    def otherSuccessResponse(selfEmploymentId: String): JsValue =
      Json.parse(
        s"""
          [
            {
              "id": "$selfEmploymentId",
              "accountingPeriod":{"start":"2018-01-01","end":"2018-12-31"},
              "accountingType":"CASH",
              "commencementDate":"2018-01-01",
              "cessationDate":"2018-12-31",
              "tradingName":"business",
              "businessDescription":"a business",
              "businessAddressLineOne":"64 Zoo Lane",
              "businessAddressLineTwo":"Happy Place",
              "businessAddressLineThree":"Magical Land",
              "businessAddressLineFour":"England",
              "businessPostcode":"ZL1 064"
            }
          ]
          """.stripMargin)

    def multipleSuccessResponse(id1: String, id2: String): JsValue =
      Json.parse(
        s"""
          [
            {
              "id": "$id1",
              "accountingPeriod":{"start":"2017-01-01","end":"2017-12-31"},
              "accountingType":"CASH",
              "commencementDate":"2017-01-01",
              "cessationDate":"2017-12-31",
              "tradingName":"firstBusiness",
              "businessDescription":"a first business",
              "businessAddressLineOne":"64 Zoo Lane",
              "businessAddressLineTwo":"Happy Place",
              "businessAddressLineThree":"Magical Land",
              "businessAddressLineFour":"England",
              "businessPostcode":"ZL1 064"
            },
            {
              "id": "$id2",
              "accountingPeriod":{"start":"2017-01-01","end":"2017-12-31"},
              "accountingType":"CASH",
              "commencementDate":"2017-01-01",
              "cessationDate":"2017-12-31",
              "tradingName":"secondBusiness",
              "businessDescription":"a second business",
              "businessAddressLineOne":"742 Evergreen Terrace",
              "businessAddressLineTwo":"Springfield",
              "businessAddressLineThree":"Oregon",
              "businessAddressLineFour":"USA",
              "businessPostcode":"51MP 50N5"
            }
          ]
          """.stripMargin)

    def emptyBusinessDetailsResponse(): JsValue = JsArray()

    def failureResponse(code: String, reason: String): JsValue =
      Json.parse(s"""
                    |{
                    |   "code": "$code",
                    |   "reason":"$reason"
                    |}
      """.stripMargin)
  }

  object GetPropertyDetails {
    def successResponse(): JsValue =
      Json.parse(
        s"""{}"""
      )
  }

  object GetReportDeadlinesData {
    def successResponse(obligationsModel: ReportDeadlinesModel): JsValue = {
      Json.toJson(obligationsModel)
    }

    def emptyResponse(): JsValue = {
      Json.parse(
        """[]"""
      )
    }

    def failureResponse(code: String, reason: String): JsValue =
      Json.parse(
        s"""
           |{
           |  "code": $code,
           |  "reason": $reason
           |}
         """.stripMargin)

    val multipleReportDeadlinesDataSuccessModel = ReportDeadlinesModel(List(
      ReportDeadlineModel(
        start = "2017-04-06",
        end = "2017-07-05",
        due = LocalDate.now(),
        met = true
      ), ReportDeadlineModel(
        start = "2017-07-06",
        end = "2017-10-05",
        due = LocalDate.now().plusDays(1),
        met = false
      ), ReportDeadlineModel(
        start = "2017-10-06",
        end = "2018-01-05",
        due = LocalDate.now().minusDays(1),
        met = false
      ))
    )

    val multipleReceivedOpenReportDeadlinesModel = ReportDeadlinesModel(List(
      ReportDeadlineModel(
        start = "2016-04-01",
        end = "2016-06-30",
        due = "2016-07-31",
        met = true
      ), ReportDeadlineModel(
        start = "2016-07-01",
        end = "2016-09-30",
        due = LocalDate.now().minusDays(309),
        met = true
      ), ReportDeadlineModel(
        start = "2016-10-01",
        end = "2016-12-31",
        due = LocalDate.now().minusDays(217),
        met = true
      ), ReportDeadlineModel(
        start = "2017-01-01",
        end = "2017-03-31",
        due = LocalDate.now().minusDays(128),
        met = false
      ), ReportDeadlineModel(
        start = "2017-04-01",
        end = "2017-06-30",
        due = LocalDate.now().minusDays(36),
        met = false
      ), ReportDeadlineModel(
        start = "2017-07-01",
        end = "2017-09-30",
        due = LocalDate.now().plusDays(30),
        met = false
      ),ReportDeadlineModel(
        start = "2017-10-01",
        end = "2018-01-31",
        due = LocalDate.now().plusDays(146),
        met = false),
      ReportDeadlineModel(
        start = "2017-11-01",
        end = "2018-02-01",
        due = LocalDate.now().plusDays(174),
        met = false)
    ))

    val singleReportDeadlinesDataSuccessModel = ReportDeadlinesModel(List(
      ReportDeadlineModel(
        start = "2017-04-06",
        end = "2017-07-05",
        due = LocalDate.now(),
        met = true
      )
    ))

    val otherReportDeadlinesDataSuccessModel = ReportDeadlinesModel(List(
      ReportDeadlineModel(
        start = "2017-04-06",
        end = "2017-07-05",
        due = LocalDate.now().minusDays(1),
        met = true
      )
    ))

    val singleObligationOverdueModel = ReportDeadlinesModel(List(
      ReportDeadlineModel(
        start = "2017-04-06",
        end = "2017-07-05",
        due = LocalDate.now().minusDays(1),
        met = false
      )
    ))

    val singleObligationPlusYearOpenModel = ReportDeadlinesModel(List(ReportDeadlineModel(
        start = "2017-04-06",
        end = "2017-07-05",
        due = LocalDate.now().plusYears(1),
        met = false
      )
    ))



    val emptyModel = ReportDeadlinesModel(List())
  }
}
