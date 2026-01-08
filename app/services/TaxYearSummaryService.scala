/*
 * Copyright 2025 HM Revenue & Customs
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

package services

import auth.MtdItUser
import enums.CesaSAReturn
import models.incomeSourceDetails.TaxYear
import models.liabilitycalculation.{LiabilityCalculationError, LiabilityCalculationResponse, LiabilityCalculationResponseModel}
import models.taxyearsummary._

import javax.inject.Inject


class TaxYearSummaryService @Inject()() {

  def determineCannotDisplayCalculationContentScenario(
                                                        latestCalcResponse: LiabilityCalculationResponseModel,
                                                        taxYear: TaxYear
                                                      )(implicit user: MtdItUser[_]): TaxYearViewScenarios = {

    lazy val taxYear2023 = TaxYear(2023, 2024)
    val irsaEnrolement = user.authUserDetails.saUtr

    latestCalcResponse match {
      case response: LiabilityCalculationResponse if response.metadata.calculationTrigger == Some(CesaSAReturn) =>
        LegacyAndCesa
      case response: LiabilityCalculationError if user.isAgent() && response.status != 404 && taxYear.isBefore(taxYear2023) =>
        AgentBefore2023TaxYear
      case response: LiabilityCalculationError if irsaEnrolement.isDefined && response.status != 404 && taxYear.isBefore(taxYear2023) =>
        IrsaEnrolementHandedOff
      case response: LiabilityCalculationError if response.status == 404 && taxYear.isBefore(taxYear2023) =>
        LegacyAndCesa
      case _: LiabilityCalculationError if irsaEnrolement.isEmpty && taxYear.isBefore(taxYear2023) =>
        NoIrsaAEnrolement
      case response: LiabilityCalculationResponse if response.metadata.calculationTrigger != Some(CesaSAReturn) =>
        MtdSoftware
      case _ =>
        Default
    }
  }
}
