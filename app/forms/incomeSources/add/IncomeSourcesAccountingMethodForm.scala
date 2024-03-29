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

package forms.incomeSources.add

import enums.IncomeSourceJourney.IncomeSourceType
import play.api.data.Form
import play.api.data.Forms.{mapping, optional, text}

object IncomeSourcesAccountingMethodForm {

  def apply(incomeSourceType: IncomeSourceType): Form[Option[String]] = {

    val response: String = "incomeSources.add." + incomeSourceType.key + ".AccountingMethod"
    val radioEmptyError: String = "incomeSources.add.AccountingMethod.no-selection"
    val validRadioOptions = Set("cash", "traditional")

    Form(
      mapping(
        response -> optional(text)
          .verifying(radioEmptyError, value => value.isDefined && validRadioOptions.contains(value.get))
      )(identity)(Option(_))
    )
  }

}