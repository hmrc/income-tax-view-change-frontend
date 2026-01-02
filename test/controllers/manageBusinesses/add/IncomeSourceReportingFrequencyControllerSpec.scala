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

import connectors.{BusinessDetailsConnector, ITSAStatusConnector}
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{Add, IncomeSourceJourneyType}
import enums.{MTDIndividual, MTDUserRole}
import mocks.auth.MockAuthActions
import mocks.services.{MockIncomeSourceRFService, MockSessionService}
import models.UIJourneySessionData
import models.incomeSourceDetails.{AddIncomeSourceData, IncomeSourceReportingFrequencySourceData}
import models.updateIncomeSource.{TaxYearSpecific, UpdateIncomeSourceResponseError, UpdateIncomeSourceResponseModel}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import org.mockito.stubbing.OngoingStubbing
import play.api
import play.api.Application
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import services._
import services.manageBusinesses.IncomeSourceRFService
import testConstants.BaseTestConstants.testSelfEmploymentId
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.notCompletedUIJourneySessionData

import java.time.LocalDate
import scala.concurrent.Future

class IncomeSourceReportingFrequencyControllerSpec extends MockAuthActions with MockSessionService with MockIncomeSourceRFService {

  lazy val mockITSAStatusService: ITSAStatusService = mock(classOf[ITSAStatusService])
  lazy val mockUpdateIncomeSourceService: UpdateIncomeSourceService = mock(classOf[UpdateIncomeSourceService])
  lazy val mockCalculationListService: CalculationListService = mock(classOf[CalculationListService])
  lazy val mockDateService: DateService = mock(classOf[DateService])

