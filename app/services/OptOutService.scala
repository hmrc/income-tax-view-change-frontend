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
import models.itsaStatus.StatusDetail
import models.optOut.{NextUpdatesQuarterlyReportingContentChecks, OptOutMessageResponse, YearStatusDetail}
import services.optout.OptOutRulesService
import services.optout.OptOutRulesService.{toFinalized, toQuery, toSymbol}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}


@Singleton
class OptOutService @Inject()(itsaStatusService: ITSAStatusService, calculationListService: CalculationListService, dateService: DateServiceInterface) {
  def getNextUpdatesQuarterlyReportingContentChecks(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[NextUpdatesQuarterlyReportingContentChecks] = {
    val endYear = dateService.getCurrentTaxYearEnd
    val currentYear = TaxYear(endYear)
    val previousYear = currentYear.addYears(-1)

    val taxYearITSAStatus: Future[Map[TaxYear, StatusDetail]] = itsaStatusService.getStatusTillAvailableFutureYears(previousYear)
    val previousYearCalcStatus: Future[Option[Boolean]] = calculationListService.isTaxYearCrystallised(previousYear.endYear)

    for {
      statusMap <- taxYearITSAStatus
      isCurrentYearStatusMandatoryOrVoluntary = statusMap(currentYear).isMandatedOrVoluntary
      isPreviousYearStatusMandatoryOrVoluntary = statusMap(previousYear).isMandatedOrVoluntary
      calStatus <- previousYearCalcStatus
      optOutChecks = NextUpdatesQuarterlyReportingContentChecks(
        isCurrentYearStatusMandatoryOrVoluntary,
        isPreviousYearStatusMandatoryOrVoluntary,
        calStatus)
    } yield optOutChecks
  }

  /* todo: consider the two solutions
  *  solution 1: in this branch: services.optout.OptOutRulesService
  *  solution 2: checkout git branch OptOutExperiment and review OutputSpecV14 file
  *  then fully integrate the chosen solution and remove solution-1 if not chosen
  * */
  val optOutOptions = new OptOutOptionsSolution1()
  //val optOutOutcome = new OptOutOptionsSolution2()

  implicit class BooleanOptionToFuture(opl: Option[Boolean]) {
    def toF: Future[Boolean] = opl
      .map(v => Future.successful(v))
      .getOrElse(Future.successful(false))
  }
  implicit class TypeToFuture[T](t: T) {
    def toF: Future[T] = Future.successful(t)
  }

  def displayOptOutMessage()(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[OptOutMessageResponse] = {

    val endYear = dateService.getCurrentTaxYearEnd

    val currentYear = TaxYear(endYear)
    val previousYear = currentYear.addYears(-1)
    val nextYear = currentYear.addYears(+1)

    val taxYearITSAStatus: Future[Map[TaxYear, StatusDetail]] = itsaStatusService.getStatusTillAvailableFutureYears(previousYear)
    val finalisedStatus: Future[Option[Boolean]] = calculationListService.isTaxYearCrystallised(previousYear.endYear)

    for {
      statusMap <- taxYearITSAStatus
      finalisedStatus <- finalisedStatus
      finalisedStatusBool <- finalisedStatus.toF
      outcomeOptionsResponse <- optOutOptions.getOptOutOptionsFor(finalisedStatusBool,
        YearStatusDetail(previousYear, statusMap(previousYear)),
        YearStatusDetail(currentYear, statusMap(currentYear)),
        YearStatusDetail(nextYear, statusMap(nextYear))
      ).toF
    } yield outcomeOptionsResponse
  }
}

trait OptOutOptions {
  def getOptOutOptionsFor(finalisedStatus: Boolean,
                          previousYearState: YearStatusDetail,
                          currentYearState: YearStatusDetail,
                          nextYearState: YearStatusDetail ): OptOutMessageResponse
}

/* todo: to be refactored */
class OptOutOptionsSolution1 extends OptOutOptions {

  val optOutRulesService = new OptOutRulesService()

  def getOptOutOptionsFor(finalisedStatus: Boolean,
                          previousYearState: YearStatusDetail,
                          currentYearState: YearStatusDetail,
                          nextYearState: YearStatusDetail): OptOutMessageResponse = {

    val finalised = toFinalized(finalisedStatus)
    val pySymbol = toSymbol(previousYearState.statusDetail)
    val cySymbol = toSymbol(currentYearState.statusDetail)
    val nySymbol = toSymbol(nextYearState.statusDetail)

    val response = optOutRulesService.findOptOutOptions(toQuery(finalised, pySymbol, cySymbol, nySymbol)).map {
      case "PY" => OptOutMessageResponse(canOptOut = true, taxYears = Array(previousYearState.taxYear))
      case "CY" => OptOutMessageResponse(canOptOut = true, taxYears = Array(currentYearState.taxYear))
      case "NY" => OptOutMessageResponse(canOptOut = true, taxYears = Array(nextYearState.taxYear))
    } reduce { (l, r) =>
      OptOutMessageResponse(canOptOut = true, taxYears = Array.concat(l.taxYears, r.taxYears))
    }

    response.copy(taxYears = response.taxYears.sortBy(_.startYear))

  }
}
