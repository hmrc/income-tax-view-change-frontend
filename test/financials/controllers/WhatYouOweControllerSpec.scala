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

package financials.controllers

import common.connectors.ITSAStatusConnector
import common.enums.{MTDIndividual, MTDPrimaryAgent, MTDSupportingAgent}
import common.mocks.auth.MockAuthActions
import common.mocks.services.MockDateService
import common.models.admin.{CreditsRefundsRepay, PenaltiesAndAppeals}
import common.models.incomeSourceDetails.TaxYear
import common.models.itsaStatus.ITSAStatusYearOfMigrationModel
import common.services.{DateService, DateServiceInterface, YearOfMigrationService}
import financials.controllers.claimToAdjustPoa.routes as claimToAdjustPoaRoutes
import financials.controllers.routes.{ChargeSummaryController, MoneyInYourAccountController, PaymentController}
import financials.forms.utils.SessionKeys.gatewayPage
import financials.models.outstandingCharges.{OutstandingChargeModel, OutstandingChargesModel}
import financials.models.{BalanceDetails, FinancialDetailsModel, WYOClaimToAdjustViewModel, WhatYouOweChargesList, WhatYouOweViewModel}
import financials.services.WhatYouOweService
import financials.testConstants.ChargeConstants
import financials.testConstants.FinancialDetailsTestConstants.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api
import play.api.Application
import play.api.http.Status
import play.api.test.Helpers.*
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import scala.concurrent.Future
import org.scalatestplus.mockito.MockitoSugar.mock => sMock

