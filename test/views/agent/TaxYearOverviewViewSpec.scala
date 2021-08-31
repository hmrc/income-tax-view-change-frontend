/*
 * Copyright 2021 HM Revenue & Customs
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

/*
 * Copyright 2021 HM Revenue & Customs
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

package views.agent

import assets.BaseTestConstants.{testMtdItUser, testMtditid}
import config.featureswitch._
import models.calculation.{AllowancesAndDeductions, CalcOverview, Calculation}
import models.financialDetails.{DocumentDetail, DocumentDetailWithDueDate, FinancialDetail, SubItem}
import models.financialTransactions.TransactionModel
import models.reportDeadlines.{ObligationsModel, ReportDeadlineModel, ReportDeadlineModelWithIncomeType, ReportDeadlinesModel}
import org.jsoup.nodes.Element
import play.twirl.api.Html
import testUtils.ViewSpec
import views.html.agent.TaxYearOverview

import java.time.LocalDate
import scala.collection.JavaConversions._

class TaxYearOverviewViewSpec extends ViewSpec with FeatureSwitching {

  val taxYearOverview: TaxYearOverview = app.injector.instanceOf[TaxYearOverview]
  val testYear: Int = 2020
  val testCalcOverview: CalcOverview = CalcOverview(
    calculation = Calculation(
      crystallised = true,
      timestamp = Some("2020-04-06T12:34:56.789Z"),
      totalIncomeTaxAndNicsDue = Some(100.00),
      totalIncomeReceived = Some(150.00),
      allowancesAndDeductions = AllowancesAndDeductions(totalAllowancesAndDeductions = Some(25.00), totalReliefs = Some(25.00)),
      totalTaxableIncome = Some(30.00)
    ),
    transaction = Some(TransactionModel())
  )
  val testDunningLockChargesList: List[DocumentDetailWithDueDate] = List(
    DocumentDetailWithDueDate(DocumentDetail(taxYear = "2020", transactionId = "testId", outstandingAmount = Some(0.00), originalAmount = Some(100.00),
      documentDescription = Some("ITSA- POA 1"), documentDate = date(29, 3, 2018)), Some(LocalDate.now), dunningLock = true) ,
    DocumentDetailWithDueDate(DocumentDetail(taxYear = "2020", transactionId = "testId2", outstandingAmount = Some(100.00), originalAmount = Some(200.00),
      documentDescription = Some("ITSA - POA 2"), documentDate = date(29, 3, 2018)), Some(date(7, 4, 2020))),
    DocumentDetailWithDueDate(DocumentDetail(taxYear = "2020", transactionId = "testId3", outstandingAmount = Some(100.00), originalAmount = Some(100.00),
      documentDescription = Some("TRM New Charge"), documentDate = date(29, 3, 2018)), Some(date(8, 4, 2020)), dunningLock = true))

  def date(day: Int, month: Int, year: Int): LocalDate = LocalDate.of(year, month, day)

  val testCharges: List[DocumentDetailWithDueDate] = List(
    DocumentDetailWithDueDate(DocumentDetail(taxYear = "2020", transactionId = "testId", outstandingAmount = Some(0.00), originalAmount = Some(100.00),
      documentDescription = Some("ITSA- POA 1"), documentDate = date(29, 3, 2018)), Some(LocalDate.now)),
    DocumentDetailWithDueDate(DocumentDetail(taxYear = "2020", transactionId = "testId2", outstandingAmount = Some(100.00), originalAmount = Some(200.00),
      documentDescription = Some("ITSA - POA 2"), documentDate = date(29, 3, 2018)), Some(date(7, 4, 2020))),
    DocumentDetailWithDueDate(DocumentDetail(taxYear = "2020", transactionId = "testId3", outstandingAmount = Some(100.00), originalAmount = Some(100.00),
      documentDescription = Some("TRM New Charge"), documentDate = date(29, 3, 2018)), Some(date(8, 4, 2020))),
    DocumentDetailWithDueDate(DocumentDetail(taxYear = "2020", transactionId = "testId", outstandingAmount = Some(0.00), originalAmount = Some(100.00),
      documentDescription = Some("ITSA- POA 1"), documentDate = date(29, 3, 2018), latePaymentInterestAmount = Some(200.0)), Some(LocalDate.now.plusMonths(1)), true),
    DocumentDetailWithDueDate(DocumentDetail(taxYear = "2020", transactionId = "testId2", outstandingAmount = Some(100.00), originalAmount = Some(200.00),
      documentDescription = Some("ITSA - POA 2"), documentDate = date(29, 3, 2018), latePaymentInterestAmount = Some(80.0)), Some(LocalDate.now.plusMonths(2)), true),
    DocumentDetailWithDueDate(DocumentDetail(taxYear = "2020", transactionId = "testId3", outstandingAmount = Some(100.00), originalAmount = Some(100.00),
      documentDescription = Some("TRM New Charge"), documentDate = date(29, 3, 2018), interestOutstandingAmount = Some(0.0)), Some(LocalDate.now.plusMonths(3)), true)
  )

  implicit val localDateOrdering: Ordering[LocalDate] = Ordering.by(_.toEpochDay)

  val testObligations: ObligationsModel = ObligationsModel(Seq(
    ReportDeadlinesModel("XA00001234", List(
      ReportDeadlineModel(
        start = LocalDate.of(2020, 1, 1),
        end = LocalDate.of(2020, 3, 31),
        due = LocalDate.of(2020, 3, 31),
        obligationType = "Quarterly",
        dateReceived = Some(LocalDate.of(2020, 3, 30)),
        periodKey = "#001"
      ),
      ReportDeadlineModel(
        start = LocalDate.of(2020, 4, 1),
        end = LocalDate.of(2020, 6, 30),
        due = LocalDate.of(2020, 6, 30),
        obligationType = "EOPS",
        dateReceived = Some(LocalDate.of(2020, 6, 29)),
        periodKey = "EOPS"
      )
    )),
    ReportDeadlinesModel("1234", List(
      ReportDeadlineModel(
        start = LocalDate.of(2020, 1, 1),
        end = LocalDate.of(2020, 3, 31),
        due = LocalDate.of(2020, 3, 31),
        obligationType = "Quarterly",
        dateReceived = None,
        periodKey = "#001"
      )
    )),
    ReportDeadlinesModel(testMtditid, List(
      ReportDeadlineModel(
        start = LocalDate.of(2020, 4, 1),
        end = LocalDate.of(2020, 6, 30),
        due = LocalDate.of(2020, 6, 30),
        obligationType = "Crystallised",
        dateReceived = Some(LocalDate.of(2020, 6, 29)),
        periodKey = "Crystallised"
      )
    ))
  ))
  val groupedObligations: Seq[(LocalDate, Seq[ReportDeadlineModelWithIncomeType])] = {
    testObligations.allDeadlinesWithSource(previous = true).reverse.groupBy[LocalDate] { reportDeadlineWithIncomeType =>
      reportDeadlineWithIncomeType.obligation.due
    }.toList.sortBy(_._1)
  }

  def view(taxYear: Int = testYear,
           overview: Option[CalcOverview] = Some(testCalcOverview),
           documentDetailsWithDueDates: List[DocumentDetailWithDueDate] = testCharges,
           obligations: ObligationsModel = testObligations): Html = {
    taxYearOverview(
      taxYear = taxYear,
      overviewOpt = overview,
      documentDetailsWithDueDates = documentDetailsWithDueDates,
      obligations = obligations,
      backUrl = "/testBack"
    )
  }

  def multipleDunningLockView(taxYear: Int = testYear,
           overview: Option[CalcOverview] = Some(testCalcOverview),
           documentDetailsWithDueDates: List[DocumentDetailWithDueDate] = testDunningLockChargesList,
           obligations: ObligationsModel = testObligations): Html = {
    taxYearOverview(
      taxYear = taxYear,
      overviewOpt = overview,
      documentDetailsWithDueDates = documentDetailsWithDueDates,
      obligations = obligations,
      backUrl = "/testBack"
    )
  }
  import mockImplicitDateFormatter.longDate

  object TaxYearOverviewMessages {
    val heading: String = "Tax year overview"
    val title: String = s"$heading - Your client’s Income Tax details - GOV.UK"

    val calculationDate: String = "Calculation date"
    val totalDue: String = "Total Due"

    def fromToEstimate(from: String, to: String): String = s"$from to $to estimate"

    val estimateMessage: String = "This calculation is from the last time you viewed your tax calculation in your own software. You will need to view it in your software for the most up to date version."

    val contents: String = "Contents"
    val calculationTabLabel: String = "Tax calculation"
    val paymentsTabLabel: String = "Payments"
    val updatesTabLabel: String = "Updates"

    val calculationTabHeadingCrystallised: String = "Tax calculation"

    def calculationTabHeadingEstimate(from: String, to: String): String = s"$from to $to estimate"

    val sectionHeader: String = "Section"
    val amountHeader: String = "Amount"
    val calculationTabIncome: String = "Income"
    val calculationTabAllowances: String = "Allowances and deductions"
    val calculationTabTotalIncome: String = "Total income on which tax is due"
    val calculationTabIncomeTaxDue: String = "Income Tax and National Insurance contributions due"
    val taxCalculationNoData: String = "No calculation yet"
    val taxCalculationNoDataNote: String = "You will be able to see your latest tax year calculation here once you have sent an update and viewed it in your software."


    val paymentsTabHeading: String = "Payments"
    val paymentsTabNoPayments: String = "No payments currently due."
    val paymentsTabTypeHeading: String = "Payment type"
    val paymentsTabDueHeading: String = "Due date"
    val paymentsTabStatusHeading: String = "Status"
    val paymentsTabAmountHeading: String = "Amount"

    val paymentsTabPaymentOnAccount1: String = "Payment on account 1 of 2"
    val paymentsTabPaymentOnAccount2: String = "Payment on account 2 of 2"
    val paymentsTabBalancingCharge: String = "Remaining balance"
    val lpiPaymentOnAccount1: String = "Late payment interest on payment on account 1 of 2"
    val lpiPaymentOnAccount2: String = "Late payment interest on payment on account 2 of 2"
    val lpiRemainingBalance: String = "Late payment interest on remaining balance"

    val paymentsTabPaid: String = "Paid"
    val paymentsTabPartPaid: String = "Part Paid"
    val paymentsTabUnpaid: String = "Unpaid"

    val paymentsTabOverdue: String = "Overdue"

    val paymentUnderReview: String = "Payment under review"

    val updatesTabHeading: String = "Updates"

    def updatesTabUpdatesDue(due: String): String = s"Due $due"

    def updatesTabCaption(from: String, to: String): String = s"$from to $to"

    val updatesTabUpdateTypeHeading: String = "Update type"
    val updatesTabIncomeSourceHeading: String = "Income source"
    val updatesTabDateSubmittedHeading: String = "Date submitted"

    val updatesTabQuarterlyUpdate: String = "Quarterly Update"
    val updatesTabAnnualUpdate: String = "Annual Update"
    val updatesTabFinalDeclaration: String = "Final Declaration"

    val updatesTabPropertyIncome: String = "Property income"
    val updatesTabAllIncome: String = "All income sources"

    val back: String = "Back"
  }

  "TaxYearOverview" should {

    "have a title" in new Setup(view()) {
      document.title shouldBe TaxYearOverviewMessages.title
    }

    "have a back link" in new Setup(view()) {
      val backLink: Element = content.backLink
      backLink.text shouldBe TaxYearOverviewMessages.back
      backLink.attr("href") shouldBe "/testBack"
    }

    "have a summary list with information about the calculation" which {
      "has a calculation date" in new Setup(view()) {
        val listRow: Element = content.selectHead("dl").selectNth("div", 1)
        listRow.selectNth("dd", 1).text shouldBe TaxYearOverviewMessages.calculationDate
        listRow.selectNth("dd", 2).text shouldBe "6 April 2020"
      }
      "has a tax due row" when {
        "the calculation is an estimate" in new Setup(view()) {
          val listRow: Element = content.selectHead("dl").selectNth("div", 2)
          listRow.selectNth("dd", 1).text shouldBe TaxYearOverviewMessages.totalDue
          listRow.selectNth("dd", 2).text shouldBe "£100.00"
        }
        "the calculation is a crystallised calculation" in new Setup(view(overview = Some(testCalcOverview.copy(crystallised = false)))) {
          val listRow: Element = content.selectHead("dl").selectNth("div", 2)
          listRow.selectNth("dd", 1).text shouldBe TaxYearOverviewMessages.fromToEstimate("6 April 2019", "6 April 2020")
          listRow.selectNth("dd", 2).text shouldBe "£100.00"
        }
      }
    }

    "have information about the estimate calculation" when {
      "the calculation is an estimate" in new Setup(view(overview = Some(testCalcOverview.copy(crystallised = false)))) {
        content.selectHead("div.panel").text shouldBe TaxYearOverviewMessages.estimateMessage
      }
    }

    "not display the information about an estimate calculation" when {
      "the calculation is crystallised" in new Setup(view()) {
        content.select("div.panel").headOption shouldBe None
      }
    }

    "have tabs to display information about the user's tax year" which {
      "has a contents label" in new Setup(view()) {
        content.selectHead("div.govuk-tabs").selectHead("h2.govuk-tabs__title").text shouldBe TaxYearOverviewMessages.contents
      }
      "has a calculation tab" which {
        "has a tab label" in new Setup(view()) {
          val tab: Element = content.selectHead("div.govuk-tabs").selectHead("ul").selectNth("li", 1).selectHead("a")
          tab.text shouldBe TaxYearOverviewMessages.calculationTabLabel
          tab.attr("href") shouldBe "#taxCalculation"
          tab.attr("role") shouldBe "tab"
          tab.attr("aria-controls") shouldBe "taxCalculation"
          tab.attr("aria-selected") shouldBe "true"
        }
        "has the tab contents" which {
          "has different headings" when {
            "it is an estimate" in new Setup(view(overview = Some(testCalcOverview.copy(crystallised = false)))) {
              content.selectHead("dl > div:nth-child(1) > dd:nth-child(1)").text shouldBe TaxYearOverviewMessages.calculationDate

            }
            "it is crystallised" in new Setup(view()) {

              content.selectHead("dl > div:nth-child(2) > dd:nth-child(1)").text shouldBe TaxYearOverviewMessages.totalDue

            }
            "there is no calculation data" in new Setup(view(overview = None)) {
              content.selectHead("#taxCalculation").selectHead("h2").text shouldBe TaxYearOverviewMessages.taxCalculationNoData
            }
          }
          "has a paragraph" when {
            "there is no calculation data" in new Setup(view(overview = None)) {
              content.selectHead("#taxCalculation").selectHead("p").text shouldBe TaxYearOverviewMessages.taxCalculationNoDataNote
            }
          }
          "has a table detailing a users income calculation" which {

            "display the section header in the Tax Calculation tab" in new Setup(view()) {
              val sectionHeader: Element = content.selectHead(" #income-deductions-table tr:nth-child(1) th:nth-child(1)")
              sectionHeader.text shouldBe TaxYearOverviewMessages.sectionHeader
            }

            "display the amount header in the Tax Calculation tab" in new Setup(view()) {
              val amountHeader: Element = content.selectHead(" #income-deductions-table tr:nth-child(1) th:nth-child(2)")
              amountHeader.text shouldBe TaxYearOverviewMessages.amountHeader
            }

            "has a row for the user's income" in new Setup(view()) {
              val incomeLink: Element = content.selectHead(" #income-deductions-table tr:nth-child(1) td:nth-child(1) a")
              incomeLink.text shouldBe TaxYearOverviewMessages.calculationTabIncome
              incomeLink.attr("href") shouldBe controllers.agent.routes.IncomeSummaryController.showIncomeSummary(2020).url
              content.selectHead("#income-deductions-table tr:nth-child(1) td:nth-child(2)").text shouldBe "£150.00"
            }
            "has a row for the user's allowances and deductions" in new Setup(view()) {
              val row: Element = content.selectHead("#taxCalculation").selectNth("table", 1).selectNth("tr", 2)
              row.selectNth("td", 1).selectHead("a").text shouldBe TaxYearOverviewMessages.calculationTabAllowances
              row.selectNth("td", 1).selectHead("a").attr("href") shouldBe "/report-quarterly/income-and-expenses/view/agents/calculation/2020/deductions"
              row.selectNth("td", 2).text shouldBe "−£50.00"
            }
            "has a row for the total income on which tax is due" in new Setup(view()) {
              val row: Element = content.selectHead("#taxCalculation").selectNth("table", 1).selectNth("tr", 3)
              row.selectNth("td", 1).text shouldBe TaxYearOverviewMessages.calculationTabTotalIncome
              row.selectNth("td", 2).text shouldBe "£30.00"
            }
          }
          "has a table details a users tax calculation" in new Setup(view()) {
            val row: Element = content.selectHead("#taxCalculation").selectNth("table", 2).selectNth("tr", 1)
            row.selectNth("td", 1).text shouldBe TaxYearOverviewMessages.calculationTabIncomeTaxDue
            row.selectNth("td", 1).selectHead("a")
              .attr("href") shouldBe controllers.agent.routes.TaxDueSummaryController.showTaxDueSummary(2020).url
            row.selectNth("td", 2).text shouldBe "£100.00"
          }
        }
      }
      "has a payments tab" which {
        "has a tab label" in new Setup(view()) {
          val tab: Element = content.selectHead("div.govuk-tabs").selectHead("ul").selectNth("li", 2).selectHead("a")
          tab.text shouldBe TaxYearOverviewMessages.paymentsTabLabel
          tab.attr("href") shouldBe "#payments"
          tab.attr("role") shouldBe "tab"
          tab.attr("aria-controls") shouldBe "payments"
          tab.attr("aria-selected") shouldBe "false"
        }
        "has a tab contents" which {
          "has a heading" in new Setup(view()) {
            content.selectHead("#payments").selectHead("h2").text shouldBe TaxYearOverviewMessages.paymentsTabHeading
          }
          "has a caption" in new Setup(view()) {
            content.selectHead("#payments").selectHead(" caption").text.contains(TaxYearOverviewMessages.paymentsTabLabel)
          }
          "has no payments due content" when {
            "no payments are due" in new Setup(view(documentDetailsWithDueDates = Nil)) {
              content.selectHead("#payments").selectHead("p").text shouldBe TaxYearOverviewMessages.paymentsTabNoPayments
              content.selectHead("#payments").doesNotHave("table")
              content.selectHead("#payments").selectHead("h2").text shouldBe TaxYearOverviewMessages.paymentsTabHeading
            }
          }
          "has a table of the payments for the year" which {
            "has table headings" in new Setup(view()) {
              val row: Element = content.selectHead("#payments").selectHead("table").selectNth("tr", 1)
              row.selectNth("th", 1).text shouldBe TaxYearOverviewMessages.paymentsTabTypeHeading
              row.selectNth("th", 2).text shouldBe TaxYearOverviewMessages.paymentsTabDueHeading
              row.selectNth("th", 3).text shouldBe TaxYearOverviewMessages.paymentsTabStatusHeading
              row.selectNth("th", 4).text shouldBe TaxYearOverviewMessages.paymentsTabAmountHeading
            }
            "has a row for a part paid (overdue) payment on account 2" in new Setup(view()) {
              val row: Element = content.selectHead("#payments").selectHead("table").selectNth("tr", 2)
              val firstColumn: Element = row.selectNth("td", 1)
              firstColumn.selectNth("div", 1).text shouldBe TaxYearOverviewMessages.paymentsTabOverdue
              firstColumn.selectNth("div", 1).attr("class") shouldBe "govuk-tag govuk-tag--red"
              firstColumn.selectNth("div", 2).selectHead("a").text shouldBe TaxYearOverviewMessages.paymentsTabPaymentOnAccount2
              firstColumn.selectNth("div", 2).selectHead("a").attr("href") shouldBe
                controllers.agent.routes.ChargeSummaryController.showChargeSummary(testYear, "testId2").url

              row.selectNth("td", 2).text shouldBe "7 April 2020"
              row.selectNth("td", 3).text shouldBe TaxYearOverviewMessages.paymentsTabPartPaid
              row.selectNth("td", 4).text shouldBe "£200.00"
            }
            "has a row for a unpaid (overdue) balancing charge" in new Setup(view()) {
              val row: Element = content.selectHead("#payments").selectHead("table").selectNth("tr", 3)
              val firstColumn: Element = row.selectNth("td", 1)
              firstColumn.selectNth("div", 1).text shouldBe TaxYearOverviewMessages.paymentsTabOverdue
              firstColumn.selectNth("div", 1).attr("class") shouldBe "govuk-tag govuk-tag--red"
              firstColumn.selectNth("div", 2).selectHead("a").text shouldBe TaxYearOverviewMessages.paymentsTabBalancingCharge
              firstColumn.selectNth("div", 2).selectHead("a").attr("href") shouldBe
                controllers.agent.routes.ChargeSummaryController.showChargeSummary(testYear, "testId3").url

              row.selectNth("td", 2).text shouldBe "8 April 2020"
              row.selectNth("td", 3).text shouldBe TaxYearOverviewMessages.paymentsTabUnpaid
              row.selectNth("td", 4).text shouldBe "£100.00"
            }
            "has a row for a paid payment on account 1" in new Setup(view()) {
              val row: Element = content.selectHead("#payments").selectHead("table").selectNth("tr", 4)
              row.selectNth("td", 1).selectHead("div").selectHead("a").text shouldBe TaxYearOverviewMessages.paymentsTabPaymentOnAccount1
              row.selectNth("td", 1).selectHead("div").selectHead("a").attr("href") shouldBe
                controllers.agent.routes.ChargeSummaryController.showChargeSummary(testYear, "testId").url
              row.selectNth("td", 2).text shouldBe LocalDate.now.toLongDate
              row.selectNth("td", 3).text shouldBe TaxYearOverviewMessages.paymentsTabPaid
              row.selectNth("td", 4).text shouldBe "£100.00"
            }
            "display the payment type as a link to Charge Summary in the Payments tab for late payment interest POA1" in new Setup(view()) {
              val paymentTypeLink: Element = content.selectHead("#payments-table tr:nth-child(5) td:nth-child(1) a")
              paymentTypeLink.text shouldBe TaxYearOverviewMessages.lpiPaymentOnAccount1
              paymentTypeLink.attr("href") shouldBe controllers.agent.routes.ChargeSummaryController.showChargeSummary(
                testYear, "testId", true).url
            }

            "display the Due date in the Payments tab for late payment interest POA1" in new Setup(view()) {
              content.selectHead("#payments-table tr:nth-child(5) td:nth-child(2)").text shouldBe LocalDate.now().plusMonths(1).toLongDate
            }

            "display the Status in the payments tab for late payment interest POA1" in new Setup(view()) {
              content.selectHead("#payments-table tr:nth-child(5) td:nth-child(3)").text shouldBe TaxYearOverviewMessages.paymentsTabPartPaid
            }

            "display the Amount in the payments tab for late payment interest POA1" in new Setup(view()) {
              content.selectHead("#payments-table tr:nth-child(5) td:nth-child(4)").text shouldBe "£200.00"
            }

            "display the payment type as a link to Charge Summary in the Payments tab for late payment interest POA2" in new Setup(view()) {
              val paymentTypeLink: Element = content.selectHead("#payments-table tr:nth-child(6) td:nth-child(1) a")
              paymentTypeLink.text shouldBe TaxYearOverviewMessages.lpiPaymentOnAccount2
              paymentTypeLink.attr("href") shouldBe controllers.agent.routes.ChargeSummaryController.showChargeSummary(
                testYear, "testId2", true).url
            }

            "display the Due date in the Payments tab for late payment interest POA2" in new Setup(view()) {
              content.selectHead("#payments-table tr:nth-child(6) td:nth-child(2)").text shouldBe LocalDate.now().plusMonths(2).toLongDate
            }

            "display the Status in the payments tab for late payment interest POA2" in new Setup(view()) {
              content.selectHead("#payments-table tr:nth-child(6) td:nth-child(3)").text shouldBe TaxYearOverviewMessages.paymentsTabPartPaid
            }

            "display the Amount in the payments tab for late payment interest POA2" in new Setup(view()) {
              content.selectHead("#payments-table tr:nth-child(6) td:nth-child(4)").text shouldBe "£80.00"
            }

            "display the payment type as a link to Charge Summary in the Payments tab for late payment interest remaining balance" in new Setup(view()) {
              val paymentTypeLink: Element = content.selectHead("#payments-table tr:nth-child(7) td:nth-child(1) a")
              paymentTypeLink.text shouldBe TaxYearOverviewMessages.lpiRemainingBalance
              paymentTypeLink.attr("href") shouldBe controllers.agent.routes.ChargeSummaryController.showChargeSummary(
                testYear, "testId3", true).url
            }

            "display the Due date in the Payments tab for late payment interest remaining balance" in new Setup(view()) {
              content.selectHead("#payments-table tr:nth-child(7) td:nth-child(2)").text shouldBe LocalDate.now().plusMonths(3).toLongDate
            }

            "display the Status in the payments tab for late payment interest remaining balance" in new Setup(view()) {
              content.selectHead("#payments-table tr:nth-child(7) td:nth-child(3)").text shouldBe TaxYearOverviewMessages.paymentsTabUnpaid
            }

            "display the Amount in the payments tab for late payment interest remaining balance" in new Setup(view()) {
              content.selectHead("#payments-table tr:nth-child(7) td:nth-child(4)").text shouldBe "£100.00"
            }
            "display the Dunning lock subheading in the payments tab for multiple lines POA and Remaining Balance" in new Setup(multipleDunningLockView()) {
              content.doesNotHave("#payments-table tbody tr:nth-child(2) td:nth-child(1) div:nth-child(3)")
              content.selectHead("#payments-table tbody tr:nth-child(3) td:nth-child(1) div:nth-child(3)").text shouldBe TaxYearOverviewMessages.paymentUnderReview
              content.selectHead("#payments-table tbody tr:nth-child(4) td:nth-child(1) div:nth-child(2)").text shouldBe TaxYearOverviewMessages.paymentUnderReview
            }
          }
        }
      }
      "has an updates tab" which {
        "has a tab label" in new Setup(view()) {
          val tab: Element = content.selectHead("div.govuk-tabs").selectHead("ul").selectNth("li", 3).selectHead("a")
          tab.text shouldBe TaxYearOverviewMessages.updatesTabLabel
          tab.attr("href") shouldBe "#updates"
          tab.attr("role") shouldBe "tab"
          tab.attr("aria-controls") shouldBe "updates"
          tab.attr("aria-selected") shouldBe "false"
        }
        "has a tab contents" which {
          "has a heading" in new Setup(view()) {
            content.selectHead("#updates").selectHead("h2").text shouldBe TaxYearOverviewMessages.updatesTabHeading
          }
          "has all obligations grouped by their due date" which {
            "has a first grouped obligations" which {
              "has a heading of the due date for those obligations" in new Setup(view()) {
                content.selectHead("#updates").selectNth("div", 1).selectHead("h3").text shouldBe TaxYearOverviewMessages.updatesTabUpdatesDue("31 March 2020")
              }
              "has a table of the obligations in that group" which {
                "has a caption of the start and end dates" in new Setup(view()) {
                  content.selectHead("#updates").selectNth("div", 1).selectHead("table")
                    .selectHead("caption").text shouldBe TaxYearOverviewMessages.updatesTabCaption("1 January 2020", "31 March 2020")
                }
                "has table headings for each table column" in new Setup(view()) {
                  val row: Element = content.selectHead("#updates").selectNth("div", 1).selectHead("table").selectHead("thead").selectHead("tr")
                  row.selectNth("th", 1).text shouldBe TaxYearOverviewMessages.updatesTabUpdateTypeHeading
                  row.selectNth("th", 2).text shouldBe TaxYearOverviewMessages.updatesTabIncomeSourceHeading
                  row.selectNth("th", 3).text shouldBe TaxYearOverviewMessages.updatesTabDateSubmittedHeading
                }
                "has the first obligation (quarterly, non submitted) for the users property" in new Setup(view()) {
                  val row: Element = content.selectHead("#updates").selectNth("div", 1).selectHead("table").selectHead("tbody").selectNth("tr", 1)
                  row.selectNth("td", 1).text shouldBe TaxYearOverviewMessages.updatesTabQuarterlyUpdate
                  row.selectNth("td", 2).text shouldBe TaxYearOverviewMessages.updatesTabPropertyIncome
                  row.selectNth("td", 3).text shouldBe ""
                }
                "has the second obligation (quarterly, submitted) for the users business" in new Setup(view()) {
                  val row: Element = content.selectHead("#updates").selectNth("div", 1).selectHead("table").selectHead("tbody").selectNth("tr", 2)
                  row.selectNth("td", 1).text shouldBe TaxYearOverviewMessages.updatesTabQuarterlyUpdate
                  row.selectNth("td", 2).text shouldBe testMtdItUser.incomeSources.businesses.head.tradingName.get
                  row.selectNth("td", 3).text shouldBe "30 March 2020"
                }
              }
            }
            "has the second grouped obligations" which {
              "has a heading of the due date for those obligations" in new Setup(view()) {
                content.selectHead("#updates").selectNth("div", 2).selectHead("h3").text shouldBe TaxYearOverviewMessages.updatesTabUpdatesDue("30 June 2020")
              }
              "has a table of the obligations in that group" which {
                "has a caption of the start and end dates" in new Setup(view()) {
                  content.selectHead("#updates").selectNth("div", 2).selectHead("table")
                    .selectHead("caption").text shouldBe TaxYearOverviewMessages.updatesTabCaption("1 April 2020", "30 June 2020")
                }
                "has table headings for each table column" in new Setup(view()) {
                  val row: Element = content.selectHead("#updates").selectNth("div", 2).selectHead("table").selectHead("thead").selectHead("tr")
                  row.selectNth("th", 1).text shouldBe TaxYearOverviewMessages.updatesTabUpdateTypeHeading
                  row.selectNth("th", 2).text shouldBe TaxYearOverviewMessages.updatesTabIncomeSourceHeading
                  row.selectNth("th", 3).text shouldBe TaxYearOverviewMessages.updatesTabDateSubmittedHeading
                }
                "has the first obligation (annual, submitted) for the users business" in new Setup(view()) {
                  val row: Element = content.selectHead("#updates").selectNth("div", 2).selectHead("table").selectHead("tbody").selectNth("tr", 1)
                  row.selectNth("td", 1).text shouldBe TaxYearOverviewMessages.updatesTabAnnualUpdate
                  row.selectNth("td", 2).text shouldBe testMtdItUser.incomeSources.businesses.head.tradingName.get
                  row.selectNth("td", 3).text shouldBe "29 June 2020"
                }
                "has the second obligation (final declaration, submitted) for the user" in new Setup(view()) {
                  val row: Element = content.selectHead("#updates").selectNth("div", 2).selectHead("table").selectHead("tbody").selectNth("tr", 2)
                  row.selectNth("td", 1).text shouldBe TaxYearOverviewMessages.updatesTabFinalDeclaration
                  row.selectNth("td", 2).text shouldBe TaxYearOverviewMessages.updatesTabAllIncome
                  row.selectNth("td", 3).text shouldBe "29 June 2020"
                }
              }
            }
          }
        }
      }
    }
  }
}
