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
import models.optOut.{NextUpdatesQuarterlyReportingContentChecks, OptOutOneYearViewModel}
import services.{CalculationListService, DateServiceInterface, ITSAStatusService}
import OptOutService._
import connectors.OptOutConnector
import models.optOut.OptOutUpdateRequestModel.{OptOutUpdateRequest, OptOutUpdateResponse}
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}


object OptOutService {
  implicit class TypeToFuture[T](t: T) {
    def toF: Future[T] = Future.successful(t)
  }
}

@Singleton
class OptOutService @Inject()(optOutConnector: OptOutConnector,
                              itsaStatusService: ITSAStatusService,
                              calculationListService: CalculationListService,
                              dateService: DateServiceInterface) {

  def getNextUpdatesQuarterlyReportingContentChecks(implicit user: MtdItUser[_],
                                                    hc: HeaderCarrier,
                                                    ec: ExecutionContext): Future[NextUpdatesQuarterlyReportingContentChecks] = {
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

  private def nextUpdatesPageOneYear(optOutData: OptOutData, optOutYear: OptOut): OptOutOneYearViewModel = {
    OptOutOneYearViewModel(optOutYear.taxYear)
  }

  def displayOptOutMessage()(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Option[OptOutOneYearViewModel]] = {

    val processSteps = for {
      optOutData <- setupOptOutData()
    }
    yield optOutData.optOutForSingleYear(nextUpdatesPageOneYear)

    processSteps recover {
      case e =>
        Logger("application").error(s"trying to get opt-out status but failed with message: ${e.getMessage}")
        None
    }
  }


  private def setupOptOutData()(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[OptOutData] = {

    val currentYear  = dateService.getCurrentTaxYear
    val previousYear = currentYear.previousYear
    val nextYear     = currentYear.nextYear

    for {
      finalisedStatus <- calculationListService.isTaxYearCrystallised(previousYear)
      statusMap <- itsaStatusService.getStatusTillAvailableFutureYears(previousYear)
    }
    yield createOptOutData(previousYear, currentYear, nextYear, finalisedStatus, statusMap)
  }

  private def createOptOutData(previousYear: TaxYear, currentYear: TaxYear, nextYear: TaxYear, finalisedStatus: Boolean, statusMap: Map[TaxYear, StatusDetail]) = {

    val previousYearOptOut = PreviousTaxYearOptOut(statusMap(previousYear).status, previousYear, finalisedStatus)
    val currentTaxYearOptOut = CurrentTaxYearOptOut(statusMap(currentYear).status, currentYear)
    val nextTaxYearOptOut = NextTaxYearOptOut(statusMap(nextYear).status, nextYear, currentTaxYearOptOut)

    OptOutData(previousYearOptOut, currentTaxYearOptOut, nextTaxYearOptOut)
  }

  def makeOptOutUpdateRequestForYear(taxYear: TaxYear)(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[OptOutUpdateResponse] = {
    optOutConnector.requestOptOutForTaxYear(taxYear, user.nino)
  }
}
