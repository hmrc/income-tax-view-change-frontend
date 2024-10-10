/*
 * Copyright 2024 HM Revenue & Customs
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

package testConstants.claimToAdjustPoa

import models.claimToAdjustPoa.{Increase, MainIncomeLower, PoAAmendmentData}
import play.api.libs.json.JsValue
import testConstants.BaseIntegrationTestConstants.testDate
import testConstants.IncomeSourceIntegrationTestConstants.testValidFinancialDetailsModelJson

object ClaimToAdjustPoaTestConstants {

  val testTaxYearPoa = 2024

  val validSession: PoAAmendmentData = PoAAmendmentData(Some(MainIncomeLower), Some(BigDecimal(1000.00)))
  val validSessionIncreaseCase : PoAAmendmentData = PoAAmendmentData(Some(Increase), Some(BigDecimal(10000.00)))

  def validFinancialDetailsResponseBody(taxYear: Int): JsValue =
    testValidFinancialDetailsModelJson(2000, 2000, (taxYear - 1).toString, testDate.toString, poaRelevantAmount = Some(3000))

}
