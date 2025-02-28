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

import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{Add, IncomeSourceJourneyType}
import enums.{MTDIndividual, MTDUserRole}
import forms.incomeSources.add.IncomeSourceReportingMethodForm._
import mocks.auth.MockAuthActions
import mocks.services.MockSessionService
import models.admin.{IncomeSourcesFs, IncomeSourcesNewJourney}
import models.incomeSourceDetails.AddIncomeSourceData
import models.updateIncomeSource.{TaxYearSpecific, UpdateIncomeSourceResponseError, UpdateIncomeSourceResponseModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import org.mockito.stubbing.OngoingStubbing
import play.api
import play.api.Application
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import services._
import testConstants.BaseTestConstants.testSelfEmploymentId
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.{completedUIJourneySessionData, notCompletedUIJourneySessionData}

import java.time.LocalDate
import scala.concurrent.Future

class IncomeSourceReportingMethodControllerSpec extends MockAuthActions with MockSessionService {

  lazy val mockITSAStatusService: ITSAStatusService = mock(classOf[ITSAStatusService])
  lazy val mockUpdateIncomeSourceService: UpdateIncomeSourceService = mock(classOf[UpdateIncomeSourceService])
  lazy val mockCalculationListService: CalculationListService = mock(classOf[CalculationListService])
  lazy val mockDateService: DateService = mock(classOf[DateService])

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[SessionService].toInstance(mockSessionService),
      api.inject.bind[ITSAStatusService].toInstance(mockITSAStatusService),
      api.inject.bind[UpdateIncomeSourceService].toInstance(mockUpdateIncomeSourceService),
      api.inject.bind[CalculationListService].toInstance(mockCalculationListService),
      api.inject.bind[DateService].toInstance(mockDateService)
    ).build()

  lazy val testController = app.injector.instanceOf[IncomeSourceReportingMethodController]

  val TAX_YEAR_2024: LocalDate = LocalDate.of(2024, 4, 6)
  val TAX_YEAR_2023: LocalDate = LocalDate.of(2023, 4, 6)
  val TAX_YEAR_2022: LocalDate = LocalDate.of(2022, 4, 6)

  val formData: Map[String, String] = Map(
    newTaxYear1ReportingMethod -> "Q",
    newTaxYear2ReportingMethod -> "A",
    taxYear1 -> "2022",
    taxYear1ReportingMethod -> "A",
    taxYear2 -> "2023",
    taxYear2ReportingMethod -> "Q"
  )

  val title: String = "Is this date correct? - Manage your Income Tax updates - GOV.UK"
  val titleAgent: String = "Is this date correct? - Manage your client’s Income Tax updates - GOV.UK"
  val heading: String = "Your new business is set to report annually."
  val paragraph1: String = "Because this is a new business, for up to 2 tax years you can submit its income and expenses once a year in your tax return, even if:"
  val reportingFrequencyUlLi1: String = "you are voluntarily opted in or required to report quarterly for your other businesses"
  val reportingFrequencyUlLi2: String = "your income from self-employment or property, or both, exceed the income threshold"
  val paragraph2: String = "You can choose to report quarterly, which means submitting an update every 3 months in addition to your tax return"
  val reportingFrequencyFormH1: String = "Do you want to change to report quarterly?"
  val reportingFrequencyFormNoSelectionError: String = "Select yes if you want to report quarterly or select no if you want to report annually"
  val continueButtonText: String = "Continue"

  def getReportingFrequencyTableMessages(currentTaxYear: Int): (String, String) = {
    (s"Reporting frequency ${currentTaxYear} to ${currentTaxYear+1}", "Annual")
  }

  def getWarningInsetTextMessage(currentTaxYear: Int): String = {
    s"From April ${currentTaxYear} when this 2-year tax period ends, you could be required to report quarterly."
  }


  object Scenario extends Enumeration {
    type Scenario = Value
    val NO_LATENCY, NON_ELIGIBLE_ITSA_STATUS, ITSA_STATUS_ERROR, FIRST_YEAR_CRYSTALLISED,
    CURRENT_TAX_YEAR_IN_LATENCY_YEARS, LATENCY_PERIOD_EXPIRED, CURRENT_TAX_YEAR_2024_IN_LATENCY_YEARS = Value
  }

  import Scenario._

  def setupMockDateServiceCall(scenario: Scenario): OngoingStubbing[LocalDate] = scenario match {
    case LATENCY_PERIOD_EXPIRED | CURRENT_TAX_YEAR_2024_IN_LATENCY_YEARS => when(mockDateService.getCurrentTaxYearStart).thenReturn(TAX_YEAR_2024)
    case _ => when(mockDateService.getCurrentTaxYearStart).thenReturn(TAX_YEAR_2023)
  }

  def setupMockIncomeSourceDetailsCall(scenario: Scenario, incomeSourceType: IncomeSourceType): Unit = (scenario, incomeSourceType) match {
    case (NO_LATENCY, UkProperty) => mockUKPropertyIncomeSource()
    case (NO_LATENCY, ForeignProperty) => mockForeignPropertyIncomeSource()
    case (NO_LATENCY, SelfEmployment) => mockSingleBusinessIncomeSource()
    case (CURRENT_TAX_YEAR_2024_IN_LATENCY_YEARS, UkProperty) => mockUKPropertyIncomeSourceWithLatency2024()
    case (CURRENT_TAX_YEAR_2024_IN_LATENCY_YEARS, ForeignProperty) => mockForeignPropertyIncomeSourceWithLatency2024()
    case (CURRENT_TAX_YEAR_2024_IN_LATENCY_YEARS, SelfEmployment) => mockBusinessIncomeSourceWithLatency2024()
    case (_, UkProperty) => mockUKPropertyIncomeSourceWithLatency2023()
    case (_, ForeignProperty) => mockForeignPropertyIncomeSourceWithLatency2023()
    case (_, SelfEmployment) => mockBusinessIncomeSourceWithLatency2023()
  }

  def setupMockITSAStatusCall(scenario: Scenario): OngoingStubbing[Future[Boolean]] = scenario match {
    case NON_ELIGIBLE_ITSA_STATUS =>
      when(mockITSAStatusService.hasMandatedOrVoluntaryStatusCurrentYear(any, any, any))
        .thenReturn(Future.successful(false))
    case ITSA_STATUS_ERROR =>
      when(mockITSAStatusService.hasMandatedOrVoluntaryStatusCurrentYear(any, any, any))
        .thenReturn(Future.failed(new Exception("Failed to retrieve ITSAStatus")))
    case _ =>
      when(mockITSAStatusService.hasMandatedOrVoluntaryStatusCurrentYear(any, any, any))
        .thenReturn(Future.successful(true))
  }

  def setupMockIsTaxYearCrystallisedCall(scenario: Scenario): OngoingStubbing[Future[Boolean]] = scenario match {
    case FIRST_YEAR_CRYSTALLISED =>
      when(mockCalculationListService.determineTaxYearCrystallised(ArgumentMatchers.eq(TAX_YEAR_2022.getYear))(any, any)).thenReturn(Future(true))
    case CURRENT_TAX_YEAR_IN_LATENCY_YEARS =>
      when(mockCalculationListService.determineTaxYearCrystallised(ArgumentMatchers.eq(TAX_YEAR_2022.getYear))(any, any)).thenReturn(Future(false))
    case CURRENT_TAX_YEAR_2024_IN_LATENCY_YEARS =>
      when(mockCalculationListService.determineTaxYearCrystallised(ArgumentMatchers.eq(TAX_YEAR_2023.getYear))(any, any)).thenReturn(Future(false))
    case _ =>
      when(mockCalculationListService.determineTaxYearCrystallised(ArgumentMatchers.eq(TAX_YEAR_2023.getYear))(any, any)).thenReturn(Future(false))
  }

  def setupMockUpdateIncomeSourceCall(numberOfSuccessResponses: Int): Unit = {
    val tySpecific1 = TaxYearSpecific("2022", latencyIndicator = false)
    val tySpecific2 = TaxYearSpecific("2023", latencyIndicator = true)

    numberOfSuccessResponses match {
      case 0 =>
        when(mockUpdateIncomeSourceService.updateTaxYearSpecific(
          ArgumentMatchers.anyString(),
          ArgumentMatchers.eq(testSelfEmploymentId),
          ArgumentMatchers.eq(tySpecific1))(any, any))
          .thenReturn(Future.successful(UpdateIncomeSourceResponseError(INTERNAL_SERVER_ERROR.toString, "something's broken :(")))

        when(mockUpdateIncomeSourceService.updateTaxYearSpecific(
          ArgumentMatchers.anyString(),
          ArgumentMatchers.eq(testSelfEmploymentId),
          ArgumentMatchers.eq(tySpecific2))(any, any))
          .thenReturn(Future.successful(UpdateIncomeSourceResponseError(INTERNAL_SERVER_ERROR.toString, "something else is broken :(")))
      case 1 =>
        when(mockUpdateIncomeSourceService.updateTaxYearSpecific(
          ArgumentMatchers.anyString(),
          ArgumentMatchers.eq(testSelfEmploymentId),
          ArgumentMatchers.eq(tySpecific1))(any, any))
          .thenReturn(Future.successful(UpdateIncomeSourceResponseError(INTERNAL_SERVER_ERROR.toString, "something's broken :(")))

        when(mockUpdateIncomeSourceService.updateTaxYearSpecific(
          ArgumentMatchers.anyString(),
          ArgumentMatchers.eq(testSelfEmploymentId),
          ArgumentMatchers.eq(tySpecific2))(any, any))
          .thenReturn(Future.successful(UpdateIncomeSourceResponseModel("2024-01-31T09:26:17Z")))
      case _ =>
        when(mockUpdateIncomeSourceService.updateTaxYearSpecific(
          ArgumentMatchers.anyString(),
          ArgumentMatchers.eq(testSelfEmploymentId),
          ArgumentMatchers.eq(tySpecific1))(any, any))
          .thenReturn(Future.successful(UpdateIncomeSourceResponseModel("2024-01-31T09:26:17Z")))

        when(mockUpdateIncomeSourceService.updateTaxYearSpecific(
          ArgumentMatchers.anyString(),
          ArgumentMatchers.eq(testSelfEmploymentId),
          ArgumentMatchers.eq(tySpecific2))(any, any))
          .thenReturn(Future.successful(UpdateIncomeSourceResponseModel("2024-01-31T09:26:17Z")))
    }
  }

  def setupMockCalls(isAgent: Boolean, incomeSourceType: IncomeSourceType, mtdRole: MTDUserRole, scenario: Scenario): Unit = {
    enable(IncomeSourcesFs)
    enable(IncomeSourcesNewJourney)
    setupMockSuccess(mtdRole)
    setupMockIncomeSourceDetailsCall(scenario, incomeSourceType)
    setupMockDateServiceCall(scenario)
    setupMockITSAStatusCall(scenario)
    setupMockIsTaxYearCrystallisedCall(scenario)
    setupMockGetMongo(Right(Some(notCompletedUIJourneySessionData(IncomeSourceJourneyType(Add, incomeSourceType)))))
    setupMockSetMongoData(true)
    setupMockGetSessionKeyMongoTyped[String](key = AddIncomeSourceData.incomeSourceIdField, journeyType = IncomeSourceJourneyType(Add, incomeSourceType), result = Right(Some(testSelfEmploymentId)))
  }

  def getTestTitleIncomeSourceType(incomeSourceType: IncomeSourceType): String = {
    incomeSourceType match {
      case SelfEmployment => "Self Employment"
      case UkProperty => "UK Property"
      case ForeignProperty => "Foreign Property"
    }
  }

  def getValidationErrorTabTitle(): String = {
    s"${messages("htmlTitle.invalidInput", heading)}"
  }

  val incomeSourceTypes: Seq[IncomeSourceType] = List(SelfEmployment, UkProperty, ForeignProperty)


  mtdAllRoles.foreach { mtdRole =>
    val isAgent = mtdRole != MTDIndividual
    incomeSourceTypes.foreach { incomeSourceType =>
      s"show($isAgent, $incomeSourceType)" when {
        val action = testController.show(isAgent, incomeSourceType)
        val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
        s"the user is authenticated as a $mtdRole" should {
          "render the reporting frequency page" when {
            s"within latency period (before 2024) with FS enabled" in {
              setupMockCalls(isAgent = isAgent, incomeSourceType = incomeSourceType, mtdRole, CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
              val thisTaxYear: Int = TAX_YEAR_2023.getYear
              val result = action(fakeRequest)
              val document: Document = Jsoup.parse(contentAsString(result))

              val tableMessages: (String, String) = getReportingFrequencyTableMessages(currentTaxYear = thisTaxYear)
              val warningInsetTextMessages: String = getWarningInsetTextMessage(currentTaxYear = thisTaxYear)

              val documentTableC1 = document.getElementsByTag("th")
              val documentTableC2 = document.getElementsByTag("td")
              val documentTableR1C1: String = documentTableC1.get(0).text
              val documentTableR1C2: String = documentTableC2.get(0).text
              val documentTableR2C1: String = documentTableC1.get(1).text
              val documentTableR2C2: String = documentTableC2.get(1).text

              status(result) shouldBe OK
              if (isAgent) document.title shouldBe titleAgent else document.title shouldBe title
              document.getElementsByClass("govuk-heading-l margin-bottom-100").get(0).text shouldBe heading
              document.getElementById("paragraph-1").text shouldBe paragraph1
              document.getElementById("inset-text-bullet-1").text shouldBe reportingFrequencyUlLi1
              document.getElementById("inset-text-bullet-2").text shouldBe reportingFrequencyUlLi2
              documentTableR1C1 shouldBe tableMessages._1
              documentTableR1C2 shouldBe tableMessages._2
              documentTableR2C1 shouldBe tableMessages._1
              documentTableR2C2 shouldBe tableMessages._2
              document.getElementById("paragraph-2").text shouldBe paragraph2
              document.getElementById("warning-inset").text shouldBe warningInsetTextMessages
              document.getElementById("reporting-quarterly-form").getElementsByClass("govuk-fieldset__legend--m").get(1).text shouldBe reportingFrequencyFormH1
              document.getElementById("reporting-quarterly-form-continue-button").text shouldBe continueButtonText

            }

            s"within latency period (after 2024) with FS enabled" in {
              setupMockCalls(isAgent = isAgent, incomeSourceType = incomeSourceType, mtdRole, CURRENT_TAX_YEAR_2024_IN_LATENCY_YEARS)
              val thisTaxYear: Int = TAX_YEAR_2024.getYear
              val result = action(fakeRequest)
              val document: Document = Jsoup.parse(contentAsString(result))

              val tableMessages: (String, String) = getReportingFrequencyTableMessages(currentTaxYear = thisTaxYear)
              val warningInsetTextMessages: String = getWarningInsetTextMessage(currentTaxYear = thisTaxYear)

              val documentTable = document.getElementsByClass("govuk-summary-list").get(0)
              val documentTableR1C1: String = documentTable.getAllElements.get(0).text
              val documentTableR1C2: String = documentTable.getAllElements.get(1).text
              val documentTableR2C1: String = documentTable.getAllElements.get(2).text
              val documentTableR2C2: String = documentTable.getAllElements.get(3).text

              status(result) shouldBe OK
              if (isAgent) document.title shouldBe titleAgent else document.title shouldBe title
              document.getElementsByClass("govuk-heading-l margin-bottom-100").get(0).text shouldBe heading
              document.getElementById("paragraph-1").text shouldBe paragraph1
              document.getElementById("inset-text-bullet-1").text shouldBe reportingFrequencyUlLi1
              document.getElementById("inset-text-bullet-2").text shouldBe reportingFrequencyUlLi2
              documentTableR1C1 shouldBe tableMessages._1
              documentTableR1C2 shouldBe tableMessages._2
              documentTableR2C1 shouldBe tableMessages._1
              documentTableR2C2 shouldBe tableMessages._2
              document.getElementById("paragraph-2").text shouldBe paragraph2
              document.getElementById("warning-inset").text shouldBe warningInsetTextMessages
              document.getElementById("reporting-quarterly-form").getElementsByClass("govuk-fieldset__legend--m").get(1).text shouldBe reportingFrequencyFormH1
              document.getElementById("reporting-quarterly-form-continue-button").text shouldBe continueButtonText

            }
          }

          "return 303 SEE_OTHER and redirect to home page" when {
            s"navigating to the ${getTestTitleIncomeSourceType(incomeSourceType)} page with FS disabled" in {
              setupMockCalls(isAgent = isAgent, incomeSourceType = incomeSourceType, mtdRole, CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
              disable(IncomeSourcesFs)

              val expectedRedirectUrl = if (isAgent) controllers.routes.HomeController.showAgent.url
              else controllers.routes.HomeController.show().url

              val result = action(fakeRequest)
              status(result) shouldBe SEE_OTHER
              redirectLocation(result) shouldBe Some(expectedRedirectUrl)
            }
          }

          "return 303 SEE_OTHER and redirect to the You Cannot Go Back Page" when {
            s"user has already visited the obligations page for ${getTestTitleIncomeSourceType(incomeSourceType)}" in {
              setupMockCalls(isAgent = isAgent, incomeSourceType, mtdRole, CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
              setupMockGetMongo(Right(Some(completedUIJourneySessionData(IncomeSourceJourneyType(Add, incomeSourceType)))))
              val expectedBackErrorRedirectUrl = if (isAgent)
                controllers.manageBusinesses.add.routes.ReportingMethodSetBackErrorController.showAgent(incomeSourceType).url
              else
                controllers.manageBusinesses.add.routes.ReportingMethodSetBackErrorController.show(incomeSourceType).url

              val result = action(fakeRequest)
              status(result) shouldBe SEE_OTHER
              redirectLocation(result) shouldBe Some(expectedBackErrorRedirectUrl)
            }
          }

          "return 500 INTERNAL_SERVER_ERROR" when {
            s"navigating to the ${getTestTitleIncomeSourceType(incomeSourceType)} page and ITSA status returns an error" in {
              setupMockCalls(isAgent = isAgent, incomeSourceType = incomeSourceType, mtdRole, ITSA_STATUS_ERROR)
              val result = action(fakeRequest)
              status(result) shouldBe INTERNAL_SERVER_ERROR
            }
          }
        }
        testMTDAuthFailuresForRole(action, mtdRole)(fakeRequest)
      }

      s"submit($isAgent, $incomeSourceType)" when {
        val action = testController.submit(isAgent, incomeSourceType)
        val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole).withMethod("POST")
        s"the user is authenticated as a $mtdRole" should {
          "return 303 SEE_OTHER and redirect to Obligations page" when {
            s"completing the ${getTestTitleIncomeSourceType(incomeSourceType)} form and updates send successfully" in {
              setupMockCalls(isAgent = isAgent, incomeSourceType = incomeSourceType, mtdRole, CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
              val expectedRedirectUrl = if (isAgent) controllers.manageBusinesses.add.routes.IncomeSourceAddedController.showAgent(incomeSourceType).url
              else controllers.manageBusinesses.add.routes.IncomeSourceAddedController.show(incomeSourceType).url

              setupMockUpdateIncomeSourceCall(2)
              val result = action(fakeRequest.withFormUrlEncodedBody(formData.toSeq: _*))
              status(result) shouldBe SEE_OTHER
              redirectLocation(result) shouldBe Some(expectedRedirectUrl)
            }
          }

          "return 303 SEE_OTHER and redirect to Reporting Method Not Saved page" when {
            s"completing the ${getTestTitleIncomeSourceType(incomeSourceType)} form and one update fails" in {
              setupMockCalls(isAgent = isAgent, incomeSourceType = incomeSourceType, mtdRole, CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
              setupMockUpdateIncomeSourceCall(1)
              val expectedRedirectUrl = if (isAgent)
                controllers.manageBusinesses.add.routes.IncomeSourceReportingMethodNotSavedController.showAgent(incomeSourceType).url
              else controllers.manageBusinesses.add.routes.IncomeSourceReportingMethodNotSavedController.show(incomeSourceType).url
              val result = action(fakeRequest.withFormUrlEncodedBody(formData.toSeq: _*))
              status(result) shouldBe SEE_OTHER
              redirectLocation(result) shouldBe Some(expectedRedirectUrl)            }

            s"completing the ${getTestTitleIncomeSourceType(incomeSourceType)} form and both updates fail" in {
              setupMockCalls(isAgent = isAgent, incomeSourceType = incomeSourceType, mtdRole, CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
              val expectedRedirectUrl = if (isAgent)
                controllers.manageBusinesses.add.routes.IncomeSourceReportingMethodNotSavedController.showAgent(incomeSourceType).url
              else controllers.manageBusinesses.add.routes.IncomeSourceReportingMethodNotSavedController.show(incomeSourceType).url

              setupMockUpdateIncomeSourceCall(0)
              val result = action(fakeRequest.withFormUrlEncodedBody(formData.toSeq: _*))
              status(result) shouldBe SEE_OTHER
              redirectLocation(result) shouldBe Some(expectedRedirectUrl)            }
          }

          "return 303 SEE_OTHER and redirect to home page" when {
            s"POST request to the ${getTestTitleIncomeSourceType(incomeSourceType)} page with FS disabled" in {
              setupMockCalls(isAgent = isAgent, incomeSourceType = incomeSourceType, mtdRole, CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
              disable(IncomeSourcesFs)

              val expectedRedirectUrl = if (isAgent) controllers.routes.HomeController.showAgent.url else controllers.routes.HomeController.show().url

              val result = action(fakeRequest.withFormUrlEncodedBody(formData.toSeq: _*))
              status(result) shouldBe SEE_OTHER
              redirectLocation(result) shouldBe Some(expectedRedirectUrl)            }
          }

          "return 400 BAD_REQUEST" when {
            s"invalid form input on the ${getTestTitleIncomeSourceType(incomeSourceType)} page" in {
              setupMockCalls(isAgent = isAgent, incomeSourceType = incomeSourceType, mtdRole, CURRENT_TAX_YEAR_IN_LATENCY_YEARS)
              val invalidFormData: Map[String, String] = Map(
                newTaxYear1ReportingMethod -> "",
                newTaxYear2ReportingMethod -> "",
                taxYear1 -> "2022",
                taxYear1ReportingMethod -> "A",
                taxYear2 -> "2023",
                taxYear2ReportingMethod -> "Q"
              )
              val result = action(fakeRequest.withFormUrlEncodedBody(invalidFormData.toSeq: _*))

              status(result) shouldBe BAD_REQUEST

              val document: Document = Jsoup.parse(contentAsString(result))
              document.title shouldBe getValidationErrorTabTitle()
            }
          }
        }
        testMTDAuthFailuresForRole(action, mtdRole)(fakeRequest)
      }
    }
  }
}
