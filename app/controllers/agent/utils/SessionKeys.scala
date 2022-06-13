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

package controllers.agent.utils

object SessionKeys {

  val clientNino: String = "ClientNino"
  val clientMTDID: String = "ClientMTDID"
  val clientUTR: String = "ClientUTR"
  val clientFirstName: String = "ClientFirstName"
  val clientLastName: String = "ClientLastName"
  val chargeSummaryBackPage: String = "chargeSummaryBackPage"

  val confirmedClient: String = "ConfirmedClient"

  val calculationId = "calculationId"
}
