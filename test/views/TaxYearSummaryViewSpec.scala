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

package views

import config.featureswitch.FeatureSwitching
import implicits.ImplicitCurrencyFormatter.{CurrencyFormatter, CurrencyFormatterInt}
import implicits.ImplicitDateFormatterImpl
import models.financialDetails._
import models.incomeSourceDetails.TaxYear
import models.liabilitycalculation.viewmodels.{CalculationSummary, TYSClaimToAdjustViewModel, TaxYearSummaryViewModel}
import models.liabilitycalculation.{Message, Messages}
import models.obligations.{ObligationWithIncomeType, ObligationsModel}
import models.taxyearsummary.TaxYearSummaryChargeItem
import org.jsoup.nodes.Element
import play.twirl.api.Html
import testConstants.ChargeConstants
import testConstants.FinancialDetailsTestConstants.{MFADebitsDocumentDetailsWithDueDates, fullDocumentDetailModel}
import testConstants.NextUpdatesTestConstants._
import testUtils.ViewSpec
import views.html.TaxYearSummary

import java.time.LocalDate

class TaxYearSummaryViewSpec extends ViewSpec with FeatureSwitching with ChargeConstants {

  val testYear: Int = 2018
  val hrefForecastSelector: String = """a[href$="#forecast"]"""

  val implicitDateFormatter: ImplicitDateFormatterImpl = app.injector.instanceOf[ImplicitDateFormatterImpl]
  val taxYearSummaryView: TaxYearSummary = app.injector.instanceOf[TaxYearSummary]

  import TaxYearSummaryMessages._
  import implicitDateFormatter._

  def modelComplete(crystallised: Boolean, unattendedCalc: Boolean = false): CalculationSummary =
    CalculationSummary(
      timestamp = Some("2020-01-01T00:35:34.185Z".toZonedDateTime.toLocalDate),
      income = 1,
      deductions = 2.02,
      totalTaxableIncome = 3,
      taxDue = 4.04,
      crystallised = crystallised,
      unattendedCalc = unattendedCalc,
      forecastIncome = Some(12500),
      forecastIncomeTaxAndNics = Some(5000.99),
      forecastAllowancesAndDeductions = Some(4200.00),
      forecastTotalTaxableIncome = Some(8300),
      periodFrom = Some(LocalDate.of(testYear - 1, 1, 1)),
      periodTo = Some(LocalDate.of(testYear, 1, 1))
    )

  val date: String = dateService.getCurrentDate.toLongDate

  val modelWithMultipleErrorMessages = modelComplete(crystallised = false).copy(messages = Some(Messages(errors = Some(List(
    Message("C15014", date),
    Message("C55014", date),
    Message("C15015", ""),
    Message("C15016", ""),
    Message("C15102", ""),
    Message("C15103", ""),
    Message("C15104", ""),
    Message("C15105", ""),
    Message("C15322", ""),
    Message("C15323", ""),
    Message("C15325", ""),
    Message("C15523", ""),
    Message("C15524", ""),
    Message("C15506", ""),
    Message("C15507", "1000"),
    Message("C15510", "50"),
    Message("C15518", ""),
    Message("C15530", ""),
    Message("C15319", ""),
    Message("C15320", ""),
    Message("C15522", ""),
    Message("C15531", ""),
    Message("C15324", ""),
    Message("C55316", ""),
    Message("C55317", ""),
    Message("C55318", ""),
    Message("C55501", ""),
    Message("C55502", ""),
    Message("C55503", ""),
    Message("C55508", ""),
    Message("C55008", date),
    Message("C55011", date),
    Message("C55009", ""),
    Message("C55010", ""),
    Message("C55012", date),
    Message("C55013", date),
    Message("C55009", ""),
    Message("C55511", ""),
    Message("C55519", ""),
    Message("C55515", ""),
    Message("C55516", ""),
    Message("C55517", ""),
    Message("C55520", ""),
    Message("C95005", ""),
    Message("C159014", ""),
    Message("C159015", ""),
    Message("C159016", ""),
    Message("C159018", ""),
    Message("C159019", ""),
    Message("C159026", ""),
    Message("C159027", ""),
    Message("C159028", ""),
    Message("C159030", ""),
    Message("C159102", ""),
    Message("C159106", ""),
    Message("C159110", "50"),
    Message("C159115", ""),
    Message("C159500", ""),
    Message("C559099", ""),
    Message("C559100", ""),
    Message("C559101", ""),
    Message("C559103", ""),
    Message("C559107", ""),
    Message("C559104", ""),
    Message("C559105", ""),
    Message("C559113", ""),
    Message("C559114", "")
  ))
  )))

  val modelWithErrorMessages = modelComplete(crystallised = false)
    .copy(messages = Some(Messages(
      errors = Some(List(
        Message("C15015", "you’ve claimed to carry forward a loss to set against general income of the next year. You also need to make the claim in the same year the loss arose.")
      ))
    )))

  val testDunningLockChargesList: List[TaxYearSummaryChargeItem] = List(
    TaxYearSummaryChargeItem.fromChargeItem(chargeItemModel(transactionType = PoaOneDebit, dueDate = Some(LocalDate.of(2019, 6, 15)), dunningLock = true),
      dueDate = Some(LocalDate.of(2019, 6, 15))),
    TaxYearSummaryChargeItem.fromChargeItem(chargeItemModel(transactionType = PoaTwoDebit, dueDate = Some(LocalDate.of(2019, 7, 15)), dunningLock = false),
      dueDate = Some(LocalDate.of(2019, 7, 15))),
    TaxYearSummaryChargeItem.fromChargeItem(chargeItemModel(transactionType = BalancingCharge, dueDate = Some(LocalDate.of(2019, 8, 15)), dunningLock = true),
      dueDate = Some(LocalDate.of(2019, 8, 15))))

  val testChargesList: List[TaxYearSummaryChargeItem] = List(
    TaxYearSummaryChargeItem.fromChargeItem(
      chargeItemModel(transactionType = PoaOneDebit, dueDate = Some(LocalDate.of(2019, 6, 15)), accruingInterestAmount = Some(100.0)),
      dueDate = Some(LocalDate.of(2019, 6, 15)), isLatePaymentInterest = true),
    TaxYearSummaryChargeItem.fromChargeItem(
      chargeItemModel(transactionType = PoaTwoDebit, dueDate = Some(LocalDate.of(2019, 7, 15)), accruingInterestAmount = Some(80.0)),
      dueDate = Some(LocalDate.of(2019, 7, 15)), isLatePaymentInterest = true),
    TaxYearSummaryChargeItem.fromChargeItem(
      chargeItemModel(transactionType = BalancingCharge, dueDate = Some(LocalDate.of(2019, 8, 15)), interestOutstandingAmount = Some(0.0)),
      dueDate = Some(LocalDate.of(2019, 8, 15)), isLatePaymentInterest = true)
  )

  val testChargesWithoutLpiList: List[TaxYearSummaryChargeItem] = testChargesList.map(_.copy(isAccruingInterest = false))

  val class2NicsChargesList: List[TaxYearSummaryChargeItem] = List(
    chargeItemModel(transactionType = PoaOneDebit, dueDate = Some(LocalDate.of(2021, 7, 31)), accruingInterestAmount = None),
    chargeItemModel(transactionType = BalancingCharge, codedOutStatus = Some(Nics2), dueDate = Some(LocalDate.of(2021, 7, 30)), accruingInterestAmount = None)
  ).map(TaxYearSummaryChargeItem.fromChargeItem(_))


  val payeChargeList: List[TaxYearSummaryChargeItem] = List(
    chargeItemModel(transactionType = BalancingCharge, dueDate = Some(LocalDate.of(2021, 7, 30)), codedOutStatus = Some(Accepted), accruingInterestAmount = None)
  ).map(TaxYearSummaryChargeItem.fromChargeItem)

  val testBalancingPaymentChargeWithZeroValue: List[TaxYearSummaryChargeItem] = List(
    chargeItemModel(transactionType = BalancingCharge, originalAmount = 0.0, accruingInterestAmount = None)).map(TaxYearSummaryChargeItem.fromChargeItem)

  def testPaymentsOnAccountCodedOut(codedOutStatus: CodedOutStatusType): List[TaxYearSummaryChargeItem] = List(
    chargeItemModel(transactionType = PoaOneDebit, codedOutStatus = Some(codedOutStatus), accruingInterestAmount = None),
    chargeItemModel(transactionType = PoaTwoDebit, codedOutStatus = Some(codedOutStatus), accruingInterestAmount = None)
  ).map(TaxYearSummaryChargeItem.fromChargeItem)

  val testPaymentsOnAccountCodedOut: List[TaxYearSummaryChargeItem] = List(
    chargeItemModel(transactionType = PoaOneDebit, codedOutStatus = Some(Accepted), accruingInterestAmount = None),
    chargeItemModel(transactionType = PoaTwoDebit, codedOutStatus = Some(Accepted), accruingInterestAmount = None)
  ).map(TaxYearSummaryChargeItem.fromChargeItem)

  val testPaymentsOnAccountCodedOutCancelled: List[TaxYearSummaryChargeItem] = List(
    chargeItemModel(transactionType = PoaOneDebit, codedOutStatus = Some(Cancelled), accruingInterestAmount = None, dueDate = Some(LocalDate.of(2040, 3, 31)), lpiWithDunningLock = None),
    chargeItemModel(transactionType = PoaTwoDebit, codedOutStatus = Some(Cancelled), accruingInterestAmount = None, dueDate = Some(LocalDate.of(2040, 3, 31)), lpiWithDunningLock = None)
  ).map(TaxYearSummaryChargeItem.fromChargeItem)


  val immediatelyRejectedByNps: List[TaxYearSummaryChargeItem] = List(
    chargeItemModel(transactionType = BalancingCharge, codedOutStatus = Some(Nics2), accruingInterestAmount = None),
    chargeItemModel(transactionType = BalancingCharge, interestOutstandingAmount = Some(0.0), accruingInterestAmount = None)
  ).map(TaxYearSummaryChargeItem.fromChargeItem)

