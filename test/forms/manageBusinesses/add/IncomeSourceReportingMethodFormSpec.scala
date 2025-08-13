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

package forms.manageBusinesses.add

import play.api.data.{Form, FormError}
import testUtils.TestSupport

class IncomeSourceReportingMethodFormSpec extends TestSupport {

  lazy val form: Form[IncomeSourceReportingMethodForm] = IncomeSourceReportingMethodForm.form

  "IncomeSourceReportingMethodForm" should {

    "validate when all fields are filled correctly" in {
      val formData = Map(
        "new_tax_year_1_reporting_method" -> "A",
        "new_tax_year_2_reporting_method" -> "Q",
        "taxYear1" -> "2022",
        "taxYear1_reporting_method" -> "A",
        "taxYear2" -> "2023",
        "taxYear2_reporting_method" -> "Q"
      )

      val boundForm = form.bind(formData)
      boundForm.hasErrors shouldBe false
    }

    "invalidate when tax year 1 reporting method radio button is not selected" in {
      val formData = Map(
        "new_tax_year_1_reporting_method" -> "none",
        "new_tax_year_2_reporting_method" -> "Q",
        "taxYear1" -> "2022",
        "taxYear1_reporting_method" -> "A",
        "taxYear2" -> "2023",
        "taxYear2_reporting_method" -> "Q"
      )

      val boundForm = form.bind(formData)
      boundForm.errors should contain(FormError("new_tax_year_1_reporting_method", List("new_tax_year_1_reporting_method")))
    }

    "invalidate when tax year 2 reporting method radio button is not selected" in {
      val formData = Map(
        "new_tax_year_1_reporting_method" -> "A",
        "new_tax_year_2_reporting_method" -> "",
        "taxYear1" -> "2022",
        "taxYear1_reporting_method" -> "A",
        "taxYear2" -> "2023",
        "taxYear2_reporting_method" -> "Q"
      )

      val boundForm = form.bind(formData)
      boundForm.errors should contain(FormError("new_tax_year_2_reporting_method", List("new_tax_year_2_reporting_method")))
    }
  }
  "IncomeSourceReportingMethodForm.unapply" should {
    "return Some with field values as a Tuple6" in {
      val formValues = IncomeSourceReportingMethodForm(
        newTaxYear1ReportingMethod = Some("A"),
        newTaxYear2ReportingMethod = Some("Q"),
        taxYear1 = Some("2021"),
        taxYear1ReportingMethod = None,
        taxYear2 = Some("2022"),
        taxYear2ReportingMethod = Some("A")
      )

      val unapplied = IncomeSourceReportingMethodForm.unapply(formValues)

      unapplied.get shouldBe ((Some("A"), Some("Q"), Some("2021"), None, Some("2022"), Some("A")))
    }

    "return None for a case class with None values" in {
      val formValues = IncomeSourceReportingMethodForm(
        newTaxYear1ReportingMethod = None,
        newTaxYear2ReportingMethod = None,
        taxYear1 = None,
        taxYear1ReportingMethod = None,
        taxYear2 = None,
        taxYear2ReportingMethod = None
      )

      val unapplied = IncomeSourceReportingMethodForm.unapply(formValues)

      unapplied.get shouldBe (None, None, None, None, None, None)
    }
  }

  "IncomeSourceReportingMethodForm.toFormMap" should {
    "return a map of form data" in {
      val formValues = IncomeSourceReportingMethodForm(
        newTaxYear1ReportingMethod = Some("A"),
        newTaxYear2ReportingMethod = Some("Q"),
        taxYear1 = Some("2021"),
        taxYear1ReportingMethod = None,
        taxYear2 = Some("2022"),
        taxYear2ReportingMethod = Some("A")
      )
      val expectedMap = Map(
        "new_tax_year_1_reporting_method" -> Some("A"),
        "new_tax_year_2_reporting_method" -> Some("Q"),
        "new_tax_year_1_reporting_method_tax_year" -> Some("2021"),
        "new_tax_year_2_reporting_method_tax_year" -> Some("2022"),
        "tax_year_1_reporting_method" -> None,
        "tax_year_2_reporting_method" -> Some("A")
      )
      formValues.toFormMap shouldBe expectedMap
    }
  }
}