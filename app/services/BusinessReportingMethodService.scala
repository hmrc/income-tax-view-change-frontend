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

package services

import connectors.IncomeTaxViewChangeConnector
import forms.incomeSources.add.AddBusinessReportingMethodForm
import models.incomeSourceDetails.viewmodels.BusinessReportingMethodViewModel
import models.itsaStatus.ITSAStatusResponseError
import models.updateIncomeSource.{TaxYearSpecific, UpdateIncomeSourceResponse}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BusinessReportingMethodService @Inject()(incomeTaxViewChangeConnector: IncomeTaxViewChangeConnector,
                                               dateService: DateService) {
  private val validStatus = List("MTD Mandated", "MTD Voluntary")

  def checkITSAStatusCurrentYear(nino: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[ITSAStatusResponseError, Boolean]] = {
    val yearEnd = dateService.getCurrentTaxYearEnd().toString.substring(2).toInt
    val yearStart = yearEnd - 1
    incomeTaxViewChangeConnector.getITSAStatusDetail(
      nino = nino,
      taxYear = s"$yearStart-$yearEnd",
      futureYears = false,
      history = false).map {
      case Right(listStatus) =>
        Right(listStatus.headOption.flatMap(_.itsaStatusDetails).flatMap(_.headOption).map(x => validStatus.contains(x.status)).get)
      case Left(x: ITSAStatusResponseError) => Left(x)
    }
  }

  def getBusinessReportingMethodDetails(): BusinessReportingMethodViewModel = {
    /*
      TODO S0:   1. Check Latency details for new Income source
                 2. if latency details empty redirect to business-added page else cover below scenarios

      or

      TODO S1:  1. Check current year is in TY1 from Latency
                2. Show Radio for TY1 & TY2

     or

      TODO S2a: 1. Check current year is in TY2 and TY1 is *not Crystallised (no final Declaration API#1404/API#1896)
                2. Show Radio for TY1 & TY2

      or

      TODO S2b: 1. Check current year is in TY2 and TY1 is *Crystallised ( has final Declaration API#1404/API#1896)
                2. Show Radio TY2

      or

      TODO S3:  1. If I am beyond latency period i.e I am in S0

    */
    BusinessReportingMethodViewModel(Some(2022), Some(2023))
    //BusinessReportingMethodViewModel(None, Some(2023))
    //BusinessReportingMethodViewModel(Some(2022), None)
    //BusinessReportingMethodViewModel(None, None)
  }

  def updateIncomeSourceTaxYearSpecific(nino: String, incomeSourceId: String, taxYearSpecific: AddBusinessReportingMethodForm)(implicit hc: HeaderCarrier): Future[UpdateIncomeSourceResponse] = {
    val ty1 = taxYearSpecific.taxYearReporting1 match {
      case Some(s) => Some(TaxYearSpecific(taxYearSpecific.taxYear1.get, s.toBoolean))
      case _ => None
    }
    val ty2 = taxYearSpecific.taxYearReporting2 match {
      case Some(s) => Some(TaxYearSpecific(taxYearSpecific.taxYear2.get, s.toBoolean))
      case _ => None
    }
    incomeTaxViewChangeConnector.updateIncomeSourceTaxYearSpecific(nino = nino,
      incomeSourceId = incomeSourceId,
      taxYearSpecific = List(ty1, ty2).flatten)
  }
}

