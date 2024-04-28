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
import services.optout.OptOutRulesService.{toFinalized, toQuery, toSymbol}
import models.optOut.OptOutMessageResponse
import services.optout.OptOutRulesService
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class OptOutService2 @Inject()(itsaStatusService: ITSAStatusService,
                               calculationListService: CalculationListService,
                               dateService: DateService)
                              (implicit ec: ExecutionContext) {

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

  def displayOptOutMessage()(implicit user: MtdItUser[_], hc: HeaderCarrier): Future[OptOutMessageResponse] = {

    val endYear = dateService.getCurrentTaxYearEnd

    val currentYear = TaxYear(endYear)
    val previousYear = currentYear.addYears(-1)
    val nextYear = currentYear.addYears(+1)

    /* todo: get the api call working to return good data */

//    val getStatusTillAvailableFutureYearsStub = Future.successful({
//      Map(
//        TaxYear(2023, 2024) -> StatusDetail("submittedOn", ITSAStatus.Voluntary, "statusReason", Some(BigDecimal.valueOf(10))),
//        TaxYear(2024, 2025) -> StatusDetail("submittedOn", ITSAStatus.NoStatus, "statusReason", Some(BigDecimal.valueOf(10))),
//        TaxYear(2025, 2026) -> StatusDetail("submittedOn", ITSAStatus.NoStatus, "statusReason", Some(BigDecimal.valueOf(10))),
//      )
//    })
    val taxYearITSAStatus: Future[Map[TaxYear, StatusDetail]] = itsaStatusService.getStatusTillAvailableFutureYears(previousYear)

//  val isTaxYearCrystallisedStub = Future.successful(Some(false))
    val finalisedStatus: Future[Option[Boolean]] = calculationListService.isTaxYearCrystallised(previousYear.endYear)

    for {

      statusMap <- taxYearITSAStatus
      finalisedStatus <- finalisedStatus
      finalisedStatusBool <- finalisedStatus.toF

      outcomeOptions = optOutOptions.getOptOutOptionsFor(finalisedStatusBool,
        statusMap(previousYear), statusMap(currentYear), statusMap(nextYear))

      response = OptOutMessageResponse(outcomeOptions.nonEmpty)
    } yield response
  }


}

trait OptOutOptions {
  def getOptOutOptionsFor(finalisedStatus: Boolean,
                          previousYearState: StatusDetail,
                          currentYearState: StatusDetail,
                          nextYearState: StatusDetail ): Set[String]
}

/* todo: to be refactored */
class OptOutOptionsSolution1 extends OptOutOptions {

  val optOutRulesService = new OptOutRulesService()

  def getOptOutOptionsFor(finalisedStatus: Boolean,
                          previousYearState: StatusDetail,
                          currentYearState: StatusDetail,
                          nextYearState: StatusDetail ): Set[String] = {

    val finalised = toFinalized(finalisedStatus)
    val pySymbol = toSymbol(previousYearState)
    val cySymbol = toSymbol(currentYearState)
    val nySymbol = toSymbol(nextYearState)
    val optOutOptions = optOutRulesService.findOptOutOptions(toQuery(finalised, pySymbol, cySymbol, nySymbol))

    optOutOptions
  }
}