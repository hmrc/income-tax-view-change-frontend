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
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus.ITSAStatus
import models.itsaStatus.{ITSAStatus, StatusDetail}
import models.optOut.{NextUpdatesQuarterlyReportingContentChecks, OptOutMessageResponse, OptOutQuery, OptOutRules, OptOutSymbol}
import models.optOut.OptOutRules._
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class OptOutService2 @Inject()(itsaStatusService: ITSAStatusService, calculationListService: CalculationListService, dateService: DateService)
                              (implicit ec: ExecutionContext) {


  def toF[T](opl: Option[T], failValue: T): Future[T] = opl
    .map(v => Future.successful(v))
    .getOrElse(Future.successful(failValue))

  def displayOptOutMessage()(implicit user: MtdItUser[_], hc: HeaderCarrier): Future[OptOutMessageResponse] = {

    val endYear = dateService.getCurrentTaxYearEnd

    val currentYear = TaxYear(endYear)
    val previousYear = currentYear.addYears(-1)
    val nextYear = currentYear.addYears(+1)

    val taxYearITSAStatus: Future[Map[TaxYear, StatusDetail]] = Future.successful({
      Map(
        TaxYear(2023, 2024) -> StatusDetail("submittedOn", ITSAStatus.NoStatus, "statusReason", Some(BigDecimal.valueOf(10))),
        TaxYear(2024, 2025) -> StatusDetail("submittedOn", ITSAStatus.Voluntary, "statusReason", Some(BigDecimal.valueOf(10))),
        TaxYear(2025, 2026) -> StatusDetail("submittedOn", ITSAStatus.Voluntary, "statusReason", Some(BigDecimal.valueOf(10))),
      )
    })
      //itsaStatusService.getStatusTillAvailableFutureYears(previousYear)
    val finalisedStatus: Future[Option[Boolean]] = Future.successful(Some(false))
      //calculationListService.isTaxYearCrystallised(previousYear.endYear)

    for {

      statusMap <- taxYearITSAStatus
      finalisedStatus <- finalisedStatus
      finalisedStatusBool <- toF(finalisedStatus, false)

      isCurrentYearVoluntary = statusMap(currentYear).isVoluntary
      isPreviousYearVoluntary = statusMap(previousYear).isVoluntary
      isNextYearVoluntary = statusMap(nextYear).isVoluntary

      finalised = if(finalisedStatusBool) OptOutSymbol.FinalizedYes else OptOutSymbol.FinalizedNo
      pyStatus = if(isPreviousYearVoluntary) OptOutSymbol.Voluntary else OptOutSymbol.Unknown
      cyStatus = if(isCurrentYearVoluntary) OptOutSymbol.Voluntary else (if(statusMap(currentYear).isMandated) OptOutSymbol.Mandatory else OptOutSymbol.Unknown)
      nyStatus = if(isNextYearVoluntary) OptOutSymbol.Voluntary else (if(statusMap(nextYear).isMandated) OptOutSymbol.Mandatory else OptOutSymbol.Unknown)


      optOutOutcome = OptOutRules.query2(OptOutQuery(finalised, pyStatus, cyStatus, nyStatus))
      response = OptOutMessageResponse(!optOutOutcome.isEmpty)
    } yield response
  }
}