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
import connectors.{CalculationListConnector, IncomeTaxCalculationConnector}
import models.calculationList.{CalculationListErrorModel, CalculationListModel, CalculationListResponseModel}
import models.incomeSourceDetails.TaxYear
import models.liabilitycalculation.{LiabilityCalculationError, LiabilityCalculationResponse, LiabilityCalculationResponseModel}
import models.taxyearsummary._
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class TaxYearSummaryService @Inject()(
                                       calculationListConnector: CalculationListConnector,
                                       incomeTaxCalculationConnector: IncomeTaxCalculationConnector,
                                     ) {

  def determineCannotDisplayCalculationContentScenario(nino: String, taxYear: TaxYear)(implicit user: MtdItUser[_], headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[TaxYearViewScenarios] = {

    lazy val taxYear2023 = TaxYear(2023, 2024)
    val irsaEnrolement = user.authUserDetails.saUtr

    for {
      calculationListResponse: CalculationListResponseModel <- calculationListConnector.getLegacyCalculationList(user.nino, taxYear.endYear.toString)
      getCalculationResponse: LiabilityCalculationResponseModel <- incomeTaxCalculationConnector.getCalculationResponse(user.mtditid, nino, taxYear.toString, None)
    } yield {
      (calculationListResponse, getCalculationResponse) match {
        case (_: CalculationListErrorModel, _: LiabilityCalculationError) | (_: CalculationListErrorModel, _) | (_, _: LiabilityCalculationError) if user.isAgent() && taxYear.isBefore(taxYear2023) =>
          AgentBefore2023TaxYear
        case (_: CalculationListErrorModel, _: LiabilityCalculationError) | (_: CalculationListErrorModel, _) | (_, _: LiabilityCalculationError) if taxYear.isBefore(taxYear2023) =>
          LegacyAndCesa
        case (_: CalculationListErrorModel, _: LiabilityCalculationError) | (_: CalculationListErrorModel, _) | (_, _: LiabilityCalculationError) if irsaEnrolement.isDefined =>
          IrsaEnrolementHandedOff
        case (_: CalculationListErrorModel, _: LiabilityCalculationError) | (_: CalculationListErrorModel, _) | (_, _: LiabilityCalculationError) if irsaEnrolement.isEmpty =>
          NoIrsaAEnrolement
        case (_: CalculationListModel, _: LiabilityCalculationResponse) if taxYear.isBefore(taxYear2023) =>
          MtdSoftware
        case _ =>
          Default
      }
    }
  }


}
