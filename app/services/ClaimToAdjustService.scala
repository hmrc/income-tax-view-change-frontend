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

import connectors.{CalculationListConnector, FinancialDetailsConnector}
import exceptions.MissingFieldException
import models.calculationList.{CalculationListErrorModel, CalculationListModel}
import models.core.Nino
import models.financialDetails.{DocumentDetail, FinancialDetailsErrorModel, FinancialDetailsModel}
import models.incomeSourceDetails.TaxYear
import models.incomeSourceDetails.TaxYear.makeTaxYearWithEndYear
import models.claimToAdjustPOA.PaymentOnAccountViewModel
import play.api.Logger
import play.api.http.Status.NOT_FOUND
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

import java.time.{LocalDate, Month}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ClaimToAdjustService @Inject()(val financialDetailsConnector: FinancialDetailsConnector,
                                     val calculationListConnector: CalculationListConnector,
                                     implicit val dateService: DateServiceInterface)
                                    (implicit ec: ExecutionContext) extends ClaimToAdjustHelper {

  def getPoaTaxYearForEntryPoint(nino: Nino)(implicit hc: HeaderCarrier): Future[Either[Throwable, Option[TaxYear]]] = {
    for {
      res <- getPoaForNonCrystallisedFinancialDetails(nino)
    } yield res match {
      case Right(Some(financialDetails)) =>
        val x = arePoAPaymentsPresent(financialDetails.documentDetails)
        Right(x)
      case Right(None) => Right(None)
      case Left(ex) =>
        Logger("application").error(s"[ClaimToAdjustService][getPoaTaxYearForEntryPoint] There was an error getting FinancialDetailsModel" +
          s" < cause: ${ex.getCause} message: ${ex.getMessage} >")
        Left(ex)
    }
  }

  def getPoaForNonCrystallisedTaxYear(nino: Nino)(implicit hc: HeaderCarrier): Future[Either[Throwable, Option[PaymentOnAccountViewModel]]] = {
    for {
      res <- getPoaForNonCrystallisedFinancialDetails(nino)
    } yield res match {
      case Right(Some(financialDetails)) =>
        val x = getPaymentOnAccountModel(financialDetails.documentDetails)
        Right(x)
      case Right(None) => Right(None)
      case Left(ex) => Left(ex)
    }
  }

  private def getPoaForNonCrystallisedFinancialDetails(nino: Nino)(implicit hc: HeaderCarrier): Future[Either[Throwable, Option[FinancialDetailsModel]]] = {
    checkCrystallisation(nino, getPoaAdjustableTaxYears)(hc, dateService, calculationListConnector, ec).flatMap {
      case None => Future.successful(Right(None))
      case Some(taxYear: TaxYear) => financialDetailsConnector.getFinancialDetails(taxYear.endYear, nino.value).map {
        case financialDetails: FinancialDetailsModel => Right(Some(financialDetails))
        case error: FinancialDetailsErrorModel if error.code != NOT_FOUND => Left(new Exception("There was an error whilst fetching financial details data"))
        case _ => Right(None)
      }
    }
  }

}