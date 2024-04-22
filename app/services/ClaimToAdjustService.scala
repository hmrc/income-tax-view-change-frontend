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
import models.financialDetails.{DocumentDetail, FinancialDetailsModel}
import models.incomeSourceDetails.TaxYear
import models.incomeSourceDetails.TaxYear.makeTaxYearWithEndYear
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ClaimToAdjustService @Inject()(val financialDetailsConnector: FinancialDetailsConnector,
                                     val financialDetailsService: FinancialDetailsService)
                                    (implicit ec: ExecutionContext) {

  def getPoATaxYear(implicit hc: HeaderCarrier, user: MtdItUser[_]): Future[Either[Throwable, Option[TaxYear]]] = {
    val a = financialDetailsService.getAllFinancialDetails map {
      item =>
        item.collect({ case (_, model: FinancialDetailsModel) =>
          getPoAPayments(model.documentDetails)
        })
    }
    a.map(_.head)
  }

  private def getPoAPayments(documentDetails: List[DocumentDetail]): Either[Throwable, Option[TaxYear]] = {

    val poa1: Option[TaxYear] = documentDetails.filter(_.documentDescription.exists(_.equals("ITSA- POA 1")))
      .sortBy(_.taxYear).reverse.headOption.map(_.toTaxYear)
    val poa2: Option[TaxYear] = documentDetails.filter(_.documentDescription.exists(_.equals("ITSA - POA 2")))
      .sortBy(_.taxYear).reverse.headOption.map(_.toTaxYear)

    if (poa1 == poa2) {
      Right(poa1)
    } else {
      Logger("application").error(s"[ClaimToAdjustService][getPoAPayments] " +
        s"PoA 1 & 2 most recent documents were expected to be from the same tax year. They are not. < PoA1 TaxYear: $poa1, PoA2 TaxYear: $poa2 >")
      Left(new Exception("PoA 1 & 2 most recent documents were expected to be from the same tax year. They are not."))
    }
  }


}
