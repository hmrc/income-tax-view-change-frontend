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

package models.optOut

import models.incomeSourceDetails.TaxYear
import play.api.mvc.Call

case class OptOutOneYearViewModel(oneYearOptOutTaxYear: TaxYear, showWarning: Boolean = false) {
  def startYear: String = oneYearOptOutTaxYear.startYear.toString

  def endYear: String = oneYearOptOutTaxYear.endYear.toString

  def optOutConfirmationLink(isAgent: Boolean): Call = {
    if (showWarning) {
      return controllers.optout.routes.SingleYearOptOutWarningController.show(isAgent)
    }

    if (isAgent) {
      controllers.optout.routes.ConfirmOptOutController.showAgent()
    } else {
      controllers.optout.routes.ConfirmOptOutController.show()
    }

  }
}
