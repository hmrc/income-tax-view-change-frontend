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

import models.core.AddressModel

import java.time.LocalDate

case class ViewIncomeSourcesViewModel(viewSoleTraderBusinesses: List[ViewBusinessDetailsViewModel],
                                      viewUkProperty: Option[ViewPropertyDetailsViewModel],
                                      viewForeignProperty: Option[ViewPropertyDetailsViewModel],
                                      viewCeasedBusinesses: List[ViewCeasedBusinessDetailsViewModel])

case class ViewBusinessDetailsViewModel(incomeSourceId: String,
                                        tradingName: Option[String],
                                        tradingStartDate: Option[LocalDate],
                                        address: Option[AddressModel] = None,
                                        businessAccountingMethod: Option[String] = None,
                                        itsaHasMandatedOrVoluntaryStatusCurrentYear: Boolean
                                       )

case class ViewPropertyDetailsViewModel(tradingStartDate: Option[LocalDate])

case class ViewCeasedBusinessDetailsViewModel(tradingName: Option[String],
                                              tradingStartDate: Option[LocalDate],
                                              cessationDate: LocalDate)