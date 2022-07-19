/*
 * Copyright 2022 HM Revenue & Customs
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

import config.FrontendAppConfig
import implicits.ImplicitDateFormatter
import models.repaymentHistory.{RepaymentHistory, RepaymentHistoryModel, RepaymentItem, RepaymentSupplementItem}
import org.jsoup.select.Elements
import play.api.test.FakeRequest
import testUtils.ViewSpec
import views.html.RefundToTaxPayer

import java.time.LocalDate

class RefundToTaxPayerViewSpec extends ViewSpec with ImplicitDateFormatter {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  lazy val refundToTaxPayerView: RefundToTaxPayer = app.injector.instanceOf[RefundToTaxPayer]

  object RefundToTaxPayerMessages {
    val heading: String = messages("refund-to-taxpayer.heading")
    val title: String = messages("titlePattern.serviceName.govUk", heading)
    val agentTitle: String = messages("agent.titlePattern.serviceName.govUk", heading)
    val titleWhenAgentView: String = messages("agent.titlePattern.serviceName.govUk", heading)

    val tableHeadEstimatedDate: String = messages("refund-to-taxpayer.tableHead.estimated-date")
    val tableHeadMethod: String = messages("refund-to-taxpayer.tableHead.method")
    val tableHeadTotalRefund: String = messages("refund-to-taxpayer.tableHead.total-refund")
    val tableHeadFurtherDetails: String = messages("refund-to-taxpayer.tableHead.further-details")
    val tableHeadRequestedOn: String = messages("refund-to-taxpayer.tableHead.requested-on")
    val tableHeadRefundReference: String = messages("refund-to-taxpayer.tableHead.refund-reference")
    val tableHeadRequestedAmount: String = messages("refund-to-taxpayer.tableHead.requested-amount")
    val tableHeadRefundAmount: String = messages("refund-to-taxpayer.tableHead.refund-amount")
    val tableHeadInterest: String = messages("refund-to-taxpayer.tableHead.interest")
    val tableHeadTotalInterest: String = messages("refund-to-taxpayer.tableHead.total-interest")
    val variedInterest: String = s"${messages("refund-to-taxpayer.tableHead.total-interest")} ${messages("refund-to-taxpayer.tableHead.varied-interest-rates-value", "1.76", "2.01", "31 July 2021", "30 November 2021")}"
    val tableValueMethodTypeBacs: String = messages("refund-to-taxpayer.method-type-bacs")
    val tableValueMethodTypeCard: String = messages("refund-to-taxpayer.method-type-card")
    val tableValueMethodTypePostalOrder: String = messages("refund-to-taxpayer.method-type-postal-order")

  }

  val testRepaymentHistoryModel: RepaymentHistoryModel = RepaymentHistoryModel(
    List(RepaymentHistory(
      Some(705.2),
      705.2,
      RefundToTaxPayerMessages.tableValueMethodTypeBacs,
      12345,
      Vector(
        RepaymentItem(
          Vector(
            RepaymentSupplementItem(
              Some("002420002231"),
              Some(3.78),
              Some(LocalDate.of(2021, 7, 31)),
              Some(LocalDate.of(2021, 9, 15)),
              Some(2.01)
            ),
            RepaymentSupplementItem(
              Some("002420002231"),
              Some(2.63),
              Some(LocalDate.of(2021, 9, 15)),
              Some(LocalDate.of(2021, 10, 24)),
              Some(1.76)
            ),
            RepaymentSupplementItem(
              Some("002420002231"),
              Some(3.26),
              Some(LocalDate.of(2021, 10, 24)),
              Some(LocalDate.of(2021, 11, 30)),
              Some(2.01))
          )
        )
      ), LocalDate.of(2021, 7, 23), LocalDate.of(2021, 7, 21), "000000003135")
    )
  )

  val testRepaymentHistoryModelRequestedAmountDiffersToRefundAmount: RepaymentHistoryModel =
    testRepaymentHistoryModel.copy(
      List(
        testRepaymentHistoryModel.repaymentsViewerDetails.map(_.copy(
          amountApprovedforRepayment = Some(800.12),
          amountRequested = 345.5,
          repaymentMethod = RefundToTaxPayerMessages.tableValueMethodTypeCard
        )
        )
      ).flatten
    )

  val testRepaymentHistoryModelRequestedMissingRefundAmount: RepaymentHistoryModel =
    testRepaymentHistoryModel.copy(
      List(
        testRepaymentHistoryModel.repaymentsViewerDetails.headOption.map(_.copy(
          amountApprovedforRepayment = None,
          amountRequested = 345.5,
          repaymentMethod = RefundToTaxPayerMessages.tableValueMethodTypePostalOrder
        )
        )
      ).flatten
    )

  class RefundToTaxPayerViewSetup(testRepaymentHistoryModel: RepaymentHistoryModel, saUtr: Option[String] = Some("AY888881A"), isAgent: Boolean = false) extends Setup(
    refundToTaxPayerView(testRepaymentHistoryModel, paymentHistoryRefundsEnabled = false, "testBackURL", saUtr, isAgent = isAgent)(FakeRequest(), implicitly)
  )

  "The refund to tax payer view with repayment history response model" should {
    "when the user has repayment history" should {
      s"have the title '${RefundToTaxPayerMessages.title}'" in new RefundToTaxPayerViewSetup(testRepaymentHistoryModel) {
        document.title() shouldBe RefundToTaxPayerMessages.title
      }

      s"have the h1 heading '${RefundToTaxPayerMessages.heading}'" in new RefundToTaxPayerViewSetup(testRepaymentHistoryModel) {
        layoutContent.selectHead("h1").text shouldBe RefundToTaxPayerMessages.heading
      }

      s"has a summary list of refund to tax payer" which {
        s"has summary list headings without amount requested" in new RefundToTaxPayerViewSetup(testRepaymentHistoryModel) {
          val allTableData: Elements = document.getElementById("refund-to-taxpayer-table").getElementsByTag("dt")
          allTableData.get(0).text() shouldBe RefundToTaxPayerMessages.tableHeadEstimatedDate
          allTableData.get(1).text() shouldBe RefundToTaxPayerMessages.tableHeadMethod
          allTableData.get(2).text() shouldBe RefundToTaxPayerMessages.tableHeadTotalRefund

          layoutContent.select(".govuk-details__summary").select("span").first().text shouldBe RefundToTaxPayerMessages.tableHeadFurtherDetails

          val underDetailsTable: Elements = document.getElementById("refund-to-taxpayer-table-under-details").getElementsByTag("dt")
          underDetailsTable.get(0).text() shouldBe RefundToTaxPayerMessages.tableHeadRequestedOn
          underDetailsTable.get(1).text() shouldBe RefundToTaxPayerMessages.tableHeadRefundReference
          underDetailsTable.get(2).text() shouldBe RefundToTaxPayerMessages.tableHeadRefundAmount
          underDetailsTable.get(3).text() shouldBe RefundToTaxPayerMessages.variedInterest
        }
      }

      s"has a summary list of refund to tax payer" which {
        s"has summary list values without amount requested" in new RefundToTaxPayerViewSetup(testRepaymentHistoryModel) {
          val allTableData: Elements = document.getElementById("refund-to-taxpayer-table").getElementsByTag("dd")
          allTableData.get(0).text() shouldBe "23 July 2021"
          allTableData.get(1).text() shouldBe RefundToTaxPayerMessages.tableValueMethodTypeBacs
          allTableData.get(2).text() shouldBe "£12,345.00"

          layoutContent.select(".govuk-details__summary").select("span").first().text shouldBe RefundToTaxPayerMessages.tableHeadFurtherDetails

          val underDetailsTable: Elements = document.getElementById("refund-to-taxpayer-table-under-details").getElementsByTag("dd")
          underDetailsTable.get(0).text() shouldBe "21 July 2021"
          underDetailsTable.get(1).text() shouldBe "000000003135"
          underDetailsTable.get(2).text() shouldBe "£705.20"
          underDetailsTable.get(3).text() shouldBe "£9.67"
        }
      }

      s"has a summary list of refund to tax payer" which {
        s"has summary list headings with requested amount present due to difference in refund amount and requested amount fileds" in new RefundToTaxPayerViewSetup(testRepaymentHistoryModelRequestedAmountDiffersToRefundAmount) {
          val allTableData: Elements = document.getElementById("refund-to-taxpayer-table").getElementsByTag("dt")
          allTableData.get(0).text() shouldBe RefundToTaxPayerMessages.tableHeadEstimatedDate
          allTableData.get(1).text() shouldBe RefundToTaxPayerMessages.tableHeadMethod
          allTableData.get(2).text() shouldBe RefundToTaxPayerMessages.tableHeadTotalRefund

          layoutContent.select(".govuk-details__summary").select("span").first().text shouldBe RefundToTaxPayerMessages.tableHeadFurtherDetails

          val underDetailsTable: Elements = document.getElementById("refund-to-taxpayer-table-under-details").getElementsByTag("dt")
          underDetailsTable.get(0).text() shouldBe RefundToTaxPayerMessages.tableHeadRequestedOn
          underDetailsTable.get(1).text() shouldBe RefundToTaxPayerMessages.tableHeadRefundReference
          underDetailsTable.get(2).text() shouldBe RefundToTaxPayerMessages.tableHeadRequestedAmount
          underDetailsTable.get(3).text() shouldBe RefundToTaxPayerMessages.tableHeadRefundAmount
          underDetailsTable.get(4).text() shouldBe RefundToTaxPayerMessages.variedInterest
        }
      }

      s"has a summary list of refund to tax payer" which {
        s"has summary list values with requested amount present due to difference in refund amount and requested amount fileds" in new RefundToTaxPayerViewSetup(testRepaymentHistoryModelRequestedAmountDiffersToRefundAmount) {
          val allTableData: Elements = document.getElementById("refund-to-taxpayer-table").getElementsByTag("dd")
          allTableData.get(0).text() shouldBe "23 July 2021"
          allTableData.get(1).text() shouldBe RefundToTaxPayerMessages.tableValueMethodTypeCard
          allTableData.get(2).text() shouldBe "£12,345.00"

          layoutContent.select(".govuk-details__summary").select("span").first().text shouldBe RefundToTaxPayerMessages.tableHeadFurtherDetails

          val underDetailsTable: Elements = document.getElementById("refund-to-taxpayer-table-under-details").getElementsByTag("dd")
          underDetailsTable.get(0).text() shouldBe "21 July 2021"
          underDetailsTable.get(1).text() shouldBe "000000003135"
          underDetailsTable.get(2).text() shouldBe "£345.50"
          underDetailsTable.get(3).text() shouldBe "£800.12"
          underDetailsTable.get(4).text() shouldBe "£9.67"
        }
      }

      s"has a summary list of refund to tax payer" which {
        s"has summary list headings without refund amount field when interest has not been paid" in new RefundToTaxPayerViewSetup(testRepaymentHistoryModelRequestedMissingRefundAmount) {
          val allTableData: Elements = document.getElementById("refund-to-taxpayer-table").getElementsByTag("dt")
          allTableData.get(0).text() shouldBe RefundToTaxPayerMessages.tableHeadEstimatedDate
          allTableData.get(1).text() shouldBe RefundToTaxPayerMessages.tableHeadMethod
          allTableData.get(2).text() shouldBe RefundToTaxPayerMessages.tableHeadTotalRefund

          layoutContent.select(".govuk-details__summary").select("span").first().text shouldBe RefundToTaxPayerMessages.tableHeadFurtherDetails

          val underDetailsTable: Elements = document.getElementById("refund-to-taxpayer-table-under-details").getElementsByTag("dt")
          underDetailsTable.get(0).text() shouldBe RefundToTaxPayerMessages.tableHeadRequestedOn
          underDetailsTable.get(1).text() shouldBe RefundToTaxPayerMessages.tableHeadRefundReference
          underDetailsTable.get(2).text() shouldBe RefundToTaxPayerMessages.tableHeadRequestedAmount
          underDetailsTable.get(3).text() shouldBe RefundToTaxPayerMessages.variedInterest
        }
      }

      s"has a summary list of refund to tax payer" which {
        s"has summary list values without refund amount field when interest has not been paid" in new RefundToTaxPayerViewSetup(testRepaymentHistoryModelRequestedMissingRefundAmount) {
          val allTableData: Elements = document.getElementById("refund-to-taxpayer-table").getElementsByTag("dd")
          allTableData.get(0).text() shouldBe "23 July 2021"
          allTableData.get(1).text() shouldBe RefundToTaxPayerMessages.tableValueMethodTypePostalOrder
          allTableData.get(2).text() shouldBe "£12,345.00"

          layoutContent.select(".govuk-details__summary").select("span").first().text shouldBe RefundToTaxPayerMessages.tableHeadFurtherDetails

          val underDetailsTable: Elements = document.getElementById("refund-to-taxpayer-table-under-details").getElementsByTag("dd")
          underDetailsTable.get(0).text() shouldBe "21 July 2021"
          underDetailsTable.get(1).text() shouldBe "000000003135"
          underDetailsTable.get(2).text() shouldBe "£345.50"
          underDetailsTable.get(3).text() shouldBe "£9.67"
        }
      }
    }
  }

  "The refund to tax payer view with repayment history response model when logged as an Agent" should {
    "when the user has repayment history" should {
      s"have the title '${RefundToTaxPayerMessages.agentTitle}'" in new RefundToTaxPayerViewSetup(testRepaymentHistoryModel, isAgent = true) {
        document.title() shouldBe RefundToTaxPayerMessages.agentTitle
      }

      s"have the h1 heading '${RefundToTaxPayerMessages.heading}'" in new RefundToTaxPayerViewSetup(testRepaymentHistoryModel, isAgent = true) {
        layoutContent.selectHead("h1").text shouldBe RefundToTaxPayerMessages.heading
      }

      s"has a summary list of refund to tax payer" which {
        s"has summary list headings without amount requested" in new RefundToTaxPayerViewSetup(testRepaymentHistoryModel, isAgent = true) {
          val allTableData: Elements = document.getElementById("refund-to-taxpayer-table").getElementsByTag("dt")
          allTableData.get(0).text() shouldBe RefundToTaxPayerMessages.tableHeadEstimatedDate
          allTableData.get(1).text() shouldBe RefundToTaxPayerMessages.tableHeadMethod
          allTableData.get(2).text() shouldBe RefundToTaxPayerMessages.tableHeadTotalRefund

          layoutContent.select(".govuk-details__summary").select("span").first().text shouldBe RefundToTaxPayerMessages.tableHeadFurtherDetails

          val underDetailsTable: Elements = document.getElementById("refund-to-taxpayer-table-under-details").getElementsByTag("dt")
          underDetailsTable.get(0).text() shouldBe RefundToTaxPayerMessages.tableHeadRequestedOn
          underDetailsTable.get(1).text() shouldBe RefundToTaxPayerMessages.tableHeadRefundReference
          underDetailsTable.get(2).text() shouldBe RefundToTaxPayerMessages.tableHeadRefundAmount
          underDetailsTable.get(3).text() shouldBe RefundToTaxPayerMessages.variedInterest
        }
      }

      s"has a summary list of refund to tax payer" which {
        s"has summary list values without amount requested" in new RefundToTaxPayerViewSetup(testRepaymentHistoryModel, isAgent = true) {
          val allTableData: Elements = document.getElementById("refund-to-taxpayer-table").getElementsByTag("dd")
          allTableData.get(0).text() shouldBe "23 July 2021"
          allTableData.get(1).text() shouldBe RefundToTaxPayerMessages.tableValueMethodTypeBacs
          allTableData.get(2).text() shouldBe "£12,345.00"

          layoutContent.select(".govuk-details__summary").select("span").first().text shouldBe RefundToTaxPayerMessages.tableHeadFurtherDetails

          val underDetailsTable: Elements = document.getElementById("refund-to-taxpayer-table-under-details").getElementsByTag("dd")
          underDetailsTable.get(0).text() shouldBe "21 July 2021"
          underDetailsTable.get(1).text() shouldBe "000000003135"
          underDetailsTable.get(2).text() shouldBe "£705.20"
          underDetailsTable.get(3).text() shouldBe "£9.67"
        }
      }
    }
  }
}
