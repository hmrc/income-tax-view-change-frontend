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
import models.financialDetails.{Charge, SubItem}
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

  val testCharges: List[Charge] = List(
    Charge(taxYear = "2020", transactionId = "testId", outstandingAmount = Some(0.00), originalAmount = Some(100.00), mainType = Some("SA Payment on Account 1"), items = Some(Seq(SubItem(dueDate = Some(LocalDate.now.toString))))),
    Charge(taxYear = "2020", transactionId = "testId2", outstandingAmount = Some(100.00), originalAmount = Some(200.00), mainType = Some("SA Payment on Account 2"), items = Some(Seq(SubItem(dueDate = Some("2020-04-07"))))),
    Charge(taxYear = "2020", transactionId = "testId3", originalAmount = Some(100.00), mainType = Some("SA Balancing Charge"), items = Some(Seq(SubItem(dueDate = Some("2020-04-08")))))
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
           overview: CalcOverview = testCalcOverview,
           charges: List[Charge] = testCharges,
           obligations: ObligationsModel = testObligations): Html = {
    taxYearOverview(
      taxYear = taxYear,
      overview = overview,
      charges = charges,
      obligations = obligations,
      implicitDateFormatter = mockImplicitDateFormatter,
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

    val calculationTabIncome: String = "Income"
    val calculationTabAllowances: String = "Allowances and deductions"
    val calculationTabTotalIncome: String = "Total income on which tax is due"
    val calculationTabIncomeTaxDue: String = "Income Tax and National Insurance contributions due"

    val paymentsTabHeading: String = "Payments"
    val paymentsTabNoPayments: String = "No payments currently due."
    val paymentsTabTypeHeading: String = "Payment type"
    val paymentsTabDueHeading: String = "Due date"
    val paymentsTabStatusHeading: String = "Status"
    val paymentsTabAmountHeading: String = "Amount"

    val paymentsTabPaymentOnAccount1: String = "Payment on account 1 of 2"
    val paymentsTabPaymentOnAccount2: String = "Payment on account 2 of 2"
    val paymentsTabBalancingCharge: String = "Remaining balance"

    val paymentsTabPaid: String = "Paid"
    val paymentsTabPartPaid: String = "Part Paid"
    val paymentsTabUnpaid: String = "Unpaid"

    val paymentsTabOverdue: String = "Overdue"

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
        "the calculation is a crystallised calculation" in new Setup(view(overview = testCalcOverview.copy(crystallised = false))) {
          val listRow: Element = content.selectHead("dl").selectNth("div", 2)
          listRow.selectNth("dd", 1).text shouldBe TaxYearOverviewMessages.fromToEstimate("6 April 2019", "6 April 2020")
          listRow.selectNth("dd", 2).text shouldBe "£100.00"
        }
      }
    }

    "have information about the estimate calculation" when {
      "the calculation is an estimate" in new Setup(view(overview = testCalcOverview.copy(crystallised = false))) {
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
            "it is an estimate" in new Setup(view(overview = testCalcOverview.copy(crystallised = false))) {
              val heading: Element = content.selectHead("#taxCalculation").selectHead("h2")
              heading.text shouldBe TaxYearOverviewMessages.calculationTabHeadingEstimate("6 April 2019", "6 April 2020")
            }
            "it is crystallised" in new Setup(view()) {
              content.selectHead("#taxCalculation").selectHead("h2").text shouldBe TaxYearOverviewMessages.calculationTabHeadingCrystallised
            }
          }
          "has a table detailing a users income calculation" which {
            "has a row for the user's income" in new Setup(view()) {
              val row: Element = content.selectHead("#taxCalculation").selectNth("table", 1).selectNth("tr", 1)
              row.selectNth("td", 1).selectHead("a").text shouldBe TaxYearOverviewMessages.calculationTabIncome
              row.selectNth("td", 1).selectHead("a")
                .attr("href") shouldBe controllers.agent.routes.IncomeSummaryController.showIncomeSummary(2020).url
              row.selectNth("td", 2).text shouldBe "£150.00"
            }
            "has a row for the user's allowances and deductions" in new Setup(view()) {
              val row: Element = content.selectHead("#taxCalculation").selectNth("table", 1).selectNth("tr", 2)
              row.selectNth("td", 1).selectHead("a").text shouldBe TaxYearOverviewMessages.calculationTabAllowances
              row.selectNth("td", 1).selectHead("a").attr("href") shouldBe "/report-quarterly/income-and-expenses/view/agents/calculation/2020/deductions"
              row.selectNth("td", 2).text shouldBe "-£50.00"
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
          "has no payments due content" when {
            "no payments are due" in new Setup(view(charges = Nil)) {
              content.selectHead("#payments").selectHead("p").text shouldBe TaxYearOverviewMessages.paymentsTabNoPayments
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
            "has a row for a paid payment on account 1" in new Setup(view()) {
              val row: Element = content.selectHead("#payments").selectHead("table").selectNth("tr", 2)
              row.selectNth("td", 1).selectHead("div").selectHead("a").text shouldBe TaxYearOverviewMessages.paymentsTabPaymentOnAccount1
              row.selectNth("td", 1).selectHead("div").selectHead("a").attr("href") shouldBe
                controllers.agent.routes.ChargeSummaryController.showChargeSummary(testYear, "testId").url
              row.selectNth("td", 2).text shouldBe LocalDate.now.toLongDate
              row.selectNth("td", 3).text shouldBe TaxYearOverviewMessages.paymentsTabPaid
              row.selectNth("td", 4).text shouldBe "£100.00"
            }
            "has a row for a part paid (overdue) payment on account 2" in new Setup(view()) {
              val row: Element = content.selectHead("#payments").selectHead("table").selectNth("tr", 3)
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
              val row: Element = content.selectHead("#payments").selectHead("table").selectNth("tr", 4)
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
