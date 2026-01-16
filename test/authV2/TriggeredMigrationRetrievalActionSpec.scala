package authV2

import auth.authV2.actions.TriggeredMigrationRetrievalAction
import authV2.AuthActionsTestData.{defaultIncomeSourcesData, getMtdItUser}
import config.{AgentItvcErrorHandler, ItvcErrorHandler}
import connectors.IncomeTaxCalculationConnector
import models.admin.TriggeredMigration
import models.incomeSourceDetails.{BusinessDetailsModel, TaxYear}
import models.itsaStatus.ITSAStatus.{Annual, DigitallyExempt, Dormant, Exempt, Mandated, NoStatus, Voluntary}
import models.itsaStatus.{ITSAStatusResponseModel, StatusDetail, StatusReason}
import models.liabilitycalculation.{Inputs, LiabilityCalculationError, LiabilityCalculationResponse, LiabilityCalculationResponseModel, Metadata, PersonalInformation}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{reset, when}
import org.scalatest.Assertion
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.Application
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{MessagesControllerComponents, Request, Result, Results}
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import services.{DateServiceInterface, ITSAStatusService}
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual}

import java.time.LocalDate
import scala.concurrent.Future

class TriggeredMigrationRetrievalActionSpec extends TestSupport {

  lazy val mockItsaStatusService = mock[ITSAStatusService]
  lazy val mockIncomeTaxCalculationConnector = mock[IncomeTaxCalculationConnector]
  lazy val mockDateServiceInterface = mock[DateServiceInterface]

  override lazy val app: Application =
    new GuiceApplicationBuilder()
      .configure("play.http.router" -> "testOnlyDoNotUseInAppConf.Routes")
      .build()

