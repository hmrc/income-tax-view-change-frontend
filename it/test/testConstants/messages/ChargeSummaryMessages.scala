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

import helpers.ComponentSpecBase
object ChargeSummaryMessages extends ComponentSpecBase {

  val paymentBreakdownHeading: String = messagesAPI("chargeSummary.paymentBreakdown.heading")
  val underReview: String = messagesAPI("chargeSummary.paymentBreakdown.dunningLocks.underReview")
  val notCurrentlyChargingInterest: String = messagesAPI("chargeSummary.paymentBreakdown.interestLocks.notCharging")

  val lpiCreated: String = messagesAPI("chargeSummary.lpi.chargeHistory.created.balancingCharge.text")

  val codingOutInsetPara: String = s"${messagesAPI("chargeSummary.codingOutInset-1")} ${messagesAPI("chargeSummary.codingOutInset-2")}" +
    s" ${messagesAPI("pagehelp.opensInNewTabText")} ${messagesAPI("chargeSummary.codingOutInset-3")}"
  def codingOutMessage(from: Int, to: Int): String = messagesAPI("chargeSummary.codingOutBCDMessage", from, to)
  def codingOutMessageWithStringMessagesArgument(from: Int, to: Int): String = messagesAPI("yourSelfAssessmentChargeSummary.codingOutBCDMessage", from.toString, to.toString, (from+2).toString, (to+2).toString)

  val paymentprocessingbullet1: String = s"${messagesAPI("chargeSummary.payments-bullet1-1")} ${messagesAPI("chargeSummary.payments-bullet1-2")} ${messagesAPI("pagehelp.opensInNewTabText")} ${messagesAPI("chargeSummary.payments-bullet2")}"
  val paymentprocessingbullet1Agent: String = s"${messagesAPI("chargeSummary.payments-bullet1-1")} ${messagesAPI("chargeSummary.payments-bullet1-2-agent")} ${messagesAPI("pagehelp.opensInNewTabText")} ${messagesAPI("chargeSummary.payments-bullet2-agent")}"
}
