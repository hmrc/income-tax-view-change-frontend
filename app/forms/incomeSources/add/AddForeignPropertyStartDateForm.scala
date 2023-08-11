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

import auth.MtdItUser
import forms.models.DateFormElement
import forms.validation.Constraints
import implicits.ImplicitDateFormatter
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.Messages
import services.DateService
import uk.gov.hmrc.play.language.LanguageUtils

import java.time.LocalDate
import javax.inject.{Inject, Singleton}


@Singleton
class AddForeignPropertyStartDateForm @Inject()(val dateService: DateService)(implicit val languageUtils: LanguageUtils) extends Constraints with ImplicitDateFormatter {

  val messagePrefix = "incomeSources.add.foreignProperty.startDate.error"
  val dateMustBeEntered = s"$messagePrefix.empty"
  val dateMustBeReal = s"$messagePrefix.invalid"
  val dateMustNotBeInTheFuture = s"$messagePrefix.future"

  val datePartErrorMessagesKeys:DatePartErrorMessageKeys = DatePartErrorMessageKeys(
    containsNothing = s"$messagePrefix.empty",
    containsOnlyDay = s"$messagePrefix.missingMonthYear",
    containsOnlyMonth = s"$messagePrefix.missingDayYear",
    containsOnlyYear = s"$messagePrefix.missingDayMonth",
    containsOnlyDayMonth = s"$messagePrefix.missingYear",
    containsOnlyDayYear = s"$messagePrefix.missingMonth",
    containsOnlyMonthYear = s"$messagePrefix.missingDay")

  def apply(implicit user: MtdItUser[_],messages: Messages): Form[DateFormElement] = {
    val currentDate: LocalDate = dateService.getCurrentDate()
    val allowedFutureDate = currentDate.plusWeeks(1)


    Form(
      mapping("incomeSources.add.foreignProperty.startDate" -> tuple(
        "day" -> default(text(), ""),
        "month" -> default(text(), ""),
        "year" -> default(text(), ""))
        .verifying(firstError(dateCheck(datePartErrorMessagesKeys),
          validDate(dateMustBeReal))
        ).transform[LocalDate](
        { case (day, month, year) => LocalDate.of(year.toInt, month.toInt, day.toInt) },
        date => (date.getDayOfMonth.toString, date.getMonthValue.toString, date.getYear.toString)
      ).verifying(maxDate(allowedFutureDate, dateMustNotBeInTheFuture, allowedFutureDate.toLongDate))
      )(DateFormElement.apply)(DateFormElement.unapply))
  }

}

