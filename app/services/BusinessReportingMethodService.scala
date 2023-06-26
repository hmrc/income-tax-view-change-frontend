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

import auth.MtdItUser
import config.FrontendAppConfig
import config.featureswitch.{FeatureSwitching, TimeMachineAddYear}
import connectors.IncomeTaxViewChangeConnector
import forms.incomeSources.add.AddBusinessReportingMethodForm
import models.incomeSourceDetails.viewmodels.BusinessReportingMethodViewModel
import models.incomeSourceDetails.{IncomeSourceDetailsError, IncomeSourceDetailsModel, LatencyDetails}
import models.itsaStatus.ITSAStatusResponseError
import models.updateIncomeSource.{TaxYearSpecific, UpdateIncomeSourceResponse}
import play.api.Logger
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BusinessReportingMethodService @Inject()(incomeTaxViewChangeConnector: IncomeTaxViewChangeConnector,
                                               dateService: DateService,
                                               implicit val appConfig: FrontendAppConfig) extends FeatureSwitching {
  private val validStatus = List("MTD Mandated", "MTD Voluntary")

  def checkITSAStatusCurrentYear(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    val yearEnd = dateService.getCurrentTaxYearEnd(isEnabled(TimeMachineAddYear)).toString.substring(2).toInt
    val yearStart = yearEnd - 1
    incomeTaxViewChangeConnector.getITSAStatusDetail(
      nino = user.nino,
      taxYear = s"$yearStart-$yearEnd",
      futureYears = false,
      history = false).flatMap {
      case Right(listStatus) =>
        Future.successful(listStatus.headOption.flatMap(_.itsaStatusDetails).flatMap(_.headOption).map(x => validStatus.contains(x.status)).get)
      case Left(x: ITSAStatusResponseError) =>
        Logger("application").error(s"[BusinessReportingMethodService][checkITSAStatusCurrentYear] $x")
        Future.failed(new InternalServerException("[BusinessReportingMethodService][checkITSAStatusCurrentYear] - Failed to retrieve ITSAStatus"))
    }
  }

  def isTaxYearCrystallised(ty: Int): Boolean = true

  def getBusinessReportingMethodDetails(incomeSourceId: String)(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Option[BusinessReportingMethodViewModel]] = {
    incomeTaxViewChangeConnector.getBusinessDetails(user.nino).flatMap {
      case value: IncomeSourceDetailsModel =>
        val latencyDetails: Option[LatencyDetails] = value.businesses.find(_.incomeSourceId.getOrElse("").equals(incomeSourceId)).flatMap(_.latencyDetails)
        latencyDetails match {
          case Some(x) =>
            val currentTaxYearEnd = dateService.getCurrentTaxYearEnd(isEnabled(TimeMachineAddYear))
            Future.successful(x match {
              case LatencyDetails(_, _, _, ty2, _) if ty2.toInt < currentTaxYearEnd => None
              case LatencyDetails(_, ty1, _, ty2, ty2i) if isTaxYearCrystallised(ty1.toInt) => Some(BusinessReportingMethodViewModel(None, None, Some(ty2.toInt), Some(ty2i)))
              case LatencyDetails(_, ty1, ty1i, ty2, ty2i) if !isTaxYearCrystallised(ty1.toInt) => Some(BusinessReportingMethodViewModel(Some(ty1.toInt), Some(ty1i), Some(ty2.toInt), Some(ty2i)))
            })
          case None =>
            Logger("application").info(s"[BusinessReportingMethodService][getBusinessReportingMethodDetails] latency details not available")
            Future.successful(None)
        }
      case err: IncomeSourceDetailsError =>
        Logger("application").error(s"[BusinessReportingMethodService][getBusinessReportingMethodDetails] $err")
        Future.failed(new InternalServerException("[BusinessReportingMethodService][getBusinessReportingMethodDetails] - Failed to retrieve IncomeSourceDetails"))
    }
  }
  private def annualQuarterlyToBoolean(method:Option[String]):Option[Boolean] = method match {
    case Some("A") => Some(true)
    case Some("Q") => Some(false)
    case _ => None

  }
  def updateIncomeSourceTaxYearSpecific(nino: String, incomeSourceId: String, reportingMethod: AddBusinessReportingMethodForm)(implicit hc: HeaderCarrier, ec:ExecutionContext): Future[Option[UpdateIncomeSourceResponse]] = {

    val ty1ReportingMethod = reportingMethod.taxYear1ReportingMethod
    val ty2ReportingMethod = reportingMethod.taxYear2ReportingMethod
    val newTy1ReportingMethod = reportingMethod.newTaxYear1ReportingMethod
    val newTy2ReportingMethod = reportingMethod.newTaxYear2ReportingMethod

    if (ty1ReportingMethod != newTy1ReportingMethod || ty2ReportingMethod != newTy2ReportingMethod) {
      val ty1 = newTy1ReportingMethod match {
        case Some(s) => Some(TaxYearSpecific(reportingMethod.taxYear1.get, annualQuarterlyToBoolean(Some(s)).get))
        case _ => None
      }

      val ty2 = newTy2ReportingMethod match {
        case Some(s) => Some(TaxYearSpecific(reportingMethod.taxYear2.get, annualQuarterlyToBoolean(Some(s)).get))
        case _ => None
      }

      incomeTaxViewChangeConnector.updateIncomeSourceTaxYearSpecific(nino = nino,
        incomeSourceId = incomeSourceId,
        taxYearSpecific = List(ty1, ty2).flatten).map(Some(_))

    } else {
      Future.successful(None)
    }
  }

}

