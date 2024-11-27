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
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{Cease, JourneyType}
import enums.{MTDIndividual, MTDUserRole}
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.admin.{IncomeSources, NavBarFs}
import models.incomeSourceDetails.CeaseIncomeSourceData.{dateCeasedField, incomeSourceIdField}
import models.incomeSourceDetails.{CeaseIncomeSourceData, UIJourneySessionData}
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.SessionService
import testConstants.BaseIntegrationTestConstants._
import testConstants.IncomeSourceIntegrationTestConstants.{businessOnlyResponse, foreignPropertyOnlyResponse, ukPropertyOnlyResponse}

import java.time.LocalDate

class IncomeSourceEndDateControllerISpec extends ControllerISpecHelper {

  val sessionService: SessionService = app.injector.instanceOf[SessionService]

  val hintText: String = messagesAPI("dateForm.hint")
  val continueButtonText: String = messagesAPI("base.continue")
  val testChangeDay: String = "10"
  val testChangeMonth: String = "10"
  val testChangeYear: String = "2022"

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(sessionService.deleteSession(Cease))
  }

  def getPath(mtdRole: MTDUserRole, incomeSourceType: IncomeSourceType, isChange: Boolean): String = {
    val pathStart = if(mtdRole == MTDIndividual) "/income-sources/cease" else "/agents/income-sources/cease"
    val changeIfReq = if(isChange) "/change-" else "/"
    val endPath = incomeSourceType match {
      case SelfEmployment => s"business-end-date?id=$testSelfEmploymentIdHashed"
      case UkProperty =>  "uk-property-end-date"
      case _ => "foreign-property-end-date"
    }
    pathStart + changeIfReq + endPath
  }

  def getIncomeSourceResponse(incomeSourceType: IncomeSourceType) = incomeSourceType match {
    case SelfEmployment => businessOnlyResponse
    case UkProperty => ukPropertyOnlyResponse
    case ForeignProperty => foreignPropertyOnlyResponse
  }

  def setupTestMongoData(incomeSourceType: IncomeSourceType) = {
    val incomeSourceId = incomeSourceType match {
      case SelfEmployment => testSelfEmploymentId
      case _ => testPropertyIncomeId
    }
    await(sessionService.setMongoData(UIJourneySessionData(testSessionId, s"CEASE-${incomeSourceType.key}", ceaseIncomeSourceData =
      Some(CeaseIncomeSourceData(incomeSourceId = Some(incomeSourceId), endDate = Some(LocalDate.parse(testEndDate2022)), ceaseIncomeSourceDeclare = Some(stringTrue), journeyIsComplete = Some(false))))))
  }

  mtdAllRoles.foreach { mtdUserRole =>
    List(false, true).foreach { isChange =>
      List(SelfEmployment, UkProperty, ForeignProperty).foreach { incomeSourceType =>
        val path = getPath(mtdUserRole, incomeSourceType, isChange)
        val additionalCookies = getAdditionalCookies(mtdUserRole)
        s"GET $path" when {
          s"a user is a $mtdUserRole" that {
            "is authenticated, with a valid enrolment" should {
              "render the Date Business Ceased Page" in {
                stubAuthorised(mtdUserRole)
                disable(NavBarFs)
                enable(IncomeSources)

                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, getIncomeSourceResponse(incomeSourceType))

                if(isChange) {
                  setupTestMongoData(incomeSourceType)
                }

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                verifyIncomeSourceDetailsCall(testMtditid)

                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, s"incomeSources.cease.endDate.${incomeSourceType.messagesCamel}.heading"),
                  elementTextByID("value-hint")(hintText),
                  elementTextByID("continue-button")(continueButtonText)
                )
                if (isChange) {
                  result should have(
                    elementAttributeBySelector("input[id=value.day]", "value")(testChangeDay),
                    elementAttributeBySelector("input[id=value.month]", "value")(testChangeMonth),
                    elementAttributeBySelector("input[id=value.year]", "value")(testChangeYear)
                  )
                }
              }
            }
            testAuthFailures(path, mtdUserRole)
          }
        }

        s"POST $path" when {
          s"a user is a $mtdUserRole" that {
            "is authenticated, with a valid enrolment" should {
              s"redirect to Check income source details" when {
                "form is filled correctly" in {
                  val formData: Map[String, Seq[String]] = {
                    Map("value.day" -> Seq("10"), "value.month" -> Seq("10"), "value.year" -> Seq("2022"))
                  }
                  stubAuthorised(mtdUserRole)
                  disable(NavBarFs)
                  enable(IncomeSources)
                  IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, getIncomeSourceResponse(incomeSourceType))

                  setupTestMongoData(incomeSourceType)

                  val result = buildPOSTMTDPostClient(path, additionalCookies, formData).futureValue

                  val expectedUrl = if (mtdUserRole == MTDIndividual) {
                    controllers.incomeSources.cease.routes.CeaseCheckIncomeSourceDetailsController.show(incomeSourceType).url
                  } else {
                    controllers.incomeSources.cease.routes.CeaseCheckIncomeSourceDetailsController.showAgent(incomeSourceType).url
                  }

                  result should have(
                    httpStatus(SEE_OTHER),
                    redirectURI(expectedUrl)
                  )
                  sessionService.getMongoKey(dateCeasedField, JourneyType(Cease, incomeSourceType)).futureValue shouldBe Right(Some(LocalDate.parse(testEndDate2022)))
                  if (incomeSourceType == SelfEmployment) {
                    sessionService.getMongoKey(incomeSourceIdField, JourneyType(Cease, incomeSourceType)).futureValue shouldBe Right(Some(testSelfEmploymentId))
                  }
                }
              }

              "return a BadRequest" when {
                "form is filled incorrectly" in {
                  val formData: Map[String, Seq[String]] = {
                    Map("value.day" -> Seq("aa"), "value.month" -> Seq("5"), "value.year" -> Seq("2022"))
                  }
                  stubAuthorised(mtdUserRole)
                  disable(NavBarFs)
                  enable(IncomeSources)
                  IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, getIncomeSourceResponse(incomeSourceType))

                  val result = buildPOSTMTDPostClient(path, additionalCookies, formData).futureValue

                  result should have(
                    httpStatus(BAD_REQUEST),
                    elementTextByID("value-error")(messagesAPI("base.error-prefix") + ": " +
                      messagesAPI(s"incomeSources.cease.endDate.${incomeSourceType.messagesCamel}.error.invalid"))
                  )

                  sessionService.getMongoKey(dateCeasedField, JourneyType(Cease, incomeSourceType)).futureValue shouldBe Right(None)
                  sessionService.getMongoKey(incomeSourceIdField, JourneyType(Cease, incomeSourceType)).futureValue shouldBe Right(None)

                }
              }
            }
            testAuthFailures(path, mtdUserRole, optBody = Some(Map.empty))
          }
        }
      }
    }
  }
}

