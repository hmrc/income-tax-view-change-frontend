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

package controllers.incomeSources.cease

import controllers.ControllerISpecHelper
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, UkProperty}
import enums.JourneyType.{Cease, IncomeSourceJourneyType}
import enums.{MTDIndividual, MTDUserRole}
import forms.incomeSources.cease.DeclareIncomeSourceCeasedForm
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.admin.{IncomeSourcesFs, NavBarFs}
import models.incomeSourceDetails.CeaseIncomeSourceData.ceaseIncomeSourceDeclare
import models.incomeSourceDetails.UIJourneySessionData
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.SessionService
import testConstants.BaseIntegrationTestConstants.{testMtditid, testSessionId}
import testConstants.IncomeSourceIntegrationTestConstants.multipleBusinessesAndPropertyResponse

class DeclarePropertyCeasedControllerISpec extends ControllerISpecHelper {

  val sessionService: SessionService = app.injector.instanceOf[SessionService]

  val buttonLabel: String = messagesAPI("base.continue")
  val stringTrue: String = "true"

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(sessionService.deleteSession(Cease))
  }

  def getPath(mtdRole: MTDUserRole, incomeSourceType: IncomeSourceType): String = {
    val pathStart = if(mtdRole == MTDIndividual) "" else "/agents"
    incomeSourceType match {
      case UkProperty => pathStart + "/income-sources/cease/uk-property-declare"
      case _ => pathStart + "/income-sources/cease/foreign-property-declare"
    }
  }

  mtdAllRoles.foreach { mtdUserRole =>
    List(UkProperty, ForeignProperty).foreach { incomeSourceType =>
      val path = getPath(mtdUserRole, incomeSourceType)
      val additionalCookies = getAdditionalCookies(mtdUserRole)
      s"GET $path" when {
        s"a user is a $mtdUserRole" that {
          "is authenticated, with a valid enrolment" should {
            "render the Cease Property Page" in {
              stubAuthorised(mtdUserRole)
              disable(NavBarFs)
              enable(IncomeSourcesFs)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)
              val result = buildGETMTDClient(path, additionalCookies).futureValue
              IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

              val checkboxLabelMessage: String = messagesAPI(s"incomeSources.cease.${incomeSourceType.key}.checkboxLabel")

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, s"incomeSources.cease.${incomeSourceType.key}.heading"),
                elementTextBySelector("label")(checkboxLabelMessage),
                elementTextByID("continue-button")(buttonLabel)
              )
            }
          }
          testAuthFailures(path, mtdUserRole)
        }
      }

      s"POST $path" when {
        s"a user is a $mtdUserRole" that {
          "is authenticated, with a valid enrolment" should {
            "redirect to IncomeSourceEndDateControllerUrl" when {
              "form is filled correctly" in {
                stubAuthorised(mtdUserRole)
                disable(NavBarFs)
                enable(IncomeSourcesFs)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)
                await(sessionService.setMongoData(UIJourneySessionData(testSessionId, s"CEASE-${incomeSourceType.key}")))

                val result = buildPOSTMTDPostClient(path,
                  additionalCookies,
                  body = DeclareIncomeSourceCeasedForm(Some("true"), "csrfToken").toFormMap).futureValue

                val expectedRedirectUrl = if(mtdUserRole == MTDIndividual) {
                  controllers.incomeSources.cease.routes.IncomeSourceEndDateController.show(None, incomeSourceType).url
                } else {
                  controllers.incomeSources.cease.routes.IncomeSourceEndDateController.showAgent(None, incomeSourceType).url
                }

                result should have(
                  httpStatus(SEE_OTHER),
                  redirectURI(expectedRedirectUrl)
                )
                sessionService.getMongoKey(ceaseIncomeSourceDeclare, IncomeSourceJourneyType(Cease, incomeSourceType)).futureValue shouldBe Right(Some(stringTrue))

              }
            }
            "return a BadRequest" when {
              "form is filled incorrectly" in {
                stubAuthorised(mtdUserRole)
                disable(NavBarFs)
                enable(IncomeSourcesFs)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

                await(sessionService.setMongoData(UIJourneySessionData(testSessionId, s"CEASE-${incomeSourceType.key}")))

                val result = buildPOSTMTDPostClient(path,
                  additionalCookies,
                  body = Map.empty).futureValue

                val checkboxErrorMessage: String = messagesAPI(s"incomeSources.cease.${incomeSourceType.key}.checkboxError")

                result should have(
                  httpStatus(BAD_REQUEST),
                  elementTextByID("cease-income-source-declaration-error")(messagesAPI("base.error-prefix") + " " + checkboxErrorMessage)
                )
              }
            }
          }
          testAuthFailures(path, mtdUserRole, optBody = Some(Map.empty))
        }
      }
    }
  }
}
