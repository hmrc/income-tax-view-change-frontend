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

package controllers.manageBusinesses.add

import controllers.ControllerISpecHelper
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{Add, IncomeSourceJourneyType}
import enums.{MTDIndividual, MTDUserRole}
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.UIJourneySessionData
import models.admin.NavBarFs
import models.core.{CheckMode, Mode, NormalMode}
import models.incomeSourceDetails.AddIncomeSourceData.dateStartedField
import models.incomeSourceDetails.AddIncomeSourceData
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.SessionService
import testConstants.BaseIntegrationTestConstants.{testMtditid, testSessionId}
import testConstants.IncomeSourceIntegrationTestConstants.{businessOnlyResponse, noPropertyOrBusinessResponse, ukPropertyOnlyResponse}

import java.time.LocalDate

class AddIncomeSourceStartDateControllerISpec extends ControllerISpecHelper {

  val prefixSoleTraderBusiness: String = "add-business-start-date"
  val continueButtonText: String = messagesAPI("base.continue")
  val hintTextUKProperty: String =  messagesAPI("incomeSources.add.UKPropertyStartDate.hint") + " " + messagesAPI("incomeSources.add.UKPropertyStartDate.hint2") + " " +
    messagesAPI("dateForm.hint")
  val prefixForeignProperty = "incomeSources.add.foreignProperty.startDate"

  val sessionService: SessionService = app.injector.instanceOf[SessionService]
  val journeyTypeSE: IncomeSourceJourneyType = IncomeSourceJourneyType(Add, SelfEmployment)
  val journeyTypeUK: IncomeSourceJourneyType = IncomeSourceJourneyType(Add, UkProperty)
  val journeyTypeFP: IncomeSourceJourneyType = IncomeSourceJourneyType(Add, ForeignProperty)
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
    journeyType = IncomeSourceJourneyType(Add, incomeSourceType).toString,
    addIncomeSourceData = Some(testAddIncomeSourceData(incomeSourceType)))

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(sessionService.deleteSession(Add))
  }

  def getPath(mtdRole: MTDUserRole, incomeSourceType: IncomeSourceType, mode: Mode): String = {
    val pathStart = if(mtdRole == MTDIndividual) "/manage-your-businesses" else "/agents/manage-your-businesses"
    val pathEnd = s"/${if(mode == CheckMode) "change-" else ""}business-start-date"
    incomeSourceType match {
      case SelfEmployment => s"$pathStart/add-sole-trader$pathEnd"
      case UkProperty => s"$pathStart/add-uk-property$pathEnd"
      case ForeignProperty => s"$pathStart/add-foreign-property$pathEnd"
    }
  }

  def getIncomeSourceDetailsResponse(incomeSourceType: IncomeSourceType) = {
    incomeSourceType match {
      case SelfEmployment => businessOnlyResponse
      case UkProperty => ukPropertyOnlyResponse
      case ForeignProperty => noPropertyOrBusinessResponse
    }
  }

  def getJourneyType(incomeSourceType: IncomeSourceType): IncomeSourceJourneyType = {
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

  List(CheckMode, NormalMode).foreach { mode =>
    List(SelfEmployment, UkProperty, ForeignProperty).foreach { incomeSourceType =>
      mtdAllRoles.foreach { mtdUserRole =>
        val path = getPath(mtdUserRole, incomeSourceType, mode)
        val additionalCookies = getAdditionalCookies(mtdUserRole)
        s"GET $path" when {
          s"a user is a $mtdUserRole" that {
            "is authenticated, with a valid enrolment" should {
              "render the Business Start Date Check Page" when {
                "using the manage businesses journey" in {
                  disable(NavBarFs)
                  stubAuthorised(mtdUserRole)
                  IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, getIncomeSourceDetailsResponse(incomeSourceType))

                  await(sessionService.setMongoData(testUIJourneySessionData(incomeSourceType)))

                  val result = buildGETMTDClient(path, additionalCookies).futureValue
                  IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

                  val expectedHintText: String = "For example, 27 3 2020"

                  val descriptionStart = "The date your business started trading can be today, in the past or up to 7 days in the future."
                  val descriptionEnd = incomeSourceType match {
                    case SelfEmployment => "This is the date we’ll use to calculate your Class 2 National Insurance charge, if appropriate."
                    case UkProperty => "This is the date you first received rental income from this UK property business, such as letting or renting out a property or land."
                    case ForeignProperty => "This is the date you first received rental income from this foreign property business, such as letting or renting out a property or land."
                  }

                  result should have(
                    httpStatus(OK),
                    pageTitle(mtdUserRole, s"${getPrefix(incomeSourceType)}.heading"),
                    elementTextByID("value-hint")(expectedHintText),
                    elementTextByID("business-start-date-description-1")(descriptionStart),
                    elementTextByID("business-start-date-description-2")(descriptionEnd),
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
                controllers.manageBusinesses.add.routes.AddIncomeSourceStartDateCheckController.show(mtdUserRole != MTDIndividual, mode, incomeSourceType).url
              s"redirect to $addBusinessStartDateCheckUrl" when {
                "a valid date is submitted" in {
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
