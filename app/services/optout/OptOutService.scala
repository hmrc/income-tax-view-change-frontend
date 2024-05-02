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

package services.optout

import auth.MtdItUser
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.StatusDetail
import models.optOut.{NextUpdatesQuarterlyReportingContentChecks, OptOutMessageResponse, YearStatusDetail}
import services.optout.OptOutService.BooleanOptionToFuture
import services.{CalculationListService, DateServiceInterface, ITSAStatusService}
import OptOutService._
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}


object OptOutService {
  val optOutOptions = new OptOutOptionsTacticalSolution
  implicit class BooleanOptionToFuture(opl: Option[Boolean]) {
    def toF: Future[Boolean] = opl
      .map(v => Future.successful(v))
      .getOrElse(Future.successful(false))
  }
  implicit class TypeToFuture[T](t: T) {
    def toF: Future[T] = Future.successful(t)
  }
}

@Singleton
class OptOutService @Inject()(itsaStatusService: ITSAStatusService, calculationListService: CalculationListService, dateService: DateServiceInterface) {

  def getNextUpdatesQuarterlyReportingContentChecks(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[NextUpdatesQuarterlyReportingContentChecks] = {
    val yearEnd = dateService.getCurrentTaxYearEnd
    val currentYear = TaxYear.forYearEnd(yearEnd)
    val previousYear = currentYear.previousYear

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

  def displayOptOutMessage()(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[OptOutMessageResponse] = {

    val processSteps = for {
      yearEnd <- dateService.getCurrentTaxYearEnd.toF
      currentYear <- TaxYear.forYearEnd(yearEnd).toF
      previousYear <- currentYear.previousYear.toF
      nextYear <- currentYear.nextYear.toF
      finalisedStatus <- calculationListService.isTaxYearCrystallised(previousYear.endYear)
      statusMap <- itsaStatusService.getStatusTillAvailableFutureYears(previousYear)
      finalisedStatusBool <- finalisedStatus.toF
      outcomeOptionsResponse <- optOutOptions.getOptOutOptionsFor(finalisedStatusBool,
        YearStatusDetail(previousYear, statusMap(previousYear)),
        YearStatusDetail(currentYear, statusMap(currentYear)),
        YearStatusDetail(nextYear, statusMap(nextYear))
      ).toF
    } yield outcomeOptionsResponse

    processSteps recover {
      case e =>
        Logger("application").error(s"trying to get opt-out status but failed with message: ${e.getMessage}")
        OptOutMessageResponse()
    }
  }
}

