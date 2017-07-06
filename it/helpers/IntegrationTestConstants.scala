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

import models.{CalculationDataModel, ObligationModel, ObligationsModel}
import play.api.libs.json.{JsValue, Json}
import utils.ImplicitDateFormatter

object IntegrationTestConstants extends ImplicitDateFormatter {

  val testDate = "2018-05-05".toLocalDate

  val testMtditidEnrolmentKey = "HMRC-MTD-IT"
  val testMtditidEnrolmentIdentifier = "MTDITID"
  val testMtditid = "XAITSA123456"

  val testNinoEnrolmentKey = "HMRC-NI"
  val testNinoEnrolmentIdentifier = "NINO"
  val testNino = "AA123456A"
  val testCalcId = "01234567"

  val testYear = "2018"
  val testCalcType = "it"

  val testSelfEmploymentId = "ABC123456789"

  object GetLastCalculation {
    def successResponse(calcID: String, calcTimestamp: String, calcAmount: BigDecimal): JsValue =
      Json.parse(s"""
         |{
         |   "calcID": "$calcID",
         |   "calcTimestamp": "$calcTimestamp",
         |   "calcAmount": $calcAmount
         |}
         |""".stripMargin)

    def failureResponse(code: String, reason: String): JsValue =
      Json.parse(s"""
         |{
         |   "code": "$code",
         |   "reason":"$reason"
         |}
      """.stripMargin)
  }

  object GetCalculationData {
    def successResponse(incomeTaxYTD: BigDecimal,
                        incomeTaxThisPeriod: BigDecimal,
                        profitFromSelfEmployment: BigDecimal,
                        profitFromUkLandAndProperty: BigDecimal,
                        totalIncomeReceived: BigDecimal,
                        personalAllowance: BigDecimal,
                        totalIncomeOnWhichTaxIsDue: BigDecimal,
                        payPensionsProfitAtBRT: BigDecimal,
                        incomeTaxOnPayPensionsProfitAtBRT: BigDecimal,
                        payPensionsProfitAtHRT: BigDecimal,
                        incomeTaxOnPayPensionsProfitAtHRT: BigDecimal,
                        payPensionsProfitAtART: BigDecimal,
                        incomeTaxOnPayPensionsProfitAtART: BigDecimal,
                        incomeTaxDue: BigDecimal,
                        nicTotal: BigDecimal,
                        rateBRT: BigDecimal,
                        rateHRT: BigDecimal,
                        rateART: BigDecimal): JsValue ={
      Json.parse(s"""
        |{
        | "incomeTaxYTD": "$incomeTaxYTD",
        | "incomeTaxThisPeriod": "$incomeTaxThisPeriod",
        | "profitFromSelfEmployment": "$profitFromSelfEmployment",
        | "profitFromUkLandAndProperty": "$profitFromUkLandAndProperty",
        | "totalIncomeReceived": "$totalIncomeReceived",
        | "personalAllowance": "$personalAllowance",
        | "totalIncomeOnWhichTaxIsDue": "$totalIncomeOnWhichTaxIsDue",
        | "payPensionsProfitAtBRT": "$payPensionsProfitAtBRT",
        | "incomeTaxOnPayPensionsProfitAtBRT": "$incomeTaxOnPayPensionsProfitAtBRT",
        | "payPensionsProfitAtHRT": "$payPensionsProfitAtHRT",
        | "incomeTaxOnPayPensionsProfitAtHRT": "$incomeTaxOnPayPensionsProfitAtHRT",
        | "payPensionsProfitAtART": "$payPensionsProfitAtART",
        | "incomeTaxOnPayPensionsProfitAtART": "$incomeTaxOnPayPensionsProfitAtART",
        | "incomeTaxDue": "$incomeTaxDue",
        | "nicTotal": "$nicTotal",
        | "rateBRT": "$rateBRT",
        | "rateHRT": "$rateHRT",
        | "rateART": "$rateART"
        |}
        """.stripMargin)
    }

    val calculationDataSuccessModel = CalculationDataModel(incomeTaxYTD = Some(90500),
      incomeTaxThisPeriod = Some(2000),
      profitFromSelfEmployment = Some(200000),
      profitFromUkLandAndProperty = Some(10000),
      totalIncomeReceived = Some(230000),
      personalAllowance = Some(11500),
      totalIncomeOnWhichTaxIsDue = Some(198500),
      payPensionsProfitAtBRT = Some(20000),
      incomeTaxOnPayPensionsProfitAtBRT = Some(4000),
      payPensionsProfitAtHRT = Some(100000),
      incomeTaxOnPayPensionsProfitAtHRT = Some(40000),
      payPensionsProfitAtART = Some(50000),
      incomeTaxOnPayPensionsProfitAtART = Some(22500),
      incomeTaxDue = Some(66500),
      nicTotal = Some(24000),
      rateBRT = Some(20),
      rateHRT = Some(40),
      rateART = Some(45)
    )

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

  object GetObligationsData {
    def successResponse(obligationsModel: ObligationsModel): JsValue = {
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

    val multipleObligationsDataSuccessModel = ObligationsModel(List(
      ObligationModel(
        start = "2017-04-06",
        end = "2017-07-05",
        due = LocalDate.now(),
        met = true
      ), ObligationModel(
        start = "2017-07-06",
        end = "2017-10-05",
        due = LocalDate.now().plusDays(1),
        met = false
      ), ObligationModel(
        start = "2017-10-06",
        end = "2018-01-05",
        due = LocalDate.now().minusDays(1),
        met = false
      ))
    )

    val singleObligationsDataSuccessModel = ObligationsModel(List(
      ObligationModel(
        start = "2017-04-06",
        end = "2017-07-05",
        due = LocalDate.now(),
        met = true
      )
    ))

    val emptyModel = ObligationsModel(List())
  }
}