  val rejectedByNpsPartWay: List[TaxYearSummaryChargeItem] = List(
    chargeItemModel(transactionType = BalancingCharge, codedOutStatus = Some(Nics2), accruingInterestAmount = None),
    chargeItemModel(transactionType = BalancingCharge, codedOutStatus = Some(Cancelled), accruingInterestAmount = None)
  ).map(TaxYearSummaryChargeItem.fromChargeItem)

  val codingOutPartiallyCollected: List[TaxYearSummaryChargeItem] = List(
    chargeItemModel(transactionType = BalancingCharge, codedOutStatus = Some(Nics2), accruingInterestAmount = None),
    chargeItemModel(transactionType = BalancingCharge, interestOutstandingAmount = Some(0.0), accruingInterestAmount = None),
    chargeItemModel(transactionType = BalancingCharge, codedOutStatus = Some(Cancelled), accruingInterestAmount = None)
  ).map(TaxYearSummaryChargeItem.fromChargeItem)

  val mfaCharges: List[TaxYearSummaryChargeItem] = List(
    chargeItemModel(transactionId = "MFADEBIT01", transactionType = MfaDebitCharge, originalAmount = 100.0, outstandingAmount = 100.0, accruingInterestAmount = None),
    chargeItemModel(transactionId = "MFADEBIT02", transactionType = MfaDebitCharge, originalAmount = 100.0, outstandingAmount = 100.0, accruingInterestAmount = None),
    chargeItemModel(transactionId = "MFADEBIT03", transactionType = MfaDebitCharge, originalAmount = 100.0, outstandingAmount = 100.0, accruingInterestAmount = None)
  ).map(TaxYearSummaryChargeItem.fromChargeItem)

  val documentDetailWithDueDateMissingDueDate: List[ChargeItem] = List(
    chargeItemModel(dueDate = None)
  )

  val emptyChargeList: List[TaxYearSummaryChargeItem] = List.empty

  val testWithOneMissingDueDateChargesList: List[TaxYearSummaryChargeItem] = List(
    TaxYearSummaryChargeItem.fromChargeItem(chargeItemModel()),
    TaxYearSummaryChargeItem.fromChargeItem(chargeItemModel(), dueDate = None)
  )

  val testWithMissingOriginalAmountChargesList: List[TaxYearSummaryChargeItem] = List(
    TaxYearSummaryChargeItem.fromChargeItem(chargeItemModel(originalAmount = 1000.0)),
  )

  val ctaLink: String = "/report-quarterly/income-and-expenses/view/adjust-poa/start"

  val testObligationsModel: ObligationsModel = ObligationsModel(Seq(nextUpdatesDataSelfEmploymentSuccessModel))

  val emptyCTAModel: TYSClaimToAdjustViewModel = TYSClaimToAdjustViewModel(poaTaxYear = None)

  val testCTAModel: TYSClaimToAdjustViewModel = TYSClaimToAdjustViewModel(poaTaxYear = Some(TaxYear(2023, 2024)))

  def estimateView(chargeItems: List[TaxYearSummaryChargeItem] = testChargesList, isAgent: Boolean = false, obligations: ObligationsModel = testObligationsModel): Html = taxYearSummaryView(
    testYear, TaxYearSummaryViewModel(Some(modelComplete(crystallised = false)), previousCalculationSummary = None, chargeItems, obligations, ctaViewModel = emptyCTAModel, LPP2Url = "", pfaEnabled = false, hasAmendments = false), "testBackURL", isAgent, ctaLink = ctaLink)

  def class2NicsView(isAgent: Boolean = false): Html = taxYearSummaryView(
    testYear, TaxYearSummaryViewModel(Some(modelComplete(crystallised = false)), previousCalculationSummary = None, class2NicsChargesList
      , testObligationsModel, ctaViewModel = emptyCTAModel, LPP2Url = "", pfaEnabled = false, hasAmendments = false), "testBackURL", isAgent, ctaLink = ctaLink)

  def estimateViewWithNoCalcData(isAgent: Boolean = false): Html = taxYearSummaryView(
    testYear, TaxYearSummaryViewModel(None, None, testChargesList, testObligationsModel, ctaViewModel = emptyCTAModel, LPP2Url = "", pfaEnabled = false, hasAmendments = false), "testBackURL", isAgent, ctaLink = ctaLink)

  def unattendedCalcView(isAgent: Boolean = false, unattendedCalc: Boolean): Html = taxYearSummaryView(
    testYear, TaxYearSummaryViewModel(Some(modelComplete(crystallised = false, unattendedCalc = unattendedCalc)), None, testChargesList, testObligationsModel, ctaViewModel = emptyCTAModel, LPP2Url = "", pfaEnabled = false, hasAmendments = false), "testBackUrl", isAgent, ctaLink = ctaLink)

  def multipleDunningLockView(isAgent: Boolean = false): Html = taxYearSummaryView(
    testYear, TaxYearSummaryViewModel(Some(modelComplete(crystallised = false)), None, testDunningLockChargesList, testObligationsModel, ctaViewModel = emptyCTAModel, LPP2Url = "", pfaEnabled = false, hasAmendments = false), "testBackURL", isAgent, ctaLink = ctaLink)

  def crystallisedView(isAgent: Boolean = false): Html = taxYearSummaryView(
    testYear, TaxYearSummaryViewModel(Some(modelComplete(crystallised = true)), None, testChargesList, testObligationsModel, ctaViewModel = emptyCTAModel, LPP2Url = "", pfaEnabled = false, hasAmendments = false), "testBackURL", isAgent, ctaLink = ctaLink)

  def payeView(isAgent: Boolean = false): Html = taxYearSummaryView(
    testYear, TaxYearSummaryViewModel(Some(modelComplete(crystallised = false)), None, payeChargeList, testObligationsModel, ctaViewModel = emptyCTAModel, LPP2Url = "", pfaEnabled = false, hasAmendments = false), "testBackURL", isAgent, ctaLink = ctaLink)

  def testBalancingPaymentChargeWithZeroValueView(isAgent: Boolean = false): Html = taxYearSummaryView(
    testYear, TaxYearSummaryViewModel(Some(modelComplete(crystallised = false)), None, testBalancingPaymentChargeWithZeroValue, testObligationsModel, ctaViewModel = emptyCTAModel, LPP2Url = "", pfaEnabled = false, hasAmendments = false), "testBackURL", isAgent, ctaLink = ctaLink)

  def testPaymentOnAccountChargesCodedOutAcceptedView(isAgent: Boolean = false): Html = taxYearSummaryView(
    testYear, TaxYearSummaryViewModel(Some(modelComplete(crystallised = false)), None, testPaymentsOnAccountCodedOut(Accepted), testObligationsModel, ctaViewModel = emptyCTAModel, LPP2Url = "", pfaEnabled = false, hasAmendments = false), "testBackURL", isAgent, ctaLink = ctaLink)

  def testPaymentOnAccountChargesCodedOutFullyCollectedView(isAgent: Boolean = false): Html = taxYearSummaryView(
    testYear, TaxYearSummaryViewModel(Some(modelComplete(crystallised = false)), None, testPaymentsOnAccountCodedOut(FullyCollected), testObligationsModel, ctaViewModel = emptyCTAModel, LPP2Url = "", pfaEnabled = false, hasAmendments = false), "testBackURL", isAgent, ctaLink = ctaLink)


  def testPaymentOnAccountChargesCodedOutCancelledView(isAgent: Boolean = false): Html = taxYearSummaryView(
    testYear, TaxYearSummaryViewModel(Some(modelComplete(crystallised = false)), None, testPaymentsOnAccountCodedOutCancelled, testObligationsModel, ctaViewModel = emptyCTAModel, LPP2Url = "", pfaEnabled = false, hasAmendments = false), "testBackURL", isAgent, ctaLink = ctaLink)

  def immediatelyRejectedByNpsView(isAgent: Boolean = false): Html = taxYearSummaryView(
    testYear, TaxYearSummaryViewModel(Some(modelComplete(crystallised = false)), None, immediatelyRejectedByNps, testObligationsModel, ctaViewModel = emptyCTAModel, LPP2Url = "", pfaEnabled = false, hasAmendments = false), "testBackURL", isAgent, ctaLink = ctaLink)

  def rejectedByNpsPartWayView(isAgent: Boolean = false): Html = taxYearSummaryView(
    testYear, TaxYearSummaryViewModel(Some(modelComplete(crystallised = false)), None, rejectedByNpsPartWay, testObligationsModel, ctaViewModel = emptyCTAModel, LPP2Url = "", pfaEnabled = false, hasAmendments = false), "testBackURL", isAgent, ctaLink = ctaLink)

  def codingOutPartiallyCollectedView(isAgent: Boolean = false): Html = taxYearSummaryView(
    testYear, TaxYearSummaryViewModel(Some(modelComplete(crystallised = false)), None, codingOutPartiallyCollected, testObligationsModel, ctaViewModel = emptyCTAModel, LPP2Url = "", pfaEnabled = false, hasAmendments = false), "testBackURL", isAgent, ctaLink = ctaLink)

  def forecastCalcView(isAgent: Boolean = false): Html = taxYearSummaryView(
    testYear, TaxYearSummaryViewModel(Some(modelComplete(crystallised = false)), None, testChargesList, testObligationsModel, showForecastData = true, ctaViewModel = emptyCTAModel, LPP2Url = "", pfaEnabled = false, hasAmendments = false), "testBackURL", isAgent, ctaLink = ctaLink)

  def forecastCalcViewCrystallised(isAgent: Boolean = false): Html = taxYearSummaryView(
    testYear, TaxYearSummaryViewModel(Some(modelComplete(crystallised = true)), None, testChargesList, testObligationsModel, showForecastData = true, ctaViewModel = emptyCTAModel, LPP2Url = "", pfaEnabled = false, hasAmendments = false), "testBackURL", isAgent, ctaLink = ctaLink)

