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

package controllers.manageBusinesses.cease

import controllers.ControllerISpecHelper
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{Cease, IncomeSourceJourneyType}
import enums.{MTDIndividual, MTDUserRole}
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.UIJourneySessionData
import models.admin.NavBarFs
import models.core.NormalMode
import models.incomeSourceDetails.CeaseIncomeSourceData.ceaseIncomeSourceDeclare
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.SessionService
import testConstants.BaseIntegrationTestConstants.{testMtditid, testSelfEmploymentIdHashed, testSessionId}
import testConstants.IncomeSourceIntegrationTestConstants.multipleBusinessesAndPropertyResponse

class DeclareIncomeSourceCeasedControllerISpec extends ControllerISpecHelper {
  val sessionService: SessionService = app.injector.instanceOf[SessionService]

  val stringTrue: String = "true"

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(sessionService.deleteSession(Cease))
  }

  def getPath(mtdRole: MTDUserRole, incomeSourceType: IncomeSourceType): String = {
    val pathStart = if(mtdRole == MTDIndividual) "" else "/agents"
    incomeSourceType match {
      case SelfEmployment => pathStart + s"/manage-your-businesses/cease/business-confirm-cease?id=$testSelfEmploymentIdHashed"
      case UkProperty => pathStart + "/manage-your-businesses/cease/uk-property-confirm-cease"
      case _ => pathStart + "/manage-your-businesses/cease/foreign-property-confirm-cease"
    }
  }

  val incomeSourceTypes = List(SelfEmployment, UkProperty, ForeignProperty)

  mtdAllRoles.foreach { mtdUserRole =>
    incomeSourceTypes.foreach { incomeSourceType =>
      val path = getPath(mtdUserRole, incomeSourceType)
      val additionalCookies = getAdditionalCookies(mtdUserRole)
      s"GET $path" when {
        s"a user is a $mtdUserRole" that {
          "is authenticated, with a valid enrolment" should {
            "render the income source ceased Page" in {
              stubAuthorised(mtdUserRole)
              disable(NavBarFs)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

              val result = buildGETMTDClient(path, additionalCookies).futureValue
              IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

              val buttonLabel: String = messagesAPI(s"incomeSources.cease.${incomeSourceType.key}.continue")

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, s"incomeSources.cease.${incomeSourceType.key}.heading"),
                elementTextByID("confirm-button")(buttonLabel)
              )
            }
          }
          testAuthFailures(path, mtdUserRole)
        }
      }

      s"POST $path" when {
        val optIdHash = if(incomeSourceType == SelfEmployment) Some(testSelfEmploymentIdHashed) else None
        s"a user is a $mtdUserRole" that {
          "is authenticated, with a valid enrolment" should {
            "redirect to IncomeSourceEndDateControllerUrl" when {
              "continue is pressed correctly" in {
                stubAuthorised(mtdUserRole)
                disable(NavBarFs)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)
                await(sessionService.setMongoData(UIJourneySessionData(testSessionId, s"CEASE-${incomeSourceType.key}")))

                val result = buildPOSTMTDPostClient(path,
                  additionalCookies, body = Map()).futureValue

                val expectedRedirectUrl = controllers.manageBusinesses.cease.routes
                  .IncomeSourceEndDateController.show(optIdHash, incomeSourceType, mtdUserRole != MTDIndividual, NormalMode).url

                result should have(
                  httpStatus(SEE_OTHER),
                  redirectURI(expectedRedirectUrl)
                )
                sessionService.getMongoKey(ceaseIncomeSourceDeclare, IncomeSourceJourneyType(Cease, incomeSourceType)).futureValue shouldBe Right(Some(stringTrue))

              }
            }
          }
          testAuthFailures(path, mtdUserRole, optBody = Some(Map.empty))
        }
      }
    }
  }
}
