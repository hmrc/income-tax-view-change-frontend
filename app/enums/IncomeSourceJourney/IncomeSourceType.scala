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

import play.api.libs.json.{JsString, Writes}
import play.api.mvc.JavascriptLiteral

sealed trait IncomeSourceType {
  val key: String
  val journeyType: String
  val startDateMessagesPrefix: String
  val addStartDateCheckMessagesPrefix: String
  val endDateMessagePrefix: String
  val reportingMethodChangeErrorPrefix: String
  val newReportingMethodChangeErrorPrefixAnnual: String
  val newReportingMethodChangeErrorPrefixQuarterly: String
  val ceaseCheckDetailsPrefix: String
  val ceaseCheckAnswersPrefix: String
  val messagesSuffix: String
  val messagesCamel: String
}

case object SelfEmployment extends IncomeSourceType {
  override val key = "SE"
  override val journeyType: String = "SE"
  override val startDateMessagesPrefix: String = "add-business-start-date"
  override val addStartDateCheckMessagesPrefix: String = "add-business-start-date-check"
  override val endDateMessagePrefix: String = "incomeSources.cease.endDate.selfEmployment"
  override val reportingMethodChangeErrorPrefix: String = "incomeSources.manage.businessReportingMethodError"
  override val newReportingMethodChangeErrorPrefixAnnual: String = "manageBusinesses.manage.propertyReportingMethod.form.error.annual"
  override val newReportingMethodChangeErrorPrefixQuarterly: String = "manageBusinesses.manage.propertyReportingMethod.form.error.quarterly"
  override val ceaseCheckDetailsPrefix: String = "incomeSources.ceaseBusiness.checkDetails"
  override val ceaseCheckAnswersPrefix: String = "cease-check-answers"
  override val messagesSuffix: String = "sole-trader"
  override val messagesCamel: String = "selfEmployment"
}

case object UkProperty extends IncomeSourceType {
  override val key = "UK"
  override val journeyType: String = "UKPROPERTY"
  override val startDateMessagesPrefix: String = "incomeSources.add.UKPropertyStartDate"
  override val addStartDateCheckMessagesPrefix: String = "add-uk-property-start-date-check"
  override val endDateMessagePrefix: String = "incomeSources.cease.endDate.ukProperty"
  override val reportingMethodChangeErrorPrefix: String = "incomeSources.manage.uKPropertyReportingMethodError"
  override val newReportingMethodChangeErrorPrefixAnnual: String = "manageBusinesses.manage.propertyReportingMethod.form.error.annual"
  override val newReportingMethodChangeErrorPrefixQuarterly: String = "manageBusinesses.manage.propertyReportingMethod.form.error.quarterly"
  override val ceaseCheckDetailsPrefix: String = "incomeSources.ceaseUKProperty.checkDetails"
  override val ceaseCheckAnswersPrefix: String = "cease-check-answers-uk"
  override val messagesSuffix: String = "uk-property"
  override val messagesCamel: String = "ukProperty"
}

case object ForeignProperty extends IncomeSourceType {
  override val key = "FP"
  override val journeyType: String = "FOREIGNPROPERTY"
  override val startDateMessagesPrefix: String = "incomeSources.add.foreignProperty.startDate"
  override val addStartDateCheckMessagesPrefix: String = "add-foreign-property-start-date-check"
  override val endDateMessagePrefix: String = "incomeSources.cease.endDate.foreignProperty"
  override val reportingMethodChangeErrorPrefix: String = "incomeSources.manage.foreignPropertyReportingMethodError"
  override val newReportingMethodChangeErrorPrefixAnnual: String = "manageBusinesses.manage.propertyReportingMethod.form.error.annual"
  override val newReportingMethodChangeErrorPrefixQuarterly: String = "manageBusinesses.manage.propertyReportingMethod.form.error.quarterly"
  override val ceaseCheckDetailsPrefix: String = "incomeSources.ceaseForeignProperty.checkDetails"
  override val ceaseCheckAnswersPrefix: String = "cease-check-answers-fp"
  override val messagesSuffix: String = "foreign-property"
  override val messagesCamel: String = "foreignProperty"
}

object IncomeSourceType {

  implicit val incomeSourceTypeJSLBinder: JavascriptLiteral[IncomeSourceType] = (value: IncomeSourceType) => s"""'${value.toString}'"""

  implicit def writes[T <: IncomeSourceType]: Writes[T] = Writes {
    incomeSourceType => JsString(incomeSourceType.toString)
  }
}
