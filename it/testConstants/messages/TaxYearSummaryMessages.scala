/*
 * Copyright 2017 HM Revenue & Customs
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

package testConstants.messages

import helpers.servicemocks.AuthStub.{lang, messagesAPI}

object TaxYearSummaryMessages {

  val noCalcHeading: String = messagesAPI("tax-year-summary.tax-calculation.no-calc")
  val noCalcNote: String = messagesAPI("tax-year-summary.tax-calculation.no-calc.note")

  val underReview: String = messagesAPI("tax-year-summary.payments.paymentUnderReview")
  val overdue: String = messagesAPI("tax-year-summary.payments.overdue")
  val poa1Lpi: String = messagesAPI("tax-year-summary.payments.lpi.paymentOnAccount1.text")
  val poa1: String = messagesAPI("tax-year-summary.payments.paymentOnAccount1.text")
  val poa2: String = messagesAPI("tax-year-summary.payments.paymentOnAccount2.text")
  val balancingPayment: String = messagesAPI("tax-year-summary.payments.balancingCharge.text")
  val class2Nic: String = messagesAPI("tax-year-summary.payments.class2Nic.text")
  val cancelledPayeSA: String = messagesAPI("tax-year-summary.payments.cancelledPayeSelfAssessment.text")
  val noPaymentsDue: String = messagesAPI("tax-year-summary.payments.no-payments")

  def updateTabDue(dueDate: String): String = messagesAPI("updateTab.due", dueDate)
  val quarterlyUpdate: String = messagesAPI("updateTab.updateType.quarterly")
  val annualUpdate: String = messagesAPI("updateTab.updateType.eops")
  val propertyIncome: String = messagesAPI("updateTab.obligationType.property")


}
