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

package returns.services

import common.auth.MtdItUser
import common.config.FrontendAppConfig
import common.models.core.Nino
import common.models.incomeSourceDetails.TaxYear
import common.services.DateServiceInterface
import returns.models.{FinancialDetailsErrorModel, FinancialDetailsModel, FinancialDetailsResponseModel}
import play.api.Logger
import play.api.http.Status.NOT_FOUND
import returns.connectors.GetFinancialDetailsConnector
import shared.connectors.CalculationListConnector
import shared.models.calculationList.{CalculationListErrorModel, CalculationListModel}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FinancialDetailsService @Inject()(val financialDetailsConnector: GetFinancialDetailsConnector,
                                        val calculationListConnector: CalculationListConnector,
                                        implicit val dateService: DateServiceInterface)
                                       (implicit val appConfig: FrontendAppConfig, ec: ExecutionContext) {

  def getFinancialDetails(taxYear: Int, nino: String)(implicit hc: HeaderCarrier, mtdItUser: MtdItUser[_]): Future[FinancialDetailsResponseModel] = {
    financialDetailsConnector.getFinancialDetails(taxYear, nino)
  }

  def getPoaTaxYearForEntryPoint(nino: Nino)
                                (implicit hc: HeaderCarrier, user: MtdItUser[_]): Future[Either[Throwable, Option[TaxYear]]] = {
    val validTaxYearsWithPoas = getPoaAdjustableTaxYears.foldLeft[Future[Either[Exception, List[TaxYear]]]](Future.successful(Right(List.empty))) { (acc, item) =>
      financialDetailsConnector.getFinancialDetails(item.endYear, nino.value).flatMap {
        case financialDetails: FinancialDetailsModel if financialDetails.arePoaPaymentsPresent().isDefined => acc.map {
          case Left(error) => Left(error)
          case Right(yearsList) => Right(yearsList :+ item)
        }
        case error: FinancialDetailsErrorModel if error.code != NOT_FOUND => Future.successful(Left(new Exception("There was an error whilst fetching financial details data")))
        case _ => acc
      }
    } //this code produces either a Future[Left[Error]] if there was an error getting the finDetails, or a List (of 0-2) valid tax years with POAs
    validTaxYearsWithPoas.flatMap {
      case Left(error) => Future.successful(Left(error))
      case Right(taxYearsList) => checkCrystallisation(nino, taxYearsList)(hc, dateService, user, calculationListConnector, ec).map {
        case None => Right(None)
        case Some(taxYear: TaxYear) => Right(Some(taxYear))
      }
    }
  }

  protected def checkCrystallisation(nino: Nino, taxYearList: List[TaxYear])
                                    (implicit hc: HeaderCarrier, dateService: DateServiceInterface, user: MtdItUser[_],
                                     calculationListConnector: CalculationListConnector, ec: ExecutionContext): Future[Option[TaxYear]] = {
    taxYearList.foldLeft(Future.successful(Option.empty[TaxYear])) { (acc, item) =>
      acc.flatMap {
        case Some(_) => acc
        case None => isTaxYearNonCrystallised(item, nino)(hc, dateService, user, calculationListConnector, ec) map {
          case true => Some(item)
          case false => None
        }
      }
    }
  }

  protected def getPoaAdjustableTaxYears(implicit dateService: DateServiceInterface): List[TaxYear] = {
    if (dateService.isAfterTaxReturnDeadlineButBeforeTaxYearEnd) {
      List(
        TaxYear.makeTaxYearWithEndYear(dateService.getCurrentTaxYearEnd)
      )
    } else {
      List(
        TaxYear.makeTaxYearWithEndYear(dateService.getCurrentTaxYearEnd).addYears(-1),
        TaxYear.makeTaxYearWithEndYear(dateService.getCurrentTaxYearEnd)
      ).sortBy(_.endYear)
    }
  }

  protected def isTaxYearNonCrystallised(taxYear: TaxYear, nino: Nino)
                                        (implicit hc: HeaderCarrier, dateService: DateServiceInterface, user: MtdItUser[_],
                                         calculationListConnector: CalculationListConnector, ec: ExecutionContext): Future[Boolean] = {
    if (taxYear.isFutureTaxYear(dateService)) {
      Future.successful(true)
    } else {
      calculationListConnector.getCalculationList(nino, taxYear.endYear.toString, user.mtditid).flatMap {
        case res: CalculationListModel => Future.successful(res.crystallised.getOrElse(false))
        case err: CalculationListErrorModel if err.code == 404 =>
          Logger("application").info("User had no calculations for this tax year, therefore is non-crystallised")
          Future.successful(false)
        case err: CalculationListErrorModel =>
          Logger("application").error("getCalculationList returned a non-valid response")
          Future.failed(new InternalServerException(err.message))
      }.map(!_)
    }
  }
}