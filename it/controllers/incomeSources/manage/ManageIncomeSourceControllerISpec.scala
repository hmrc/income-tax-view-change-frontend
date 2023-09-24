/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.incomeSources.manage

import audit.models.MangeIncomeSourcesAuditModel
import auth.MtdItUser
import config.featureswitch.IncomeSources
import enums.IncomeSourceJourney.SelfEmployment
import helpers.ComponentSpecBase
import helpers.servicemocks.{AuditStub, IncomeTaxViewChangeStub}
import models.incomeSourceDetails.IncomeSourceDetailsModel
import models.incomeSourceDetails.viewmodels.{CeasedBusinessDetailsViewModel, ViewBusinessDetailsViewModel, ViewPropertyDetailsViewModel}
import play.api.http.Status.OK
import play.api.test.FakeRequest
import testConstants.BaseIntegrationTestConstants.{credId, testMtditid, testNino, testSaUtr}
import testConstants.BusinessDetailsIntegrationTestConstants.{business1, business2, business3}
import testConstants.IncomeSourceIntegrationTestConstants.{foreignPropertyAndCeasedBusiness, multipleBusinessesAndUkProperty, multipleBusinessesWithBothPropertiesAndCeasedBusiness}
import testConstants.PropertyDetailsIntegrationTestConstants.{foreignProperty, ukProperty}
import uk.gov.hmrc.auth.core.AffinityGroup.Individual

class ManageIncomeSourceControllerISpec extends ComponentSpecBase {

  val showIndividualViewIncomeSourceControllerUrl: String = controllers.incomeSources.manage.routes.ManageIncomeSourceController.show(false).url
  val pageTitleMsgKey = "view-income-sources.heading"
  val soleTraderBusinessName1: String = "business"
  val soleTraderBusinessName2: String = "secondBusiness"
  val chooseMessage: String = messagesAPI("view-income-sources.choose")
  val startDateMessage: String = messagesAPI("view-income-sources.table-head.date-started")
  val ceasedDateMessage: String = messagesAPI("view-income-sources.table-head.date-ended")
  val businessNameMessage: String = messagesAPI("view-income-sources.table-head.business-name")
  val ukPropertyStartDate: String = "1 January 2017"
  val foreignPropertyStartDate: String = "1 January 2017"
  val ceasedBusinessMessage: String = messagesAPI("view-income-sources.ceased-businesses-h2")
  val ceasedBusinessName: String = "ceasedBusiness"

  s"calling GET ${showIndividualViewIncomeSourceControllerUrl}" should {
    "render the View Income Source page for an Individual" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with multiple businesses and a uk property")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndUkProperty)
        When(s"I call GET ${showIndividualViewIncomeSourceControllerUrl}")
        val res = IncomeTaxViewChangeFrontend.getManageIncomeSource
        verifyIncomeSourceDetailsCall(testMtditid)

        res should have(
          httpStatus(OK),
          pageTitleIndividual(pageTitleMsgKey),
          elementTextByID("table-head-business-name")(businessNameMessage),
          elementTextByID("table-row-trading-name-0")(soleTraderBusinessName1),
          elementTextByID("table-row-trading-name-1")(soleTraderBusinessName2),
          elementTextByID("view-link-business-1")(chooseMessage),
          elementTextByID("table-head-date-started-uk")(startDateMessage),
          elementTextByID("table-row-trading-start-date-uk")(ukPropertyStartDate)
        )
      }

      "User is authorised with different data" in {
        Given("I wiremock stub a successful Income Source Details response with a foreign property and a ceased business")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyAndCeasedBusiness)
        When(s"I call GET ${showIndividualViewIncomeSourceControllerUrl}")
        val res = IncomeTaxViewChangeFrontend.getManageIncomeSource
        verifyIncomeSourceDetailsCall(testMtditid)

        res should have(
          httpStatus(OK),
          pageTitleIndividual(pageTitleMsgKey),
          elementTextByID("ceased-businesses-heading")(ceasedBusinessMessage),
          elementTextByID("ceased-businesses-table-head-date-ended")(ceasedDateMessage),
          elementTextByID("ceased-business-table-row-trading-name-0")(ceasedBusinessName),
          elementTextByID("table-head-date-started-foreign")(startDateMessage),
          elementTextByID("table-row-trading-start-date-foreign")(foreignPropertyStartDate)
        )
      }
    }
    "return the audit event" when {
      "User is authorised" in {
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesWithBothPropertiesAndCeasedBusiness)
        IncomeTaxViewChangeFrontend.getManageIncomeSource
        verifyIncomeSourceDetailsCall(testMtditid)

        AuditStub.verifyAuditEvent(
          MangeIncomeSourcesAuditModel(
            soleTraderBusinesses = List(
              ViewBusinessDetailsViewModel(
                incomeSourceId = business1.incomeSourceId,
                tradingName = business1.tradingName,
                tradingStartDate = business1.tradingStartDate
              ),
              ViewBusinessDetailsViewModel(
                incomeSourceId = business2.incomeSourceId,
                tradingName = business2.tradingName,
                tradingStartDate = business2.tradingStartDate
              )
            ),
            ukProperty = Some(
              ViewPropertyDetailsViewModel(
                tradingStartDate = ukProperty.tradingStartDate
              )
            ),
            foreignProperty = Some(
              ViewPropertyDetailsViewModel(
                tradingStartDate = foreignProperty.tradingStartDate
              )
            ),
            ceasedBusinesses = List(
              CeasedBusinessDetailsViewModel(
                tradingName = business3.tradingName,
                incomeSourceType = SelfEmployment,
                tradingStartDate = business3.tradingStartDate,
                cessationDate = business3.cessation.flatMap(_.date).get
              )
            )
          )(
            MtdItUser(
              mtditid = testMtditid,
              nino = testNino,
              userName = None,
              incomeSources = IncomeSourceDetailsModel(
                mtdbsa = testMtditid,
                yearOfMigration = None,
                businesses = List(business1, business2, business3),
                properties = List(ukProperty, foreignProperty)
              ),
              btaNavPartial = None,
              saUtr = Some(testSaUtr),
              credId = Some(credId),
              userType = Some(Individual),
              arn = None
            )(FakeRequest())
          )
        )
      }
    }
  }
}