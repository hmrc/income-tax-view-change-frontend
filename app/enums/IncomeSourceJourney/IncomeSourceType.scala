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

enum IncomeSourceType(val key: String, val journeyType: String, val startDateMessagesPrefix: String,
                      val addStartDateCheckMessagesPrefix: String, val endDateMessagePrefix: String, 
                      val reportingMethodChangeErrorPrefix: String, val newReportingMethodChangeErrorPrefixAnnual: String,
                      val newReportingMethodChangeErrorPrefixQuarterly: String, val ceaseCheckAnswersPrefix: String, 
                      val messagesSuffix: String, val messagesCamel: String):
  case SelfEmployment extends IncomeSourceType("SE", "SE", "add-business-start-date", "add-business-start-date-check",
                      "incomeSources.cease.endDate.selfEmployment", "incomeSources.manage.businessReportingMethodError",
                      "manageBusinesses.manage.propertyReportingMethod.new.form.error.annual", 
                      "manageBusinesses.manage.propertyReportingMethod.new.form.error.quarterly",
                      "cease-check-answers",
                      "sole-trader",
                      "selfEmployment")
  case UkProperty extends IncomeSourceType("UK", "UKPROPERTY", "incomeSources.add.UKPropertyStartDate", 
                      "add-uk-property-start-date-check", "incomeSources.cease.endDate.ukProperty",
                      "incomeSources.manage.uKPropertyReportingMethodError",
                      "manageBusinesses.manage.propertyReportingMethod.new.form.error.annual",
                      "manageBusinesses.manage.propertyReportingMethod.new.form.error.quarterly",
                      "cease-check-answers-uk", "uk-property", "ukProperty")
  case ForeignProperty extends IncomeSourceType("FP", "FOREIGNPROPERTY", "incomeSources.add.foreignProperty.startDate",
                                                "add-foreign-property-start-date-check", 
                                                "incomeSources.cease.endDate.foreignProperty",
                                                "incomeSources.manage.foreignPropertyReportingMethodError",
                                                "manageBusinesses.manage.propertyReportingMethod.new.form.error.annual",
                                                "manageBusinesses.manage.propertyReportingMethod.new.form.error.quarterly",
                                                "cease-check-answers-fp", "foreign-property", "foreignProperty")
  
  object IncomeSourceType:
    given incomeSourceTypeJSLBinder: JavascriptLiteral[IncomeSourceType] = (value: IncomeSourceType) => s"""'${value.toString}'"""

    given writes[T <: IncomeSourceType]: Writes[T] = Writes {
      incomeSourceType => JsString(incomeSourceType.toString)
    }