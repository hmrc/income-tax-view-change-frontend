/*
 * Copyright 2024 HM Revenue & Customs
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

import enums.IncomeSourceJourney.IncomeSourceType
import models.incomeSourceDetails.{IncomeSourceDetailsModel, TaxYear}
import services.DateServiceInterface

import java.time.LocalDate

case class IncomeSourceCeasedObligationsViewModel(
                                                   incomeSourceType: IncomeSourceType,
                                                   remainingLatentBusiness: Boolean,
                                                   allBusinessesCeased: Boolean,
                                                   businessName: Option[String],
                                                   isAgent: Boolean
                                                 ) {

}

object IncomeSourceCeasedObligationsViewModel {
  def apply(
             incomeSourceDetailsModel: IncomeSourceDetailsModel,
             incomeSourceType: IncomeSourceType,
             businessName: Option[String],
             isAgent: Boolean
           ): IncomeSourceCeasedObligationsViewModel = {

    val remainingLatentBusiness: Boolean =
      incomeSourceDetailsModel.remainingNoneCeasedBusinesses == 1 && incomeSourceDetailsModel.isAnyOfActiveBusinessesLatent

    val allBusinessesCeased: Boolean = incomeSourceDetailsModel.areAllBusinessesCeased

    IncomeSourceCeasedObligationsViewModel(
      incomeSourceType = incomeSourceType,
      remainingLatentBusiness = remainingLatentBusiness,
      allBusinessesCeased = allBusinessesCeased,
      businessName = businessName,
      isAgent = isAgent
    )
  }
}