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

package hub.models.newHomePage

import testUtils.TestSupport

class ProofOfYourIncomeCardViewModelSpec extends TestSupport{

  val baseModel = ProofOfYourIncomeCardViewModel(2022, "DF", 2026, false, None)
  "getCardHeader" should {
    "return correct value if isLegacy = false and itsCurrentTaxYear = false" in{
      baseModel.getCardHeader() shouldBe messages("newHome.overview.tax-year.proofOfIncome.card.previousTY.header",
        baseModel.taxYearStart.toString, (baseModel.taxYearStart + 1).toString)
    }
    "return correct value if isLegacy = true and itsCurrentTaxYear = false" in {
      val model = baseModel.copy(isLegacy = true, saUtr = Some("1234567890"))
      model.getCardHeader() shouldBe messages("newHome.overview.tax-year.proofOfIncome.card.legacy.header",
        model.taxYearStart.toString, (model.taxYearStart + 1).toString)
    }

    "return correct value if isLegacy = false and itsCurrentTaxYear = true" in {
      val model = baseModel.copy(currentYear = 2022)
      model.getCardHeader() shouldBe messages("newHome.overview.tax-year.proofOfIncome.card.currentTY.header",
        model.taxYearStart.toString, (model.taxYearStart + 1).toString)
    }

    "return correct value if isLegacy = true and itsCurrentTaxYear = true" in {
      val model = baseModel.copy(isLegacy = true, saUtr = Some("1234567890"), currentYear = 2022)
      model.getCardHeader() shouldBe messages("newHome.overview.tax-year.proofOfIncome.card.legacy.header",
        model.taxYearStart.toString, (model.taxYearStart + 1).toString)
    }
  }

  "getCardRows" should {
    "return correct value if isLegacy = false and calculationType supports proof of income" in {
      baseModel.getCardRows() shouldBe Seq(
        CardRows(messages("newHome.overview.tax-year.proofOfIncome.card.sa302key"),
          messages("newHome.overview.tax-year.proofOfIncome.card.sa302value.proof"),
          Some(messages("newHome.overview.tax-year.proofOfIncome.card.sa302value.action"))),
        CardRows(messages("newHome.overview.tax-year.proofOfIncome.card.taxYearOverviewKey"),
          messages("newHome.overview.tax-year.proofOfIncome.card.taxYearOverviewValue.proof"),
          Some(messages("newHome.overview.tax-year.proofOfIncome.card.sa302value.action")))
      )
    }
    "return correct value if isLegacy = false and calculationType supports incomplete journey" in {
      val model = baseModel.copy(calculationType = "IC")
      model.getCardRows() shouldBe Seq(
        CardRows(messages("newHome.overview.tax-year.proofOfIncome.card.sa302key"),
          messages("newHome.overview.tax-year.proofOfIncome.card.sa302value.incomplete"),
          Some(messages("newHome.overview.tax-year.proofOfIncome.card.sa302value.action"))),
        CardRows(messages("newHome.overview.tax-year.proofOfIncome.card.taxYearOverviewKey"),
          messages("newHome.overview.tax-year.proofOfIncome.card.taxYearOverviewValue.notAvailable"),
          None)
      )
    }

    "return correct value if isLegacy = true" in {
      val model = baseModel.copy(isLegacy = true)
      model.getCardRows() shouldBe Seq(CardRows(messages("newHome.overview.tax-year.proofOfIncome.card.legacy"),
        messages("newHome.overview.tax-year.proofOfIncome.card.sa302value.legacy"),
        Some(messages("newHome.overview.tax-year.proofOfIncome.card.sa302value.action"))))
    }
  }
}