class WhatYouOweControllerSpec extends MockAuthActions
  with ChargeConstants with MockDateService{

  lazy val mockWhatYouOweService: WhatYouOweService = mock(classOf[WhatYouOweService])
  implicit val hc: HeaderCarrier = HeaderCarrier()
  lazy val mockDateServiceInjected: DateService = mock(classOfDateService)
  lazy val mockYearOfMigrationService = sMock[YearOfMigrationService]

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[WhatYouOweService].toInstance(mockWhatYouOweService),
      api.inject.bind[YearOfMigrationService].toInstance(mockYearOfMigrationService),
      api.inject.bind[DateService].toInstance(mockDateServiceInjected),
      api.inject.bind[ITSAStatusConnector].toInstance(mockItsaStatusConnector),
      api.inject.bind[DateServiceInterface].toInstance(mockDateServiceInjected)
    ).build()

  lazy val testController = app.injector.instanceOf[WhatYouOweController]

  def testFinancialDetail(taxYear: Int): FinancialDetailsModel = financialDetailsModel(taxYear)

  def whatYouOweChargesListFull: WhatYouOweChargesList = WhatYouOweChargesList(
    BalanceDetails(1.00, 2.00, 0.00, 3.00, None, None, None, None, None, None, None),
    List(chargeItemModel(TaxYear.forYearEnd(2019)))
      ++ List(chargeItemModel(TaxYear.forYearEnd(2020)))
      ++ List(chargeItemModel(TaxYear.forYearEnd(2021))),
    Some(OutstandingChargesModel(List(
      OutstandingChargeModel("BCD", Some(LocalDate.parse("2020-12-31")), 10.23, 1234), OutstandingChargeModel("ACI", None, 1.23, 1234))
    ))
  )

  def whatYouOweChargesListWithReviewReconcile: WhatYouOweChargesList = WhatYouOweChargesList(
    BalanceDetails(1.00, 2.00, 0.00, 3.00, None, None, None, None, None, None, None),
    financialDetailsReviewAndReconcileCi,
    Some(OutstandingChargesModel(List(
      OutstandingChargeModel("BCD", Some(LocalDate.parse("2020-12-31")), 10.23, 1234), OutstandingChargeModel("ACI", None, 1.23, 1234))
    ))
  )

  def whatYouOweChargesListWithOverdueCharge: WhatYouOweChargesList = WhatYouOweChargesList(
    BalanceDetails(1.00, 2.00, 0.00, 3.00, None, None, None, None, None, None, None),
    financialDetailsOverdueCharges,
    Some(OutstandingChargesModel(List(
      OutstandingChargeModel("POA1RR-debit", Some(LocalDate.parse("2010-12-31")), 10.23, 1234), OutstandingChargeModel("POA1RR-debit", Some(LocalDate.parse("2010-12-31")), 1.23, 1234))
    ))
  )

  def whatYouOweChargesListEmpty: WhatYouOweChargesList = WhatYouOweChargesList(BalanceDetails(1.00, 2.00, 0.00, 3.00, None, None, None, None, None, None, None), List.empty)

  def whatYouOweChargesListWithBalancingChargeNotOverdue: WhatYouOweChargesList = WhatYouOweChargesList(
    BalanceDetails(1.00, 2.00, 0.00, 3.00, None, None, None, None, None, None, None),
    financialDetailsBalancingChargeNotOverdue,
    Some(OutstandingChargesModel(List(
      OutstandingChargeModel("BCD", Some(LocalDate.parse("2020-12-31")), 10.23, 1234), OutstandingChargeModel("BCD", None, 1.23, 1234))
    ))
  )

  def whatYouOweChargesListWithLpp2: WhatYouOweChargesList = WhatYouOweChargesList(
    BalanceDetails(1.00, 2.00, 0.00, 3.00, None, None, None, None, None, None, None),
    financialDetailsLPP2,
    Some(OutstandingChargesModel(List()))
  )

  def whatYouOweChargesListWithLPP2NoChargeRef: WhatYouOweChargesList = WhatYouOweChargesList(
    BalanceDetails(1.00, 2.00, 0.00, 3.00, None, None, None, None, None, None, None),
    financialDetailsLPP2NoChargeRef,
    Some(OutstandingChargesModel(List()))
  )

  def ctaViewModel(isFSEnabled: Boolean, poaTaxYear: Option[TaxYear]): WYOClaimToAdjustViewModel = {
    if (isFSEnabled) {
      WYOClaimToAdjustViewModel(poaTaxYear = poaTaxYear)
    } else {
      WYOClaimToAdjustViewModel(poaTaxYear = poaTaxYear)
    }
  }

  def wyoViewModel(isAgent: Boolean,
                   charges: WhatYouOweChargesList = whatYouOweChargesListFull,
                   currentTaxYear: Int = fixedDate.getYear,
                   hasLpiWithDunningLock: Boolean = false,
                   dunningLock: Boolean = false,
                   taxYear: Int = fixedDate.getYear,
                   adjustPaymentsOnAccountFSEnabled: Boolean = false,
                   claimToAdjustViewModel: Option[WYOClaimToAdjustViewModel] = None,
                   LPP2Url: String = "",
                   hasOverdueOrAccruingInterestCharges: Boolean = false,
                   hasCrystallisedInterest: Boolean = false,
                   poaTaxYear: Option[TaxYear] = None,
                   returnsFrontendEnabled: Boolean = false,
                   moneyInYourAccountUrlOpt: Option[String] = None,
                  ): WhatYouOweViewModel = WhatYouOweViewModel(
    currentDate = mockDateServiceInjected.getCurrentDate,
    hasOverdueOrAccruingInterestCharges = hasOverdueOrAccruingInterestCharges,
    hasCrystallisedInterest = hasCrystallisedInterest,
    whatYouOweChargesList = charges,
    hasLpiWithDunningLock = hasLpiWithDunningLock,
    currentTaxYear = currentTaxYear,
    backUrl = "testBackURL",
    utr = Some("1234567890"),
    dunningLock = dunningLock,
    moneyInYourAccountUrl = if (moneyInYourAccountUrlOpt.isDefined) moneyInYourAccountUrlOpt.get else {
      if (isAgent) MoneyInYourAccountController.showAgent().url else MoneyInYourAccountController.show().url
    },
    creditAndRefundEnabled = true,
    taxYearSummaryUrl = taxYearEnd => if (isAgent) {
      appConfig.returnsTaxYearSummaryAgentUrl(taxYearEnd, returnsFrontendEnabled = returnsFrontendEnabled)
    } else {
      appConfig.returnsTaxYearSummaryIndividualUrl(taxYearEnd, returnsFrontendEnabled = returnsFrontendEnabled)
    },
    claimToAdjustViewModel = claimToAdjustViewModel.getOrElse(ctaViewModel(adjustPaymentsOnAccountFSEnabled, poaTaxYear)),
    lpp2Url = LPP2Url,
    adjustPoaUrl = claimToAdjustPoaRoutes.AmendablePoaController.show(isAgent = isAgent).url,
    chargeSummaryUrl = (taxYearEnd: Int, transactionId: String, isInterest: Boolean, origin: Option[String]) => if (isAgent)
      ChargeSummaryController.showAgent(taxYearEnd, transactionId, isInterest).url
    else
      ChargeSummaryController.show(taxYearEnd, transactionId, isInterest, origin).url,
    paymentHandOffUrl = PaymentController.paymentHandoff(_, None).url,
    selfServeTimeToPayEnabled = true,
    selfServeTimeToPayStartUrl = "/self-serve-time-to-pay"
  )

  val noFinancialDetailErrors = List(testFinancialDetail(2018))
  val hasFinancialDetailErrors = List(testFinancialDetail(2018), testFinancialDetailsErrorModel)
  val hasAFinancialDetailError = List(testFinancialDetailsErrorModel)
  val interestChargesWarningText = "! Warning Interest to date is estimated. To stop it increasing every day, pay the related tax in full. It can then take up to 3 working days for the total interest to be calculated and shown here."

  override def beforeEach(): Unit = {
    super.beforeEach()

    when(mockDateServiceInterface.getCurrentDate).thenReturn(fixedDate)
    when(mockDateServiceInterface.getCurrentTaxYearEnd).thenReturn(fixedDate.getYear + 1)
    when(mockDateServiceInjected.getCurrentDate).thenReturn(fixedDate)
    when(mockDateServiceInjected.getCurrentTaxYearEnd).thenReturn(fixedDate.getYear + 1)
    when(mockDateServiceInjected.getCurrentTaxYearStart).thenReturn(LocalDate.of(fixedDate.getYear, 4, 6))
    when(mockDateServiceInjected.getCurrentTaxYear).thenReturn(TaxYear(fixedDate.getYear, fixedDate.getYear + 1))
  }
  
  //ToDo Update these tests to have returnsFrontendEnabled = true when the FS is built
  mtdAllRoles.foreach { case mtdUserRole =>
    val isAgent = mtdUserRole != MTDIndividual
    val action = if (isAgent) testController.showAgent() else testController.show()
    val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdUserRole)
    s"show${if (isAgent) "Agent" else ""}" when {
      s"the $mtdUserRole is authenticated" should {
        if (mtdUserRole == MTDSupportingAgent) {
          testSupportingAgentDeniedAccess(action)(fakeRequest)
        } else {
          "render the what you owe page" that {
            "has payments owed" when {
              "the user has a fill list of charges" in {
                setupMockSuccess(mtdUserRole)
                mockItsaStatusRetrievalAction()
                mockSingleBISWithCurrentYearAsMigrationYear()
                when(mockWhatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any()))
                  .thenReturn(Future.successful(whatYouOweChargesListFull))
                when(mockWhatYouOweService.createWhatYouOweViewModel(any(), any(), any(), any(), any(), any())(any(), any()))
                  .thenReturn(Future(Some(wyoViewModel(isAgent))))
                when(mockDateServiceInjected.getCurrentDate).thenReturn(fixedDate)
                when(mockDateServiceInjected.getCurrentTaxYearEnd).thenReturn(fixedDate.getYear + 1)
                when(mockYearOfMigrationService.getYearOfMigration(any())(any(), any()))
                  .thenReturn(Future.successful(ITSAStatusYearOfMigrationModel(Some("2025"))))

                val result = action(fakeRequest)
                status(result) shouldBe Status.OK
                result.futureValue.session.get(gatewayPage) shouldBe Some("whatYouOwe")
              }

              "the user has no charges" in {
                setupMockSuccess(mtdUserRole)
                mockItsaStatusRetrievalAction()
                mockSingleBISWithCurrentYearAsMigrationYear()
                when(mockWhatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any()))
                  .thenReturn(Future.successful(whatYouOweChargesListEmpty))
                when(mockWhatYouOweService.createWhatYouOweViewModel(any(), any(), any(), any(), any(), any())(any(), any()))
                  .thenReturn(Future(Some(wyoViewModel(isAgent))))
                when(mockYearOfMigrationService.getYearOfMigration(any())(any(), any()))
                  .thenReturn(Future.successful(ITSAStatusYearOfMigrationModel(Some("2025"))))

                val result = action(fakeRequest)
                status(result) shouldBe Status.OK
                result.futureValue.session.get(gatewayPage) shouldBe Some("whatYouOwe")
              }
            }

            "displays the money in your account" when {
              "the user has available credit in his account and CreditsRefundsRepay FS enabled" in {
                def whatYouOweWithAvailableCredits: WhatYouOweChargesList = WhatYouOweChargesList(
                  BalanceDetails(1.00, 2.00, 0.00, 3.00, Some(300.00), None, None, Some(350.00), None, None, None), List.empty)

                setupMockSuccess(mtdUserRole, false, List(CreditsRefundsRepay))
                mockItsaStatusRetrievalAction()
                mockSingleBISWithCurrentYearAsMigrationYear()
                when(mockWhatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any()))
                  .thenReturn(Future.successful(whatYouOweWithAvailableCredits))
                when(mockWhatYouOweService.createWhatYouOweViewModel(any(), any(), any(), any(), any(), any())(any(), any()))
                  .thenReturn(Future(Some(wyoViewModel(isAgent, whatYouOweWithAvailableCredits, moneyInYourAccountUrlOpt = Some("testMoneyInYourAccountUrl")))))
                when(mockYearOfMigrationService.getYearOfMigration(any())(any(), any()))
                  .thenReturn(Future.successful(ITSAStatusYearOfMigrationModel(Some("2025"))))


                val result = action(fakeRequest)
                status(result) shouldBe Status.OK
                result.futureValue.session.get(gatewayPage) shouldBe Some("whatYouOwe")
                val doc: Document = Jsoup.parse(contentAsString(result))
                Option(doc.getElementById("money-in-your-account")).isDefined shouldBe true
                doc.select("#money-in-your-account").select("div h2").text() shouldBe messages("whatYouOwe.moneyOnAccount")
                doc.select("#money-in-your-account-content-link").attr("href") shouldBe "testMoneyInYourAccountUrl"
              }

              "the user has available credit in his account and CreditsRefundsRepay FS enabled with no year of migration" in {
                def whatYouOweWithAvailableCredits: WhatYouOweChargesList = WhatYouOweChargesList(
                  BalanceDetails(1.00, 2.00, 0.00, 3.00, Some(300.00), None, None, Some(350.00), None, None, None), List.empty)

                setupMockSuccess(mtdUserRole, false, List(CreditsRefundsRepay))
                mockItsaStatusRetrievalAction()
                mockSingleBISWithCurrentYearAsMigrationYear()
                when(mockWhatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any()))
                  .thenReturn(Future.successful(whatYouOweWithAvailableCredits))
                when(mockWhatYouOweService.createWhatYouOweViewModel(any(), any(), any(), any(), any(), any())(any(), any()))
                  .thenReturn(Future(Some(wyoViewModel(isAgent, whatYouOweWithAvailableCredits, moneyInYourAccountUrlOpt = Some("testMoneyInYourAccountUrl")))))
                when(mockYearOfMigrationService.getYearOfMigration(any())(any(), any()))
                  .thenReturn(Future.successful(ITSAStatusYearOfMigrationModel(None)))


                val result = action(fakeRequest)
                status(result) shouldBe Status.OK
                result.futureValue.session.get(gatewayPage) shouldBe Some("whatYouOwe")
                val doc: Document = Jsoup.parse(contentAsString(result))
                Option(doc.getElementById("money-in-your-account")).isDefined shouldBe true
                doc.select("#money-in-your-account").select("div h2").text() shouldBe messages("whatYouOwe.moneyOnAccount")
                doc.select("#money-in-your-account-content-link").attr("href") shouldBe "testMoneyInYourAccountUrl"
              }
            }

            "does not display the money in your account" when {
              "the user has available credit in his account but CreditsRefundsRepay FS disabled" in {
                def whatYouOweWithZeroAvailableCredits: WhatYouOweChargesList = WhatYouOweChargesList(
                  BalanceDetails(1.00, 2.00, 0.00, 3.00, Some(0.00), None, None, None, None, None, None), List.empty)

                setupMockSuccess(mtdUserRole)
                mockItsaStatusRetrievalAction()
                mockSingleBISWithCurrentYearAsMigrationYear()
                when(mockWhatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any()))
                  .thenReturn(Future.successful(whatYouOweWithZeroAvailableCredits))
                when(mockWhatYouOweService.createWhatYouOweViewModel(any(), any(), any(), any(), any(), any())(any(), any()))
                  .thenReturn(Future(Some(wyoViewModel(isAgent, whatYouOweWithZeroAvailableCredits))))


                val result = action(fakeRequest)
                status(result) shouldBe Status.OK
                result.futureValue.session.get(gatewayPage) shouldBe Some("whatYouOwe")
                val doc: Document = Jsoup.parse(contentAsString(result))
                Option(doc.getElementById("money-in-your-account")).isDefined shouldBe false
              }
            }

            "contains the adjust POA" when {
              "there are adjustable POA" in {
                setupMockSuccess(mtdUserRole)
                mockItsaStatusRetrievalAction()
                mockSingleBISWithCurrentYearAsMigrationYear()

                val poaModel: WYOClaimToAdjustViewModel = ctaViewModel(true, Some(TaxYear(2017, 2018)))

                when(mockWhatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any()))
                  .thenReturn(Future.successful(whatYouOweChargesListFull))
                when(mockWhatYouOweService.createWhatYouOweViewModel(any(), any(), any(), any(), any(), any())(any(), any()))
                  .thenReturn(Future(Some(wyoViewModel(isAgent, claimToAdjustViewModel = Some(poaModel)))))


                val result = action(fakeRequest)
                contentAsString(result).contains("Adjust your payments on account for the 2017 to 2018 tax year") shouldBe true
              }
              "there are no adjustable POAs" in {
                setupMockSuccess(mtdUserRole)
                mockItsaStatusRetrievalAction()
                mockSingleBISWithCurrentYearAsMigrationYear()

                when(mockWhatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any()))
                  .thenReturn(Future.successful(whatYouOweChargesListFull))
                when(mockWhatYouOweService.createWhatYouOweViewModel(any(), any(), any(), any(), any(), any())(any(), any()))
                  .thenReturn(Future(Some(wyoViewModel(isAgent))))


                val result = action(fakeRequest)
                contentAsString(result).contains("Adjust payments on account for the") shouldBe false

              }
            }

            "that includes poa extra charges in charges table" when {
              "ReviewAndReconcilePoa FS is enabled" in {
                mockSingleBISWithCurrentYearAsMigrationYear()
                setupMockSuccess(mtdUserRole)
                mockItsaStatusRetrievalAction()

                when(mockWhatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any()))
                  .thenReturn(Future.successful(whatYouOweChargesListWithReviewReconcile))
                when(mockWhatYouOweService.createWhatYouOweViewModel(any(), any(), any(), any(), any(), any())(any(), any()))
                  .thenReturn(Future(Some(wyoViewModel(isAgent, charges = whatYouOweChargesListWithReviewReconcile))))

                val result = action(fakeRequest)

                status(result) shouldBe Status.OK
                contentAsString(result).contains("First payment on account: extra amount from your tax return") shouldBe true
              }
            }

            "that includes a interest charges warning" when {
              "an overdue charge exists" in {
                mockSingleBISWithCurrentYearAsMigrationYear()
                setupMockSuccess(mtdUserRole)
                mockItsaStatusRetrievalAction()
                when(mockWhatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any()))
                  .thenReturn(Future.successful(whatYouOweChargesListWithOverdueCharge))
                when(mockWhatYouOweService.createWhatYouOweViewModel(any(), any(), any(), any(), any(), any())(any(), any()))
                  .thenReturn(Future(Some(wyoViewModel(isAgent, charges = whatYouOweChargesListWithOverdueCharge, hasOverdueOrAccruingInterestCharges = true))))

                val result = action(fakeRequest)

                status(result) shouldBe Status.OK
                Jsoup.parse(contentAsString(result)).getElementById("interest-charges-warning").text() shouldBe interestChargesWarningText
              }

              "Review and Reconcile charge with accruing interest exists" in {
                mockSingleBISWithCurrentYearAsMigrationYear()
                setupMockSuccess(mtdUserRole)
                mockItsaStatusRetrievalAction()
                when(mockWhatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any()))
                  .thenReturn(Future.successful(whatYouOweChargesListWithOverdueCharge))
                when(mockWhatYouOweService.createWhatYouOweViewModel(any(), any(), any(), any(), any(), any())(any(), any()))
                  .thenReturn(Future(Some(wyoViewModel(isAgent, charges = whatYouOweChargesListWithOverdueCharge, hasOverdueOrAccruingInterestCharges = true))))

                val result = action(fakeRequest)

                status(result) shouldBe Status.OK
                Jsoup.parse(contentAsString(result)).getElementById("interest-charges-warning").text() shouldBe interestChargesWarningText
              }
            }

            "that hides the interest charge warning" when {
              "there are no overdue charges or unpaid Review & Reconcile charges" in {
                mockSingleBISWithCurrentYearAsMigrationYear()
                setupMockSuccess(mtdUserRole)
                mockItsaStatusRetrievalAction()
                when(mockWhatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any()))
                  .thenReturn(Future.successful(whatYouOweChargesListWithBalancingChargeNotOverdue))
                when(mockWhatYouOweService.createWhatYouOweViewModel(any(), any(), any(), any(), any(), any())(any(), any()))
                  .thenReturn(Future(Some(wyoViewModel(isAgent, charges = whatYouOweChargesListWithBalancingChargeNotOverdue))))

                val result = action(fakeRequest)
                status(result) shouldBe Status.OK
                Option(Jsoup.parse(contentAsString(result)).getElementById("interest-charges-warning")).isDefined shouldBe false
              }
            }

            "user has a second late payment penalty with a chargeReference, so url can be generated" in {
              mockSingleBISWithCurrentYearAsMigrationYear()
              setupMockSuccess(mtdUserRole, false, List(PenaltiesAndAppeals))
              mockItsaStatusRetrievalAction()

              when(mockWhatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any()))
                .thenReturn(Future.successful(whatYouOweChargesListWithLpp2))
              when(mockWhatYouOweService.createWhatYouOweViewModel(any(), any(), any(), any(), any(), any())(any(), any()))
                .thenReturn(Future(Some(wyoViewModel(isAgent, charges = whatYouOweChargesListWithLpp2))))

              val result = action(fakeRequest)
              status(result) shouldBe Status.OK
            }
          }

          "render the error page" when {
            "PaymentsDueService returns an exception" in {
              setupMockSuccess(mtdUserRole)
              mockItsaStatusRetrievalAction()
              mockSingleBISWithCurrentYearAsMigrationYear()
              when(mockWhatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any()))
                .thenReturn(Future.failed(new Exception("failed to retrieve data")))
              when(mockWhatYouOweService.createWhatYouOweViewModel(any(), any(), any(), any(), any(), any())(any(), any()))
                .thenReturn(Future(None))


              val result = action(fakeRequest)
              status(result) shouldBe Status.INTERNAL_SERVER_ERROR
            }

            "fetching POA entry point fails" in {
              setupMockSuccess(mtdUserRole)
              mockItsaStatusRetrievalAction()
              mockSingleBISWithCurrentYearAsMigrationYear()

              when(mockWhatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any()))
                .thenReturn(Future.successful(whatYouOweChargesListFull))
              when(mockWhatYouOweService.createWhatYouOweViewModel(any(), any(), any(), any(), any(), any())(any(), any()))
                .thenReturn(Future(None))

              val result = action(fakeRequest)
              status(result) shouldBe Status.INTERNAL_SERVER_ERROR
            }

            "user has a second late payment penalty without a chargeReference, so url cannot be generated" in {
              mockSingleBISWithCurrentYearAsMigrationYear()
              setupMockSuccess(mtdUserRole, false, List(PenaltiesAndAppeals))
              mockItsaStatusRetrievalAction()

              when(mockWhatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any()))
                .thenReturn(Future.successful(whatYouOweChargesListWithLPP2NoChargeRef))
              when(mockWhatYouOweService.createWhatYouOweViewModel(any(), any(), any(), any(), any(), any())(any(), any()))
                .thenReturn(Future(None))

              val result = action(fakeRequest)
              status(result) shouldBe Status.INTERNAL_SERVER_ERROR
            }
          }
        }
      }
      testMTDAuthFailuresForRole(action, mtdUserRole, supportingAgentAccessAllowed = false)(fakeRequest)
    }
  }

  "getMoneyInYourAccountUrl" should {
    "return the correct url for an agent" in {
      when(mockYearOfMigrationService.getYearOfMigration(any())(any(), any()))
        .thenReturn(Future.successful(ITSAStatusYearOfMigrationModel(Some("2025"))))

      val result = testController.getMoneyInYourAccountUrl(getAgentUser(fakeGetRequestBasedOnMTDUserType(MTDPrimaryAgent)))
      result.futureValue shouldBe routes.MoneyInYourAccountController.showAgent().url
    }

    "return the correct url for an individual" in {
      when(mockYearOfMigrationService.getYearOfMigration(any())(any(), any()))
        .thenReturn(Future.successful(ITSAStatusYearOfMigrationModel(Some("2025"))))

      val result = testController.getMoneyInYourAccountUrl()
      result.futureValue shouldBe routes.MoneyInYourAccountController.show().url
    }

    "return the correct url for an individual when no year of migration" in {
      when(mockYearOfMigrationService.getYearOfMigration(any())(any(), any()))
        .thenReturn(Future.successful(ITSAStatusYearOfMigrationModel(None)))

      val result = testController.getMoneyInYourAccountUrl()
      result.futureValue shouldBe routes.NotMigratedUserController.show().url
    }
  }
}
