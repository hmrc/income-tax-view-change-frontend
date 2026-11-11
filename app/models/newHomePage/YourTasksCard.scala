/*
 * Copyright 2026 HM Revenue & Customs
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

package models.newHomePage

import java.time.LocalDate

enum YourTasksCard {
  val cardContent: String
  val cardLinkText: String
  val cardLink: String
  val amountOrCount: Option[String]

  case OverdueTaskCard(
                        cardContent: String,
                        cardLinkText: String,
                        cardLink: String,
                        tagContent: String,
                        dueDate: Option[LocalDate],
                        amountOrCount: Option[String] = None,
                        cardType: YourTaskCardType)

  case DatelessTaskCard(cardContent: String,
                        cardLinkText: String,
                        cardLink: String,
                        amountOrCount: Option[String],
                        cardType: YourTaskCardType)

  case UpcomingTaskCard(cardContent: String,
                        cardLinkText: String,
                        cardLink: String,
                        tagContent: String,
                        dueDate: Option[LocalDate],
                        amountOrCount: Option[String] = None,
                        maturityLevel: MaturityLevel,
                        cardType: YourTaskCardType)

}