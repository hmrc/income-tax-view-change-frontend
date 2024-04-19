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

package services

import auth.MtdItUser
import connectors.FinancialDetailsConnector
import models.core.Nino
import models.financialDetails.{DocumentDetail, FinancialDetailsModel}
import models.incomeSourceDetails.TaxYear
import models.incomeSourceDetails.TaxYear.makeTaxYearWithEndYear
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ClaimToAdjustService @Inject()(val financialDetailsConnector: FinancialDetailsConnector,
                                     val financialDetailsService: FinancialDetailsService)
                                    (implicit ec: ExecutionContext) {

  def canCustomerClaimToAdjust(implicit hc: HeaderCarrier, user: MtdItUser[_]): Future[Boolean] = {
    maybePoATaxYear.map {
      case Some(_) => true
      case None => false
    }
  }

  def maybePoATaxYear(implicit hc: HeaderCarrier, user: MtdItUser[_]): Future[Option[TaxYear]] = {
    val a = financialDetailsService.getAllFinancialDetails map {
      item => item.collect({ case (_, model: FinancialDetailsModel) => getPoAPayments(model.documentDetails)})
    }
    a.map(_.headOption.flatten)
  }

  def getPoAPayments(documentDetails: List[DocumentDetail]): Option[TaxYear] = {
    val poa1: List[DocumentDetail] = documentDetails.filter(_.documentDescription.exists(_.equals("ITSA- POA 1")))
    val poa2: List[DocumentDetail] = documentDetails.filter(_.documentDescription.exists(_.equals("ITSA - POA 2")))

    if ((poa1.length == 1) && (poa2.length == 1)) {
      if (poa1.head.taxYear == poa2.head.taxYear) {
        Some(makeTaxYearWithEndYear(poa1.head.taxYear))
      } else {
        None
      }
    } else {
      None
    }
  }

}
