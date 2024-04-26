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
import models.optOut.{NextUpdatesQuarterlyReportingContentChecks, OptOutMessageResponse}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class OptOutService_MISUV_7542 @Inject()(itsaStatusService: ITSAStatusService,
                                         calculationListService: CalculationListService,
                                         dateService: DateService)
                                        (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext) {

  /*

  Query: N,V,E,E - Y,V,E,E -> PY

  * N,V,E,E,PY
  * .,V,E,E,PY
  * N,V,E,E,PY
  * N,V,E,E,PY
  * N,V,E,E,PY
  * N,V,E,E,PY
  *
  * */

  def displayOptOutMessage(): Future[OptOutMessageResponse] = {

    val endYear = dateService.getCurrentTaxYearEnd

    val currentYear = TaxYear(endYear)
    val previousYear = currentYear.addYears(-1)
    val nextYear = currentYear.addYears(+1)


    val taxYearITSAStatus: Future[Map[TaxYear, StatusDetail]] = itsaStatusService.getStatusTillAvailableFutureYears(previousYear)
    val previousYearCalcStatus: Future[Option[Boolean]] = calculationListService.isTaxYearCrystallised(previousYear.endYear)

    for {
      statusMap <- taxYearITSAStatus

      isCurrentYearVoluntary = statusMap(currentYear).isVoluntary
      isPreviousYearVoluntary = statusMap(previousYear).isVoluntary
      isNextYearVoluntary = statusMap(nextYear).isVoluntary



      calStatus <- previousYearCalcStatus
//      optOutChecks = OptOutMessageResponse(
//        isCurrentYearVoluntary,
//        isPreviousYearVoluntary,
//        isNextYearVoluntary,
//        calStatus)
    } yield ???

    Future.successful(OptOutMessageResponse())
  }

}
