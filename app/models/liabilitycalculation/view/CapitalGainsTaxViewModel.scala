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

package models.liabilitycalculation.view

import models.liabilitycalculation.taxcalculation.{BusinessAssetsDisposalsAndInvestorsRel, CapitalGainsTax, CgtTaxBands}

case class CapitalGainsTaxViewModel(
                                     totalTaxableGains: Option[BigDecimal] = None,
                                     adjustments: Option[BigDecimal] = None,
                                     foreignTaxCreditRelief: Option[BigDecimal] = None,
                                     taxOnGainsAlreadyPaid: Option[BigDecimal] = None,
                                     capitalGainsTaxDue: Option[BigDecimal] = None,
                                     capitalGainsOverpaid: Option[BigDecimal] = None,
                                     propertyAndInterestTaxBands: Option[Seq[CgtTaxBands]] = None,
                                     otherGainsTaxBands: Option[Seq[CgtTaxBands]] = None,
                                     businessAssetsDisposalsAndInvestorsRel: Option[BusinessAssetsDisposalsAndInvestorsRel] = None
                                   )

object CapitalGainsTaxViewModel {
  def apply(capitalGainsTaxOpt: Option[CapitalGainsTax]): CapitalGainsTaxViewModel = {
    capitalGainsTaxOpt match {
      case Some(cgt) => CapitalGainsTaxViewModel(
        totalTaxableGains = Some(cgt.totalTaxableGains),
        adjustments = cgt.adjustments,
        foreignTaxCreditRelief = cgt.foreignTaxCreditRelief,
        taxOnGainsAlreadyPaid = cgt.taxOnGainsAlreadyPaid,
        capitalGainsTaxDue = Some(cgt.capitalGainsTaxDue),
        capitalGainsOverpaid = cgt.capitalGainsOverpaid,
        propertyAndInterestTaxBands = cgt.residentialPropertyAndCarriedInterest.map(rpc =>
          rpc.cgtTaxBands),
        otherGainsTaxBands = cgt.otherGains.map(rpc =>
          rpc.cgtTaxBands),
        businessAssetsDisposalsAndInvestorsRel = cgt.businessAssetsDisposalsAndInvestorsRel
      )
      case None => CapitalGainsTaxViewModel()
    }
  }
}