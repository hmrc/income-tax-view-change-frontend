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

import assets.FinancialDetailsTestConstants._
import config.featureswitch.FeatureSwitching
import models.chargeHistory.ChargeHistoryModel
import models.financialDetails.{DocumentDetail, DocumentDetailWithDueDate, FinancialDetail, FinancialDetailsModel, Payment, PaymentsWithChargeType, SubItem}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements
import org.scalatest.Assertion
import play.twirl.api.Html
import testUtils.{TestSupport, ViewSpec}
import views.html.agent.ChargeSummary

import java.time.LocalDate

class ChargeSummaryViewSpec extends TestSupport with FeatureSwitching with ViewSpec {

  class Setup(documentDetailWithDueDate: DocumentDetailWithDueDate,
              chargeHistoryOpt: Option[List[ChargeHistoryModel]] = Some(List()),
              latePaymentInterestCharge: Boolean = false,
              paymentAllocations: List[PaymentsWithChargeType]= List(),
              paymentAllocationEnabled: Boolean = false,
              paymentBreakdown: List[FinancialDetail] = List(),
							payments: FinancialDetailsModel = FinancialDetailsModel(List(), List())
             ) {

    val chargeSummary: ChargeSummary = app.injector.instanceOf[ChargeSummary]

    val chargeSummaryView: Html = chargeSummary(
      documentDetailWithDueDate = documentDetailWithDueDate,
      chargeHistoryOpt = chargeHistoryOpt,
      backUrl = "testBackURL",
      paymentAllocations,
      paymentBreakdown = paymentBreakdown,
			payments = payments,
			latePaymentInterestCharge = latePaymentInterestCharge,
      paymentAllocationEnabled
    )

    val document: Document = Jsoup.parse(chargeSummaryView.toString())


    lazy val content: Element = document.selectHead("#content")

    def getElementById(id: String): Option[Element] = Option(document.getElementById(id))

    def verifyChargesHistoryContent(rows: String*): Assertion = {
      content select Selectors.table text() shouldBe
        s"""
           |Date Description Amount
           |${rows.mkString("\n")}
           |""".stripMargin.trim.linesIterator.mkString(" ")
    }


    def verifyPaymentBreakdownRow(rowNumber: Int, expectedKeyText: String, expectedValueText: String): Assertion = {
      val paymentBreakdownRow = document.select(s".govuk-summary-list:nth-of-type(2) .govuk-summary-list__row:nth-of-type($rowNumber)")
      paymentBreakdownRow.select(".govuk-summary-list__key").text() shouldBe expectedKeyText
      paymentBreakdownRow.select(".govuk-summary-list__value").text() shouldBe expectedValueText
    }
  }

  object Messages {
    def poaHeading(year: Int, number: Int): String = s"Tax year 6 April ${year - 1} to 5 April $year Payment on account $number of 2"

    def poaInterestHeading(year: Int, number: Int) = s"Tax year 6 April ${year - 1} to 5 April $year Late payment interest on payment on account $number of 2"
    def balancingChargeHeading(year: Int): String = s"Tax year 6 April ${year - 1} to 5 April $year Remaining balance"
    def balancingChargeInterestHeading(year: Int) =  s"Tax year 6 April ${year - 1} to 5 April $year Late payment interest on remaining balance"

    val dueDate = "Due date"
    val interestPeriod = "Interest period"
    val fullPaymentAmount = "Full payment amount"
    val remainingToPay = "Remaining to pay"
    val paymentBreakdownHeading = "Payment breakdown"
    val chargeHistoryHeading = "Payment history"
    val historyRowPOA1Created = "29 Mar 2018 Payment on account 1 of 2 created £1,400.00"
    val dunningLockBannerHeader = "Important"
    val dunningLockBannerLink = "This tax decision is being reviewed (opens in new tab)"
    def dunningLockBannerText(formattedAmount: String, date: String) =
      s"$dunningLockBannerLink. You still need to pay the total of $formattedAmount as you may be charged interest if not paid by $date."

  }

  val paymentBreakdown: List[FinancialDetail] = List(
    financialDetail(originalAmount = 123.45, chargeType = "ITSA England & NI"),
    financialDetail(originalAmount = 2345.67, chargeType = "NIC2-GB"),
    financialDetail(originalAmount = 3456.78, chargeType = "Voluntary NIC2-NI"),
    financialDetail(originalAmount = 5678.9, chargeType = "NIC4 Wales"),
    financialDetail(originalAmount = 9876.54, chargeType = "CGT"),
    financialDetail(originalAmount = 543.21, chargeType = "SL")
  )

