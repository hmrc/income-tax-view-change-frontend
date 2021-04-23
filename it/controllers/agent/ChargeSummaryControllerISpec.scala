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

package controllers.agent

import assets.BaseIntegrationTestConstants._
import assets.IncomeSourceIntegrationTestConstants.{multipleBusinessesAndPropertyResponse, propertyOnlyResponse}
import audit.models.ChargeSummaryAudit
import auth.MtdItUser
import config.featureswitch.{AgentViewer, FeatureSwitching, NewFinancialDetailsApi}
import controllers.agent.utils.SessionKeys
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.{AuditStub, IncomeTaxViewChangeStub}
import models.financialDetails.{Charge, FinancialDetailsModel, SubItem}
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.test.FakeRequest

import java.time.LocalDate

class ChargeSummaryControllerISpec extends ComponentSpecBase with FeatureSwitching {

  val clientDetails: Map[String, String] = Map(
    SessionKeys.clientFirstName -> "Test",
    SessionKeys.clientLastName -> "User",
    SessionKeys.clientUTR -> "1234567890",
    SessionKeys.clientNino -> testNino,
    SessionKeys.clientMTDID -> testMtditid,
    SessionKeys.confirmedClient -> "true"
  )

  val getCurrentTaxYearEnd: LocalDate = {
    val currentDate: LocalDate = LocalDate.now
    if (currentDate.isBefore(LocalDate.of(currentDate.getYear, 4, 6))) LocalDate.of(currentDate.getYear, 4, 5)
    else LocalDate.of(currentDate.getYear + 1, 4, 5)
  }

  val currentYear: Int = LocalDate.now().getYear
  val testArn: String = "1"

  s"GET ${routes.ChargeSummaryController.showChargeSummary(currentYear, "testid").url}" should {
    s"return $OK with correct page title" in {

      enable(AgentViewer)
      enable(NewFinancialDetailsApi)
      stubAuthorisedAgentUser(authorised = true)

      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

      IncomeTaxViewChangeStub.stubGetFinancialDetailsResponse(
        nino = testNino,
        from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
        to = getCurrentTaxYearEnd.toString
      )(
        status = OK,
        response = Json.toJson(FinancialDetailsModel(
          financialDetails = List(
            Charge(
              taxYear = getCurrentTaxYearEnd.getYear.toString,
              transactionId = "testid",
              transactionDate = Some(getCurrentTaxYearEnd.toString),
              `type` = None,
              totalAmount = Some(1000.00),
              originalAmount = Some(1000.00),
              outstandingAmount = Some(500.00),
              clearedAmount = Some(500.00),
              chargeType = Some("POA1"),
              mainType = Some("SA Payment on Account 1"),
              items = Some(Seq(SubItem(None, None, None, None, None, None, None, Some(LocalDate.now.toString), None, None)))
            )
          )
        ))
      )

      val result = IncomeTaxViewChangeFrontend.getChargeSummary(
        getCurrentTaxYearEnd.getYear.toString, "testid", clientDetails
      )

      AuditStub.verifyAuditContains(ChargeSummaryAudit(
        MtdItUser(
          testMtditid, testNino, None,
          multipleBusinessesAndPropertyResponse, Some("1234567890"), None, Some("Agent"), Some(testArn)
        )(FakeRequest()),
        Charge("2022", "testId", Some("2022-04-05"), None, Some(1000), Some(1000), Some(500), Some(500), Some("POA1"),
          Some("SA Payment on Account 1"), Some(List(SubItem(None, None, None, None, None, None, None, Some(LocalDate.now.toString),
            None, None)))), agentReferenceNumber = Some(testArn)
      ).detail)

      result should have(
        httpStatus(OK),

        pageTitle("Payment on account 1 of 2 - Your client’s Income Tax details - GOV.UK")
      )
    }
  }
}
