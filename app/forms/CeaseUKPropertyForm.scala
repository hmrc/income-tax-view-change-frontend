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

package forms

import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Invalid, Valid}


object CeaseUKPropertyForm {

  val declaration: String = "cease-uk-property-declaration"
  val declarationUnselectedError: String = "incomeSources.ceaseUKProperty.radioError"
  val ceaseCsrfToken: String = "csrfToken"
  val emailInvalidError: String = "feedback.email.error"

  val declarationSelected: Constraint[String] = Constraint(value =>
    if(value == "declaration") {
      Valid
    } else {
      Invalid(declarationUnselectedError)
    }
  )

  val form: Form[CeaseUKPropertyForm] = Form[CeaseUKPropertyForm](
    mapping(
      declaration -> text
        .verifying(declarationSelected),
      ceaseCsrfToken -> text
    )(CeaseUKPropertyForm.apply)(CeaseUKPropertyForm.unapply)
  )
}


case class CeaseUKPropertyForm(
                                declaration: String,
                                csrfToken: String
                              ) {
  def toFormMap: Map[String, Seq[String]] =
    Map(CeaseUKPropertyForm.declaration -> Seq(declaration),
      CeaseUKPropertyForm.ceaseCsrfToken -> Seq(csrfToken)
    )
}