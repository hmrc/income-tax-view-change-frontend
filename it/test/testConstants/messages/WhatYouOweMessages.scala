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

object WhatYouOweMessages extends ComponentSpecBase {

  val hmrcAdjustment: String = messagesAPI("whatYouOwe.hmrcAdjustment.text")
  val hmrcAdjustmentHeading: String = messagesAPI("whatYouOwe.hmrcAdjustment.heading")
  val hmrcAdjustmentLine1: String = messagesAPI("whatYouOwe.hmrcAdjustment.line1")
  val underReview: String = messagesAPI("whatYouOwe.paymentUnderReview")

}
