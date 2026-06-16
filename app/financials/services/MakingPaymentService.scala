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

package financials.services

import common.auth.MtdItUser
import common.services.DateServiceInterface
import models.financialDetails.{DocumentDetail, FinancialDetailsErrorModel, FinancialDetailsModel, MakingPaymentViewModel}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MakingPaymentService @Inject()(financialDetailsService: FinancialDetailsService,
                                     implicit val dateService: DateServiceInterface) {

  def createViewModel(backUrl: String,
                      paymentHandoffUrl: String,
                      whatYouOweUrl: String,
                      moneyInYourAccountUrl: String,
                      payPenaltyUrl: String)
                     (implicit user: MtdItUser[_],
                      hc: HeaderCarrier,
                      ec: ExecutionContext): Future[Option[MakingPaymentViewModel]] = {
    financialDetailsService.getAllFinancialDetails.map { financialDetails =>
      if (financialDetails.exists(_._2.isInstanceOf[FinancialDetailsErrorModel])) {
        None
      } else {
        val financialDetailsModels = financialDetails.collect { case (_, model: FinancialDetailsModel) => model }
        val balanceDetails = financialDetailsModels.map(_.balanceDetails)
        Some(MakingPaymentViewModel(
          backUrl = backUrl,
          paymentHandoffUrl = paymentHandoffUrl,
          whatYouOweUrl = whatYouOweUrl,
          moneyInYourAccountUrl = moneyInYourAccountUrl,
          payPenaltyUrl = payPenaltyUrl,
          hasInterest = financialDetailsModels.flatMap(_.documentDetails).exists(hasInterest) ||
            balanceDetails.exists(_.overDueAmount > 0),
          hasPenalty = financialDetailsModels.flatMap(_.toChargeItem).exists(charge => charge.isPenalty && charge.remainingToPayByChargeOrInterest > 0),
          unallocatedCredit = positiveCredit(balanceDetails.flatMap(_.unallocatedCredit))
            .orElse(positiveCredit(balanceDetails.flatMap(_.totalCreditAvailableForRepayment)))
        ))
      }
    }
  }

  private def positiveCredit(credits: List[BigDecimal]): Option[BigDecimal] =
    credits.filter(_ > 0).sortWith(_ > _).headOption

  private def hasInterest(documentDetail: DocumentDetail): Boolean =
    documentDetail.accruingInterestAmount.exists(_ > 0) ||
      documentDetail.interestOutstandingAmount.exists(_ > 0) ||
      documentDetail.latePaymentInterestAmount.exists(_ > 0)
}