  def noForecastDataView(isAgent: Boolean = false): Html = taxYearSummaryView(
    testYear, TaxYearSummaryViewModel(Some(modelComplete(crystallised = false)), None, testChargesList, testObligationsModel, ctaViewModel = emptyCTAModel, LPP2Url = "", pfaEnabled = false, hasAmendments = false), "testBackURL", isAgent, ctaLink = ctaLink)

  def forecastWithNoCalcData(isAgent: Boolean = false): Html = taxYearSummaryView(
    testYear, TaxYearSummaryViewModel(None, None, testChargesList, testObligationsModel, showForecastData = true, ctaViewModel = emptyCTAModel, LPP2Url = "", pfaEnabled = false, hasAmendments = false), "testBackURL", isAgent, ctaLink = ctaLink)

  def mfaDebitsView(isAgent: Boolean): Html = taxYearSummaryView(
    testYear, TaxYearSummaryViewModel(Some(modelComplete(crystallised = true)), None, mfaCharges, testObligationsModel, ctaViewModel = emptyCTAModel, LPP2Url = "", pfaEnabled = false, hasAmendments = false), "testBackURL", isAgent, ctaLink = ctaLink)

  def calculationMultipleErrorView(isAgent: Boolean = false): Html = taxYearSummaryView(
    testYear, TaxYearSummaryViewModel(Some(modelWithMultipleErrorMessages), None, testChargesList, testObligationsModel, ctaViewModel = emptyCTAModel, LPP2Url = "", pfaEnabled = false, hasAmendments = false), "testBackURL", isAgent, ctaLink = ctaLink)

  def calculationSingleErrorView(isAgent: Boolean = false): Html = taxYearSummaryView(
    testYear, TaxYearSummaryViewModel(Some(modelWithErrorMessages), None, testChargesList, testObligationsModel, ctaViewModel = emptyCTAModel, LPP2Url = "", pfaEnabled = false, hasAmendments = false), "testBackURL", isAgent, ctaLink = ctaLink)

  def poaView(isAgent: Boolean = false): Html = {
    val ctaLink = if (isAgent) "/report-quarterly/income-and-expenses/view/agents/adjust-poa/start" else "/report-quarterly/income-and-expenses/view/adjust-poa/start"
    taxYearSummaryView(
      testYear, TaxYearSummaryViewModel(Some(modelWithErrorMessages), None, testChargesList, testObligationsModel, ctaViewModel = testCTAModel, LPP2Url = "", pfaEnabled = false, hasAmendments = false), "testBackURL", isAgent, ctaLink = ctaLink)
  }

  def calculationWithLatestAmendmentsView(isAgent: Boolean) = taxYearSummaryView(
    testYear, TaxYearSummaryViewModel(Some(modelComplete(crystallised = false)), None, List.empty, testObligationsModel, ctaViewModel = emptyCTAModel, LPP2Url = "", pfaEnabled = true, hasAmendments = true), "testBackURL", isAgent, ctaLink = ctaLink
  )

  def calculationWithLatestAndPreviousAmendmentsView(isAgent: Boolean) = taxYearSummaryView(
    testYear, TaxYearSummaryViewModel(Some(modelComplete(crystallised = false)), Some(modelComplete(crystallised = false)), List.empty, testObligationsModel, ctaViewModel = emptyCTAModel, LPP2Url = "", pfaEnabled = true, hasAmendments = true), "testBackURL", isAgent, ctaLink = ctaLink
  )

  def calculationWithLatestAmendmentButPfaDisabledView(isAgent: Boolean) = taxYearSummaryView(
    testYear, TaxYearSummaryViewModel(Some(modelComplete(crystallised = false)), None, List.empty, testObligationsModel, ctaViewModel = emptyCTAModel, LPP2Url = "", pfaEnabled = false, hasAmendments = true), "testBackURL", isAgent, ctaLink = ctaLink
  )

  def calculationWithNoAmendmentPfaEnabled(isAgent: Boolean) = taxYearSummaryView(
    testYear, TaxYearSummaryViewModel(Some(modelComplete(crystallised = false)), None, List.empty, testObligationsModel, ctaViewModel = emptyCTAModel, LPP2Url = "", pfaEnabled = true, hasAmendments = false), "testBackURL", isAgent, ctaLink = ctaLink
  )

  implicit val localDateOrdering: Ordering[LocalDate] = Ordering.by(_.toEpochDay)

  object TaxYearSummaryMessages {
    val heading: String = "Tax year summary"
    val title: String = "Tax year summary - Manage your Self Assessment - GOV.UK"
    val agentTitle: String = "Tax year summary - Manage your Self Assessment - GOV.UK"
    val secondaryHeading: String = "6 April {0} to 5 April {1}"
    val calculationDate: String = "Calculation date"
    val calcDate: String = "1 January 2020"
    val estimate: String = s"6 April ${testYear - 1} to 1 January 2020 estimate"
    val totalDue: String = "Total tax bill"
    val taxDue: String = "£4.04"
    val calDateFrom: String = implicitDateFormatter.longDate(LocalDate.of(testYear - 1, 1, 1)).toLongDate
    val calDateTo: String = implicitDateFormatter.longDate(LocalDate.of(testYear, 1, 1)).toLongDate
    val calcDateInfo: String = "This is not your final tax bill, it’s only based on figures we’ve already received."
    val calcEstimateInfo: String = "This is a year to date estimate based on figures we’ve already received."
    val taxCalculation: String = s"$calDateFrom to $calDateTo"
    val taxCalculationHeading: String = "Calculation"
    val taxCalculationTab: String = "Calculation"
    val taxCalculationNoData: String = "No calculation yet"
    val forecastNoData: String = "No forecast yet"
    val forecastNoDataNote: String = "You will be able to see your forecast for the whole year once you have sent an update."
    val unattendedCalcPara: String = s"! Warning We’ve updated the calculation for you. Check your record-keeping software for more details."
    val taxCalculationNoDataNote: String = "You will be able to see your latest tax year calculation here once you have sent an update."
    val charges: String = "Charges"
    val updates: String = "Updates"
    val income: String = "Income"
    val section: String = "Section"
    val allowancesAndDeductions: String = "Allowances and deductions"
    val totalIncomeDue: String = "Total income on which tax is due"
    val incomeTaxNationalInsuranceDue: String = "Self Assessment tax amount"
    val chargeType: String = "Charge type"
    val dueDate: String = "Due date"
    val amount: String = "Amount"
    val paymentOnAccount1: String = "First payment on account"
    val paymentOnAccount2: String = "Second payment on account"
    val unpaid: String = "Unpaid"
    val paid: String = "Paid"
    val partPaid: String = "Part paid"
    val noPaymentsDue: String = "No payments currently due."
    val updateType: String = "Update type"
    val updateIncomeSource: String = "Income source"
    val updateDateSubmitted: String = "Date submitted"
    val lpiPaymentOnAccount1: String = "Late payment interest on first payment on account"
    val lpiPaymentOnAccount2: String = "Late payment interest on second payment on account"
    val lpiRemainingBalance: String = "Late payment interest on balancing payment"
    val paymentUnderReview: String = "Payment under review"
    val taxYearSummaryClass2Nic: String = "Class 2 National Insurance"
    val remainingBalance: String = "Balancing payment"
    val codedOutPoa1: String = "First payment on account collected through PAYE tax code"
    val codedOutPoa2: String = "Second payment on account collected through PAYE tax code"
    val payeSA: String = "Balancing payment collected through PAYE tax code"
    val hmrcAdjustment: String = "HMRC adjustment"
    val cancelledPaye: String = "Cancelled PAYE Self Assessment (through your PAYE tax code)"
    val na: String = "N/A"
    val messageHeader: String = "We cannot show this calculation because:"
    val messageAction: String = "! Warning You need to amend and resubmit your return."
    val messageError1: String = "you’ve claimed to carry forward a loss to set against general income of the next year. You also need to make the claim in the same year the loss arose."
    val messageError2: String = "you are using cash basis accounting. This means that you cannot claim to set losses against other taxable income."
    val updateDescription: String = "A quarterly update covers income and expenses for the tax year so far."
    val claimToAdjustPoaParagraph: String = "You can reduce both payments on account if you expect the total of your Income Tax and Class 4 National Insurance contributions to be different from the total amount of your current payments on account."
    val claimToAdjustPoaLinkText: String = "Adjust payments on account"
    val claimToAdjustPoaLinkIndividual: String = "/report-quarterly/income-and-expenses/view/adjust-poa/start"
    val claimToAdjustPoaLinkAgent: String = "/report-quarterly/income-and-expenses/view/agents/adjust-poa/start"

    val latestCalculationTab: String = "Latest calculation"
    val latestCalculationDesc: String = "Your tax return was amended on 1 January 2020 and as a result this is your most up-to-date calculation."

    val previousCalculationTab: String = "Previous calculation"
    val previousCalculationDesc: String = "When your tax return is amended it changes your tax calculation. If this happens, this page shows any previous tax calculations you may have."
    val previousCalculationSubheading: String = "Calculation made on 1 January 2020"
    val previousCalculationNote: String = "The tax return was filed then."
    val previousCalculationAmendSubheading: String = "Amending a submitted tax return"
    val previousCalculationBulletStart: String = "You can change your tax return after you have filed it. To do this online you must:"
    val previousCalculationBullet1: String = "use the software or HMRC online service used to submit the return"
    val previousCalculationBullet2: String = "do it within 12 months of the Self Assessment deadline"
    val previousCalculationExample: String = "For example, for the 2025 to 2026 tax year, you’ll usually need to make the change online by 31 January 2028."
    val previousCalculationContactHmrc: String = "If that date has passed, or you cannot amend your return for another reason, you’ll need to contact HMRC."
    val previousCalculationBill: String = "Your calculation as well as your bill will then be updated based on what you report. This may mean you have to pay more tax or that you can claim a refund."

