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

import connectors.{BusinessDetailsConnector, ITSAStatusConnector}
import enums.IncomeSourceJourney._
import enums.JourneyType.{Cease, IncomeSourceJourneyType}
import enums.MTDIndividual
import mocks.auth.MockAuthActions
import mocks.services.{MockNextUpdatesService, MockSessionService}
import models.UIJourneySessionData
import models.incomeSourceDetails._
import models.incomeSourceDetails.viewmodels.{DatesModel, ObligationsViewModel}
import models.obligations._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, reset, when}
import org.mockito.stubbing.OngoingStubbing
import play.api
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.mvc.Result
import play.api.test.Helpers.{defaultAwaitTimeout, status}
import services.{DateService, DateServiceInterface, NextUpdatesService, SessionService}
import testConstants.BaseTestConstants.{testNino, testPropertyIncomeId, testSelfEmploymentId, testSessionId}
import testConstants.BusinessDetailsTestConstants.testIncomeSource
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.{foreignPropertyIncomeWithCeasedForiegnPropertyIncome, ukPropertyIncomeWithCeasedUkPropertyIncome}
import testConstants.incomeSources.IncomeSourcesObligationsTestConstants.quarterlyObligationDatesSimple
import utils.IncomeSourcesUtils

import java.time.LocalDate
import scala.concurrent.Future

