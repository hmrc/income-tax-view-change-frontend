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

package controllers.constants

import models.incomeSourceDetails.TaxYear
import services.DateService

object ConfirmOptOutControllerConstants {

  val isAgent: Boolean = false
  val confirmOptOutPageUrl = controllers.optOut.routes.ConfirmOptOutController.show(isAgent).url
  val submitConfirmOptOutPageUrl = controllers.optOut.routes.ConfirmOptOutController.submit(isAgent).url

  val confirmedPageUrl = controllers.optOut.routes.ConfirmedOptOutController.show(isAgent).url
  val optOutErrorPageUrl = controllers.optOut.routes.OptOutErrorController.show(isAgent).url

  def currentTaxYear(dateService: DateService) = TaxYear.forYearEnd(dateService.getCurrentTaxYearEnd)

  def previousYear(dateService: DateService) = currentTaxYear(dateService).addYears(-1)

  def expectedTitle(dateService: DateService) = s"Confirm and opt out for the ${previousYear(dateService).startYear} to ${previousYear(dateService).endYear} tax year"

  val summary = "If you opt out, you can submit your tax return through your HMRC online account or software."
  val infoMessage = s"In future, you could be required to report quarterly again if, for example, your income increases or the threshold for reporting quarterly changes. If this happens, we’ll write to you to let you know."
  val emptyBodyString = ""
  val optOutExpectedTitle = "Check your answers"

}