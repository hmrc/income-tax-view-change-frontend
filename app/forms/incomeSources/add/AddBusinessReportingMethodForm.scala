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

import forms.validation.Constraints
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.{JsArray, Json, __}

object AddBusinessReportingMethodForm extends Constraints {
  private val taxYear1 = "tax_year_1_reporting_tax_year"
  private val taxYear2 = "tax_year_2_reporting_tax_year"
  private val taxYearReporting1 = "tax_year_1_reporting"
  private val taxYearReporting2 = "tax_year_2_reporting"
  private val radioMustBeSelectedMessageKey = "incomeSources.add.businessReportingMethod.error"
  private val validRadioOptions = Set("true", "false")

  val form: Form[AddBusinessReportingMethodForm] = Form[AddBusinessReportingMethodForm](
      mapping(
        taxYearReporting1 -> optional(text)
          .verifying(taxYearReporting1, taxYearReporting1 => taxYearReporting1.isDefined && validRadioOptions.contains(taxYearReporting1.get)),
        taxYearReporting2 -> optional(text)
          .verifying(taxYearReporting2, taxYearReporting2 => taxYearReporting2.isDefined && validRadioOptions.contains(taxYearReporting2.get)),
        taxYear1 -> optional(text),
        taxYear2 -> optional(text)
      )(AddBusinessReportingMethodForm.apply)(AddBusinessReportingMethodForm.unapply)
  )

  def updateErrorMessagesWithValues(form:Form[AddBusinessReportingMethodForm]):Form[AddBusinessReportingMethodForm] = {
    form.errors.foldLeft[Form[AddBusinessReportingMethodForm]](form.discardingErrors)((a, b) => {
      a.data.get(b.message + "_tax_year") match {
        case Some(year) =>
          val taxYearTo = year.toInt
          val taxYearFrom = taxYearTo - 1
          a.withError(b.message, radioMustBeSelectedMessageKey, taxYearFrom.toString, taxYearTo.toString)
        case _ => a
      }
    })
  }
}

case class AddBusinessReportingMethodForm(taxYearReporting1: Option[String], taxYearReporting2: Option[String],
                                          taxYear1: Option[String], taxYear2: Option[String]) {

  def toFormMap(): Map[String, Option[String]] = Map("taxYearReporting1" -> taxYearReporting1,
    "taxYearReporting2" -> taxYearReporting2,
    "taxYear1" -> taxYear1,
    "taxYear2" -> taxYear2)

}