  val paymentBreakdownWithLocks: List[FinancialDetail] = List(
    financialDetail(originalAmount = 123.45, chargeType = "ITSA England & NI"),
    financialDetail(originalAmount = 2345.67, chargeType = "NIC2-GB", dunningLock = Some("Stand over order")),
    financialDetail(originalAmount = 9876.54, chargeType = "CGT", dunningLock = Some("Stand over order")),
    financialDetail(originalAmount = 543.21, chargeType = "SL")
  )

  "The agent charge summary view" should {

    "have the correct heading for a POA 1" in new Setup(documentDetailPOA1) {
      document.select("h1").text shouldBe Messages.poaHeading(2018, 1)
    }

    "have the correct heading for a POA 2" in new Setup(documentDetailPOA2) {
      document.select("h1").text shouldBe Messages.poaHeading(2018, 2)
    }

    "have the correct heading for a balancing charge" in new Setup(documentDetailBalancingCharge) {
      document.select("h1").text shouldBe Messages.balancingChargeHeading(2018)
    }

    "display a due date" in new Setup(documentDetailWithDueDateModel()) {
      document.select(".govuk-summary-list .govuk-summary-list__row:nth-of-type(1) .govuk-summary-list__value")
        .text shouldBe "OVERDUE 15 May 2019"
    }

    "display a charge amount" in new Setup(documentDetailWithDueDateModel(originalAmount = Some(1500))) {
      document.select(".govuk-summary-list .govuk-summary-list__row:nth-of-type(2) .govuk-summary-list__value")
        .text shouldBe "£1,500.00"
    }

    "display a remaining amount" in new Setup(documentDetailWithDueDateModel(outstandingAmount = Some(1600))) {
      document.select(".govuk-summary-list .govuk-summary-list__row:nth-of-type(3) .govuk-summary-list__value")
        .text shouldBe "£1,600.00"
    }

    "display the original amount if no outstanding amount is present" in new Setup(documentDetailWithDueDateModel(outstandingAmount = None, originalAmount = Some(1700))) {
      document.select(".govuk-summary-list .govuk-summary-list__row:nth-of-type(3) .govuk-summary-list__value")
        .text shouldBe "£1,700.00"
    }

    "not have a payment link when there is an outstanding amount of 0" in new Setup(documentDetailWithDueDateModel(outstandingAmount = Some(0))) {
      document.select("div#payment-link-2018").text() shouldBe ""
    }

    "has a link to view what you owe" in new Setup(documentDetailPOA1) {
      val link: Option[Elements] = getElementById("what-you-owe-link").map(_.select("a"))
      link.map(_.attr("href")) shouldBe Some("/report-quarterly/income-and-expenses/view/agents/payments-owed")
      link.map(_.text) shouldBe Some("what you owe")
    }

    "has a back link" in new Setup(documentDetailPOA1) {
      document.backLink.text shouldBe "Back"
    }

    "not display Payment (charge) history" when {
      "charge history is not provided" in new Setup(documentDetailPOA1, chargeHistoryOpt = None) {
        content doesNotHave Selectors.h2
        content doesNotHave Selectors.table
      }
    }

    "not display a notification banner when there are no dunning locks in payment breakdown" in new Setup(
      documentDetailWithDueDateModel(), paymentBreakdown = paymentBreakdown) {

      document.doesNotHave(Selectors.id("dunningLocksBanner"))
    }

    "display a notification banner when there are dunning locks in payment breakdown" which {

      s"has the '${Messages.dunningLockBannerHeader}' heading" in new Setup(documentDetailWithDueDateModel(), paymentBreakdown = paymentBreakdownWithLocks) {
        document.selectById("dunningLocksBanner")
          .select(Selectors.h2).text() shouldBe Messages.dunningLockBannerHeader
      }

      "has the link for Payment under review which opens in new tab" in new Setup(documentDetailWithDueDateModel(), paymentBreakdown = paymentBreakdownWithLocks) {
        val link: Elements = document.selectById("dunningLocksBanner").select(Selectors.link)

        link.text() shouldBe Messages.dunningLockBannerLink
        link.attr("href") shouldBe "https://www.gov.uk/tax-appeals"
        link.attr("target") shouldBe "_blank"
      }

      "shows the same remaining amount and a due date as in the charge summary list" which {
        "display a remaining amount" in new Setup(documentDetailWithDueDateModel(
          outstandingAmount = Some(1600)), paymentBreakdown = paymentBreakdownWithLocks) {

          document.selectById("dunningLocksBanner")
            .selectNth(Selectors.div, 2).text() shouldBe Messages.dunningLockBannerText("£1,600.00", "15 May 2019")
        }

        "display 0 if a cleared amount equal to the original amount is present but an outstanding amount is not" in new Setup(
          documentDetailWithDueDateModel(outstandingAmount = Some(0)), paymentBreakdown = paymentBreakdownWithLocks) {

          document.selectById("dunningLocksBanner")
            .selectNth(Selectors.div, 2).text() shouldBe Messages.dunningLockBannerText("£0.00", "15 May 2019")
        }

        "display the original amount if no cleared or outstanding amount is present" in new Setup(
          documentDetailWithDueDateModel(outstandingAmount = None, originalAmount = Some(1700)), paymentBreakdown = paymentBreakdownWithLocks) {

          document.selectById("dunningLocksBanner")
            .selectNth(Selectors.div, 2).text() shouldBe Messages.dunningLockBannerText("£1,700.00", "15 May 2019")
        }
      }

      "not display the Payment breakdown list when payments breakdown is empty" in new Setup(documentDetailWithDueDateModel(), paymentBreakdown = Nil) {
        document.doesNotHave(Selectors.id("heading-payment-breakdown"))
      }

      "display the Payment breakdown list" which {

        "has a correct heading" in new Setup(documentDetailWithDueDateModel(), paymentBreakdown = paymentBreakdown) {
          document.selectById("heading-payment-breakdown").text shouldBe Messages.paymentBreakdownHeading
        }

        "has payment rows with charge types and original amounts" in new Setup(documentDetailWithDueDateModel(), paymentBreakdown = paymentBreakdown) {
          verifyPaymentBreakdownRow(1, "Income Tax", "£123.45")
          verifyPaymentBreakdownRow(2, "Class 2 National Insurance", "£2,345.67")
          verifyPaymentBreakdownRow(3, "Voluntary Class 2 National Insurance", "£3,456.78")
          verifyPaymentBreakdownRow(4, "Class 4 National Insurance", "£5,678.90")
          verifyPaymentBreakdownRow(5, "Capital Gains Tax", "£9,876.54")
          verifyPaymentBreakdownRow(6, "Student Loans", "£543.21")
        }

        "has payment rows with Under review note when there are dunning locks on a payment" in new Setup(documentDetailWithDueDateModel(), paymentBreakdown = paymentBreakdownWithLocks) {
          verifyPaymentBreakdownRow(1, "Income Tax", "£123.45")
          verifyPaymentBreakdownRow(2, "Class 2 National Insurance", "£2,345.67 Under review")
          verifyPaymentBreakdownRow(3, "Capital Gains Tax", "£9,876.54 Under review")
          verifyPaymentBreakdownRow(4, "Student Loans", "£543.21")
        }

      }
    }

    "display Payment (charge) history" when {
      "charge history list is given" when {

        "the list is empty" should {
          "display the charge history heading" in new Setup(documentDetailPOA1, chargeHistoryOpt = Some(Nil), paymentAllocations = Nil,paymentAllocationEnabled = false) {
            content select Selectors.h2 text() shouldBe Messages.chargeHistoryHeading
          }

          "display the Charge creation time (Document date), Name of charge type and Amount of the charge type" when {
            "a payment on account 1 of 2" in new Setup(documentDetailPOA1, chargeHistoryOpt = Some(Nil)) {
              verifyChargesHistoryContent("29 Mar 2018 Payment on account 1 of 2 created £1,400.00")
            }
            "a payment on account 2 of 2" in new Setup(documentDetailPOA2, chargeHistoryOpt = Some(Nil)) {
              verifyChargesHistoryContent("29 Mar 2018 Payment on account 2 of 2 created £1,400.00")
            }
            "New balancing charge" in new Setup(documentDetailBalancingCharge, chargeHistoryOpt = Some(Nil)) {
              verifyChargesHistoryContent("29 Mar 2018 Remaining balance created £1,400.00")
            }
            "Amended balancing charge" in new Setup(documentDetailAmendedBalCharge, chargeHistoryOpt = Some(Nil)) {
              verifyChargesHistoryContent("29 Mar 2018 Remaining balance created £1,400.00")
            }
          }


          "display the Charge creation time (Document date), Name of charge type and Amount of the charge type when paymentAllocations FS disabled" when {
            "a payment on account 1 of 2" in new Setup(documentDetailPOA1, chargeHistoryOpt = Some(Nil), paymentAllocationEnabled = false) {
              verifyChargesHistoryContent("29 Mar 2018 Payment on account 1 of 2 created £1,400.00")
            }
            "a payment on account 2 of 2" in new Setup(documentDetailPOA2, chargeHistoryOpt = Some(Nil),paymentAllocationEnabled = false) {
              verifyChargesHistoryContent("29 Mar 2018 Payment on account 2 of 2 created £1,400.00")
            }
            "New balancing charge" in new Setup(documentDetailBalancingCharge, chargeHistoryOpt = Some(Nil), paymentAllocationEnabled = false) {
              verifyChargesHistoryContent("29 Mar 2018 Remaining balance created £1,400.00")
            }
            "Amended balancing charge" in new Setup(documentDetailAmendedBalCharge, chargeHistoryOpt = Some(Nil), paymentAllocationEnabled = false) {
              verifyChargesHistoryContent("29 Mar 2018 Remaining balance created £1,400.00")
            }
          }

          "display the Charge creation time (Document date), Name of charge type and Amount of the charge type " +
            "when paymentAllocations FS enabled but paymentAllocations are empty" when {
            "a payment on account 1 of 2" in new Setup(documentDetailPOA1, chargeHistoryOpt = Some(Nil), paymentAllocationEnabled = true,
              paymentAllocations = Nil) {
              verifyChargesHistoryContent("29 Mar 2018 Payment on account 1 of 2 created £1,400.00")
            }
            "a payment on account 2 of 2" in new Setup(documentDetailPOA2, chargeHistoryOpt = Some(Nil),paymentAllocationEnabled = true,
              paymentAllocations = Nil) {
              verifyChargesHistoryContent("29 Mar 2018 Payment on account 2 of 2 created £1,400.00")
            }
            "New balancing charge" in new Setup(documentDetailBalancingCharge, chargeHistoryOpt = Some(Nil), paymentAllocationEnabled = true,
              paymentAllocations = Nil) {
              verifyChargesHistoryContent("29 Mar 2018 Remaining balance created £1,400.00")
            }
            "Amended balancing charge" in new Setup(documentDetailAmendedBalCharge, chargeHistoryOpt = Some(Nil), paymentAllocationEnabled = true,
              paymentAllocations = Nil) {
              verifyChargesHistoryContent("29 Mar 2018 Remaining balance created £1,400.00")
            }
          }


        }

        "the list contains records" should {
          val fullChargeHistory = List(
            ChargeHistoryModel("n/a", "n/a", "n/a", "n/a", 12345, LocalDate.of(2018, 7, 6), "amended return"),
            ChargeHistoryModel("n/a", "n/a", "n/a", "n/a", 54321, LocalDate.of(2019, 8, 12), "Customer Request")
          )

          "display the charge history heading" in new Setup(documentDetailPOA1, chargeHistoryOpt = Some(fullChargeHistory)) {
            content select Selectors.h2 text() shouldBe Messages.chargeHistoryHeading
          }

          "display the list of amendments with Charge creation as first line" when {
            "a payment on account 1 of 2" in new Setup(documentDetailPOA1, chargeHistoryOpt = Some(fullChargeHistory)) {
              verifyChargesHistoryContent(
                "29 Mar 2018 Payment on account 1 of 2 created £1,400.00",
                "6 Jul 2018 Payment on account 1 of 2 reduced due to amended return £12,345.00",
                "12 Aug 2019 Payment on account 1 of 2 reduced by taxpayer request £54,321.00")
            }
            "a payment on account 2 of 2" in new Setup(documentDetailPOA2, chargeHistoryOpt = Some(fullChargeHistory)) {
              verifyChargesHistoryContent(
                "29 Mar 2018 Payment on account 2 of 2 created £1,400.00",
                "6 Jul 2018 Payment on account 2 of 2 reduced due to amended return £12,345.00",
                "12 Aug 2019 Payment on account 2 of 2 reduced by taxpayer request £54,321.00")
            }
            "balancing charge" in new Setup(documentDetailBalancingCharge, chargeHistoryOpt = Some(fullChargeHistory)) {
              verifyChargesHistoryContent(
                "29 Mar 2018 Remaining balance created £1,400.00",
                "6 Jul 2018 Remaining balance reduced due to amended return £12,345.00",
                "12 Aug 2019 Remaining balance reduced by taxpayer request £54,321.00")
            }
            "Amended balancing charge" in new Setup(documentDetailAmendedBalCharge, chargeHistoryOpt = Some(fullChargeHistory)) {
              verifyChargesHistoryContent(
                "29 Mar 2018 Remaining balance created £1,400.00",
                "6 Jul 2018 Remaining balance reduced due to amended return £12,345.00",
                "12 Aug 2019 Remaining balance reduced by taxpayer request £54,321.00")
            }
          }
        }

        "the list contains ChargeHistory and Payment breakdown" should {
          val fullChargeHistory = List(
            ChargeHistoryModel("n/a", "n/a", "n/a", "n/a", 12345, LocalDate.of(2018, 7, 6), "amended return"),
            ChargeHistoryModel("n/a", "n/a", "n/a", "n/a", 54321, LocalDate.of(2019, 8, 12), "Customer Request")
          )

          "display the payment breakdown in h2 and charge history heading in h3" in new Setup(documentDetailPOA1, chargeHistoryOpt = Some(fullChargeHistory), paymentBreakdown = paymentBreakdown) {
            content select Selectors.h2 text() shouldBe Messages.paymentBreakdownHeading
            content select Selectors.h3 text() shouldBe Messages.chargeHistoryHeading
          }
        }
      }
    }

    "show payment allocations in history table" when {
      "allocations are enabled and present in the list" when {
        val fullChargeHistory = List(
          ChargeHistoryModel("n/a", "n/a", "n/a", "n/a", 12345, LocalDate.of(2018, 7, 6), "amended return"),
          ChargeHistoryModel("n/a", "n/a", "n/a", "n/a", 54321, LocalDate.of(2019, 8, 12), "Customer Request")
        )

        val typePOA1 = "SA Payment on Account 1"
        val typePOA2 = "SA Payment on Account 2"
        val typeBalCharge = "SA Balancing Charge"

        def paymentsForCharge(mainType: String, chargeType: String, date: String, amount: BigDecimal): PaymentsWithChargeType =
          PaymentsWithChargeType(
            payments = List(Payment(reference = Some("reference"), amount = Some(amount), method = Some("method"),
              lot = Some("lot"), lotItem = Some("lotItem"), date = Some(date), transactionId = None)),
            mainType = Some(mainType), chargeType = Some(chargeType))

				def payments() = FinancialDetailsModel(List(DocumentDetail("9999", "PAYID01", Some("Payment on Account"), Some(10000.0), Some(1000.0), LocalDate.now(), paymentLot = Some("lot"), paymentLotItem = Some("lotItem"))), List())

        val paymentAllocationsPOA1 = List(
          paymentsForCharge(typePOA1, "ITSA NI", "2018-03-30", 1500.0),
          paymentsForCharge(typePOA1, "NIC4 Scotland", "2018-03-31", 1600.0)
        )

        val paymentAllocationsPOA2 = List(
          paymentsForCharge(typePOA2, "ITSA Wales", "2018-04-01", 2400.0),
          paymentsForCharge(typePOA2, "NIC4-GB", "2018-04-15", 2500.0),
        )

        val paymentAllocationsBalCharge = List(
          paymentsForCharge(typeBalCharge, "ITSA England & NI", "2019-12-10", 3400.0),
          paymentsForCharge(typeBalCharge, "NIC4-NI", "2019-12-11", 3500.0),
          paymentsForCharge(typeBalCharge, "NIC2 Wales", "2019-12-12", 3600.0),
          paymentsForCharge(typeBalCharge, "CGT", "2019-12-13", 3700.0),
          paymentsForCharge(typeBalCharge, "SL", "2019-12-14", 3800.0),
          paymentsForCharge(typeBalCharge, "Voluntary NIC2-GB", "2019-12-15", 3900.0)
        )

        "chargeHistory enabled, having Payment created in the first row and payment allocations for POA1" in new Setup(documentDetailPOA1, chargeHistoryOpt = Some(fullChargeHistory) ,paymentAllocationEnabled = true, paymentAllocations = paymentAllocationsPOA1) {
        verifyChargesHistoryContent("29 Mar 2018 Payment on account 1 of 2 created £1,400.00",
          "6 Jul 2018 Payment on account 1 of 2 reduced due to amended return £12,345.00",
          "12 Aug 2019 Payment on account 1 of 2 reduced by taxpayer request £54,321.00",
          "30 Mar 2018 Payment allocated to Income Tax for payment on account 1 of 2 £1,500.00",
          "31 Mar 2018 Payment allocated to Class 4 National Insurance for payment on account 1 of 2 £1,600.00"
        )
      }


        "chargeHistory enabled, having Payment created in the first row and payment allocations for POA2" in new Setup(documentDetailPOA2, chargeHistoryOpt = Some(fullChargeHistory) ,paymentAllocationEnabled = true, paymentAllocations = paymentAllocationsPOA2) {
          verifyChargesHistoryContent(
            "29 Mar 2018 Payment on account 2 of 2 created £1,400.00",
            "6 Jul 2018 Payment on account 2 of 2 reduced due to amended return £12,345.00",
            "12 Aug 2019 Payment on account 2 of 2 reduced by taxpayer request £54,321.00",
            "1 Apr 2018 Payment allocated to Income Tax for payment on account 2 of 2 £2,400.00",
            "15 Apr 2018 Payment allocated to Class 4 National Insurance for payment on account 2 of 2 £2,500.00"
          )
        }

        "chargeHistory enabled, having Payment created in the first row and payment allocations for remaining balance" in new Setup(documentDetailAmendedBalCharge, chargeHistoryOpt = Some(fullChargeHistory) ,paymentAllocationEnabled = true, paymentAllocations = paymentAllocationsBalCharge) {
          verifyChargesHistoryContent(
            "29 Mar 2018 Remaining balance created £1,400.00",
            "6 Jul 2018 Remaining balance reduced due to amended return £12,345.00",
            "12 Aug 2019 Remaining balance reduced by taxpayer request £54,321.00",
            "10 Dec 2019 Payment allocated to Income Tax for remaining balance £3,400.00",
            "11 Dec 2019 Payment allocated to Class 4 National Insurance for remaining balance £3,500.00",
            "12 Dec 2019 Payment allocated to Class 2 National Insurance for remaining balance £3,600.00",
            "13 Dec 2019 Payment allocated to Capital Gains Tax for remaining balance £3,700.00",
            "14 Dec 2019 Payment allocated to Student Loans for remaining balance £3,800.00",
            "15 Dec 2019 Payment allocated to Voluntary Class 2 National Insurance for remaining balance £3,900.00"
          )
        }

				"chargeHistory enabled with a matching link to the payment allocations page" in new Setup(documentDetailWithDueDateModel(), chargeHistoryOpt = Some(fullChargeHistory) ,paymentAllocationEnabled = true, paymentAllocations = paymentAllocationsBalCharge, payments = payments()) {
					document.select(Selectors.table).select("a").size shouldBe 6
					document.select(Selectors.table).select("a").forall(_.attr("href") == controllers.agent.routes.PaymentAllocationsController.viewPaymentAllocation("PAYID01").url) shouldBe true
				}
      }

    }

    "display Late payment interest on accounts" when {

      "have the correct heading for a POA 1 late interest charge" in new Setup(documentDetailPOA1, latePaymentInterestCharge = true) {
        document.select("h1").text() shouldBe Messages.poaInterestHeading(2018, 1)
      }
      "have the correct heading for a POA 2 late interest charge" in new Setup(documentDetailPOA2, latePaymentInterestCharge = true) {
        document.select("h1").text() shouldBe Messages.poaInterestHeading(2018, 2)
      }
      "have the correct heading for a balancing charge late interest charge" in new Setup(documentDetailBalancingCharge, latePaymentInterestCharge = true) {
        document.select("h1").text() shouldBe Messages.balancingChargeInterestHeading(2018)
      }

      "display an interest period for a late interest charge" in new Setup(documentDetailPOA1, latePaymentInterestCharge = true) {
        document.select(".govuk-summary-list .govuk-summary-list__row:nth-of-type(2) .govuk-summary-list__value")
          .text() shouldBe "29 Mar 2018 to 15 Jun 2018"
      }

      "display a charge amount for a late interest charge" in new Setup(documentDetailPOA1, latePaymentInterestCharge = true) {
        document.select(".govuk-summary-list .govuk-summary-list__row:nth-of-type(3) .govuk-summary-list__value")
          .text() shouldBe "£100.00"
      }
      "display a remaining amount for a late interest charge" in new Setup(documentDetailPOA1, latePaymentInterestCharge = true) {
        document.select(".govuk-summary-list .govuk-summary-list__row:nth-of-type(4) .govuk-summary-list__value")
          .text() shouldBe "£80.00"
      }
    }
  }
}
