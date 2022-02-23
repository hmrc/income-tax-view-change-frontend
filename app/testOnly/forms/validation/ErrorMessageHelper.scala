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

package testOnly.forms.validation

import play.api.data.{Field, Form, FormError}
import testOnly.forms.validation.models.{FieldError, SummaryError, TargetIds}


object ErrorMessageHelper {

  import ErrorMessageFactory.{SummaryErrorLoc, TargetIdsLoc}

  @inline private def filterFieldError(errors: Seq[FormError]): Option[FieldError] =
    errors match {
      case Nil => None
      case _ =>
        val args = errors.head.args
        if (args.isEmpty) None else Some(args.head.asInstanceOf[FieldError])
    }

  def getFieldError(form: Form[_], fieldName: String): Option[FieldError] = {
    val err = form.errors.filter(err => {
      if (err.key == fieldName) true // if the error is from the field itself
      else {
        // or if it's a cross validation error designated for the field
        val args = err.args
        err.args.size match {
          case 3 => val targets = args(TargetIdsLoc).asInstanceOf[TargetIds]
            targets.anchor == fieldName || targets.otherIds.contains(fieldName)
          case _ => false
        }
      }
    })
    filterFieldError(err)
  }

  def getFieldError(field: Field): Option[FieldError] = {
    val err = field.errors
    filterFieldError(err)
  }

  def getFieldError(field: Field, parentForm: Form[_]): Option[FieldError] =
    getFieldError(parentForm, field.name)

  def getFieldError(field: Field, parentForm: Option[Form[_]] = None): Option[FieldError] =
    parentForm match {
      case Some(form) => getFieldError(field, form)
      case _ => getFieldError(field)
    }

  /**
   *
   * @param form
   * @return (String,SummaryError) where the String is the anchor and SummaryError describes the error message
   */
  def getSummaryErrors(form: Form[_]): Seq[(String, SummaryError)] = {
    val err = form.errors
    err.map(e => {
      e.args.size match {
        case 3 =>
          // cross validation error message
          (e.args(TargetIdsLoc).asInstanceOf[TargetIds].anchor, e.args(SummaryErrorLoc).asInstanceOf[SummaryError])
        case 2 =>
          // error message for the field itself
          (e.key, e.args(SummaryErrorLoc).asInstanceOf[SummaryError])
      }
    })
  }

}