  lazy val mcc = app.injector.instanceOf[MessagesControllerComponents]
  lazy val itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler]
  lazy val agentErrorHandler = app.injector.instanceOf[AgentItvcErrorHandler]

  val action = new TriggeredMigrationRetrievalAction(
    appConfig,
    mockItsaStatusService,
    mockIncomeTaxCalculationConnector,
    mockDateServiceInterface
  )(
    ec,
    itvcErrorHandler,
    agentErrorHandler,
    mcc
  )

  def defaultAsyncBody(
                        requestTestCase: Request[_] => Assertion
                      ): Request[_] => Future[Result] = testRequest => {
    requestTestCase(testRequest)
    Future.successful(Results.Ok("Successful"))
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockIncomeTaxCalculationConnector)
  }

  val validITSAStatuses = Seq(Voluntary, Mandated)
  val invalidITSAStatuses = Seq(Annual, NoStatus, Exempt, DigitallyExempt, Dormant)

  def incomeSourcesWithChannel(channel: String) = defaultIncomeSourcesData.copy(
    channel = channel,
    businesses = List(BusinessDetailsModel("testId", None, None, None, Some(LocalDate.now()), None, None, None)))

  val testCalcResponse: LiabilityCalculationResponse = LiabilityCalculationResponse(
    inputs = Inputs(personalInformation = PersonalInformation(taxRegime = "UK", class2VoluntaryContributions = None)),
    messages = None,
    calculation = None,
    metadata = Metadata(
      calculationTimestamp = Some("2019-02-15T09:35:15.094Z"),
      calculationType = "crystallisation",
      calculationReason = Some("customerRequest"),
      periodFrom = Some(LocalDate.of(2022, 1, 1)),
      periodTo = Some(LocalDate.of(2023, 1, 1))))

  ".apply()" should {
    "redirect to the home page" when {
      "an individual user has a channel of confirmed and is on a triggered migration page" in {
        enable(TriggeredMigration)

        val confirmedMtdUser = getMtdItUser(Individual, incomeSources = incomeSourcesWithChannel("3"))

        val result = action(true).invokeBlock(confirmedMtdUser, defaultAsyncBody(_.headers.get("Gov-Test-Scenario") shouldBe Some("")))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some("/report-quarterly/income-and-expenses/view")
      }
      "an individual user has a channel of customer led and is on a triggered migration page" in {
        enable(TriggeredMigration)

        val confirmedMtdUser = getMtdItUser(Individual, incomeSources = incomeSourcesWithChannel("1"))

        val result = action(true).invokeBlock(confirmedMtdUser, defaultAsyncBody(_.headers.get("Gov-Test-Scenario") shouldBe Some("")))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some("/report-quarterly/income-and-expenses/view")
      }
      "an agent user has a channel of confirmed and is on a triggered migration page" in {
        enable(TriggeredMigration)

        val confirmedMtdUser = getMtdItUser(Agent, incomeSources = incomeSourcesWithChannel("3"))

        val result = action(true).invokeBlock(confirmedMtdUser, defaultAsyncBody(_.headers.get("Gov-Test-Scenario") shouldBe Some("")))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some("/report-quarterly/income-and-expenses/view/agents/client-income-tax")
      }
      "an agent user has a channel of customer led and is on a triggered migration page" in {
        enable(TriggeredMigration)

        val confirmedMtdUser = getMtdItUser(Agent, incomeSources = incomeSourcesWithChannel("1"))

        val result = action(true).invokeBlock(confirmedMtdUser, defaultAsyncBody(_.headers.get("Gov-Test-Scenario") shouldBe Some("")))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some("/report-quarterly/income-and-expenses/view/agents/client-income-tax")
      }
      "the user is unconfirmed and their CY ITSA status is missing but their CY+1 ITSA status is available" in {
        enable(TriggeredMigration)

        val confirmedMtdUser = getMtdItUser(Individual, incomeSources = incomeSourcesWithChannel("2"))

        when(mockDateServiceInterface.getCurrentTaxYear).thenReturn(TaxYear(2025, 2026))

        when(mockItsaStatusService.getITSAStatusDetail(eqTo(mockDateServiceInterface.getCurrentTaxYear), any(), any())(any(), any(), any()))
          .thenReturn(Future(List(ITSAStatusResponseModel("2025", None))))

        when(mockItsaStatusService.getITSAStatusDetail(eqTo(mockDateServiceInterface.getCurrentTaxYear.nextYear), any(), any())(any(), any(), any()))
          .thenReturn(Future(List(ITSAStatusResponseModel("2026", Some(List(StatusDetail("", Voluntary, StatusReason.Complex)))))))

        when(mockIncomeTaxCalculationConnector.getCalculationResponse(any(), any(), any(), any())(any(), any())).thenReturn(Future(testCalcResponse))

        val result = action(true).invokeBlock(confirmedMtdUser, defaultAsyncBody(_.headers.get("Gov-Test-Scenario") shouldBe None))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some("/report-quarterly/income-and-expenses/view")
      }
    }

    "allow the request to proceed" when {
      "the user has a channel of confirmed and is not on the triggered migration page" in {
        enable(TriggeredMigration)

        val confirmedMtdUser = getMtdItUser(Individual, incomeSources = incomeSourcesWithChannel("1"))

        val result = action(false).invokeBlock(confirmedMtdUser, defaultAsyncBody(_.headers.get("Gov-Test-Scenario") shouldBe None))

        status(result) shouldBe OK
        contentAsString(result) shouldBe "Successful"
      }
      "the user has a channel of customer led and is not on the triggered migration page" in {
        enable(TriggeredMigration)

        val confirmedMtdUser = getMtdItUser(Individual, incomeSources = incomeSourcesWithChannel("3"))

        val result = action(false).invokeBlock(confirmedMtdUser, defaultAsyncBody(_.headers.get("Gov-Test-Scenario") shouldBe None))

        status(result) shouldBe OK
        contentAsString(result) shouldBe "Successful"
      }

      "the user is unconfirmed and their ITSA status is not voluntary or mandated" in {
        enable(TriggeredMigration)

        val confirmedMtdUser = getMtdItUser(Individual, incomeSources = incomeSourcesWithChannel("2"))

        when(mockItsaStatusService.getITSAStatusDetail(any(), any(), any())(any(), any(), any()))
          .thenReturn(Future(List(ITSAStatusResponseModel("2025", Some(List(StatusDetail("", Annual, StatusReason.Complex)))))))

        val result = action(true).invokeBlock(confirmedMtdUser, defaultAsyncBody(_.headers.get("Gov-Test-Scenario") shouldBe None))

        status(result) shouldBe OK
        contentAsString(result) shouldBe "Successful"
      }

      "the user is unconfirmed, their ITSA status is voluntary, and their calculation is crystallised" in {
        enable(TriggeredMigration)

        val confirmedMtdUser = getMtdItUser(Individual, incomeSources = incomeSourcesWithChannel("2"))

        when(mockItsaStatusService.getITSAStatusDetail(any(), any(), any())(any(), any(), any()))
          .thenReturn(Future(List(ITSAStatusResponseModel("2025", Some(List(StatusDetail("", Voluntary, StatusReason.Complex)))))))

        when(mockIncomeTaxCalculationConnector.getCalculationResponse(any(), any(), any(), any())(any(), any())).thenReturn(Future(testCalcResponse))

        val result = action(true).invokeBlock(confirmedMtdUser, defaultAsyncBody(_.headers.get("Gov-Test-Scenario") shouldBe None))

        status(result) shouldBe OK
        contentAsString(result) shouldBe "Successful"
      }
      "the user is unconfirmed, their ITSA status is mandatory, and their calculation is crystallised" in {
        enable(TriggeredMigration)

        val confirmedMtdUser = getMtdItUser(Individual, incomeSources = incomeSourcesWithChannel("2"))

        when(mockItsaStatusService.getITSAStatusDetail(any(), any(), any())(any(), any(), any()))
          .thenReturn(Future(List(ITSAStatusResponseModel("2025", Some(List(StatusDetail("", Mandated, StatusReason.Complex)))))))

        when(mockIncomeTaxCalculationConnector.getCalculationResponse(any(), any(), any(), any())(any(), any())).thenReturn(Future(testCalcResponse))

        val result = action(true).invokeBlock(confirmedMtdUser, defaultAsyncBody(_.headers.get("Gov-Test-Scenario") shouldBe None))

        status(result) shouldBe OK
        contentAsString(result) shouldBe "Successful"
      }

      "the triggered migration feature switch is disabled" in {
        disable(TriggeredMigration)

        val confirmedMtdUser = getMtdItUser(Individual, incomeSources = incomeSourcesWithChannel("2"))

        val result = action(true).invokeBlock(confirmedMtdUser, defaultAsyncBody(_.headers.get("Gov-Test-Scenario") shouldBe None))

        status(result) shouldBe OK
        contentAsString(result) shouldBe "Successful"
      }
    }

    "redirect the user to the triggered migration check HMRC records page" when {
      "the user is unconfirmed, their ITSA status is voluntary, and their calculation is not crystallised" in {
        enable(TriggeredMigration)

        val confirmedMtdUser = getMtdItUser(Individual, incomeSources = incomeSourcesWithChannel("2"))

        when(mockItsaStatusService.getITSAStatusDetail(any(), any(), any())(any(), any(), any()))
          .thenReturn(Future(List(ITSAStatusResponseModel("2025", Some(List(StatusDetail("", Voluntary, StatusReason.Complex)))))))

        when(mockIncomeTaxCalculationConnector.getCalculationResponse(any(), any(), any(), any())(any(), any())).thenReturn(
          Future(testCalcResponse.copy(metadata = testCalcResponse.metadata.copy(calculationType = "inYear"))))

        val result = action(true).invokeBlock(confirmedMtdUser, defaultAsyncBody(_.headers.get("Gov-Test-Scenario") shouldBe None))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some("/report-quarterly/income-and-expenses/view/check-your-active-businesses/hmrc-record")
      }
      "the user is unconfirmed, their ITSA status is mandatory, and their calculation is not crystallised" in {
        enable(TriggeredMigration)

        val confirmedMtdUser = getMtdItUser(Individual, incomeSources = incomeSourcesWithChannel("2"))

        when(mockItsaStatusService.getITSAStatusDetail(any(), any(), any())(any(), any(), any()))
          .thenReturn(Future(List(ITSAStatusResponseModel("2025", Some(List(StatusDetail("", Mandated, StatusReason.Complex)))))))

        when(mockIncomeTaxCalculationConnector.getCalculationResponse(any(), any(), any(), any())(any(), any())).thenReturn(
          Future(testCalcResponse.copy(metadata = testCalcResponse.metadata.copy(calculationType = "inYear"))))

        val result = action(true).invokeBlock(confirmedMtdUser, defaultAsyncBody(_.headers.get("Gov-Test-Scenario") shouldBe None))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some("/report-quarterly/income-and-expenses/view/check-your-active-businesses/hmrc-record")
      }
    }

    "return an error result" when {
      "there is an error retrieving the user's calculation during triggered migration retrieval" in {
        enable(TriggeredMigration)

        val confirmedMtdUser = getMtdItUser(Individual, incomeSources = incomeSourcesWithChannel("2"))

        when(mockItsaStatusService.getITSAStatusDetail(any(), any(), any())(any(), any(), any()))
          .thenReturn(Future(List(ITSAStatusResponseModel("2025", Some(List(StatusDetail("", Mandated, StatusReason.Complex)))))))

        when(mockIncomeTaxCalculationConnector.getCalculationResponse(any(), any(), any(), any())(any(), any())).thenReturn(
          Future(LiabilityCalculationError(500, "Internal Server Error")))

        val result = action(true).invokeBlock(confirmedMtdUser, defaultAsyncBody(_.headers.get("Gov-Test-Scenario") shouldBe None))

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
      "The user has no ITSA statuses available" in {
        enable(TriggeredMigration)

        val confirmedMtdUser = getMtdItUser(Individual, incomeSources = incomeSourcesWithChannel("2"))

        when(mockDateServiceInterface.getCurrentTaxYear).thenReturn(TaxYear(2025, 2026))

        when(mockItsaStatusService.getITSAStatusDetail(eqTo(mockDateServiceInterface.getCurrentTaxYear), any(), any())(any(), any(), any()))
          .thenReturn(Future(List()))

        when(mockItsaStatusService.getITSAStatusDetail(eqTo(mockDateServiceInterface.getCurrentTaxYear.nextYear), any(), any())(any(), any(), any()))
          .thenReturn(Future(List()))

        when(mockIncomeTaxCalculationConnector.getCalculationResponse(any(), any(), any(), any())(any(), any())).thenReturn(Future(testCalcResponse))

        val result = action(true).invokeBlock(confirmedMtdUser, defaultAsyncBody(_.headers.get("Gov-Test-Scenario") shouldBe None))

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }
}
