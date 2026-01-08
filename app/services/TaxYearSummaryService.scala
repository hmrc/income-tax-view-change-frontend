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
import models.incomeSourceDetails.TaxYear
import models.liabilitycalculation.*
import models.taxyearsummary.*
import play.api.Logger

import javax.inject.Inject

// noinspection ScalaStyle
class TaxYearSummaryService @Inject()() {

  def checkSubmissionChannel(liabilityCalculationResponse: Option[LiabilityCalculationResponseModel]): TaxYearViewScenarios = {

    liabilityCalculationResponse match {
      case Some(LiabilityCalculationError(404, message)) =>
        Logger("application").debug(s"[TaxYearSummaryService][checkSubmissionChannel] LiabilityCalculationError - LegacyAndCesa, status: 404, error message: $message")
        LegacyAndCesa
      case Some(LiabilityCalculationResponse(_, _, _, _, Some(IsLegacyWithCesa))) =>
        Logger("application").debug(s"[TaxYearSummaryService][checkSubmissionChannel] LiabilityCalculationResponse - IsLegacyWithCesa")
        LegacyAndCesa
      case Some(LiabilityCalculationResponse(_, _, _, _, Some(IsLegacy))) =>
        Logger("application").debug(s"[TaxYearSummaryService][checkSubmissionChannel] LiabilityCalculationResponse - IsLegacy")
        LegacyAndCesa
      case Some(LiabilityCalculationResponse(_, _, _, _, Some(IsMTD))) =>
        Logger("application").debug(s"[TaxYearSummaryService][checkSubmissionChannel] LiabilityCalculationResponse - IsMTD - show MtdSoftwareShowCalc calc panel")
        MtdSoftwareShowCalc
      case response =>
        Logger("application").debug(s"[TaxYearSummaryService][checkSubmissionChannel] Catch all - show MtdSoftwareShowCalc calc panel - $response")
        MtdSoftwareShowCalc
    }
  }


  def determineCannotDisplayCalculationContentScenario(
                                                        liabilityCalculationResponse: Option[LiabilityCalculationResponseModel],
                                                        taxYear: TaxYear
                                                      )(implicit mtdItUser: MtdItUser[_]): TaxYearViewScenarios = {

    lazy val taxYear2023 = TaxYear(2023, 2024)
    val irsaEnrolement: Option[String] = mtdItUser.authUserDetails.saUtr

    (liabilityCalculationResponse, irsaEnrolement) match {
      case (Some(LiabilityCalculationError(status, _)), _) if mtdItUser.isAgent() && taxYear.isBefore(taxYear2023)=>
        Logger("application").debug(s"[TaxYearSummaryService][determineCannotDisplayCalculationContentScenario] AgentBefore2023TaxYear")
        AgentBefore2023TaxYear
      case (Some(LiabilityCalculationError(status, _)), Some(_)) if taxYear.isBefore(taxYear2023) && status != 404 =>
        Logger("application").debug(s"[TaxYearSummaryService][determineCannotDisplayCalculationContentScenario] IrsaEnrolementHandedOff")
        IrsaEnrolementHandedOff
      case (Some(LiabilityCalculationError(status, _)), None) if taxYear.isBefore(taxYear2023) && status != 404 =>
        Logger("application").debug(s"[TaxYearSummaryService][determineCannotDisplayCalculationContentScenario] NoIrsaAEnrolement")
        NoIrsaAEnrolement
      case _ =>
        Logger("application").debug(s"[TaxYearSummaryService][determineCannotDisplayCalculationContentScenario] checkSubmissionChannel")
        checkSubmissionChannel(liabilityCalculationResponse)
    }
  }
}
