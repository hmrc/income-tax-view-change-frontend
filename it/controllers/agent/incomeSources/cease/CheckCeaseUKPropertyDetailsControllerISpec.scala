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

package controllers.agent.incomeSources.cease

import config.featureswitch.IncomeSources
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.updateIncomeSource.{Cessation, UpdateIncomeSourceRequestModel, UpdateIncomeSourceResponseModel}
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.json.Json
import testConstants.BaseIntegrationTestConstants.{clientDetailsWithConfirmation, testMtditid, testNino}
import testConstants.IncomeSourceIntegrationTestConstants.{businessAndPropertyResponse, ukPropertyOnlyResponse}
import testConstants.PropertyDetailsIntegrationTestConstants.ukProperty

import java.time.LocalDate

class CheckCeaseUKPropertyDetailsControllerISpec extends ComponentSpecBase {
  val cessationDate = "2022-04-23"
  val sessionCeaseUKPropertyEndDate = Map(forms.utils.SessionKeys.ceaseUKPropertyEndDate -> cessationDate)
  val showCheckCeaseUKPropertyDetailsControllerUrl = controllers.incomeSources.cease.routes.CheckCeaseUKPropertyDetailsController.showAgent().url
  val submitCheckCeaseUKPropertyDetailsControllerUrl = controllers.incomeSources.cease.routes.CheckCeaseUKPropertyDetailsController.submitAgent().url
  val formAction = controllers.incomeSources.cease.routes.CheckCeaseUKPropertyDetailsController.submitAgent().url
  val businessEndShortLongDate = "23 Apr 2022"
  val businessStopDateLabel = messagesAPI("incomeSources.ceaseUKProperty.checkDetails.content")
  val pageTitleMsgKey = messagesAPI("incomeSources.ceaseUKProperty.checkDetails.heading")
  val timestamp = "2023-01-31T09:26:17Z"
  val redirectUri = controllers.incomeSources.cease.routes.CeaseUKPropertySuccessController.showAgent().url
  val request: UpdateIncomeSourceRequestModel = UpdateIncomeSourceRequestModel(
    nino = testNino,
    incomeSourceId = ukProperty.incomeSourceId,
    cessation = Some(Cessation(true, Some(LocalDate.parse(cessationDate))))
  )

  s"calling GET ${showCheckCeaseUKPropertyDetailsControllerUrl}" should {
    "render the Cease UK Property Page" when {
      "User is authorised" in {
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          status = OK,
          response = businessAndPropertyResponse
        )

        val res = IncomeTaxViewChangeFrontend.getCheckCeaseUKPropertyDetails(sessionCeaseUKPropertyEndDate ++ clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)

        res should have(
          httpStatus(OK),
          pageTitleAgent(pageTitleMsgKey),
          elementTextByID("businessStopDate")(businessEndShortLongDate),
          elementTextByID("businessStopDateLabel")(businessStopDateLabel),
          elementAttributeBySelector("form", "action")(formAction)
        )
      }
    }
  }

  s"calling POST ${showCheckCeaseUKPropertyDetailsControllerUrl}" should {
    s"redirect to $redirectUri" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with multiple business and property")
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true )
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)
        IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))
        When(s"I call POST ${submitCheckCeaseUKPropertyDetailsControllerUrl}")

        val res = IncomeTaxViewChangeFrontend.postCheckCeaseUKPropertyDetails(sessionCeaseUKPropertyEndDate ++ clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)
        IncomeTaxViewChangeStub.verifyUpdateIncomeSource(Some(Json.toJson(request).toString()))

        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(redirectUri),
        )
      }
    }
  }


}
