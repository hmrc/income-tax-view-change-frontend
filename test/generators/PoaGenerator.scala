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

package generators

import models.claimToAdjustPoa.PaymentOnAccountViewModel
import models.incomeSourceDetails.TaxYear
import org.scalacheck.Gen

trait PoaGenerator {

  val poaViewModelGen: Gen[PaymentOnAccountViewModel] = for {
    poaOneTransactionId <- Gen.alphaNumStr
    poaTwoTransactionId <- Gen.alphaNumStr
    taxYear <- Gen.const(TaxYear.makeTaxYearWithEndYear(2024))
    totalAmountOne <- Gen.choose(1000, 5000)
    totalAmountTwo <- Gen.choose(1000, 5000)
    relevantAmountOne <- Gen.choose(500, totalAmountOne)
    relevantAmountTwo <- Gen.choose(500, totalAmountTwo)
    previouslyAdjusted <- Gen.option(Gen.oneOf(true, false))
    partiallyPaid <- Gen.oneOf(true, false)
    fullyPaid <- Gen.oneOf(true, false)
  } yield PaymentOnAccountViewModel(
    poaOneTransactionId,
    poaTwoTransactionId,
    taxYear,
    totalAmountOne,
    totalAmountTwo,
    relevantAmountOne,
    relevantAmountTwo,
    previouslyAdjusted,
    partiallyPaid,
    fullyPaid
  )

}
