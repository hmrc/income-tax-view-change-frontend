package controllers.optOut

import config.{AgentItvcErrorHandler, ItvcErrorHandler}
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.controllers.predicates.MockAuthenticationPredicate
import mocks.services.MockOptOutService
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus
import models.optout.OptOutMultiYearViewModel
import play.api.http.Status
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{defaultAwaitTimeout, status}
import services.optout.CurrentOptOutTaxYear
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome
import testUtils.TestSupport
import views.html.optOut.OptOutChooseTaxYear

import scala.concurrent.Future

class OptOutChooseTaxYearControllerSpec extends TestSupport
  with MockAuthenticationPredicate with MockFrontendAuthorisedFunctions with MockOptOutService {

  val optOutChooseTaxYear: OptOutChooseTaxYear = app.injector.instanceOf[OptOutChooseTaxYear]
  val mcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]

  val itvcErrorHandler: ItvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler]
  val itvcErrorHandlerAgent: AgentItvcErrorHandler = app.injector.instanceOf[AgentItvcErrorHandler]

  val controller = new OptOutChooseTaxYearController(optOutChooseTaxYear, mockOptOutService)(appConfig,
    ec, testAuthenticator, mockAuthService, itvcErrorHandler, itvcErrorHandlerAgent, mcc)

  val currentTaxYear = TaxYear.forYearEnd(2024)
  val nextTaxYear = currentTaxYear.nextYear
  val previousTaxYear = currentTaxYear.previousYear

  val optOutTaxYear = CurrentOptOutTaxYear(ITSAStatus.Voluntary, currentTaxYear)
  val eligibleTaxYearResponse = Future.successful(Some(OptOutMultiYearViewModel()))
  val noEligibleTaxYearResponse = Future.successful(None)
  val optOutYearsOffered = Seq(previousTaxYear, currentTaxYear, nextTaxYear)
  val optOutYearsOfferedFuture = Future.successful(optOutYearsOffered)

  val counts: Future[Map[Int, Int]] = Future.successful(Map(2022 -> 1, 2023 -> 1, 2024 -> 0))

  def testLogic(isAgent: Boolean): Unit = {

    "show method is invoked" should {
      s"return result with status" in {

        val requestGET = if (isAgent) fakeRequestConfirmedClient() else fakeRequestWithNinoAndOrigin("PTA")

        setupMockAuthorisationSuccess(isAgent)
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
        mockNextUpdatesPageMultiYearOptOutViewModel(eligibleTaxYearResponse)
        mockGetTaxYearsAvailableForOptOut(optOutYearsOfferedFuture)
        mockGetSubmissionCountForTaxYear(optOutYearsOffered, counts)

        val result: Future[Result] = controller.show(isAgent)(requestGET)

        status(result) shouldBe Status.OK
      }
    }

  }

  "OptOutChooseTaxYearController - Individual" when {
    testLogic(isAgent = false)
  }

  "OptOutChooseTaxYearController - Agent" when {
    testLogic(isAgent = true)
  }

}