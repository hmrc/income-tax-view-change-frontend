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

package enums.IncomeSourceJourney

import forms.utils.SessionKeys
import play.api.libs.json.{JsString, Writes}
import play.api.mvc.JavascriptLiteral

sealed trait IncomeSourceType {
  val key: String
  val startDateMessagesPrefix: String
  val addStartDateCheckMessagesPrefix: String
  val startDateSessionKey: String
  val reportingMethodChangeErrorPrefix: String
}

case object SelfEmployment extends IncomeSourceType {
  override val key = "SE"
  override val startDateMessagesPrefix = "add-business-start-date"
  override val addStartDateCheckMessagesPrefix = "add-business-start-date-check"
  override val startDateSessionKey = SessionKeys.addBusinessStartDate
  override val reportingMethodChangeErrorPrefix = "incomeSources.manage.businessReportingMethodError"
}

case object UkProperty extends IncomeSourceType {
  override val key = "UK"
  override val startDateMessagesPrefix = "incomeSources.add.UKPropertyStartDate"
  override val addStartDateCheckMessagesPrefix = "add-uk-property-start-date-check"
  override val startDateSessionKey = SessionKeys.addUkPropertyStartDate
  override val reportingMethodChangeErrorPrefix = "incomeSources.manage.uKPropertyReportingMethodError"
}

case object ForeignProperty extends IncomeSourceType {
  override val key = "FP"
  override val startDateMessagesPrefix = "incomeSources.add.foreignProperty.startDate"
  override val addStartDateCheckMessagesPrefix = "add-foreign-property-start-date-check"
  override val startDateSessionKey = SessionKeys.foreignPropertyStartDate
  override val reportingMethodChangeErrorPrefix = "incomeSources.manage.foreignPropertyReportingMethodError"
}

object IncomeSourceType {
  def get(key: String): Either[Exception,IncomeSourceType] = {
    key match {
      case "FP" => Right(ForeignProperty)
      case "UK" => Right(UkProperty)
      case "SE" => Right(SelfEmployment)
      case _ => Left(new Exception("Invalid incomeSourceType"))
    }
  }

  implicit val incomeSourceTypeJSLBinder: JavascriptLiteral[IncomeSourceType] = (value: IncomeSourceType) => s"""'${value.toString}'"""

  implicit def writes[T <: IncomeSourceType]: Writes[T] = Writes {
    incomeSourceType => JsString(incomeSourceType.toString)
  }
}
