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

package hub.controllers

import hub.testConstants.HubIntegrationTestConstants.b2CessationDate
import common.auth.MtdItUser
import common.controllers.ControllerISpecHelper
import common.enums.MTDIndividual
import common.helpers.servicemocks.MTDIndividualAuthStub
import common.implicits.{ImplicitDateFormatter, ImplicitDateFormatterImpl}
import common.models.core.{AccountingPeriodModel, CessationModel}
import common.models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel}
import common.testConstants.BaseIntegrationTestConstants.*
import hub.testConstants.messages.HomeMessages.*
import hub.helpers.serviceMocks.PenaltyDetailsStub
import hub.helpers.NextUpdatesStub
import obligations.testConstants.NextUpdatesIntegrationTestConstants.*
import play.api.http.Status.*
import play.api.i18n.{Messages, MessagesApi}
import play.api.test.FakeRequest
import common.helpers.GetInsourceDetailsStub

class HomeControllerISpec extends ControllerISpecHelper {

  val implicitDateFormatter: ImplicitDateFormatter = app.injector.instanceOf[ImplicitDateFormatterImpl]
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())

  val incomeSourceDetailsModel: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    nino = testNino,
    mtdbsa = testMtditid,
    yearOfMigration = Some(getCurrentTaxYearEnd.getYear.toString),
    businesses = List(BusinessDetailsModel(
      "testId",
      incomeSource = Some(testIncomeSource),
      Some(AccountingPeriodModel(currentDate, currentDate.plusYears(1))),
      None,
      Some(getCurrentTaxYearEnd),
      Some(b2TradingStart),
      None,
      Some(CessationModel(Some(b2CessationDate))),
      address = Some(address)
    )),
    properties = Nil
  )

  val testUser: MtdItUser[_] = getTestUser(MTDIndividual, incomeSourceDetailsModel)

  val path = "/"


  "GET /" when {
    "an authenticated user" should {
      "render the home page" which {
        "retrieving the obligations was unsuccessful" in {
          MTDIndividualAuthStub.stubAuthorisedAndMTDEnrolled()

          GetInsourceDetailsStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
            status = OK,
            response = incomeSourceDetailsModel
          )

          NextUpdatesStub.stubGetNextUpdatesError(testNino)

          val result = buildGETMTDClient(path).futureValue

          result should have(
            httpStatus(INTERNAL_SERVER_ERROR),
            pageTitleInd(titleInternalServer, isErrorPage = true)
          )
        }
        "retrieving the income sources was unsuccessful" in {
          MTDIndividualAuthStub.stubAuthorisedAndMTDEnrolled()

          GetInsourceDetailsStub.stubGetIncomeSourceDetailsErrorResponse(testMtditid)(
            status = INTERNAL_SERVER_ERROR)

          PenaltyDetailsStub.stubNoPenaltiesDataResponse(testMtditid)

          val result = buildGETMTDClient(path).futureValue

          result should have(
            httpStatus(INTERNAL_SERVER_ERROR),
            pageTitleInd(titleInternalServer, isErrorPage = true)
          )
        }
      }

      testAuthFailures(path, MTDIndividual)
    }
  }
}
