/*
 * Copyright 2026 HM Revenue & Customs
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

package financials.views

import common.testUtils.{TestSupport, ViewSpec}
import financials.models.MakingPaymentViewModel
import financials.views.html.MakingPaymentView
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.FakeRequest

class MakingPaymentViewSpec extends TestSupport with ViewSpec {

  val makingPaymentView: MakingPaymentView = app.injector.instanceOf[MakingPaymentView]

  val penaltyP3MessageText: String = s"${messages("making-payment.penalty.p3.before")} ${messages("making-payment.penalty.p3.link")} ${messages("pagehelp.opensInNewTabText")}. ${messages("making-payment.penalty.p3.after")}"

  def viewModel(hasInterest: Boolean = false,
                hasPenalty: Boolean = false,
                unallocatedCredit: Option[BigDecimal] = None,
                hasOverdue: Boolean = false,
                hasAllPenaltiesOverdue: Boolean = false,
                hasOverdueNonPenaltyCharges: Boolean = false,
                hasNotOverdueLPP: Boolean = false): MakingPaymentViewModel =
    MakingPaymentViewModel(
      backUrl = "/what-you-owe",
      paymentHandoffUrl = "/payment?amountInPence=10000",
      whatYouOweUrl = "/what-you-owe",
      moneyInYourAccountUrl = "/money-in-your-account",
      payPenaltyUrl = "/pay-penalty",
      hasInterest = hasInterest,
      hasPenalty = hasPenalty,
      unallocatedCredit = unallocatedCredit,
      hasAllPenaltiesOverdue = hasAllPenaltiesOverdue,
      hasOverdueNonPenaltyCharges = hasOverdueNonPenaltyCharges,
      hasNotOverdueLPP = hasNotOverdueLPP
    )

  def render(model: MakingPaymentViewModel = viewModel()): Document =
    Jsoup.parse(makingPaymentView(model)(FakeRequest(), individualUser, messages).body)

  "MakingPaymentView" should {

    "render the static content without the first section heading when there are no extra sections" in {
      val document = render()

      document.select("h1").text shouldBe messages("making-payment.heading")
      document.select("#main-content h2").isEmpty shouldBe true
      document.getElementById("payment-goes-towards").text shouldBe messages("making-payment.what-payment-goes-towards.p1")
      document.select("#main-content li").get(0).text shouldBe messages("making-payment.what-payment-goes-towards.bullet1")
      document.select("#main-content li").get(1).text shouldBe messages("making-payment.what-payment-goes-towards.bullet2")
      document.getElementById("continue-to-payment-button").attr("href") shouldBe "/payment?amountInPence=10000"
    }

    "render the accruing interest section and add a heading to the static content" in {
      val document = render(viewModel(hasInterest = true))
      val headings = document.select("#main-content h2")

      headings.get(0).text shouldBe messages("making-payment.what-payment-goes-towards.heading")
      headings.get(1).text shouldBe messages("making-payment.accruing-interest.heading")
      document.getElementById("what-you-owe-link").attr("href") shouldBe "/what-you-owe"
    }

    "render penalty and money in account sections when present" in {
      val document = render(viewModel(hasPenalty = true, unallocatedCredit = Some(BigDecimal(400))))
      val headings = document.select("#main-content h2")

      headings.get(0).text shouldBe messages("making-payment.what-payment-goes-towards.heading")
      headings.get(1).text shouldBe messages("making-payment.penalty.heading")
      headings.get(2).text shouldBe messages("making-payment.money-in-account.heading")
      document.getElementById("pay-penalty-link").attr("href") shouldBe "/pay-penalty"
      document.getElementById("pay-penalty-link").attr("target") shouldBe "_blank"
      document.getElementById("money-in-account-link").attr("href") shouldBe "/money-in-your-account"
      document.getElementById("money-in-account-p1").text should include("£400.00")
      document.getElementById("money-in-account-p1").text should include("account; your current balance")
    }

    "render penalty section with H1/P2/P3 when penalties not overdue" in {
      val document = render(viewModel(hasPenalty = true))
      val headings = document.select("#main-content h2")

      headings.get(0).text shouldBe messages("making-payment.what-payment-goes-towards.heading")
      headings.get(1).text shouldBe messages("making-payment.penalty.heading")
      document.select("#penalty-p1").isEmpty shouldBe true
      document.selectById("penalty-p2").text() shouldBe messages("making-payment.penalty.p2")
      document.selectById("penalty-p3").text() shouldBe penaltyP3MessageText
      document.getElementById("pay-penalty-link").attr("href") shouldBe "/pay-penalty"
      document.getElementById("pay-penalty-link").attr("target") shouldBe "_blank"
    }

    "render penalty section with H1/P1/P2/P3 when LPP not overdue with overdue non-penalty charges" in {
      val document = render(viewModel(hasPenalty = true, hasNotOverdueLPP = true, hasOverdueNonPenaltyCharges = true))
      val headings = document.select("#main-content h2")

      headings.get(0).text shouldBe messages("making-payment.what-payment-goes-towards.heading")
      headings.get(1).text shouldBe messages("making-payment.penalty.heading")
      document.selectById("penalty-p1").text() shouldBe messages("making-payment.penalty.p1")
      document.selectById("penalty-p2").text() shouldBe messages("making-payment.penalty.p2")
      document.selectById("penalty-p3").text() shouldBe penaltyP3MessageText
      document.getElementById("pay-penalty-link").attr("href") shouldBe "/pay-penalty"
      document.getElementById("pay-penalty-link").attr("target") shouldBe "_blank"
    }

    "render penalty section with H1/P2/P3 when Penalties not overdue with no overdue non penalty charges" in {
      val document = render(viewModel(hasPenalty = true, hasNotOverdueLPP = true))
      val headings = document.select("#main-content h2")

      headings.get(0).text shouldBe messages("making-payment.what-payment-goes-towards.heading")
      headings.get(1).text shouldBe messages("making-payment.penalty.heading")
      document.select("#penalty-p1").isEmpty shouldBe true
      document.selectById("penalty-p2").text() shouldBe messages("making-payment.penalty.p2")
      document.selectById("penalty-p3").text() shouldBe penaltyP3MessageText
      document.getElementById("pay-penalty-link").attr("href") shouldBe "/pay-penalty"
      document.getElementById("pay-penalty-link").attr("target") shouldBe "_blank"
    }

    "render no penalties section when all the penalties are overdue" in {
      val document = render(viewModel(hasAllPenaltiesOverdue = true))
      document.select("h1").text shouldBe messages("making-payment.heading")
      document.select("#main-content h2").isEmpty shouldBe true
      document.getElementById("payment-goes-towards").text shouldBe messages("making-payment.what-payment-goes-towards.p1")
      document.select("#main-content li").get(0).text shouldBe messages("making-payment.what-payment-goes-towards.bullet1")
      document.select("#main-content li").get(1).text shouldBe messages("making-payment.what-payment-goes-towards.bullet2")
      document.getElementById("continue-to-payment-button").attr("href") shouldBe "/payment?amountInPence=10000"
    }
  }
}
