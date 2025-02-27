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

package models.creditsandrefunds

import exceptions.MissingFieldException
import models.repaymentHistory.{RepaymentHistory, RepaymentHistoryStatus, RepaymentItem, TotalInterest}
import scala.util.Try

import java.time.LocalDate

case class RefundToTaxPayerViewModel(
                                      amountApprovedForRepayment: Option[BigDecimal],
                                      amountRequested: BigDecimal,
                                      repaymentMethod: String,
                                      totalRepaymentAmount: BigDecimal,
                                      repaymentItems: Seq[RepaymentItem],
                                      estimatedRepaymentDate: LocalDate,
                                      creationDate: LocalDate,
                                      repaymentRequestNumber: String,
                                      status: RepaymentHistoryStatus,
                                      aggregate: Option[TotalInterest]
                                    ) {
  def getInterestContent: TotalInterest = {
    aggregate.getOrElse(TotalInterest(LocalDate.MIN, 0.00, LocalDate.MAX, 0.00, 0.00))
  }

  def getApprovedAmount: BigDecimal = {
    amountApprovedForRepayment.getOrElse(0.00)
  }
}

object RefundToTaxPayerViewModel {
  def createViewModel(item: RepaymentHistory): Either[Exception, RefundToTaxPayerViewModel] = {
    Try {
      RefundToTaxPayerViewModel(
        amountApprovedForRepayment = item.amountApprovedforRepayment,
        amountRequested = item.amountRequested,
        repaymentMethod = item.repaymentMethod.get,
        totalRepaymentAmount = item.totalRepaymentAmount.get,
        repaymentItems = item.repaymentItems.get,
        estimatedRepaymentDate = item.estimatedRepaymentDate.get,
        creationDate = item.creationDate.get,
        repaymentRequestNumber = item.repaymentRequestNumber,
        status = item.status,
        aggregate = item.aggregate
      )
    }.toOption match {
      case Some(viewModel) => Right(viewModel)
      case None => Left(MissingFieldException("Missing field while constructing RefundToTaxPayerViewModel"))
    }
  }
}
