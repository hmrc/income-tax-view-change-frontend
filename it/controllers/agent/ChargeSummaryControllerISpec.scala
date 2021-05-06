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
import config.featureswitch.{AgentViewer, FeatureSwitching, NewFinancialDetailsApi}
import controllers.agent.utils.SessionKeys
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.financialDetails.{DocumentDetail, FinancialDetail, FinancialDetailsModel, SubItem}
import play.api.http.Status._
import play.api.libs.json.Json

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

  s"GET ${routes.ChargeSummaryController.showChargeSummary(currentYear, "testId").url}" should {
    s"return $OK with correct page title" in {

      enable(AgentViewer)
      enable(NewFinancialDetailsApi)
      stubAuthorisedAgentUser(authorised = true)

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
              outstandingAmount = Some(500.00),
              originalAmount = Some(1000.00)
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

      result should have(
        httpStatus(OK),

        pageTitle("Payment on account 1 of 2 - Your client’s Income Tax details - GOV.UK")
      )
    }
  }
}
