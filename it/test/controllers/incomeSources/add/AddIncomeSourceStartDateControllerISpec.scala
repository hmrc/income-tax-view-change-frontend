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

package controllers.incomeSources.add

import controllers.ControllerISpecHelper
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{Add, JourneyType}
import enums.{MTDIndividual, MTDUserRole}
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.admin.{IncomeSources, NavBarFs}
import models.incomeSourceDetails.AddIncomeSourceData.dateStartedField
import models.incomeSourceDetails.{AddIncomeSourceData, UIJourneySessionData}
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.SessionService
import testConstants.BaseIntegrationTestConstants.{testMtditid, testSessionId}
import testConstants.IncomeSourceIntegrationTestConstants.{businessOnlyResponse, noPropertyOrBusinessResponse, ukPropertyOnlyResponse}

import java.time.LocalDate

class AddIncomeSourceStartDateControllerISpec extends ControllerISpecHelper {

  val prefixSoleTraderBusiness: String = "add-business-start-date"
  val continueButtonText: String = messagesAPI("base.continue")
  val prefixForeignProperty = "incomeSources.add.foreignProperty.startDate"

  val sessionService: SessionService = app.injector.instanceOf[SessionService]
  val journeyTypeSE: JourneyType = JourneyType(Add, SelfEmployment)
  val journeyTypeUK: JourneyType = JourneyType(Add, UkProperty)
  val journeyTypeFP: JourneyType = JourneyType(Add, ForeignProperty)
  val testBusinessStartDate: LocalDate = LocalDate.of(2022, 10, 10)
  val testBusinessName: String = "Test Business"
  val testBusinessTrade: String = "Plumbing"

  val testAddIncomeSourceData: IncomeSourceType => AddIncomeSourceData = (incomeSourceType: IncomeSourceType) =>
    if (incomeSourceType.equals(SelfEmployment)) {
      AddIncomeSourceData(
        businessName = Some(testBusinessName),
        businessTrade = Some(testBusinessTrade),
      )
    } else {
      AddIncomeSourceData(
        businessName = None,
        businessTrade = None
      )
    }