  private val existingRfData: IncomeSourceReportingFrequencySourceData =
    IncomeSourceReportingFrequencySourceData(
      displayOptionToChangeForCurrentTaxYear = true,
      displayOptionToChangeForNextTaxYear = true,
      isReportingQuarterlyCurrentYear = true,
      isReportingQuarterlyForNextYear = true
    )

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[IncomeSourceRFService].toInstance(mockIncomeSourceRFService),
      api.inject.bind[SessionService].toInstance(mockSessionService),
      api.inject.bind[ITSAStatusService].toInstance(mockITSAStatusService),
      api.inject.bind[UpdateIncomeSourceService].toInstance(mockUpdateIncomeSourceService),
      api.inject.bind[CalculationListService].toInstance(mockCalculationListService),
      api.inject.bind[DateService].toInstance(mockDateService),
      api.inject.bind[ITSAStatusConnector].toInstance(mockItsaStatusConnector),
      api.inject.bind[BusinessDetailsConnector].toInstance(mockBusinessDetailsConnector),
      api.inject.bind[DateServiceInterface].toInstance(mockDateServiceInterface)
    ).build()

  lazy val testController = app.injector.instanceOf[IncomeSourceReportingFrequencyController]

  val TAX_YEAR_2024: LocalDate = LocalDate.of(2024, 4, 6)
  val TAX_YEAR_2023: LocalDate = LocalDate.of(2023, 4, 6)
  val TAX_YEAR_2022: LocalDate = LocalDate.of(2022, 4, 6)

  val formData: Map[String, String] = Map("report-quarterly" -> "yes")


  object Scenario extends Enumeration {
    type Scenario = Value
    val NO_LATENCY, NON_ELIGIBLE_ITSA_STATUS, ITSA_STATUS_ERROR, FIRST_YEAR_CRYSTALLISED,
    CURRENT_TAX_YEAR_IN_LATENCY_YEARS, LATENCY_PERIOD_EXPIRED, CURRENT_TAX_YEAR_2024_IN_LATENCY_YEARS = Value
  }

  import Scenario._

  def setupMockDateServiceCall(scenario: Scenario): OngoingStubbing[LocalDate] = scenario match {
    case LATENCY_PERIOD_EXPIRED | CURRENT_TAX_YEAR_2024_IN_LATENCY_YEARS => {
      when(mockDateService.getCurrentTaxYearEnd).thenReturn(TAX_YEAR_2024.getYear)
      when(mockDateService.getCurrentTaxYearStart).thenReturn(TAX_YEAR_2023)
      when(mockDateService.getCurrentDate).thenReturn(TAX_YEAR_2023)
    }
    case _ => {
      when(mockDateService.getCurrentTaxYearEnd).thenReturn(TAX_YEAR_2024.getYear)
      when(mockDateService.getCurrentTaxYearStart).thenReturn(TAX_YEAR_2023)
      when(mockDateService.getCurrentDate).thenReturn(TAX_YEAR_2023)
    }
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
      when(mockITSAStatusService.hasMandatedOrVoluntaryStatusCurrentYear(any)(any, any, any))
        .thenReturn(Future.successful(false))
    case ITSA_STATUS_ERROR =>
      when(mockITSAStatusService.hasMandatedOrVoluntaryStatusCurrentYear(any)(any, any, any))
        .thenReturn(Future.failed(new Exception("Failed to retrieve ITSAStatus")))
    case _ =>
      when(mockITSAStatusService.hasMandatedOrVoluntaryStatusCurrentYear(any)(any, any, any))
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
    setupMockSuccess(mtdRole)
    setupMockIncomeSourceDetailsCall(scenario, incomeSourceType)
    setupMockDateServiceCall(scenario)
    setupMockITSAStatusCall(scenario)
    setupMockIsTaxYearCrystallisedCall(scenario)
    setupMockGetMongo(Right(Some(notCompletedUIJourneySessionData(IncomeSourceJourneyType(Add, incomeSourceType)))))
    setupMockSetMongoData(true)
    setupMockGetSessionKeyMongoTyped[String](key = AddIncomeSourceData.incomeSourceIdField, journeyType = IncomeSourceJourneyType(Add, incomeSourceType), result = Right(Some(testSelfEmploymentId)))
    mockRedirectChecksForIncomeSourceRF()
  }

  def getTestTitleIncomeSourceType(incomeSourceType: IncomeSourceType): String = {
    incomeSourceType match {
      case SelfEmployment => "Self Employment"
      case UkProperty => "UK Property"
      case ForeignProperty => "Foreign Property"
    }
  }

  private def sessionWith(previousChoice: Boolean, incomeSourceType: IncomeSourceType): UIJourneySessionData =
    notCompletedUIJourneySessionData(IncomeSourceJourneyType(Add, incomeSourceType)).copy(
      addIncomeSourceData = Some(
        AddIncomeSourceData(
          incomeSourceId = Some(testSelfEmploymentId),
          changeReportingFrequency = Some(previousChoice)
        )
      ),
      incomeSourceReportingFrequencyData = Some(existingRfData)
    )

  private def submitChangeYes(isAgent: Boolean, incomeSourceType: IncomeSourceType, fakeRequest: play.api.test.FakeRequest[_]) = {
    val action = testController.submit(isAgent, true, incomeSourceType)
    action(fakeRequest.withFormUrlEncodedBody("reporting-quarterly-form" -> "true"))
  }


  val incomeSourceTypes: Seq[IncomeSourceType] = List(SelfEmployment, UkProperty, ForeignProperty)

  mtdAllRoles.foreach { mtdRole =>
    val isAgent = mtdRole != MTDIndividual
    incomeSourceTypes.foreach { incomeSourceType =>
      s"show($isAgent, $incomeSourceType)" when {
        val action = testController.show(isAgent, false, incomeSourceType)
        val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
        s"the user is authenticated as a $mtdRole" should {
          "render the reporting frequency page" when {
            s"using the manage businesses journey" in {

              setupMockCalls(isAgent = isAgent, incomeSourceType = incomeSourceType, mtdRole, CURRENT_TAX_YEAR_2024_IN_LATENCY_YEARS)
              mockItsaStatusRetrievalAction()
              val result = action(fakeRequest)

              status(result) shouldBe OK
            }
          }
        }
        testMTDAuthFailuresForRole(action, mtdRole)(fakeRequest)
      }

      s"submit($isAgent, $incomeSourceType)" when {
        val action = testController.submit(isAgent, false, incomeSourceType)
        val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole).withMethod("POST")
        s"the user is authenticated as a $mtdRole" should {
          "return 303 SEE_OTHER and redirect to the check details page" when {
            s"changing answer, yes selected, previous choice was true and RF data exists" in {
              setupMockCalls(isAgent = isAgent, incomeSourceType = incomeSourceType, mtdRole, CURRENT_TAX_YEAR_2024_IN_LATENCY_YEARS)
              mockItsaStatusRetrievalAction()
              setupMockGetMongo(Right(Some(sessionWith(previousChoice = true, incomeSourceType))))

              val expectedRedirectUrl =
                controllers.manageBusinesses.add.routes.IncomeSourceRFCheckDetailsController.show(isAgent, incomeSourceType).url

              val result = submitChangeYes(isAgent, incomeSourceType, fakeRequest)

              status(result) shouldBe SEE_OTHER
              redirectLocation(result) shouldBe Some(expectedRedirectUrl)
            }
          }

          "return 303 SEE_OTHER and redirect to the ChooseTaxYear Page" when {
            s"changing answer, yes selected, previous choice was false (or missing) even if RF data exists" in {
              setupMockCalls(isAgent = isAgent, incomeSourceType = incomeSourceType, mtdRole, CURRENT_TAX_YEAR_2024_IN_LATENCY_YEARS)
              mockItsaStatusRetrievalAction()
              setupMockGetMongo(Right(Some(sessionWith(previousChoice = false, incomeSourceType))))

              val expectedRedirectUrl =
                controllers.manageBusinesses.add.routes.ChooseTaxYearController.show(isAgent, false, incomeSourceType).url

              val result = submitChangeYes(isAgent, incomeSourceType, fakeRequest)

              status(result) shouldBe SEE_OTHER
              redirectLocation(result) shouldBe Some(expectedRedirectUrl)
            }
          }
          "return 303 SEE_OTHER and redirect to the ChooseTaxYear Page" when {
            s"completing the form with yes selected and updates send successfully" in {
              setupMockCalls(isAgent = isAgent, incomeSourceType = incomeSourceType, mtdRole, CURRENT_TAX_YEAR_2024_IN_LATENCY_YEARS)
              mockItsaStatusRetrievalAction()

              val expectedRedirectUrl = controllers.manageBusinesses.add.routes.ChooseTaxYearController.show(isAgent, false, incomeSourceType).url

              val validFormData: Map[String, String] = Map("reporting-quarterly-form" -> "true")
              val result = action(fakeRequest.withFormUrlEncodedBody(validFormData.toSeq: _*))

              status(result) shouldBe SEE_OTHER
              redirectLocation(result) shouldBe Some(expectedRedirectUrl)
            }
          }

          "return 303 SEE_OTHER and redirect to the check details page Page" when {
            s"completing the form with no selected and updates send successfully" in {
              setupMockCalls(isAgent = isAgent, incomeSourceType = incomeSourceType, mtdRole, CURRENT_TAX_YEAR_2024_IN_LATENCY_YEARS)
              mockItsaStatusRetrievalAction()

              val expectedRedirectUrl = controllers.manageBusinesses.add.routes.IncomeSourceRFCheckDetailsController.show(isAgent, incomeSourceType).url

              val validFormData: Map[String, String] = Map("reporting-quarterly-form" -> "false")
              val result = action(fakeRequest.withFormUrlEncodedBody(validFormData.toSeq: _*))

              status(result) shouldBe SEE_OTHER
              redirectLocation(result) shouldBe Some(expectedRedirectUrl)
            }
          }

          "return 400 BAD_REQUEST" when {
            s"invalid form input on the ${getTestTitleIncomeSourceType(incomeSourceType)} page" in {
              setupMockCalls(isAgent = isAgent, incomeSourceType = incomeSourceType, mtdRole, CURRENT_TAX_YEAR_2024_IN_LATENCY_YEARS)
              mockItsaStatusRetrievalAction()
              val invalidFormData: Map[String, String] = Map("report-quarterly" -> "")
              val result = action(fakeRequest.withFormUrlEncodedBody(invalidFormData.toSeq: _*))

              status(result) shouldBe BAD_REQUEST
            }
          }
        }
        testMTDAuthFailuresForRole(action, mtdRole)(fakeRequest)
      }
    }
  }
}
