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

import forms.validation.CustomConstraints
import play.api.data.Form
import play.api.data.Forms._

object AddUKPropertyReportingMethodForm extends CustomConstraints {
  val newTaxYear1ReportingMethod = "new_tax_year_1_reporting_method"
  val newTaxYear2ReportingMethod = "new_tax_year_2_reporting_method"
  val taxYear1 = s"${newTaxYear1ReportingMethod}_tax_year"
  val taxYear2 = s"${newTaxYear2ReportingMethod}_tax_year"
  val taxYear1ReportingMethod = "tax_year_1_reporting_method"
  val taxYear2ReportingMethod = "tax_year_2_reporting_method"
  private val radioMustBeSelectedMessageKey = "incomeSources.add.ukPropertyReportingMethod.error"
  private val validRadioOptions = Set("A", "Q")

  val form: Form[AddUKPropertyReportingMethodForm] = Form[AddUKPropertyReportingMethodForm](
    mapping(
      newTaxYear1ReportingMethod -> optional(text)
        .verifying(newTaxYear1ReportingMethod, taxYearReporting1 => taxYearReporting1.isEmpty || validRadioOptions.contains(taxYearReporting1.get)),
      newTaxYear2ReportingMethod -> optional(text)
        .verifying(newTaxYear2ReportingMethod, taxYearReporting2 => taxYearReporting2.isDefined && validRadioOptions.contains(taxYearReporting2.get)),
      taxYear1 -> optional(text),
      taxYear1ReportingMethod -> optional(text),
      taxYear2 -> optional(text),
      taxYear2ReportingMethod -> optional(text)
    )(AddUKPropertyReportingMethodForm.apply)(AddUKPropertyReportingMethodForm.unapply)
  )

  def updateErrorMessagesWithValues(form: Form[AddUKPropertyReportingMethodForm]): Form[AddUKPropertyReportingMethodForm] = {
    form.errors.foldLeft[Form[AddUKPropertyReportingMethodForm]](form.discardingErrors)((reportingMethodFormValues, formError) => {
      reportingMethodFormValues.data.get(formError.message + "_tax_year") match {
        case Some(year) =>
          val taxYearTo = year.toInt
          val taxYearFrom = taxYearTo - 1
          reportingMethodFormValues.withError(formError.message, radioMustBeSelectedMessageKey, taxYearFrom.toString, taxYearTo.toString)
        case _ => reportingMethodFormValues
      }
    })
  }
}

case class AddUKPropertyReportingMethodForm(newTaxYear1ReportingMethod: Option[String],
                                            newTaxYear2ReportingMethod: Option[String],
                                            taxYear1: Option[String],
                                            taxYear1ReportingMethod: Option[String],
                                            taxYear2: Option[String],
                                            taxYear2ReportingMethod: Option[String]) {

  def toFormMap: Map[String, Option[String]] = Map(
    AddUKPropertyReportingMethodForm.newTaxYear1ReportingMethod -> newTaxYear1ReportingMethod,
    AddUKPropertyReportingMethodForm.newTaxYear2ReportingMethod -> newTaxYear2ReportingMethod,
    AddUKPropertyReportingMethodForm.taxYear1 -> taxYear1,
    AddUKPropertyReportingMethodForm.taxYear2 -> taxYear2,
    AddUKPropertyReportingMethodForm.taxYear1ReportingMethod -> taxYear1ReportingMethod,
    AddUKPropertyReportingMethodForm.taxYear2ReportingMethod -> taxYear2ReportingMethod)

}