class IncomeSourceCeasedObligationsControllerSpec
  extends MockAuthActions
    with MockSessionService with MockNextUpdatesService {

  lazy val mockIncomeSourcesUtils: IncomeSourcesUtils = mock(classOf[IncomeSourcesUtils])

  override lazy val app = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[SessionService].toInstance(mockSessionService),
      api.inject.bind[NextUpdatesService].toInstance(mockNextUpdatesService),
      api.inject.bind[IncomeSourcesUtils].toInstance(mockIncomeSourcesUtils),
      api.inject.bind[DateService].toInstance(dateService),
      api.inject.bind[ITSAStatusConnector].toInstance(mockItsaStatusConnector),
      api.inject.bind[BusinessDetailsConnector].toInstance(mockBusinessDetailsConnector),
      api.inject.bind[DateServiceInterface].toInstance(mockDateServiceInterface)
    ).build()

  lazy val testController = app.injector.instanceOf[IncomeSourceCeasedObligationsController]

  private def setMongoSessionData(
                                   incomeSourceType: IncomeSourceType,
                                   incomeSourceId: Option[String] = Some(testSelfEmploymentId),
                                   ceaseDate: Option[LocalDate] = Some(LocalDate.of(2022, 10, 10))
                                 ): Unit = {

    setupMockCreateSession(true)

    val sessionData = UIJourneySessionData(
      sessionId = testSessionId,
      journeyType = IncomeSourceJourneyType(Cease, incomeSourceType).toString,
      ceaseIncomeSourceData =
        Some(CeaseIncomeSourceData(
          incomeSourceId = incomeSourceId,
          endDate = ceaseDate,
          ceaseIncomeSourceDeclare = Some("true"),
          journeyIsComplete = None
        )),
    )

    setupMockGetMongo(Right(Some(sessionData)))
  }


  val testId = "XAIS00000000001"

  val testObligationsModel: ObligationsModel =
    ObligationsModel(Seq(
      GroupedObligationsModel("123", List(SingleObligationModel(
        LocalDate.of(2022, 7, 1),
        LocalDate.of(2022, 7, 2),
        LocalDate.of(2022, 8, 2),
        "Quarterly",
        None,
        "#001",
        StatusFulfilled
      ),
        SingleObligationModel(
          LocalDate.of(2022, 7, 1),
          LocalDate.of(2022, 7, 2),
          LocalDate.of(2022, 8, 2),
          "Quarterly",
          None,
          "#002",
          StatusFulfilled
        )
      ))
    ))
  private val propertyDetailsModelUK =
    PropertyDetailsModel(
      incomeSourceId = testPropertyIncomeId,
      accountingPeriod = None,
      firstAccountingPeriodEndDate = None,
      incomeSourceType = Some("uk-property"),
      tradingStartDate = None,
      contextualTaxYear = None,
      cessation = None,
      latencyDetails = None
    )

  private val propertyDetailsModelForeign = propertyDetailsModelUK.copy(incomeSourceType = Some("foreign-property"))

  def setUpBusiness(isAgent: Boolean): OngoingStubbing[Future[ObligationsResponseModel]] = {
    val sources: IncomeSourceDetailsModel =
      IncomeSourceDetailsModel(
        nino = testNino,
        mtdbsa = "",
        yearOfMigration = Some("2022"),
        businesses = List(
          BusinessDetailsModel(
            incomeSourceId = testId,
            incomeSource = Some(testIncomeSource),
            accountingPeriod = None,
            tradingName = Some("Test name"),
            firstAccountingPeriodEndDate = None,
            tradingStartDate = Some(LocalDate.of(2022, 1, 1)),
            contextualTaxYear = None,
            cessation = None
          )),
        properties = List.empty
      )

    setupMockGetIncomeSourceDetails(sources)

    val day = LocalDate.of(2023, 1, 1)
    val dates: Seq[DatesModel] = Seq(
      DatesModel(day, day, day, "Quarterly", isFinalDec = false, obligationType = "Quarterly")
    )
    when(mockNextUpdatesService.getObligationsViewModel(any(), any())(any(), any(), any())).thenReturn(Future(ObligationsViewModel(
      quarterlyObligationDatesSimple,
      dates,
      2023,
      showPrevTaxYears = true
    )))
    when(mockNextUpdatesService.getOpenObligations()(any(), any())).
      thenReturn(Future(testObligationsModel))
  }

  def setUpProperty(isAgent: Boolean, isUkProperty: Boolean): OngoingStubbing[Future[ObligationsResponseModel]] = {

    if (isUkProperty) {
      setupMockGetIncomeSourceDetails(ukPropertyIncomeWithCeasedUkPropertyIncome)
      when(mockIncomeSourcesUtils.getActiveProperty(any())(any()))
        .thenReturn(Some(propertyDetailsModelUK))
    } else {
      setupMockGetIncomeSourceDetails(foreignPropertyIncomeWithCeasedForiegnPropertyIncome)
      when(mockIncomeSourcesUtils.getActiveProperty(any())(any()))
        .thenReturn(Some(propertyDetailsModelForeign))
    }

    val day = LocalDate.of(2023, 1, 1)

    val dates: Seq[DatesModel] = Seq(DatesModel(day, day, day, "Quarterly", isFinalDec = false, obligationType = "Quarterly"))

    when(mockNextUpdatesService.getObligationsViewModel(any(), any())(any(), any(), any()))
      .thenReturn(
        Future(
          ObligationsViewModel(
            quarterlyObligationsDates = quarterlyObligationDatesSimple,
            finalDeclarationDates = dates,
            currentTaxYear = 2023,
            showPrevTaxYears = true
          )
        )
      )

    when(mockNextUpdatesService.getOpenObligations()(any(), any())).
      thenReturn(Future(testObligationsModel))
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockIncomeSourceDetailsService)
    when(mockDateServiceInterface.getCurrentDate) thenReturn fixedDate
    when(mockDateServiceInterface.getCurrentTaxYearEnd) thenReturn fixedDate.getYear + 1
    disableAllSwitches()
  }

  val incomeSourceTypes = List(SelfEmployment, UkProperty, ForeignProperty)

  mtdAllRoles.foreach { mtdRole =>

    incomeSourceTypes.foreach { incomeSourceType =>

      val isAgent = mtdRole != MTDIndividual

      s"show${if (isAgent) "Agent"}($incomeSourceType)" when {

        val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
        val action = if (!isAgent) testController.show(incomeSourceType) else testController.showAgent(incomeSourceType)

        s"the user is authenticated as a $mtdRole" should {

          s"return $OK showing ceased business obligation page" when {

            if (incomeSourceType == SelfEmployment) {

              "all required data is available" in {

                setupMockSuccess(mtdRole)
                mockItsaStatusRetrievalAction()
                setUpBusiness(isAgent)
                setMongoSessionData(incomeSourceType)

                val result: Future[Result] = action(fakeRequest)
                status(result) shouldBe OK
              }
            } else {

              "all required data is available" in {

                setupMockSuccess(mtdRole)

                if(incomeSourceType == UkProperty) mockItsaStatusRetrievalAction(ukPropertyIncomeWithCeasedUkPropertyIncome)
                else mockItsaStatusRetrievalAction(foreignPropertyIncomeWithCeasedForiegnPropertyIncome)

                setUpProperty(isAgent = isAgent, isUkProperty = incomeSourceType == UkProperty)

                setupMockCreateSession(true)

                val sessionData = UIJourneySessionData(
                  sessionId = testSessionId,
                  journeyType = IncomeSourceJourneyType(Cease, incomeSourceType).toString,
                  ceaseIncomeSourceData =
                    Some(CeaseIncomeSourceData(
                      incomeSourceId = Some(testPropertyIncomeId),
                      endDate = Some(LocalDate.of(2022, 10, 10)),
                      ceaseIncomeSourceDeclare = Some("true"),
                      journeyIsComplete = None
                    )),
                )

                setupMockGetMongo(Right(Some(sessionData)))

                val result: Future[Result] = action(fakeRequest)
                status(result) shouldBe OK
              }
            }
          }

          s"return $INTERNAL_SERVER_ERROR showing error page" when {

            val isAgent = false
            if (incomeSourceType == SelfEmployment) {

              s"income source ID is missing in session for $SelfEmployment business" in {

                setupMockSuccess(mtdRole)
                setMongoSessionData(incomeSourceId = None, incomeSourceType = incomeSourceType)
                setUpBusiness(isAgent = isAgent)

                val result: Future[Result] = action(fakeRequest)
                status(result) shouldBe INTERNAL_SERVER_ERROR
              }

              s"business end date is missing in session" in {

                setupMockSuccess(mtdRole)
                setMongoSessionData(incomeSourceId = Some(testId), ceaseDate = None, incomeSourceType = incomeSourceType)
                setUpBusiness(isAgent = isAgent)
                val result: Future[Result] = action(fakeRequest)
                status(result) shouldBe INTERNAL_SERVER_ERROR
              }
            } else {
              s"income source ID doesn't match any business for $incomeSourceType business" in {

                setupMockSuccess(mtdRole)
                mockItsaStatusRetrievalAction()

                setMongoSessionData(incomeSourceId = None, incomeSourceType = incomeSourceType)
                setUpBusiness(isAgent = isAgent)
                val result: Future[Result] = action(fakeRequest)
                status(result) shouldBe INTERNAL_SERVER_ERROR
              }

              if (incomeSourceType == UkProperty) {
                s"business end date and income source ID is missing in session" in {
                  setupMockSuccess(mtdRole)
                  setMongoSessionData(incomeSourceId = None, ceaseDate = None, incomeSourceType = incomeSourceType)
                  setUpProperty(isAgent = isAgent, isUkProperty = false)
                  val result: Future[Result] = action(fakeRequest)
                  status(result) shouldBe INTERNAL_SERVER_ERROR
                }
              }
            }
          }
        }
      }
    }
  }
}