    def updateCaption(from: String, to: String): String = s"$from to $to"

    def incomeType(incomeType: String): String = {
      incomeType match {
        case "Property" => "Property income"
        case "Business" => "Business"
        case "Crystallisation" => "All income sources"
        case _ => "Business income"
      }
    }

    def updateType(updateType: String): String = {
      updateType match {
        case "Quarterly" => "Quarterly update"
        case "Crystallisation" => "Final declaration"
        case _ => updateType
      }
    }
  }

  "taxYearSummary" when {
    "the user is an individual" should {
      "display forecast data when forecast data present" in new Setup(forecastCalcView()) {
        document.title shouldBe title
        document.getOptionalSelector("#forecast").isDefined shouldBe true
        assert(document.select(hrefForecastSelector).text.contains(messagesLookUp("tax-year-summary.forecast")))

        document.getOptionalSelector("#forecast").isDefined shouldBe true
        document.getOptionalSelector(".forecast_table").isDefined shouldBe true

        val incomeForecastUrl = "/report-quarterly/income-and-expenses/view/2018/forecast-income"
        val taxDueForecastUrl = "/report-quarterly/income-and-expenses/view/2018/forecast-tax-calculation"

        document.select(".forecast_table tbody tr").size() shouldBe 4
        document.select(".forecast_table tbody tr:nth-child(1) th:nth-child(1) a").attr("href") shouldBe incomeForecastUrl
        document.select(".forecast_table tbody tr:nth-child(1) td:nth-child(2)").text() shouldBe "£12,500.00"
        document.select(".forecast_table tbody tr:nth-child(2) td:nth-child(2)").text() shouldBe "£4,200.00"
        document.select(".forecast_table tbody tr:nth-child(3) td:nth-child(2)").text() shouldBe "£8,300.00"
        document.select(".forecast_table tbody tr:nth-child(4) th:nth-child(1) a").attr("href") shouldBe taxDueForecastUrl
        document.select(".forecast_table tbody tr:nth-child(4) td:nth-child(2)").text() shouldBe "£5,000.99"
        document.select("#inset_forecast").text() shouldBe messagesLookUp("tax-year-summary.forecast_tab.insetText", testYear.toString)
      }

      "NOT display forecast data when showForecastData param is false" in new Setup(noForecastDataView()) {
        document.title shouldBe title
        document.getOptionalSelector("#tab_forecast").isDefined shouldBe false
      }

      "have the correct title" in new Setup(estimateView()) {
        document.title shouldBe title
      }

      "have the correct heading" in new Setup(estimateView()) {
        layoutContent.selectHead("h1").text.contains(heading)
      }

      "have the correct secondary heading" in new Setup(estimateView()) {
        layoutContent.selectHead("h1").text.contains(secondaryHeading)
        layoutContent.selectHead("span").text.contains(secondaryHeading)
      }

      "display the calculation date and title" in new Setup(estimateView()) {
        layoutContent.h2.selectFirst("h2").text().contains(taxCalculationHeading)
        layoutContent.selectHead("dl > div:nth-child(1) > dt:nth-child(1)").text shouldBe calculationDate
        layoutContent.selectHead("dl > div:nth-child(1) > dd:nth-child(2)").text shouldBe calcDate
      }

      "display the estimate due for an ongoing tax year" in new Setup(estimateView()) {
        layoutContent.selectHead("dl > div:nth-child(2) > dt:nth-child(1)").text shouldBe taxCalculation
        layoutContent.selectHead("dl > div:nth-child(2) > dd:nth-child(2)").text shouldBe modelComplete(crystallised = false).taxDue.toCurrencyString
      }

      "display the total due for a crystallised year" in new Setup(crystallisedView()) {
        layoutContent.selectHead("dl > div:nth-child(2) > dt:nth-child(1)").text shouldBe totalDue
        layoutContent.selectHead("dl > div:nth-child(2) > dd:nth-child(2)").text shouldBe modelComplete(crystallised = true).taxDue.toCurrencyString
      }

      "have a paragraph explaining the calc date for an ongoing year" in new Setup(estimateView()) {
        layoutContent.selectHead("p#calc-date-info").text shouldBe calcDateInfo
      }

      "have a paragraph explaining that the calc date is an estimate" in new Setup(estimateView()) {
        layoutContent.selectHead("p#calc-estimate-info").text shouldBe calcEstimateInfo
      }

      "not have a paragraph explaining the calc date for a crystallised year" in new Setup(crystallisedView()) {
        Option(document.getElementById("no-calc-data-note")) shouldBe None
      }

      "display relevant paragraph and link relating to claim to adjust PoA" in new Setup(poaView()) {
        document.getElementById("claim-to-adjust-poa").text() shouldBe claimToAdjustPoaParagraph
        document.getElementById("claim-to-adjust-poa-link").text() shouldBe claimToAdjustPoaLinkText
        document.getElementById("adjust-poa-link").attr("href") shouldBe claimToAdjustPoaLinkIndividual
      }

      "show three tabs with the correct tab headings" in new Setup(estimateView()) {
        layoutContent.selectHead("""a[href$="#taxCalculation"]""").text shouldBe taxCalculationTab
        layoutContent.selectHead("""a[href$="#payments"]""").text shouldBe charges
        layoutContent.selectHead("""a[href$="#updates"]""").text shouldBe updates
      }

      "when in an ongoing year should display the correct heading in the Tax Calculation tab" in new Setup(estimateView()) {
        layoutContent.selectHead(" #income-deductions-contributions-table caption").text shouldBe taxCalculationHeading
        layoutContent.selectHead("dl > div:nth-child(2) > dt:nth-child(1)").text shouldBe taxCalculation
      }

      "show the unattended calculation info when an unattended calc is returned" in new Setup(unattendedCalcView(unattendedCalc = true)) {
        layoutContent.selectHead(".govuk-warning-text").text shouldBe unattendedCalcPara
      }

      "not show the unattended calculation info when the calc returned isn't unattended" in new Setup(unattendedCalcView(unattendedCalc = false)) {
        layoutContent.getOptionalSelector(".govuk-warning-text") shouldBe None
      }

      "display the section header in the Tax Calculation tab" in new Setup(estimateView()) {
        val sectionHeader: Element = layoutContent.selectHead(" #income-deductions-contributions-table tr:nth-child(1) th:nth-child(1)")
        sectionHeader.text shouldBe section
      }

      "display the amount header in the Tax Calculation tab" in new Setup(estimateView()) {
        val amountHeader: Element = layoutContent.selectHead(" #income-deductions-contributions-table tr:nth-child(1) th:nth-child(2)")
        amountHeader.text shouldBe amount
      }

      "display the income row in the Tax Calculation tab" in new Setup(estimateView()) {
        val incomeLink: Element = layoutContent.selectHead(" #income-deductions-contributions-table tr:nth-child(1) th:nth-child(1) a")
        incomeLink.text shouldBe income
        incomeLink.attr("href") shouldBe controllers.routes.IncomeSummaryController.showIncomeSummary(testYear).url
        layoutContent.selectHead("#income-deductions-contributions-table tr:nth-child(1) td:nth-child(2)").text shouldBe modelComplete(crystallised = false).income.toCurrencyString
      }

      "when there is no calc data should display the correct heading in the Tax Calculation tab" in new Setup(estimateViewWithNoCalcData()) {
        layoutContent.selectHead("#taxCalculation > h2").text shouldBe taxCalculationNoData
      }

      "when there is no calc data should display the correct notes in the Tax Calculation tab" in new Setup(estimateViewWithNoCalcData()) {
        layoutContent.selectHead("#taxCalculation > p").text shouldBe taxCalculationNoDataNote
      }

      "when there is no calc data should display the correct heading in the Forecast tab" in new Setup(forecastWithNoCalcData()) {
        layoutContent.getElementById("no-forecast-data-header").text shouldBe forecastNoData
      }

      "when there is no calc data should display the correct notes in the Forecast tab" in new Setup(forecastWithNoCalcData()) {
        layoutContent.getElementById("no-forecast-data-note").text shouldBe forecastNoDataNote
      }

      "when there is a error message from calculation" in new Setup(calculationSingleErrorView()) {
        val calculationContent = layoutContent.getElementById("taxCalculation")
        calculationContent.child(0).text shouldBe messageHeader
        calculationContent.child(1).text shouldBe messageError1
        calculationContent.child(2).text shouldBe messageAction
      }

      "when there are multiple error messages from calculation" in new Setup(calculationMultipleErrorView()) {
        val calculationContent = layoutContent.getElementById("taxCalculation")
        val errorMessageList = calculationContent.child(1)
        calculationContent.child(0).text shouldBe messageHeader
        calculationContent.child(2).text shouldBe messageAction

        errorMessageList.child(0).text shouldBe s"a quarterly update cannot end after the accounting period end date of ${dateService.getCurrentDate.toLongDate}."
        errorMessageList.child(1).text shouldBe s"a quarterly update cannot end after the accounting period end date of ${dateService.getCurrentDate.toLongDate}."
        errorMessageList.child(2).text shouldBe "you’ve claimed to carry forward a loss to set against general income of the next year. You also need to make the claim in the same year the loss arose."
        errorMessageList.child(3).text shouldBe "you are using cash basis accounting. This means that you cannot claim to set losses against other taxable income."
        errorMessageList.child(4).text shouldBe "the total amount of one-off Gift Aid payments is more than the total Gift Aid payments you’ve made."
        errorMessageList.child(5).text shouldBe "gift Aid payments made this year treated as paid in the previous year are more than the total Gift Aid payments you’ve made."
        errorMessageList.child(6).text shouldBe s"the value of qualifying investments you’ve gifted to non-UK charities are more than the value of these items you’ve gifted to charity: qualifying shares and securities qualifying land and buildings"
        errorMessageList.child(7).text shouldBe "gift Aid payments to non-UK charities are more than the total Gift Aid payments you’ve made."
        errorMessageList.child(8).text shouldBe s"the amount of Trading Income Allowance you’ve claimed is more than allowed. The amount you claim cannot be more than the total of: your turnover other business income not included in turnover balancing charge on the sale of assets or cessation of business and goods and services for your own use"
        errorMessageList.child(9).text shouldBe "the amount of Trading Income Allowance you’ve claimed is more than the limit."
        errorMessageList.child(10).text shouldBe "you’ve claimed consolidated expenses. This means that you cannot claim any further expenses."
        errorMessageList.child(11).text shouldBe "you’ve claimed consolidated expenses. This means that you cannot claim any further expenses."
        errorMessageList.child(12).text shouldBe "you’ve claimed consolidated expenses. This means that you cannot claim any further expenses."
        errorMessageList.child(13).text shouldBe "the amount of Property Income Allowance you’ve claimed is more than the limit."
        errorMessageList.child(14).text shouldBe "you’ve claimed 1000 in Property Income Allowance but this is more than turnover for your UK property."
        errorMessageList.child(15).text shouldBe "the Rent a Room relief claimed for a jointly let property cannot be more than 50% of the Rent a Room limit."
        errorMessageList.child(16).text shouldBe "the amount of Rent a Room relief you’ve claimed is more than the limit."
        errorMessageList.child(17).text shouldBe "you are using cash basis accounting. This means that you cannot claim to set losses against general income."
        errorMessageList.child(18).text shouldBe "your non-allowable business entertainment costs must be the same as the allowable business entertainment costs."
        errorMessageList.child(19).text shouldBe "you’ve claimed Trading Income Allowance. This means that you cannot claim any further expenses."
        errorMessageList.child(20).text shouldBe "the amount of Annual Investment Allowance you’ve claimed for your UK property is more than the limit."
        errorMessageList.child(21).text shouldBe "the amount of Annual Investment Allowance you’ve claimed for your foreign property is more than the limit."
        errorMessageList.child(22).text shouldBe "the amount of Annual Investment Allowance you’ve claimed is more than the limit."
        errorMessageList.child(23).text shouldBe "you cannot submit details for combined expenses for self-employment. This is because your cumulative turnover is more than the limit."
        errorMessageList.child(24).text shouldBe "a Class 4 exemption cannot be applied. This is because the individual is 16 or older on 6 April of the current tax year."
        errorMessageList.child(25).text shouldBe "a Class 4 exemption cannot be applied. This is because the individual’s age is less than their State Pension age on 6 April of the current tax year."
        errorMessageList.child(26).text shouldBe "the amount you’ve claimed for private use adjustment for your UK furnished holiday lettings is more than the total allowable expenses."
        errorMessageList.child(27).text shouldBe "the amount you’ve claimed for private use adjustment for your UK unfurnished holiday lettings is more than the total allowable expenses."
        errorMessageList.child(28).text shouldBe "you cannot submit details for combined expenses for your UK property. This is because your cumulative turnover is more than the limit."
        errorMessageList.child(29).text shouldBe "you’ve claimed Property Income Allowance for your UK unfurnished holiday lettings. This means that you cannot claim any further expenses."
        errorMessageList.child(30).text shouldBe s"the update must align to the accounting period start date of ${dateService.getCurrentDate.toLongDate}."
        errorMessageList.child(31).text shouldBe s"the update must align to the accounting period start date of ${dateService.getCurrentDate.toLongDate}."
        errorMessageList.child(32).text shouldBe "updates cannot include gaps."
        errorMessageList.child(33).text shouldBe "updates cannot include overlaps."
        errorMessageList.child(34).text shouldBe s"the update must align to the accounting period end date of ${dateService.getCurrentDate.toLongDate}."
        errorMessageList.child(35).text shouldBe s"the update must align to the accounting period end date of ${dateService.getCurrentDate.toLongDate}."
        errorMessageList.child(36).text shouldBe "updates cannot include gaps."
        errorMessageList.child(37).text shouldBe "the Rent a Room threshold has been limited to the amount of rents received."
        errorMessageList.child(38).text shouldBe "the Rent a Room threshold has been limited to the amount of rents received."
        errorMessageList.child(39).text shouldBe "deducted tax cannot be applied against UK property income unless you are a non-resident landlord."
        errorMessageList.child(40).text shouldBe "deducted tax cannot be applied against UK property income unless you are a non-resident landlord."
        errorMessageList.child(41).text shouldBe "the amount of tax deducted is more than the total amount of rent you’ve received."
        errorMessageList.child(42).text shouldBe "the amount of tax deducted is more than the total amount of rent you’ve received."
        errorMessageList.child(43).text shouldBe "you must provide final confirmation of income and expenses for all business sources."
        errorMessageList.child(44).text shouldBe "the amount of relief claimed for Subscriptions for Venture Capital Trust shares is more than the limit."
        errorMessageList.child(45).text shouldBe "the amount of relief claimed for Subscriptions for shares under the Enterprise Investment Scheme is more than the limit."
        errorMessageList.child(46).text shouldBe "the amount of relief claimed for Subscriptions for shares under the Enterprise Investment Scheme where the companies are not knowledge intensive is more than the limit."
        errorMessageList.child(47).text shouldBe "the amount of relief claimed for Subscriptions for shares under the Seed Enterprise Investment Scheme is more than the limit."
        errorMessageList.child(48).text shouldBe "the amount claimed for Social Investment Tax Relief is more than the limit."
        errorMessageList.child(49).text shouldBe "the total amount of tax taken off in employment is more than your taxable pay."
        errorMessageList.child(50).text shouldBe "the total amount of tax taken off in employment is more than your taxable pay."
        errorMessageList.child(51).text shouldBe s"the total tax taken off your employment must be less than the total taxable pay including: tips other payments lump sums"
        errorMessageList.child(52).text shouldBe "your total redundancy compensation amount from employment is more than the lump sum limit."
        errorMessageList.child(53).text shouldBe "the amount of Annual Investment Allowance you’ve claimed for your EEA furnished holiday lettings or foreign property is more than the limit."
        errorMessageList.child(54).text shouldBe "the amount of Property Allowance you’ve claimed for your foreign property is more than the limit."
        errorMessageList.child(55).text shouldBe "the amount you’ve claimed for Tax taken off State Pension lump sum is more than 50% of the amount entered for State Pension lump sum."
        errorMessageList.child(56).text shouldBe "you are using cash basis accounting. This means that they cannot claim Annual Investment Allowance for your UK property."
        errorMessageList.child(57).text shouldBe "you are using cash basis accounting. This means that you cannot claim Annual Investment Allowance for your self-employment."
        errorMessageList.child(58).text shouldBe "you cannot submit details for combined expenses for your EEA furnished holiday lettings and foreign property. This is because your cumulative turnover is more than the limit."
        errorMessageList.child(59).text shouldBe "you’ve claimed Property Income Allowance for your EEA furnished holiday lettings. This means that you cannot claim for private use adjustment."
        errorMessageList.child(60).text shouldBe "the amount you’ve claimed for private use adjustment for your EEA furnished holiday lettings is more than the total allowable expenses."
        errorMessageList.child(61).text shouldBe "you’ve claimed Property Income Allowance. This means that you cannot claim any further expenses."
        errorMessageList.child(62).text shouldBe "you’ve claimed Property Income Allowance. This means that you cannot claim any further expenses."
        errorMessageList.child(63).text shouldBe "you’ve claimed Property Income Allowance for your foreign property. This means that you cannot claim for private use adjustment."
        errorMessageList.child(64).text shouldBe "the amount you’ve claimed for private use adjustment for your foreign property is more than the total allowable expenses."
        errorMessageList.child(65).text shouldBe "for your foreign property, you need to submit either consolidated or detailed expenses but not both."
        errorMessageList.child(66).text shouldBe "for your EEA furnished holiday lettings, you need to submit either consolidated or detailed expenses but not both."

      }

      "display the Allowances and deductions row in the Tax Calculation tab" in new Setup(estimateView()) {
        val allowancesLink: Element = layoutContent.selectHead(" #income-deductions-contributions-table tr:nth-child(2) th:nth-child(1) a")
        allowancesLink.text shouldBe allowancesAndDeductions
        allowancesLink.attr("href") shouldBe controllers.routes.DeductionsSummaryController.showDeductionsSummary(testYear).url
        layoutContent.selectHead("#income-deductions-contributions-table tr:nth-child(2) td:nth-child(2)").text shouldBe "£2.02"
      }

      "display the Total income on which tax is due row in the Tax Calculation tab" in new Setup(estimateView()) {
        layoutContent.selectHead("#income-deductions-contributions-table tr:nth-child(3) th:nth-child(1)").text shouldBe totalIncomeDue
        layoutContent.selectHead("#income-deductions-contributions-table tr:nth-child(3) td:nth-child(2)").text shouldBe modelComplete(crystallised = false).totalTaxableIncome.toCurrencyString
      }

      "display the Income Tax and National Insurance Contributions Due row in the Tax Calculation tab" in new Setup(estimateView()) {
        val totalTaxDueLink: Element = layoutContent.selectHead(" #income-deductions-contributions-table tr:nth-child(4) th:nth-child(1) a")
        totalTaxDueLink.text shouldBe incomeTaxNationalInsuranceDue
        totalTaxDueLink.attr("href") shouldBe controllers.routes.TaxDueSummaryController.showTaxDueSummary(testYear).url
        layoutContent.selectHead("#income-deductions-contributions-table tr:nth-child(4) td:nth-child(2)").text shouldBe modelComplete(crystallised = false).taxDue.toCurrencyString
      }

      "display the table headings in the Payments tab" in new Setup(estimateView()) {
        layoutContent.selectHead("#paymentTypeHeading").text shouldBe chargeType
        layoutContent.selectHead("#paymentDueDateHeading").text shouldBe dueDate
        layoutContent.selectHead("#paymentAmountHeading").text shouldBe amount
      }

      "display the payment type as a link to Charge Summary in the Payments tab" in new Setup(estimateView(chargeItems = testChargesWithoutLpiList)) {
        val paymentTypeLink: Element = layoutContent.selectHead("#payments-table tr:nth-child(1) a")
        paymentTypeLink.text shouldBe paymentOnAccount1
        paymentTypeLink.attr("href") shouldBe controllers.routes.ChargeSummaryController.show(testYear, fullDocumentDetailModel.transactionId).url
      }

      "display the Due date in the Payments tab" in new Setup(estimateView()) {
        layoutContent.selectHead("#payments-table tr:nth-child(1) td:nth-child(2)").text shouldBe "15 Jun 2019"
        layoutContent.selectHead("#payments-table tr:nth-child(2) td:nth-child(2)").text shouldBe "15 Jul 2019"
        layoutContent.selectHead("#payments-table tr:nth-child(3) td:nth-child(2)").text shouldBe "15 Aug 2019"
      }


      "display the Amount in the payments tab" in new Setup(estimateView(chargeItems = testChargesWithoutLpiList)) {
        layoutContent.selectHead("#payments-table tr:nth-child(1) td:nth-child(3)").text shouldBe "£1,400.00"
      }

      "display no payments due when there are no charges in the payments tab" in new Setup(estimateView(emptyChargeList)) {
        layoutContent.selectHead("#payments p").text shouldBe noPaymentsDue
        layoutContent.h2.selectFirst("h2").text().contains(charges)
        layoutContent.selectHead("#payments").doesNotHave("table")
      }

      "display the late payment interest POA1 with a dunning lock applied" in new Setup(estimateView()) {
        val paymentType: Element = layoutContent.selectHead("#payments-table tr:nth-child(1) div:nth-child(3)")
        paymentType.text shouldBe paymentUnderReview
      }

      "display the payment type as a link to Charge Summary in the Payments tab for late payment interest POA1" in new Setup(estimateView()) {
        val paymentTypeLink: Element = layoutContent.selectHead("#payments-table tr:nth-child(1) a")
        paymentTypeLink.text shouldBe lpiPaymentOnAccount1
        paymentTypeLink.attr("href") shouldBe controllers.routes.ChargeSummaryController.show(
          testYear, fullDocumentDetailModel.transactionId, true).url
      }

      "display the Due date in the Payments tab for late payment interest POA1" in new Setup(estimateView()) {
        layoutContent.selectHead("#payments-table tr:nth-child(1) td:nth-child(2)").text shouldBe "15 Jun 2019"
      }


      "display the Amount in the payments tab for late payment interest POA1" in new Setup(estimateView()) {
        layoutContent.selectHead("#payments-table tr:nth-child(1) td:nth-child(3)").text shouldBe "£100.00"
      }

      "display the payment type as a link to Charge Summary in the Payments tab for late payment interest POA2" in new Setup(estimateView()) {
        val paymentTypeLink: Element = layoutContent.selectHead("#payments-table tr:nth-child(2) a")
        paymentTypeLink.text shouldBe lpiPaymentOnAccount2
        paymentTypeLink.attr("href") shouldBe controllers.routes.ChargeSummaryController.show(
          testYear, fullDocumentDetailModel.transactionId, true).url
      }

      "display the Due date in the Payments tab for late payment interest POA2" in new Setup(estimateView()) {
        layoutContent.selectHead("#payments-table tr:nth-child(2) td:nth-child(2)").text shouldBe "15 Jul 2019"
      }


      "display the Amount in the payments tab for late payment interest POA2" in new Setup(estimateView()) {
        layoutContent.selectHead("#payments-table tr:nth-child(2) td:nth-child(3)").text shouldBe "£80.00"
      }

      "display the payment type as a link to Charge Summary in the Payments tab for late payment interest Balancing payment" in new Setup(estimateView()) {
        val paymentTypeLink: Element = layoutContent.selectHead("#payments-table tr:nth-child(3) a")
        paymentTypeLink.text shouldBe lpiRemainingBalance
        paymentTypeLink.attr("href") shouldBe controllers.routes.ChargeSummaryController.show(
          testYear, fullDocumentDetailModel.transactionId, true).url
      }

      "display the Due date in the Payments tab for late payment interest Balancing payment" in new Setup(estimateView()) {
        layoutContent.selectHead("#payments-table tr:nth-child(3) td:nth-child(2)").text shouldBe "15 Aug 2019"
      }


      "display the Amount in the payments tab for late payment interest p" in new Setup(estimateView()) {
        layoutContent.selectHead("#payments-table tr:nth-child(3) td:nth-child(3)").text shouldBe "£100.00"
      }

      "display the Dunning lock subheading in the payments tab for multiple lines POA1 and Balancing payment" in new Setup(multipleDunningLockView()) {
        layoutContent.selectHead("#payments-table tbody tr:nth-child(1) div:nth-child(3)").text shouldBe paymentUnderReview
        layoutContent.selectHead("#payments-table tbody tr:nth-child(3) div:nth-child(3)").text shouldBe paymentUnderReview
        layoutContent.doesNotHave("#payments-table tbody tr:nth-child(4) th:nth-child(1) div:nth-child(3)")
      }

      "display the Class 2 National Insurance payment link on the payments table when coding out is enabled" in new Setup(class2NicsView()) {
        val paymentTypeLink: Element = layoutContent.getElementById("paymentTypeLink-0")
        paymentTypeLink.text shouldBe taxYearSummaryClass2Nic
        paymentTypeLink.attr("href") shouldBe controllers.routes.ChargeSummaryController.show(
          testYear, fullDocumentDetailModel.transactionId).url
      }

      "display the PAYE Self Assessment link on the payments table when coding out is enabled" in new Setup(payeView()) {
        val paymentTypeLink: Element = layoutContent.getElementById("paymentTypeLink-0")
        paymentTypeLink.text shouldBe payeSA
        paymentTypeLink.attr("href") shouldBe controllers.routes.ChargeSummaryController.show(
          testYear, fullDocumentDetailModel.transactionId).url
      }

      s"display the Due date in the Payments tab for PAYE Self Assessment as ${na}" in new Setup(payeView()) {
        layoutContent.selectHead("#payments-table tr:nth-child(1) td:nth-child(2)").text shouldBe na
      }

      "display the Amount in the payments tab for PAYE Self Assessment" in new Setup(payeView()) {
        layoutContent.selectHead("#payments-table tr:nth-child(1) td:nth-child(3)").text shouldBe "£1,400.00"
      }

      "display the Due date in the Payments tab for Cancelled" in new Setup(rejectedByNpsPartWayView()) {
        layoutContent.selectHead("#payments-table tr:nth-child(1) td:nth-child(2)").text shouldBe "15 May 2019"
      }

      "display Class 2 National Insurance - User has Coding out that is requested and immediately rejected by NPS" in new Setup(immediatelyRejectedByNpsView()) {
        val paymentTypeLink: Element = layoutContent.getElementById("paymentTypeLink-0")
        paymentTypeLink.text shouldBe taxYearSummaryClass2Nic
        paymentTypeLink.attr("href") shouldBe controllers.routes.ChargeSummaryController.show(
          testYear, fullDocumentDetailModel.transactionId).url
      }

      "display Balancing payment - User has Coding out that is requested and immediately rejected by NPS" in new Setup(immediatelyRejectedByNpsView()) {
        val paymentTypeLink: Element = layoutContent.getElementById("paymentTypeLink-1")
        paymentTypeLink.text shouldBe remainingBalance
        paymentTypeLink.attr("href") shouldBe controllers.routes.ChargeSummaryController.show(
          testYear, fullDocumentDetailModel.transactionId).url
      }

      "display Class 2 Nics - User has Coding out that has been accepted and rejected by NPS part way through the year" in new Setup(rejectedByNpsPartWayView()) {
        val paymentTypeLink: Element = layoutContent.getElementById("paymentTypeLink-0")
        paymentTypeLink.text shouldBe taxYearSummaryClass2Nic
        paymentTypeLink.attr("href") shouldBe controllers.routes.ChargeSummaryController.show(
          testYear, fullDocumentDetailModel.transactionId).url
      }

      "display Cancelled Self Assessment payment - User has Coding out that has been accepted and rejected by NPS part way through the year" in new Setup(rejectedByNpsPartWayView()) {
        val paymentTypeLink: Element = layoutContent.getElementById("paymentTypeLink-1")
        paymentTypeLink.text shouldBe cancelledPaye
        paymentTypeLink.attr("href") shouldBe controllers.routes.ChargeSummaryController.show(
          testYear, fullDocumentDetailModel.transactionId).url
      }

      "display Class 2 National Insurance - At crystallization, the user has the coding out requested amount has not been fully collected (partially collected)" in new Setup(codingOutPartiallyCollectedView()) {
        val paymentTypeLink: Element = layoutContent.getElementById("paymentTypeLink-0")
        paymentTypeLink.text shouldBe taxYearSummaryClass2Nic
        paymentTypeLink.attr("href") shouldBe controllers.routes.ChargeSummaryController.show(
          testYear, fullDocumentDetailModel.transactionId).url
      }

      "display Balancing payment - At crystallization, the user has the coding out requested amount has not been fully collected (partially collected)" in new Setup(codingOutPartiallyCollectedView()) {
        val paymentTypeLink: Element = layoutContent.getElementById("paymentTypeLink-1")
        paymentTypeLink.text shouldBe remainingBalance
        paymentTypeLink.attr("href") shouldBe controllers.routes.ChargeSummaryController.show(
          testYear, fullDocumentDetailModel.transactionId).url
      }

      "display Cancelled Self Assessment payment - At crystallization, the user has the coding out requested amount has not been fully collected (partially collected)" in new Setup(codingOutPartiallyCollectedView()) {
        val paymentTypeLink: Element = layoutContent.getElementById("paymentTypeLink-2")
        paymentTypeLink.text shouldBe cancelledPaye
        paymentTypeLink.attr("href") shouldBe controllers.routes.ChargeSummaryController.show(
          testYear, fullDocumentDetailModel.transactionId).url
      }

      "display the Balancing payment on the payments table when coding out is enabled and a zero amount" in new Setup(testBalancingPaymentChargeWithZeroValueView()) {
        val paymentTypeText: Element = layoutContent.getElementById("paymentTypeText-0")
        val paymentTypeLinkOption: Option[Element] = Option(layoutContent.getElementById("paymentTypeLink-0"))
        val paymentTabRow: Element = layoutContent.getElementById("payments-table").getElementsByClass("govuk-table__row").get(1)
        paymentTabRow.getElementsByClass("govuk-table__cell").first().text() shouldBe "N/A"
        paymentTabRow.getElementsByClass("govuk-table__cell").get(1).text() shouldBe BigDecimal(0).toCurrencyString
        paymentTypeText.text shouldBe remainingBalance
        paymentTypeLinkOption.isEmpty shouldBe true
      }

      "display payments on account on the payments table when coding out is accepted" in new Setup(testPaymentOnAccountChargesCodedOutAcceptedView()) {
        val paymentTypeText1: Element = layoutContent.getElementById("paymentTypeLink-0")
        val paymentTabRow1: Element = layoutContent.getElementById("payments-table").getElementsByClass("govuk-table__row").get(1)
        paymentTabRow1.getElementsByClass("govuk-table__cell").first().text() shouldBe "N/A"
        paymentTabRow1.getElementsByClass("govuk-table__cell").get(1).text() shouldBe BigDecimal(1400).toCurrencyString
        paymentTypeText1.text shouldBe codedOutPoa1
        paymentTypeText1.attr("href") shouldBe controllers.routes.ChargeSummaryController.show(
          testYear, fullDocumentDetailModel.transactionId).url

        val paymentTypeText2: Element = layoutContent.getElementById("paymentTypeLink-1")
        val paymentTabRow2: Element = layoutContent.getElementById("payments-table").getElementsByClass("govuk-table__row").get(2)
        paymentTabRow2.getElementsByClass("govuk-table__cell").first().text() shouldBe "N/A"
        paymentTabRow2.getElementsByClass("govuk-table__cell").get(1).text() shouldBe BigDecimal(1400).toCurrencyString
        paymentTypeText2.text shouldBe codedOutPoa2
        paymentTypeText2.attr("href") shouldBe controllers.routes.ChargeSummaryController.show(
          testYear, fullDocumentDetailModel.transactionId).url
      }

      "display payments on account on the payments table when coding out is cancelled" in new Setup(testPaymentOnAccountChargesCodedOutCancelledView()) {
        val paymentTypeText1: Element = layoutContent.getElementById("paymentTypeLink-0")
        val paymentTabRow1: Element = layoutContent.getElementById("payments-table").getElementsByClass("govuk-table__row").get(1)
        paymentTabRow1.getElementsByClass("govuk-table__cell").first().text() shouldBe "31 Mar 2040"
        paymentTabRow1.getElementsByClass("govuk-table__cell").get(1).text() shouldBe BigDecimal(1400).toCurrencyString
        paymentTypeText1.text() shouldBe cancelledPaye
        paymentTypeText1.attr("href") shouldBe controllers.routes.ChargeSummaryController.show(
          testYear, fullDocumentDetailModel.transactionId).url

        val paymentTypeText2: Element = layoutContent.getElementById("paymentTypeLink-1")
        val paymentTabRow2: Element = layoutContent.getElementById("payments-table").getElementsByClass("govuk-table__row").get(2)
        paymentTabRow2.getElementsByClass("govuk-table__cell").first().text() shouldBe "31 Mar 2040"
        paymentTabRow2.getElementsByClass("govuk-table__cell").get(1).text() shouldBe BigDecimal(1400).toCurrencyString
        paymentTypeText2.text() shouldBe cancelledPaye
        paymentTypeText2.attr("href") shouldBe controllers.routes.ChargeSummaryController.show(
          testYear, fullDocumentDetailModel.transactionId).url
      }

      "display updates by due-date" in new Setup(estimateView()) {

        testObligationsModel.allDeadlinesWithSource(previous = true).groupBy[LocalDate] { nextUpdateWithIncomeType =>
          nextUpdateWithIncomeType.obligation.due
        }.toList.sortBy(_._1)(localDateOrdering).reverse.foreach { case (due: LocalDate, obligations: Seq[ObligationWithIncomeType]) =>
          layoutContent.selectHead(s"#table-default-content-$due").text shouldBe s"Due ${due.toLongDate}"
          val sectionContent = layoutContent.selectHead(s"#updates")
          obligations.zip(1 to obligations.length).foreach {
            case (testObligation, index) =>
              val divAccordion = sectionContent.selectHead(s"div:nth-of-type($index)")
              document.getElementById("update-tab-description").text() shouldBe updateDescription

              divAccordion.selectHead("caption").text shouldBe
                s"The update period from ${testObligation.obligation.start.toLongDateShort} to ${testObligation.obligation.end.toLongDateShort}"
              divAccordion.selectHead("thead").selectNth("th", 1).text shouldBe updateType
              divAccordion.selectHead("thead").selectNth("th", 2).text shouldBe updateIncomeSource
              divAccordion.selectHead("thead").selectNth("th", 3).text shouldBe updateDateSubmitted
              val row = divAccordion.selectHead("tbody").selectHead("tr")
              row.selectNth("td", 1).text shouldBe updateType(testObligation.obligation.obligationType)
              row.selectNth("td", 2).text shouldBe incomeType(testObligation.incomeType)
              row.selectNth("td", 3).text shouldBe testObligation.obligation.dateReceived.map(_.toLongDateShort).getOrElse("")
          }
        }
      }

      "display the latest calculation tab when pfa is enabled and the user has an amended latest calculation" in new Setup(calculationWithLatestAmendmentsView(false)) {
        layoutContent.selectHead("""a[href$="#latestCalculation"]""").text shouldBe latestCalculationTab

        document.getElementById("latest-calculation-overview-description").text() shouldBe latestCalculationDesc
      }

      "display the latest and previous calculation tab when pfa is enabled and the user has an amended latest calculation and a previous calculation" in new Setup(calculationWithLatestAndPreviousAmendmentsView(false)) {
        layoutContent.selectHead("""a[href$="#latestCalculation"]""").text shouldBe latestCalculationTab
        layoutContent.selectHead("""a[href$="#previousCalculation"]""").text shouldBe previousCalculationTab

        document.getElementById("latest-calculation-overview-description").text() shouldBe latestCalculationDesc

        document.getElementById("previous-calculation-overview-description").text() shouldBe previousCalculationDesc
        document.getElementsByClass("govuk-heading-m").get(0).text() shouldBe previousCalculationSubheading
        document.getElementById("previous-calculation-note").text() shouldBe previousCalculationNote
        document.getElementsByClass("govuk-heading-m").get(1).text() shouldBe previousCalculationAmendSubheading
        document.getElementById("previous-calculation-bullet-start").text() shouldBe previousCalculationBulletStart
        document.getElementById("previous-calculation-bullet-1").text() shouldBe previousCalculationBullet1
        document.getElementById("previous-calculation-bullet-2").text() shouldBe previousCalculationBullet2
        document.getElementById("previous-calculation-example").text() shouldBe previousCalculationExample
        document.getElementById("previous-calculation-contact-hmrc").text() shouldBe previousCalculationContactHmrc
        document.getElementById("previous-calculation-bill").text() shouldBe previousCalculationBill
      }

      "display a regular calculations tab if pfa is disabled" in new Setup(calculationWithLatestAmendmentButPfaDisabledView(false)) {
        layoutContent.selectHead("""a[href$="#taxCalculation"]""").text shouldBe taxCalculationTab
      }

      "display a regular calculations tab if pfa is enabled but the latest calculation has no amendments" in new Setup(calculationWithNoAmendmentPfaEnabled(false)) {
        layoutContent.selectHead("""a[href$="#taxCalculation"]""").text shouldBe taxCalculationTab
      }
    }
    "the user is an agent" should {

      "display forecast data when forecast data present" in new Setup(forecastCalcView(isAgent = true)) {
        document.title shouldBe agentTitle
        document.getOptionalSelector("#forecast").isDefined shouldBe true
        assert(document.select(hrefForecastSelector).text.contains(messagesLookUp("tax-year-summary.forecast")))

        document.getOptionalSelector("#forecast").isDefined shouldBe true
        document.getOptionalSelector(".forecast_table").isDefined shouldBe true

        val incomeForecastUrl = "/report-quarterly/income-and-expenses/view/agents/2018/forecast-income"
        val taxDueForecastUrl = "/report-quarterly/income-and-expenses/view/agents/2018/forecast-tax-calculation"

        document.select(".forecast_table tbody tr").size() shouldBe 4
        document.select(".forecast_table tbody tr:nth-child(1) th:nth-child(1) a").attr("href") shouldBe incomeForecastUrl
        document.select(".forecast_table tbody tr:nth-child(1) td:nth-child(2)").text() shouldBe "£12,500.00"
        document.select(".forecast_table tbody tr:nth-child(2) td:nth-child(2)").text() shouldBe "£4,200.00"
        document.select(".forecast_table tbody tr:nth-child(3) td:nth-child(2)").text() shouldBe "£8,300.00"
        document.select(".forecast_table tbody tr:nth-child(4) th:nth-child(1) a").attr("href") shouldBe taxDueForecastUrl
        document.select(".forecast_table tbody tr:nth-child(4) td:nth-child(2)").text() shouldBe "£5,000.99"
        document.select("#inset_forecast").text() shouldBe messagesLookUp("tax-year-summary.forecast_tab.insetText", testYear.toString)
      }

      "NOT display forecastdata when showForecastData param is false" in new Setup(noForecastDataView(isAgent = true)) {
        document.title shouldBe agentTitle
        document.getOptionalSelector("#tab_forecast").isDefined shouldBe false
      }

      "display No forecast data yet when calcData returns NOT_FOUND" in new Setup(forecastWithNoCalcData(isAgent = true)) {
        document.title shouldBe agentTitle
        document.getOptionalSelector("#forecast").isDefined shouldBe true
        assert(document.select(hrefForecastSelector).text.contains(messagesLookUp("tax-year-summary.forecast")))

        document.select(".forecast_table h2").text.contains(messagesLookUp("forecast_taxCalc.noForecast.heading"))
        document.select(".forecast_table p").text.contains(messagesLookUp("forecast_taxCalc.noForecast.text"))
      }

      "have the correct title" in new Setup(estimateView(isAgent = true)) {
        document.title shouldBe agentTitle
      }

      "display relevant paragraph and link relating to claim to adjust PoA" in new Setup(poaView(isAgent = true)) {
        document.getElementById("claim-to-adjust-poa").text() shouldBe claimToAdjustPoaParagraph
        document.getElementById("claim-to-adjust-poa-link").text() shouldBe claimToAdjustPoaLinkText
        document.getElementById("adjust-poa-link").attr("href") shouldBe claimToAdjustPoaLinkAgent
      }

      "display the income row in the Tax Calculation tab" in new Setup(estimateView(isAgent = true)) {
        val incomeLink: Element = layoutContent.selectHead("#income-deductions-contributions-table tr:nth-child(1) th:nth-child(1) a")
        incomeLink.text shouldBe income
        incomeLink.attr("href") shouldBe controllers.routes.IncomeSummaryController.showIncomeSummaryAgent(testYear).url
        layoutContent.selectHead("#income-deductions-contributions-table tr:nth-child(1) td:nth-child(2)").text shouldBe modelComplete(crystallised = false).income.toCurrencyString
      }

      "display the Allowances and deductions row in the Tax Calculation tab" in new Setup(estimateView(isAgent = true)) {
        val allowancesLink: Element = layoutContent.selectHead(" #income-deductions-contributions-table tr:nth-child(2) th:nth-child(1) a")
        allowancesLink.text shouldBe allowancesAndDeductions
        allowancesLink.attr("href") shouldBe controllers.routes.DeductionsSummaryController.showDeductionsSummaryAgent(testYear).url
        layoutContent.selectHead("#income-deductions-contributions-table tr:nth-child(2) td:nth-child(2)").text shouldBe "£2.02"
      }

      "display the Income Tax and National Insurance Contributions Due row in the Tax Calculation tab" in new Setup(estimateView(isAgent = true)) {
        val totalTaxDueLink: Element = layoutContent.selectHead("#income-deductions-contributions-table tr:nth-child(4) th:nth-child(1) a")
        totalTaxDueLink.text shouldBe incomeTaxNationalInsuranceDue

        totalTaxDueLink.attr("href") shouldBe controllers.routes.TaxDueSummaryController.showTaxDueSummaryAgent(testYear).url
        layoutContent.selectHead("#income-deductions-contributions-table tr:nth-child(4) td:nth-child(2)").text shouldBe modelComplete(crystallised = false).taxDue.toCurrencyString

      }

      "display the payment type as a link to Charge Summary in the Payments tab" in new Setup(estimateView(chargeItems = testChargesWithoutLpiList, isAgent = true)) {
        val paymentTypeLink: Element = layoutContent.selectHead("#payments-table tr:nth-child(1) a")
        paymentTypeLink.text shouldBe paymentOnAccount1
        paymentTypeLink.attr("href") shouldBe controllers.routes.ChargeSummaryController.showAgent(testYear, fullDocumentDetailModel.transactionId).url
      }

      "display the payment type as a link to Charge Summary in the Payments tab for late payment interest POA1" in new Setup(estimateView(isAgent = true)) {
        val paymentTypeLink: Element = layoutContent.selectHead("#payments-table tr:nth-child(1) a")
        paymentTypeLink.text shouldBe lpiPaymentOnAccount1
        paymentTypeLink.attr("href") shouldBe controllers.routes.ChargeSummaryController.showAgent(
          testYear, fullDocumentDetailModel.transactionId, true).url
      }

      "display the Class 2 National Insurance payment link on the payments table" in new Setup(
        class2NicsView(isAgent = true)) {
        val paymentTypeLink: Element = layoutContent.getElementById("paymentTypeLink-0")
        paymentTypeLink.text shouldBe taxYearSummaryClass2Nic
        paymentTypeLink.attr("href") shouldBe controllers.routes.ChargeSummaryController.showAgent(
          testYear, fullDocumentDetailModel.transactionId).url
      }

      "display the PAYE Self Assessment link on the payments table" in new Setup(payeView(isAgent = true)) {
        val paymentTypeLink: Element = layoutContent.getElementById("paymentTypeLink-0")
        paymentTypeLink.text shouldBe payeSA
        paymentTypeLink.attr("href") shouldBe controllers.routes.ChargeSummaryController.showAgent(
          testYear, fullDocumentDetailModel.transactionId).url
      }

      "display Class 2 National Insurance - User has Coding out that is requested and immediately rejected by NPS - Agent" in new Setup(immediatelyRejectedByNpsView(isAgent = true)) {
        val paymentTypeLink: Element = layoutContent.getElementById("paymentTypeLink-0")
        paymentTypeLink.text shouldBe taxYearSummaryClass2Nic
        paymentTypeLink.attr("href") shouldBe controllers.routes.ChargeSummaryController.showAgent(
          testYear, fullDocumentDetailModel.transactionId).url
      }

      "display Balancing payment - User has Coding out that is requested and immediately rejected by NPS - Agent" in new Setup(immediatelyRejectedByNpsView(isAgent = true)) {
        val paymentTypeLink: Element = layoutContent.getElementById("paymentTypeLink-1")
        paymentTypeLink.text shouldBe remainingBalance
        paymentTypeLink.attr("href") shouldBe controllers.routes.ChargeSummaryController.showAgent(
          testYear, fullDocumentDetailModel.transactionId).url
      }

      "display Class 2 Nics - User has Coding out that has been accepted and rejected by NPS part way through the year - Agent" in new Setup(rejectedByNpsPartWayView(isAgent = true)) {
        val paymentTypeLink: Element = layoutContent.getElementById("paymentTypeLink-0")
        paymentTypeLink.text shouldBe taxYearSummaryClass2Nic
        paymentTypeLink.attr("href") shouldBe controllers.routes.ChargeSummaryController.showAgent(
          testYear, fullDocumentDetailModel.transactionId).url
      }

      "display Cancelled Self Assessment payment - User has Coding out that has been accepted and rejected by NPS part way through the year - Agent" in new Setup(rejectedByNpsPartWayView(isAgent = true)) {
        val paymentTypeLink: Element = layoutContent.getElementById("paymentTypeLink-1")
        paymentTypeLink.text shouldBe cancelledPaye
        paymentTypeLink.attr("href") shouldBe controllers.routes.ChargeSummaryController.showAgent(
          testYear, fullDocumentDetailModel.transactionId).url
      }

      "display Class 2 National Insurance - At crystallization, the user has the coding out requested amount has not been fully collected (partially collected) - Agent" in new Setup(codingOutPartiallyCollectedView(isAgent = true)) {
        val paymentTypeLink: Element = layoutContent.getElementById("paymentTypeLink-0")
        paymentTypeLink.text shouldBe taxYearSummaryClass2Nic
        paymentTypeLink.attr("href") shouldBe controllers.routes.ChargeSummaryController.showAgent(
          testYear, fullDocumentDetailModel.transactionId).url
      }

      "display Balancing payment - At crystallization, the user has the coding out requested amount has not been fully collected (partially collected) - Agent" in new Setup(codingOutPartiallyCollectedView(isAgent = true)) {
        val paymentTypeLink: Element = layoutContent.getElementById("paymentTypeLink-1")
        paymentTypeLink.text shouldBe remainingBalance
        paymentTypeLink.attr("href") shouldBe controllers.routes.ChargeSummaryController.showAgent(
          testYear, fullDocumentDetailModel.transactionId).url
      }

      "display Cancelled Self Assessment payment - At crystallization, the user has the coding out requested amount has not been fully collected (partially collected) - Agent" in new Setup(codingOutPartiallyCollectedView(isAgent = true)) {
        val paymentTypeLink: Element = layoutContent.getElementById("paymentTypeLink-2")
        paymentTypeLink.text shouldBe cancelledPaye
        paymentTypeLink.attr("href") shouldBe controllers.routes.ChargeSummaryController.showAgent(
          testYear, fullDocumentDetailModel.transactionId).url
      }

      "display MFA Debits - Individual" in new Setup(mfaDebitsView(isAgent = false)) {
        val paymentTypeLink: Element = layoutContent.getElementById("paymentTypeLink-0")
        paymentTypeLink.text shouldBe hmrcAdjustment
        paymentTypeLink.attr("href") shouldBe controllers.routes.ChargeSummaryController.show(
          testYear, MFADebitsDocumentDetailsWithDueDates.head.documentDetail.transactionId).url
      }

      "display MFA Debits - Agent" in new Setup(mfaDebitsView(isAgent = true)) {
        val paymentTypeLink: Element = layoutContent.getElementById("paymentTypeLink-0")
        paymentTypeLink.text shouldBe hmrcAdjustment
        paymentTypeLink.attr("href") shouldBe controllers.routes.ChargeSummaryController.showAgent(
          testYear, MFADebitsDocumentDetailsWithDueDates.head.documentDetail.transactionId).url
      }

    }
  }
}
