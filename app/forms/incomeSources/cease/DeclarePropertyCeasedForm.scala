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

package forms.incomeSources.cease

import enums.IncomeSourceJourney.IncomeSourceType
import play.api.data.Form
import play.api.data.Forms._

object DeclarePropertyCeasedForm {
  val declaration: String = "cease-property-declaration"
  val ceaseCsrfToken: String = "csrfToken"

  def form(incomeSourceType: IncomeSourceType): Form[DeclarePropertyCeasedForm] = {
    val declarationUnselectedError: String = s"incomeSources.cease.${incomeSourceType.key}.checkboxError"

    Form[DeclarePropertyCeasedForm](
      mapping(
        declaration -> optional(text)
          .verifying(declarationUnselectedError, declaration => declaration.isDefined && declaration.contains("true") && declaration.get.trim.nonEmpty),
        ceaseCsrfToken -> text
      )(DeclarePropertyCeasedForm.apply)(DeclarePropertyCeasedForm.unapply)
    )
  }
}

case class DeclarePropertyCeasedForm(declaration: Option[String],
                                     csrfToken: String) {
  def toFormMap: Map[String, Seq[String]] =
    Map(DeclarePropertyCeasedForm.declaration -> Seq(declaration.getOrElse("false")),
      DeclarePropertyCeasedForm.ceaseCsrfToken -> Seq(csrfToken)
    )
}