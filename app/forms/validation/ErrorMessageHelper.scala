/*
 * Copyright 2022 HM Revenue & Customs
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

package forms.validation

import forms.validation.models.{FieldError, SummaryError}
import play.api.data.{Field, Form, FormError}


object ErrorMessageHelper {

  import ErrorMessageFactory.SummaryErrorLoc

  @inline private def filterFieldError(errors: Seq[FormError]): Option[FieldError] =
    errors match {
      case Nil => None
      case err => Some(err.head.args.head.asInstanceOf[FieldError])
    }

  def getFieldError(form: Form[_], fieldName: String): Option[FieldError] =
    filterFieldError(form.errors.filter(_.key == fieldName))

  def getFieldError(field: Field): Option[FieldError] =
    filterFieldError(field.errors)

  def getFieldError(field: Field, parentForm: Form[_]): Option[FieldError] =
    getFieldError(parentForm, field.name)

  def getFieldError(field: Field, parentForm: Option[Form[_]] = None): Option[FieldError] =
    parentForm match {
      case Some(form) => getFieldError(field, form)
      case _ => getFieldError(field)
    }

  def getSummaryErrors(form: Form[_]): Seq[(String, SummaryError)] =
    form.errors.map(e => (e.key, e.args(SummaryErrorLoc).asInstanceOf[SummaryError]))

}