  def testUIJourneySessionData(incomeSourceType: IncomeSourceType): UIJourneySessionData = UIJourneySessionData(
    sessionId = testSessionId,
    journeyType = JourneyType(Add, incomeSourceType).toString,
    addIncomeSourceData = Some(testAddIncomeSourceData(incomeSourceType)))

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(sessionService.deleteSession(Add))
  }

  def getPath(mtdRole: MTDUserRole, incomeSourceType: IncomeSourceType, isChange: Boolean): String = {
    val pathStart = if(mtdRole == MTDIndividual) "/income-sources/add" else "/agents/income-sources/add"
    val changeFlagIfReq = if(isChange) "/change-" else "/"
    incomeSourceType match {
      case SelfEmployment => pathStart + changeFlagIfReq + "business-start-date"
      case UkProperty => pathStart + changeFlagIfReq + "uk-property-start-date"
      case ForeignProperty => pathStart + changeFlagIfReq + "foreign-property-start-date"
    }
  }

  def getIncomeSourceDetailsResponse(incomeSourceType: IncomeSourceType) = {
    incomeSourceType match {
      case SelfEmployment => businessOnlyResponse
      case UkProperty => ukPropertyOnlyResponse
      case ForeignProperty => noPropertyOrBusinessResponse
    }
  }

  def getJourneyType(incomeSourceType: IncomeSourceType): JourneyType = {
    incomeSourceType match {
      case SelfEmployment => journeyTypeSE
      case UkProperty => journeyTypeUK
      case ForeignProperty => journeyTypeFP
    }
  }

  def getPrefix(incomeSourceType: IncomeSourceType): String = {
    incomeSourceType match {
      case SelfEmployment => prefixSoleTraderBusiness
      case ForeignProperty => prefixForeignProperty
      case _ => "incomeSources.add.UKPropertyStartDate"
    }
  }

  List(true, false).foreach { isChange =>
    List(SelfEmployment, UkProperty, ForeignProperty).foreach { incomeSourceType =>
      mtdAllRoles.foreach { mtdUserRole =>
        val path = getPath(mtdUserRole, incomeSourceType, isChange)
        val additionalCookies = getAdditionalCookies(mtdUserRole)
        s"GET $path" when {
          s"a user is a $mtdUserRole" that {
            "is authenticated, with a valid enrolment" should {
              "render the Business Start Date Check Page" when {
                "incomesources is enabled" in {
                  enable(IncomeSources)
                  disable(NavBarFs)
                  stubAuthorised(mtdUserRole)
                  IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, getIncomeSourceDetailsResponse(incomeSourceType))

                  await(sessionService.setMongoData(testUIJourneySessionData(incomeSourceType)))

                  val result = buildGETMTDClient(path, additionalCookies).futureValue
                  verifyIncomeSourceDetailsCall(testMtditid)

                  val expectedHintText: String =  messagesAPI(s"${getPrefix(incomeSourceType)}.hint") + " " +
                    messagesAPI("dateForm.hint")

                  result should have(
                    httpStatus(OK),
                    pageTitle(mtdUserRole, s"${getPrefix(incomeSourceType)}.heading"),
                    elementTextByID("value-hint")(expectedHintText),
                    elementTextByID("continue-button")(continueButtonText)
                  )
                }
              }
            }
            testAuthFailures(path, mtdUserRole)
          }
        }

        s"POST $path" when {
          val journeyType = getJourneyType(incomeSourceType)
          s"a user is a $mtdUserRole" that {
            "is authenticated, with a valid enrolment" should {
              val addBusinessStartDateCheckUrl =
                controllers.incomeSources.add.routes.AddIncomeSourceStartDateCheckController.show(mtdUserRole != MTDIndividual, isChange, incomeSourceType).url
              s"redirect to $addBusinessStartDateCheckUrl" when {
                "a valid date is submitted" in {
                  enable(IncomeSources)
                  disable(NavBarFs)
                  stubAuthorised(mtdUserRole)
                  IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, getIncomeSourceDetailsResponse(incomeSourceType))

                  await(sessionService.setMongoData(testUIJourneySessionData(incomeSourceType)))
                  val formData: Map[String, Seq[String]] = {
                    Map(
                      "value.day" -> Seq("10"),
                      "value.month" -> Seq("10"),
                      "value.year" -> Seq("2022")
                    )
                  }
                  val result = buildPOSTMTDPostClient(path, additionalCookies,
                    body = formData).futureValue

                  sessionService.getMongoKeyTyped[LocalDate](dateStartedField, journeyType).futureValue shouldBe Right(Some(testBusinessStartDate))

                  result should have(
                    httpStatus(SEE_OTHER),
                    redirectURI(addBusinessStartDateCheckUrl)
                  )
                }
              }

              "return a BAD_REQUEST" when {
                "form is filled incorrectly" in {
                  enable(IncomeSources)
                  disable(NavBarFs)
                  stubAuthorised(mtdUserRole)
                  IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, getIncomeSourceDetailsResponse(incomeSourceType))

                  await(sessionService.setMongoData(testUIJourneySessionData(incomeSourceType)))

                  val formData: Map[String, Seq[String]] = {
                    Map(
                      "value.day" -> Seq("$"),
                      "value.month" -> Seq("%"),
                      "value.year" -> Seq("&")
                    )
                  }
                  val result = buildPOSTMTDPostClient(path, additionalCookies, body = formData).futureValue

                  result should have(
                    httpStatus(BAD_REQUEST),
                    elementTextByID("value-error")(messagesAPI("base.error-prefix") + ": " +
                      messagesAPI(s"${getPrefix(incomeSourceType)}.error.invalid"))
                  )
                }

                "form is empty" in {
                  enable(IncomeSources)
                  disable(NavBarFs)
                  stubAuthorised(mtdUserRole)
                  IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, getIncomeSourceDetailsResponse(incomeSourceType))

                  await(sessionService.setMongoData(testUIJourneySessionData(incomeSourceType)))

                  val result = buildPOSTMTDPostClient(path, additionalCookies,
                    body = Map()).futureValue

                  result should have(
                    httpStatus(BAD_REQUEST)
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
}
