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

import java.time.LocalDate
import assets.BaseIntegrationTestConstants._
import assets.IncomeSourceIntegrationTestConstants.{multipleBusinessesAndPropertyResponse, propertyOnlyResponse}
import audit.models.ChargeSummaryAudit
import auth.MtdItUser
import config.featureswitch.{AgentViewer, FeatureSwitching, NewFinancialDetailsApi, TxmEventsApproved}
import controllers.agent.utils.SessionKeys
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.DocumentDetailsStub.docDateDetail
import helpers.servicemocks.{AuditStub, IncomeTaxViewChangeStub}
import models.financialDetails.{DocumentDetail, FinancialDetail, FinancialDetailsModel, SubItem}
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.test.FakeRequest

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

  s"GET ${routes.ChargeSummaryController.showChargeSummary(currentYear, "testId").url}" should {
    s"return $OK with correct page title and audit events when TxEventsApproved FS is enabled" in {

      enable(AgentViewer)
      enable(NewFinancialDetailsApi)
      enable(TxmEventsApproved)
      stubAuthorisedAgentUser(authorised = true)

      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

      IncomeTaxViewChangeStub.stubGetFinancialDetailsResponse(
        nino = testNino,
        from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
        to = getCurrentTaxYearEnd.toString
      )(
        status = OK,
        response = Json.toJson(FinancialDetailsModel(
          documentDetails = List(
            DocumentDetail(
              taxYear = getCurrentTaxYearEnd.getYear.toString,
              transactionId = "testId",
              documentDescription = Some("ITSA- POA 1"),
              outstandingAmount = Some(1.2),
              originalAmount = Some(10.34),
              documentDate = "2018-03-29"
            )
          ),
          financialDetails = List(
            FinancialDetail(
              taxYear = getCurrentTaxYearEnd.getYear.toString,
              mainType = Some("SA Payment on Account 1"),
              items = Some(Seq(SubItem(Some(LocalDate.now.toString))))
            )
          )
        ))
      )

      val result = IncomeTaxViewChangeFrontend.getChargeSummary(
        getCurrentTaxYearEnd.getYear.toString, "testId", clientDetails
      )

      AuditStub.verifyAuditContainsDetail(ChargeSummaryAudit(
        MtdItUser(
          testMtditid, testNino, None,
          multipleBusinessesAndPropertyResponse, Some("1234567890"), None, Some("Agent"), Some(testArn)
        )(FakeRequest()),
        docDateDetail(LocalDate.now().toString, "ITSA- POA 1"),
        agentReferenceNumber = Some("1")
      ).detail)

      result should have(
        httpStatus(OK),
        pageTitle("Payment on account 1 of 2 - Your client’s Income Tax details - GOV.UK")
      )
    }
    s"return $OK with correct page title and audit events when TxEventsApproved FS is disabled" in {

      enable(AgentViewer)
      enable(NewFinancialDetailsApi)
      disable(TxmEventsApproved)
      stubAuthorisedAgentUser(authorised = true)

      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

      IncomeTaxViewChangeStub.stubGetFinancialDetailsResponse(
        nino = testNino,
        from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
        to = getCurrentTaxYearEnd.toString
      )(
        status = OK,
        response = Json.toJson(FinancialDetailsModel(
          documentDetails = List(
            DocumentDetail(
              taxYear = getCurrentTaxYearEnd.getYear.toString,
              transactionId = "testId",
              documentDescription = Some("ITSA- POA 1"),
              outstandingAmount = Some(1.2),
              originalAmount = Some(10.34),
              documentDate = "2018-03-29"
            )
          ),
          financialDetails = List(
            FinancialDetail(
              taxYear = getCurrentTaxYearEnd.getYear.toString,
              mainType = Some("SA Payment on Account 1"),
              items = Some(Seq(SubItem(Some(LocalDate.now.toString))))
            )
          )
        ))
      )

      val result = IncomeTaxViewChangeFrontend.getChargeSummary(
        getCurrentTaxYearEnd.getYear.toString, "testId", clientDetails
      )

      AuditStub.verifyAuditDoesNotContainsDetail(ChargeSummaryAudit(
        MtdItUser(
          testMtditid, testNino, None,
          multipleBusinessesAndPropertyResponse, Some("1234567890"), None, Some("Agent"), Some(testArn)
        )(FakeRequest()),
        docDateDetail(LocalDate.now().toString, "ITSA- POA 1"),
        agentReferenceNumber = Some("1")
      ).detail)

      result should have(
        httpStatus(OK),
        pageTitle("Payment on account 1 of 2 - Your client’s Income Tax details - GOV.UK")
      )
    }
  }
}
