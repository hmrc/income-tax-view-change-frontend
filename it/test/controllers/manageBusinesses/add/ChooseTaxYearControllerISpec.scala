/*
 * Copyright 2025 HM Revenue & Customs
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
import helpers.IncomeSourceCheckDetailsConstants.{testBusinessName, testBusinessStartDate, testBusinessTrade}
import helpers.servicemocks.ITSAStatusDetailsStub.ITSAYearStatus
import helpers.servicemocks.{ITSAStatusDetailsStub, IncomeTaxViewChangeStub}
import models.UIJourneySessionData
import models.admin.OptInOptOutContentUpdateR17
import models.incomeSourceDetails.{AddIncomeSourceData, IncomeSourceReportingFrequencySourceData, LatencyDetails}
import models.itsaStatus.ITSAStatus.Voluntary
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import repositories.UIJourneySessionDataRepository
import services.{DateService, SessionService}
import testConstants.BaseIntegrationTestConstants.{testMtditid, testSessionId}
import testConstants.IncomeSourceIntegrationTestConstants.{noPropertyOrBusinessResponse, singleUKForeignPropertyResponseInLatencyPeriod}

import java.time.LocalDate

class ChooseTaxYearControllerISpec extends ControllerISpecHelper {
  override val dateService: DateService = app.injector.instanceOf[DateService] //overridden for TYS as implemented with 2023 elsewhere
  val currentTaxYear = dateService.getCurrentTaxYearEnd

  val sessionService: SessionService = app.injector.instanceOf[SessionService]
  val repository: UIJourneySessionDataRepository = app.injector.instanceOf[UIJourneySessionDataRepository]

  def getPath(mtdRole: MTDUserRole, incomeSourceType: IncomeSourceType): String = {
    val pathStart = if (mtdRole == MTDIndividual) "/manage-your-businesses" else "/agents/manage-your-businesses"
    incomeSourceType match {
      case SelfEmployment => s"$pathStart/add-sole-trader/choose-taxyear"
      case UkProperty => s"$pathStart/add-uk-property/choose-taxyear"
      case ForeignProperty => s"$pathStart/add-foreign-property/choose-taxyear"
    }
  }

  def getSubheading(incomeSourceType: IncomeSourceType): String = {
    incomeSourceType match {
      case SelfEmployment => "Sole trader"
      case UkProperty => "UK property"
      case ForeignProperty => "Foreign property"
    }
  }

  val testAddIncomeSourceDataWithStartDate: IncomeSourceType => AddIncomeSourceData = (incomeSourceType: IncomeSourceType) =>
    if (incomeSourceType.equals(SelfEmployment)) {
      AddIncomeSourceData(
        businessName = Some(testBusinessName),
        businessTrade = Some(testBusinessTrade),
        dateStarted = Some(testBusinessStartDate),
        incomeSourceId = Some("ID")
      )
    } else {
      AddIncomeSourceData(
        businessName = None,
        businessTrade = None,
        dateStarted = Some(testBusinessStartDate),
        incomeSourceId = Some("ID")
      )
    }

  def testUIJourneySessionData(incomeSourceType: IncomeSourceType): UIJourneySessionData = UIJourneySessionData(
    sessionId = testSessionId,
    journeyType = IncomeSourceJourneyType(Add, incomeSourceType).toString,
    addIncomeSourceData = Some(testAddIncomeSourceDataWithStartDate(incomeSourceType)))


  override def beforeEach(): Unit = {
    super.beforeEach()
    repository.set(UIJourneySessionData(
      testSessionId,
      "IncomeSourceReportingFrequencyJourney",
      incomeSourceReportingFrequencyData = Some(IncomeSourceReportingFrequencySourceData(false, false, false, false)))
    )
  }

  val dateNow = LocalDate.now()
  def taxYearEnd: Int = if(dateNow.isAfter(LocalDate.of(dateNow.getYear, 4, 5))) dateNow.getYear + 1 else dateNow.getYear

  override def afterEach(): Unit = {
    super.afterEach()
    repository.clearSession(testSessionId).futureValue shouldBe true
  }

  List(SelfEmployment, UkProperty, ForeignProperty).foreach { incomeSourceType =>
    mtdAllRoles.foreach { case mtdUserRole =>
      val path = getPath(mtdUserRole, incomeSourceType)
      val additionalCookies = getAdditionalCookies(mtdUserRole)


      s"GET $path" when {

        s"a user is a $mtdUserRole" that {

          "is authenticated" when {

            "OptInOptOutContentUpdateR17 is enabled" when {

              "using the manage businesses journey" in {

                enable(OptInOptOutContentUpdateR17)
                stubAuthorised(mtdUserRole)
                val latencyDetailsCty = LatencyDetails(dateNow.plusDays(1), taxYearEnd.toString, "Q", (taxYearEnd + 1).toString, "A")
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleUKForeignPropertyResponseInLatencyPeriod(latencyDetailsCty))

                await(sessionService.setMongoData(testUIJourneySessionData(incomeSourceType)))
                ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetailsWithGivenThreeStatus(
                  dateService.getCurrentTaxYearEnd + 1, ITSAYearStatus(Voluntary, Voluntary, Voluntary))

                val result = buildGETMTDClient(path, additionalCookies).futureValue

                result should have(
                  httpStatus(OK),
                  elementTextByID("choose-tax-year-heading")(s"Which tax year do you want to sign up for?"),
                  elementTextByID("choose-tax-year-subheading")(getSubheading(incomeSourceType)),
                  elementTextBySelector("[for='current-year-checkbox']")(s"${currentTaxYear - 1} to $currentTaxYear"),
                  elementTextBySelector("[for='next-year-checkbox']")(s"$currentTaxYear to ${currentTaxYear + 1}"),
                  elementTextByID("continue-button")("Continue"),
                )
              }
            }

            "OptInOptOutContentUpdateR17 is disabled" when {

              "using the manage businesses journey" in {

                disable(OptInOptOutContentUpdateR17)
                stubAuthorised(mtdUserRole)

                val latencyDetailsCty = LatencyDetails(dateNow.plusDays(1), taxYearEnd.toString, "Q", (taxYearEnd + 1).toString, "A")
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleUKForeignPropertyResponseInLatencyPeriod(latencyDetailsCty))

                await(sessionService.setMongoData(testUIJourneySessionData(incomeSourceType)))

                ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetailsWithGivenThreeStatus(
                  dateService.getCurrentTaxYearEnd + 1, ITSAYearStatus(Voluntary, Voluntary, Voluntary))

                val result = buildGETMTDClient(path, additionalCookies).futureValue

                result should have(
                  httpStatus(OK),
                  elementTextByID("choose-tax-year-heading")(s"Which tax year do you want to report quarterly for?"),
                  elementTextByID("choose-tax-year-subheading")(getSubheading(incomeSourceType)),
                  elementTextBySelector("[for='current-year-checkbox']")(s"${currentTaxYear - 1} to $currentTaxYear"),
                  elementTextBySelector("[for='next-year-checkbox']")(s"$currentTaxYear to ${currentTaxYear + 1}"),
                  elementTextByID("continue-button")("Continue"),
                )
              }
            }
          }
        }
        testAuthFailures(path, mtdUserRole, Some(Map(
          "current-year-checkbox" -> Seq("Test")
        )))
      }

      s"POST $path" when {

        s"a user is a $mtdUserRole" that {

          "is authenticated" should {

            "OptInOptOutContentUpdateR17 is enabled" when {

              "submit the reporting frequency for the income source" in {
                val isAgent = !(mtdUserRole == MTDIndividual)

                enable(OptInOptOutContentUpdateR17)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

                await(sessionService.createSession(IncomeSourceJourneyType(Add, incomeSourceType)))

                await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "ADD-SE",
                  addIncomeSourceData = Some(AddIncomeSourceData(incomeSourceId = Some("incomeSourceId"), dateStarted = Some(LocalDate.of(2024, 1, 1)))))))

                val result = buildPOSTMTDPostClient(path, additionalCookies, Map("current-year-checkbox" -> Seq("true"), "next-year-checkbox" -> Seq("true"))).futureValue

                IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

                result should have(
                  httpStatus(SEE_OTHER),
                  redirectURI(controllers.manageBusinesses.add.routes.IncomeSourceRFCheckDetailsController.show(isAgent, incomeSourceType).url)
                )
              }

              "return an error if the form is invalid" in {

                enable(OptInOptOutContentUpdateR17)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

                val journeyType = incomeSourceType match {
                  case SelfEmployment => "ADD-SE"
                  case UkProperty => "ADD-UK"
                  case ForeignProperty => "ADD-FP"
                }

                await(sessionService.setMongoData(UIJourneySessionData(testSessionId, journeyType,
                  addIncomeSourceData = Some(AddIncomeSourceData(incomeSourceId = Some("incomeSourceId"), dateStarted = Some(LocalDate.of(2024, 1, 1)))),
                  incomeSourceReportingFrequencyData = Some(IncomeSourceReportingFrequencySourceData(true, true, false, false)))))

                val result = buildPOSTMTDPostClient(path, additionalCookies, body = Map("Invalid" -> Seq("Invalid"))).futureValue

                IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

                result should have(
                  httpStatus(BAD_REQUEST),
                  elementTextByID("choose-tax-year-heading")(s"Which tax year do you want to sign up for?"),
                  elementTextByID("choose-tax-year-subheading")(getSubheading(incomeSourceType)),
                  elementTextBySelector("[for='current-year-checkbox']")(s"${currentTaxYear - 1} to $currentTaxYear"),
                  elementTextBySelector("[for='next-year-checkbox']")(s"$currentTaxYear to ${currentTaxYear + 1}"),
                  elementTextByID("continue-button")("Continue"),
                  elementTextByID("error-summary-title")("There is a problem"),
                  elementTextByID("error-summary-link")("Select the tax years you want to sign up for")
                )
              }
            }

            "OptInOptOutContentUpdateR17 is disabled" when {

              "submit the reporting frequency for the income source" in {
                val isAgent = !(mtdUserRole == MTDIndividual)

                disable(OptInOptOutContentUpdateR17)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

                await(sessionService.createSession(IncomeSourceJourneyType(Add, incomeSourceType)))

                await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "ADD-SE",
                  addIncomeSourceData = Some(AddIncomeSourceData(incomeSourceId = Some("incomeSourceId"), dateStarted = Some(LocalDate.of(2024, 1, 1)))))))

                val result = buildPOSTMTDPostClient(path, additionalCookies, Map("current-year-checkbox" -> Seq("true"), "next-year-checkbox" -> Seq("true"))).futureValue

                IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

                result should have(
                  httpStatus(SEE_OTHER),
                  redirectURI(controllers.manageBusinesses.add.routes.IncomeSourceRFCheckDetailsController.show(isAgent, incomeSourceType).url)
                )
              }

              "return an error if the form is invalid" in {

                disable(OptInOptOutContentUpdateR17)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

                val journeyType = incomeSourceType match {
                  case SelfEmployment => "ADD-SE"
                  case UkProperty => "ADD-UK"
                  case ForeignProperty => "ADD-FP"
                }

                await(sessionService.setMongoData(UIJourneySessionData(testSessionId, journeyType,
                  addIncomeSourceData = Some(AddIncomeSourceData(incomeSourceId = Some("incomeSourceId"), dateStarted = Some(LocalDate.of(2024, 1, 1)))),
                  incomeSourceReportingFrequencyData = Some(IncomeSourceReportingFrequencySourceData(true, true, false, false)))))

                val result = buildPOSTMTDPostClient(path, additionalCookies, body = Map("Invalid" -> Seq("Invalid"))).futureValue

                IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

                result should have(
                  httpStatus(BAD_REQUEST),
                  elementTextByID("choose-tax-year-heading")(s"Which tax year do you want to report quarterly for?"),
                  elementTextByID("choose-tax-year-subheading")(getSubheading(incomeSourceType)),
                  elementTextBySelector("[for='current-year-checkbox']")(s"${currentTaxYear - 1} to $currentTaxYear"),
                  elementTextBySelector("[for='next-year-checkbox']")(s"$currentTaxYear to ${currentTaxYear + 1}"),
                  elementTextByID("continue-button")("Continue"),
                  elementTextByID("error-summary-title")("There is a problem"),
                  elementTextByID("error-summary-link")("Select the tax years you want to report quarterly")
                )
              }
            }
          }
        }

        testAuthFailures(path, mtdUserRole, Some(Map(
          "current-year-checkbox" -> Seq("Test")
        )))
      }
    }
  }
}
