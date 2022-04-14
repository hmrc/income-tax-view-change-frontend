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

package testUtils

import utils.TaxCalcFallBackBackLink

class TaxCalcFallBackBackLinkSpec extends TestSupport with TaxCalcFallBackBackLink {

  private trait Test

  "The getFallbackUrl function" when {
    "the user has come from a submission page" when {
      "user is NOT an agent and calc is crystallised" should {
        "provide the correct url" in new Test {
          val expectedURL = "/report-quarterly/income-and-expenses/view/2022/final-tax-overview?origin=PTA"
          getFallbackUrl(calcPageBackLink = Some("submission"), isAgent = false,
            isCrystallised = true, taxYear = 2022, origin = Some("PTA")) shouldBe expectedURL
        }
      }
      "user IS an agent and calc is crystallised" should {
        "provide the correct url" in new Test {
          val expectedURL = "/report-quarterly/income-and-expenses/view/agents/2022/final-tax-overview"
          getFallbackUrl(calcPageBackLink = Some("submission"), isAgent = true,
            isCrystallised = true, taxYear = 2022, origin = None) shouldBe expectedURL
        }
      }
      "user is NOT an agent and calc is NOT crystallised" should {
        "provide the correct url" in new Test {
          val expectedURL = "/report-quarterly/income-and-expenses/view/tax-overview?origin=PTA"
          getFallbackUrl(calcPageBackLink = Some("submission"), isAgent = false,
            isCrystallised = false, taxYear = 2022, origin = Some("PTA")) shouldBe expectedURL
        }
      }
      "user IS an agent and calc is NOT crystallised" should {
        "provide the correct url" in new Test {
          val expectedURL = "/report-quarterly/income-and-expenses/view/agents/tax-overview"
          getFallbackUrl(calcPageBackLink = Some("submission"), isAgent = true, isCrystallised = false,
            taxYear = 2022, origin = None) shouldBe expectedURL
        }
      }
    }
    "the user has come from a ITVC page" when {
      "user is NOT an agent" should {
        "provide the correct url" in new Test {
          val expectedURL = "/report-quarterly/income-and-expenses/view/calculation/2022?origin=PTA"
          getFallbackUrl(calcPageBackLink = Some("ITVC"), isAgent = false,
            isCrystallised = true, taxYear = 2022, origin = Some("PTA")) shouldBe expectedURL
        }
      }
      "user IS an agent" should {
        "provide the correct url" in new Test {
          val expectedURL = "/report-quarterly/income-and-expenses/view/agents/calculation/2022"
          getFallbackUrl(calcPageBackLink = Some("ITVC"), isAgent = true,
            isCrystallised = true, taxYear = 2022, origin = None) shouldBe expectedURL
        }
      }
    }
  }
}
