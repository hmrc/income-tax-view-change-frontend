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


object CeaseForeignPropertyForm {

  val declaration: String = "cease-foreign-property-declaration"
  val declarationUnselectedError: String = "incomeSources.ceaseForeignProperty.checkboxError"
  val ceaseCsrfToken: String = "csrfToken"


  val form: Form[CeaseForeignPropertyForm] = Form[CeaseForeignPropertyForm](
    mapping(
      declaration -> optional(text)
        .verifying(declarationUnselectedError, declaration => declaration.isDefined && declaration.contains("true") && declaration.get.trim.nonEmpty),
      ceaseCsrfToken -> text
    )(CeaseForeignPropertyForm.apply)(CeaseForeignPropertyForm.unapply)
  )
}


case class CeaseForeignPropertyForm(declaration: Option[String],
                               csrfToken: String) {

  def toFormMap: Map[String, Seq[String]] =
    Map(CeaseForeignPropertyForm.declaration -> Seq(declaration.getOrElse("false")),
      CeaseForeignPropertyForm.ceaseCsrfToken -> Seq(csrfToken)
    )
}