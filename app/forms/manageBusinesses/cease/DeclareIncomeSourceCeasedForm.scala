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

package forms.manageBusinesses.cease

import enums.IncomeSourceJourney.IncomeSourceType
import play.api.data.Form
import play.api.data.Forms._

object DeclareIncomeSourceCeasedForm {
  val declaration: String = "cease-income-source-declaration"
  val ceaseCsrfToken: String = "csrfToken"

  def form(incomeSourceType: IncomeSourceType): Form[DeclareIncomeSourceCeasedForm] = {
    val declarationUnselectedError: String = s"incomeSources.cease.${incomeSourceType.key}.checkboxError"

    Form[DeclareIncomeSourceCeasedForm](
      mapping(
        declaration -> optional(text)
          .verifying(declarationUnselectedError, declaration => declaration.isDefined && declaration.contains("true") && declaration.get.trim.nonEmpty),
        ceaseCsrfToken -> text
      )
      (
        (declaration, ceaseCsrfToken) =>
          DeclareIncomeSourceCeasedForm(
            declaration,
            ceaseCsrfToken
          )
      )(
        form =>
          Some(
            (
              form.declaration,
              form.csrfToken
            )
          )
      )
    )
  }
}

case class DeclareIncomeSourceCeasedForm(declaration: Option[String],
                                         csrfToken: String) {
  def toFormMap: Map[String, Seq[String]] =
    Map(DeclareIncomeSourceCeasedForm.declaration -> Seq(declaration.getOrElse("false")),
      DeclareIncomeSourceCeasedForm.ceaseCsrfToken -> Seq(csrfToken)
    )
}