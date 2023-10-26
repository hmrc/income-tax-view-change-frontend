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

package connectors

import audit.AuditingService
import audit.models._
import auth.{MtdItUser, MtdItUserWithNino}
import config.FrontendAppConfig
import config.featureswitch.FeatureSwitching
import models.calculationList.{CalculationListErrorModel, CalculationListModel, CalculationListResponseModel}
import models.chargeHistory._
import models.core.{Nino, NinoResponse, NinoResponseError, NinoResponseSuccess}
import models.financialDetails._
import models.incomeSourceDetails.{IncomeSourceDetailsError, IncomeSourceDetailsModel, IncomeSourceDetailsResponse}
import models.itsaStatus.{ITSAStatusResponse, ITSAStatusResponseError, ITSAStatusResponseModel}
import models.nextUpdates.{NextUpdatesErrorModel, NextUpdatesResponseModel, ObligationsModel}
import models.outstandingCharges._
import models.paymentAllocationCharges.{FinancialDetailsWithDocumentDetailsErrorModel, FinancialDetailsWithDocumentDetailsModel, FinancialDetailsWithDocumentDetailsResponse}
import models.paymentAllocations.{PaymentAllocations, PaymentAllocationsError, PaymentAllocationsResponse}
import models.repaymentHistory.{RepaymentHistoryErrorModel, RepaymentHistoryModel, RepaymentHistoryResponseModel}
import models.updateIncomeSource._
import play.api.Logger
import play.api.http.Status
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import utils.Headers.checkAndAddTestHeader

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IncomeTaxViewChangeConnectorImpl @Inject()(val http: HttpClient,
                                                 val auditingService: AuditingService,
                                                 val appConfig: FrontendAppConfig
                                                )(implicit val ec: ExecutionContext) extends IncomeTaxViewChangeConnector

trait IncomeTaxViewChangeConnector extends RawResponseReads with FeatureSwitching {

  val http: HttpClient
  val auditingService: AuditingService
  val appConfig: FrontendAppConfig
  implicit val ec: ExecutionContext











}
