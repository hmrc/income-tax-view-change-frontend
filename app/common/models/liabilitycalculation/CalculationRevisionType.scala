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

package common.models.liabilitycalculation

import common.enums.TaxYearSummary.CalculationType.amendmentTypes

enum CalculationRevisionType {
  case Amendment
  case HmrcAutoCorrection
  case HmrcManualCorrection
  case CustomerRejection
  case RevenueAmendment
}

object CalculationRevisionType {
  def getCalculationRevisionType(calculationType: String, calculationReason: Option[String]): Option[CalculationRevisionType] = {
    val isAnAmendment = amendmentTypes.map(_.value).contains(calculationType)
    if (!isAnAmendment) None else {
      calculationReason match {
        case Some("HMRCAutoCorrection")             => Some(HmrcAutoCorrection)
        case Some("HMRCmanualCorrection")           => Some(HmrcManualCorrection)
        case Some("customerRejectionOfaCorrection") => Some(CustomerRejection)
        case Some("HMRCrevenueamendment")           => Some(RevenueAmendment)
        case _                                      => Some(Amendment)
      }
    }
  }
  
  val correctionAndRevenueAmendmentTypes: Set[CalculationRevisionType] = Set(HmrcAutoCorrection, HmrcManualCorrection, RevenueAmendment)
}