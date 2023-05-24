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

package models.incomeSourceDetails.viewmodels

import models.incomeSourceDetails.{BusinessDetailsModel, PropertyDetailsModel}

import java.time.LocalDate

case class AddIncomeSourcesViewModel(soleTraderBusinesses: List[BusinessDetailsViewModel],
                                     ukProperty: Option[PropertyDetailsViewModel],
                                     foreignProperty: Option[PropertyDetailsViewModel],
                                     ceasedBusinesses: List[CeasedBusinessDetailsViewModel])

case class BusinessDetailsViewModel(tradingName: String, tradingStartDate: LocalDate)

case class PropertyDetailsViewModel(tradingStartDate: LocalDate)

case class CeasedBusinessDetailsViewModel(tradingName: String,
                                          tradingStartDate: LocalDate,
                                          cessationDate: LocalDate)