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
import models.updateIncomeSource.{Cessation, TaxYearSpecific, UpdateIncomeSourceRequestModel, UpdateIncomeSourceResponse, UpdateIncomeSourceResponseError, UpdateIncomeSourceResponseModel}
import play.api.Logger
import play.api.http.Status
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

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

  def getBusinessDetailsUrl(nino: String): String = {
    s"${appConfig.itvcProtectedService}/income-tax-view-change/get-business-details/nino/$nino"
  }

  def getIncomeSourcesUrl(mtditid: String): String = {
    s"${appConfig.itvcProtectedService}/income-tax-view-change/income-sources/$mtditid"
  }

  def getNinoLookupUrl(mtdRef: String): String = {
    s"${appConfig.itvcProtectedService}/income-tax-view-change/nino-lookup/$mtdRef"
  }

  def getReportDeadlinesUrl(nino: String): String = {
    s"${appConfig.itvcProtectedService}/income-tax-view-change/$nino/report-deadlines"
  }

  def getPreviousObligationsUrl(nino: String): String = {
    s"${appConfig.itvcProtectedService}/income-tax-view-change/$nino/fulfilled-report-deadlines"
  }

  def getPaymentAllocationsUrl(nino: String, paymentLot: String, paymentLotItem: String): String = {
    s"${appConfig.itvcProtectedService}/income-tax-view-change/$nino/payment-allocations/$paymentLot/$paymentLotItem"
  }

  def getPreviousObligationsUrl(fromDate: LocalDate, toDate: LocalDate, nino: String): String = {
    s"${appConfig.itvcProtectedService}/income-tax-view-change/$nino/fulfilled-report-deadlines/from/$fromDate/to/$toDate"
  }

  def getChargesUrl(nino: String, from: String, to: String): String = {
    s"${appConfig.itvcProtectedService}/income-tax-view-change/$nino/financial-details/charges/from/$from/to/$to"
  }

  def getOutstandingChargesUrl(idType: String, idNumber: String, taxYear: String): String = {
    s"${appConfig.itvcProtectedService}/income-tax-view-change/out-standing-charges/$idType/$idNumber/$taxYear"
  }

  def getChargeHistoryUrl(mtdBsa: String, docNumber: String): String = {
    s"${appConfig.itvcProtectedService}/income-tax-view-change/charge-history/$mtdBsa/docId/$docNumber"
  }

  def getPaymentsUrl(nino: String, from: String, to: String): String = {
    s"${appConfig.itvcProtectedService}/income-tax-view-change/$nino/financial-details/payments/from/$from/to/$to"
  }

  def getFinancialDetailsByDocumentIdUrl(nino: String, documentNumber: String): String = {
    s"${appConfig.itvcProtectedService}/income-tax-view-change/$nino/financial-details/charges/documentId/$documentNumber"
  }

  def getRepaymentHistoryByIdUrl(nino: String, repaymentId: String): String = {
    s"${appConfig.itvcProtectedService}/income-tax-view-change/repayments/$nino/repaymentId/$repaymentId"
  }

  def getAllRepaymentHistoryUrl(nino: String): String = {
    s"${appConfig.itvcProtectedService}/income-tax-view-change/repayments/$nino"
  }

  def getUpdateIncomeSourceUrl: String = {
    s"${appConfig.itvcProtectedService}/income-tax-view-change/update-income-source"
  }

  def getITSAStatusDetailUrl(taxableEntityId: String, taxYear: String): String = {
    s"${appConfig.itvcProtectedService}/income-tax-view-change/itsa-status/status/$taxableEntityId/$taxYear"
  }

  def getBusinessDetails(nino: String)(implicit headerCarrier: HeaderCarrier): Future[IncomeSourceDetailsResponse] = {
    val url = getBusinessDetailsUrl(nino)
    Logger("application").debug(s"[IncomeTaxViewChangeConnector][getBusinessDetails] - GET $url")

    http.GET[HttpResponse](url) map { response =>
      response.status match {
        case OK =>
          Logger("application").debug(s"[IncomeTaxViewChangeConnector][getBusinessDetails] - RESPONSE status: ${response.status}, json: ${response.json}")
          response.json.validate[IncomeSourceDetailsModel].fold(
            invalid => {
              Logger("application").error(s"[IncomeTaxViewChangeConnector][getBusinessDetails] $invalid")
              IncomeSourceDetailsError(Status.INTERNAL_SERVER_ERROR, "Json Validation Error Parsing Income Source Details response")
            },
            valid => valid
          )
        case status =>
          if (status == 404) {
            Logger("application").error(s"[IncomeTaxViewChangeConnector][getBusinessDetails] - RESPONSE status: ${response.status}, body: ${response.body}")
          } else if (status >= 500) {
            Logger("application").error(s"[IncomeTaxViewChangeConnector][getBusinessDetails] - RESPONSE status: ${response.status}, body: ${response.body}")
          } else {
            Logger("application").warn(s"[IncomeTaxViewChangeConnector][getBusinessDetails] - RESPONSE status: ${response.status}, body: ${response.body}")
          }
          IncomeSourceDetailsError(response.status, response.body)
      }
    } recover {
      case ex =>
        Logger("application").error(s"[IncomeTaxViewChangeConnector][getBusinessDetails] - Unexpected future failed error, ${ex.getMessage}")
        IncomeSourceDetailsError(Status.INTERNAL_SERVER_ERROR, s"Unexpected future failed error, ${ex.getMessage}")
    }
  }

  def getIncomeSources()(
    implicit headerCarrier: HeaderCarrier, mtdItUser: MtdItUserWithNino[_]): Future[IncomeSourceDetailsResponse] = {

    val url = getIncomeSourcesUrl(mtdItUser.mtditid)
    Logger("application").debug(s"[IncomeTaxViewChangeConnector][getIncomeSources] - GET $url")

    http.GET[HttpResponse](url) map { response =>
      response.status match {
        case OK =>
          Logger("application").debug(s"[IncomeTaxViewChangeConnector][getIncomeSources] - RESPONSE status: ${response.status}, json: ${response.json}")
          response.json.validate[IncomeSourceDetailsModel].fold(
            invalid => {
              Logger("application").error(s"[IncomeTaxViewChangeConnector][getIncomeSources] $invalid")
              IncomeSourceDetailsError(Status.INTERNAL_SERVER_ERROR, "Json Validation Error Parsing Income Source Details response")
            },
            valid => {
              auditingService.extendedAudit(IncomeSourceDetailsResponseAuditModel(
                mtdItUser,
                valid.businesses.flatMap(_.incomeSourceId),
                valid.properties.flatMap(_.incomeSourceId),
                valid.yearOfMigration
              ))
              valid
            }
          )
        case status =>
          if (status >= 500) {
            Logger("application").error(s"[IncomeTaxViewChangeConnector][getIncomeSources] - RESPONSE status: ${response.status}, body: ${response.body}")
          } else {
            Logger("application").warn(s"[IncomeTaxViewChangeConnector][getIncomeSources] - RESPONSE status: ${response.status}, body: ${response.body}")
          }
          IncomeSourceDetailsError(response.status, response.body)
      }
    } recover {
      case ex =>
        Logger("application").error(s"[IncomeTaxViewChangeConnector][getIncomeSources] - Unexpected future failed error, ${ex.getMessage}")
        IncomeSourceDetailsError(Status.INTERNAL_SERVER_ERROR, s"Unexpected future failed error, ${ex.getMessage}")
    }
  }

  def getNino(mtdRef: String)(implicit headerCarrier: HeaderCarrier): Future[NinoResponse] = {

    val url = getNinoLookupUrl(mtdRef)
    Logger("application").debug(s"[IncomeTaxViewChangeConnector][getNino] - GET $url")

    http.GET[HttpResponse](url) map { response =>
      response.status match {
        case OK =>
          Logger("application").debug(s"[IncomeTaxViewChangeConnector][getNino] - RESPONSE status: ${response.status}, json: ${response.json}")
          response.json.validate[NinoResponseSuccess].fold(
            invalid => {
              Logger("application").error(s"[IncomeTaxViewChangeConnector][getNino] - Json Validation Error - $invalid")
              NinoResponseError(Status.INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing Nino Response")
            },
            valid => valid
          )
        case status =>
          if (status >= 500) {
            Logger("application").error(s"[IncomeTaxViewChangeConnector][getNino] - RESPONSE status: ${response.status}, body: ${response.body}")
          } else {
            Logger("application").warn(s"[IncomeTaxViewChangeConnector][getNino] - RESPONSE status: ${response.status}, body: ${response.body}")
          }
          NinoResponseError(response.status, response.body)
      }
    } recover {
      case ex =>
        Logger("application").error(s"[IncomeTaxViewChangeConnector][getNino] - Unexpected future failed error, ${ex.getMessage}")
        NinoResponseError(Status.INTERNAL_SERVER_ERROR, s"Unexpected future failed error, ${ex.getMessage}")
    }
  }

  def getNextUpdates()(implicit headerCarrier: HeaderCarrier, mtdUser: MtdItUser[_]): Future[NextUpdatesResponseModel] = {

    val url = getReportDeadlinesUrl(mtdUser.nino)
    Logger("application").debug(s"[IncomeTaxViewChangeConnector][getNextUpdates] - GET $url")

    http.GET[HttpResponse](url)(httpReads, headerCarrier, implicitly) map { response =>
      response.status match {
        case OK =>
          Logger("application").debug(s"[IncomeTaxViewChangeConnector][getNextUpdates] - RESPONSE status: ${response.status}, json: ${response.json}")
          response.json.validate[ObligationsModel].fold(
            invalid => {
              Logger("application").error(s"[IncomeTaxViewChangeConnector][getNextUpdates] - Json Validation Error: $invalid")
              NextUpdatesErrorModel(Status.INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing Next Updates Data Response")
            },
            valid => {
              valid.obligations.foreach { data =>
                auditingService.extendedAudit(NextUpdatesResponseAuditModel(mtdUser, data.identification, data.obligations))
              }
              valid
            }
          )
        case status =>
          if (status >= 500) {
            Logger("application").error(s"[IncomeTaxViewChangeConnector][getNextUpdates] - RESPONSE status: ${response.status}, body: ${response.body}")
          } else {
            Logger("application").warn(s"[IncomeTaxViewChangeConnector][getNextUpdates] - RESPONSE status: ${response.status}, body: ${response.body}")
          }
          NextUpdatesErrorModel(response.status, response.body)
      }
    } recover {
      case ex =>
        Logger("application").error(s"[IncomeTaxViewChangeConnector][getNextUpdates] - Unexpected future failed error, ${ex.getMessage}")
        NextUpdatesErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected future failed error, ${ex.getMessage}")
    }
  }

  def getPreviousObligations()(implicit headerCarrier: HeaderCarrier, mtdUser: MtdItUser[_]): Future[NextUpdatesResponseModel] = {

    val url = getPreviousObligationsUrl(mtdUser.nino)
    Logger("application").debug(s"[IncomeTaxViewChangeConnector][getPreviousObligations] - GET $url")

    http.GET[HttpResponse](url)(httpReads, headerCarrier, implicitly) map { response =>
      response.status match {
        case OK =>
          Logger("application").debug(s"[IncomeTaxViewChangeConnector][getPreviousObligations] - Status: ${response.status}, json: ${response.json}")
          response.json.validate[ObligationsModel].fold(
            invalid => {
              Logger("application").error(s"[IncomeTaxViewChangeConnector][getPreviousObligations] - Json Validation Error: $invalid")
              NextUpdatesErrorModel(Status.INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing Next Updates Data Response")
            },
            valid => {
              valid.obligations.foreach { data =>
                auditingService.extendedAudit(NextUpdatesResponseAuditModel(mtdUser, data.identification, data.obligations))
              }
              valid
            }
          )
        case status =>
          if (status >= 500) {
            Logger("application").error(s"[IncomeTaxViewChangeConnector][getPreviousObligations] - Status: ${response.status}, body: ${response.body}")
          } else {
            Logger("application").warn(s"[IncomeTaxViewChangeConnector][getPreviousObligations] - Status: ${response.status}, body: ${response.body}")
          }
          NextUpdatesErrorModel(response.status, response.body)
      }
    } recover {
      case ex =>
        Logger("application").error(s"[IncomeTaxViewChangeConnector][getPreviousObligations] - Unexpected failure, ${ex.getMessage}", ex)
        NextUpdatesErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected failure, ${ex.getMessage}")
    }

  }

  def getPreviousObligations(fromDate: LocalDate, toDate: LocalDate)
                            (implicit headerCarrier: HeaderCarrier, mtdUser: MtdItUser[_]): Future[NextUpdatesResponseModel] = {

    val url = getPreviousObligationsUrl(fromDate, toDate, mtdUser.nino)
    Logger("application").debug(s"[IncomeTaxViewChangeConnector][getPreviousObligations] - GET $url")

    http.GET[HttpResponse](url)(httpReads, headerCarrier, implicitly) map { response =>
      response.status match {
        case OK =>
          Logger("application").debug(s"[IncomeTaxViewChangeConnector][getPreviousObligations] - Status: ${response.status}, json: ${response.json}")
          response.json.validate[ObligationsModel].fold(
            invalid => {
              Logger("application").error(s"[IncomeTaxViewChangeConnector][getPreviousObligations] - Json Validation Error: $invalid")
              NextUpdatesErrorModel(Status.INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing Next Updates Data Response")
            },
            valid => {
              valid.obligations.foreach { data =>
                auditingService.extendedAudit(NextUpdatesResponseAuditModel(mtdUser, data.identification, data.obligations))
              }
              valid
            }
          )
        case status =>
          if (status >= 500) {
            Logger("application").error(s"[IncomeTaxViewChangeConnector][getPreviousObligations] - Status: ${response.status}, body: ${response.body}")
          } else {
            Logger("application").warn(s"[IncomeTaxViewChangeConnector][getPreviousObligations] - Status: ${response.status}, body: ${response.body}")
          }
          NextUpdatesErrorModel(response.status, response.body)
      }
    } recover {
      case ex =>
        Logger("application").error(s"[IncomeTaxViewChangeConnector][getPreviousObligations] - Unexpected failure, ${ex.getMessage}", ex)
        NextUpdatesErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected failure, ${ex.getMessage}")
    }

  }

  def getPaymentAllocations(nino: Nino, paymentLot: String, paymentLotItem: String)
                           (implicit headerCarrier: HeaderCarrier): Future[PaymentAllocationsResponse] = {

    val url = getPaymentAllocationsUrl(nino.value, paymentLot, paymentLotItem)
    Logger("application").debug(s"[IncomeTaxViewChangeConnector][getPaymentAllocations] - GET $url")

    http.GET[HttpResponse](url)(httpReads, headerCarrier, implicitly) map { response =>
      response.status match {
        case OK =>
          Logger("application").debug(s"[IncomeTaxViewChangeConnector][getPaymentAllocations] - Status: ${response.status}, json: ${response.json}")
          response.json.validate[PaymentAllocations].fold(
            invalid => {
              Logger("application").error(s"[IncomeTaxViewChangeConnector][getPaymentAllocations] - Json Validation Error: $invalid")
              PaymentAllocationsError(Status.INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing Payment Allocations Data Response")
            },
            valid => valid
          )
        case status =>
          if (status >= 500) {
            Logger("application").error(s"[IncomeTaxViewChangeConnector][getPaymentAllocations] - Status: ${response.status}, body: ${response.body}")
          } else {
            Logger("application").warn(s"[IncomeTaxViewChangeConnector][getPaymentAllocations] - Status: ${response.status}, body: ${response.body}")
          }
          PaymentAllocationsError(response.status, response.body)
      }
    } recover {
      case ex =>
        Logger("application").error(s"[IncomeTaxViewChangeConnector][getPaymentAllocations] - Unexpected failure, ${ex.getMessage}", ex)
        PaymentAllocationsError(Status.INTERNAL_SERVER_ERROR, s"Unexpected failure, ${ex.getMessage}")
    }

  }

  // TODO: MFA Credits
  def getFinancialDetails(taxYear: Int, nino: String)
                         (implicit headerCarrier: HeaderCarrier): Future[FinancialDetailsResponseModel] = {

    val dateFrom: String = (taxYear - 1).toString + "-04-06"
    val dateTo: String = taxYear.toString + "-04-05"

    val url = getChargesUrl(nino, dateFrom, dateTo)
    Logger("application").debug(s"[IncomeTaxViewChangeConnector][getFinancialDetails] - GET $url")

    http.GET[HttpResponse](url)(httpReads, headerCarrier, implicitly) map { response =>
      response.status match {
        case OK =>
          Logger("application").debug(s"[IncomeTaxViewChangeConnector][getFinancialDetails] - Status: ${response.status}, json: ${response.json}")
          response.json.validate[FinancialDetailsModel].fold(
            invalid => {
              Logger("application").error(s"[IncomeTaxViewChangeConnector][getFinancialDetails] - Json Validation Error: $invalid")
              FinancialDetailsErrorModel(Status.INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing FinancialDetails Data Response")
            },
            valid => valid
          )
        case status =>
          if (status >= 500) {
            Logger("application").error(s"[IncomeTaxViewChangeConnector][getFinancialDetails] - Status: ${response.status}, body: ${response.body}")
          } else {
            Logger("application").warn(s"[IncomeTaxViewChangeConnector][getFinancialDetails] - Status: ${response.status}, body: ${response.body}")
          }
          FinancialDetailsErrorModel(response.status, response.body)
      }
    } recover {
      case ex =>
        Logger("application").error(s"[IncomeTaxViewChangeConnector][getFinancialDetails] - Unexpected failure, ${ex.getMessage}", ex)
        FinancialDetailsErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected failure, ${ex.getMessage}")
    }

  }

  def getOutstandingCharges(idType: String, idNumber: String, taxYear: String)
                           (implicit headerCarrier: HeaderCarrier): Future[OutstandingChargesResponseModel] = {


    val url = getOutstandingChargesUrl(idType, idNumber, s"$taxYear-04-05")

    Logger("application").debug(s"[IncomeTaxViewChangeConnector][getOutstandingCharges] - GET $url")

    http.GET[HttpResponse](url) map { response =>
      response.status match {
        case OK =>
          Logger("application").debug(s"[IncomeTaxViewChangeConnector][getOutstandingCharges] - Status: ${response.status}, json: ${response.json}")
          response.json.validate[OutstandingChargesModel].fold(
            invalid => {
              Logger("application").error(s"[IncomeTaxViewChangeConnector][getOutstandingCharges] - Json Validation Error: $invalid")
              OutstandingChargesErrorModel(Status.INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing OutstandingCharges Data Response")
            },
            valid => valid
          )
        case status =>
          if (status >= 500) {
            Logger("application").error(s"[IncomeTaxViewChangeConnector][getOutstandingCharges] - Status: ${response.status}, body: ${response.body}")
          } else {
            Logger("application").warn(s"[IncomeTaxViewChangeConnector][getOutstandingCharges] - Status: ${response.status}, body: ${response.body}")
          }
          OutstandingChargesErrorModel(response.status, response.body)
      }
    } recover {
      case ex =>
        Logger("application").error(s"[IncomeTaxViewChangeConnector][getOutstandingCharges] - Unexpected failure, ${ex.getMessage}", ex)
        OutstandingChargesErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected failure, ${ex.getMessage}")
    }

  }

  def getChargeHistory(mtdBsa: String, docNumber: String)
                      (implicit headerCarrier: HeaderCarrier): Future[ChargeHistoryResponseModel] = {
    val url = getChargeHistoryUrl(mtdBsa, docNumber)
    Logger("application").debug(s"[IncomeTaxViewChangeConnector][getChargeHistory] - GET $url")

    http.GET[HttpResponse](url) map { response =>
      response.status match {
        case OK =>
          Logger("application").debug(s"[IncomeTaxViewChangeConnector][getChargeHistory] - Status: ${response.status}, json: ${response.json}")
          response.json.validate[ChargesHistoryModel].fold(
            invalid => {
              Logger("application").error(s"[IncomeTaxViewChangeConnector][getChargeHistory] - Json Validation Error: $invalid")
              ChargesHistoryErrorModel(Status.INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing ChargeHistory Data Response")
            },
            valid => valid
          )
        case status =>
          if (status == 404 || status == 403) {
            Logger("application").info(s"[IncomeTaxViewChangeConnector][getChargeHistory] - No charge history found for $docNumber - Status: ${response.status}, body: ${response.body}")
            ChargesHistoryModel("", "", "", None)
          } else {
            if (status >= 500) {
              Logger("application").error(s"[IncomeTaxViewChangeConnector][getChargeHistory] - Status: ${response.status}, body: ${response.body}")
            } else {
              Logger("application").warn(s"[IncomeTaxViewChangeConnector][getChargeHistory] - Status: ${response.status}, body: ${response.body}")
            }
            ChargesHistoryErrorModel(response.status, response.body)
          }
      }
    } recover {
      case ex =>
        Logger("application").error(s"[IncomeTaxViewChangeConnector][getChargeHistory] - Unexpected failure, ${ex.getMessage}", ex)
        ChargesHistoryErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected failure, ${ex.getMessage}")
    }

  }

  def getPayments(taxYear: Int)
                 (implicit headerCarrier: HeaderCarrier, mtdUser: MtdItUser[_]): Future[PaymentsResponse] = {
    val dateFrom: String = s"${taxYear - 1}-04-06"
    val dateTo: String = s"$taxYear-04-05"

    val url: String = getPaymentsUrl(mtdUser.nino, dateFrom, dateTo)
    Logger("application").debug(s"[IncomeTaxViewChangeConnector][getPayments] - GET $url")

    http.GET[HttpResponse](url)(httpReads, headerCarrier, implicitly) map { response =>
      response.status match {
        case OK =>
          Logger("application").warn(s"[IncomeTaxViewChangeConnector][getPayments] - Status: ${response.status}, json: ${response.json}")
          response.json.validate[Seq[Payment]].fold(
            invalid => {
              Logger("application").error(s"[IncomeTaxViewChangeConnector][getPayments] - Json validation error: $invalid")
              PaymentsError(response.status, "Json validation error")
            },
            valid => Payments(valid)
          )
        case status =>
          if (status >= 500) {
            Logger("application").error(s"[IncomeTaxViewChangeConnector][getPayments] - Status: ${response.status}, body: ${response.body}")
          } else {
            Logger("application").warn(s"[IncomeTaxViewChangeConnector][getPayments] - Status ${response.status}, body: ${response.body}")
          }
          PaymentsError(status, response.body)
      }
    }
  }

  // CUTOVER_CREDITS
  def getFinancialDetailsByDocumentId(nino: Nino, documentNumber: String)
                                     (implicit headerCarrier: HeaderCarrier): Future[FinancialDetailsWithDocumentDetailsResponse] = {
    http.GET[HttpResponse](getFinancialDetailsByDocumentIdUrl(nino.value, documentNumber))(
      httpReads,
      headerCarrier.withExtraHeaders("Accept" -> "application/vnd.hmrc.2.0+json"),
      ec
    ) map { response =>
      response.status match {
        case OK =>
          response.json.validate[FinancialDetailsWithDocumentDetailsModel].fold(
            invalid => {
              Logger("application").error(s"[IncomeTaxViewChangeConnector][getFinancialDetailsByDocumentId] - Json validation error parsing calculation response, error $invalid")
              FinancialDetailsWithDocumentDetailsErrorModel(INTERNAL_SERVER_ERROR, "Json validation error parsing calculation response")
            },
            valid => valid
          )
        case status =>
          if (status >= INTERNAL_SERVER_ERROR) {
            Logger("application").error(s"[IncomeTaxViewChangeConnector][getFinancialDetailsByDocumentId] - Response status: ${response.status}, body: ${response.body}")
          } else {
            Logger("application").warn(s"[IncomeTaxViewChangeConnector][getFinancialDetailsByDocumentId] - Response status: ${response.status}, body: ${response.body}")
          }
          FinancialDetailsWithDocumentDetailsErrorModel(response.status, response.body)
      }
    }
  }

  def getRepaymentHistoryByRepaymentId(nino: Nino, repaymentId: String)
                                      (implicit headerCarrier: HeaderCarrier): Future[RepaymentHistoryResponseModel] = {
    http.GET[HttpResponse](getRepaymentHistoryByIdUrl(nino.value, repaymentId))(
      httpReads,
      headerCarrier.withExtraHeaders("Accept" -> "application/vnd.hmrc.2.0+json"),
      ec
    ) map { response =>
      response.status match {
        case OK =>
          response.json.validate[RepaymentHistoryModel].fold(
            invalid => {
              Logger("application").error(s"[IncomeTaxViewChangeConnector][getRepaymentHistoryByRepaymentId] - Json validation error parsing repayment response, error $invalid")
              RepaymentHistoryErrorModel(INTERNAL_SERVER_ERROR, "Json validation error parsing repayment response")
            },
            valid => valid
          )
        case status =>
          if (status >= INTERNAL_SERVER_ERROR) {
            Logger("application").error(s"[IncomeTaxViewChangeConnector][getRepaymentHistoryByRepaymentId] - Response status: ${response.status}, body: ${response.body}")
          } else {
            Logger("application").warn(s"[IncomeTaxViewChangeConnector][getPaymentHistoryByRepaymentId] - Response status: ${response.status}, body: ${response.body}")
          }
          RepaymentHistoryErrorModel(response.status, response.body)
      }
    }
  }

  def getRepaymentHistoryByNino(nino: Nino)
                               (implicit headerCarrier: HeaderCarrier): Future[RepaymentHistoryResponseModel] = {

    http.GET[HttpResponse](getAllRepaymentHistoryUrl(nino.value))(
      httpReads,
      headerCarrier.withExtraHeaders("Accept" -> "application/vnd.hmrc.2.0+json"),
      ec
    ) map { response =>
      response.status match {
        case OK =>
          response.json.validate[RepaymentHistoryModel].fold(
            invalid => {
              Logger("application").error(s"[IncomeTaxViewChangeConnector][getRepaymentHistoryByRepaymentDate] - Json validation error parsing repayment response, error $invalid")
              RepaymentHistoryErrorModel(INTERNAL_SERVER_ERROR, "Json validation error parsing repayment response")
            },
            valid => valid
          )
        case status =>
          if (status >= INTERNAL_SERVER_ERROR) {
            Logger("application").error(s"[IncomeTaxViewChangeConnector][getRepaymentHistoryByRepaymentDate] - Response status: ${response.status}, body: ${response.body}")
          } else {
            Logger("application").warn(s"[IncomeTaxViewChangeConnector][getPaymentHistoryByRepaymentDate] - Response status: ${response.status}, body: ${response.body}")
          }
          RepaymentHistoryErrorModel(response.status, response.body)
      }
    }
  }

  def updateCessationDate(nino: String, incomeSourceId: String, cessationDate: Option[LocalDate])(implicit headerCarrier: HeaderCarrier): Future[UpdateIncomeSourceResponse] = {
    val body = UpdateIncomeSourceRequestModel(nino = nino, incomeSourceId = incomeSourceId,
      cessation = Some(Cessation(cessationIndicator = true, cessationDate = cessationDate)))

    http.PUT[UpdateIncomeSourceRequestModel, HttpResponse](
      getUpdateIncomeSourceUrl,
      body, Seq[(String, String)]()).map { response =>
      response.status match {
        case OK => response.json.validate[UpdateIncomeSourceResponseModel].fold(
          invalid => {
            Logger("application").error(s"[IncomeTaxViewChangeConnector][updateCessationDate] - Json validation error parsing repayment response, error $invalid")
            UpdateIncomeSourceResponseError(INTERNAL_SERVER_ERROR, "Json validation error parsing response")
          },
          valid => valid
        )
        case status =>
          if (status >= INTERNAL_SERVER_ERROR) {
            Logger("application").error(s"[IncomeTaxViewChangeConnector][updateCessationDate] - Response status: ${response.status}, body: ${response.body}")
          } else {
            Logger("application").warn(s"[IncomeTaxViewChangeConnector][updateCessationDate] - Response status: ${response.status}, body: ${response.body}")
          }
          UpdateIncomeSourceResponseError(response.status, response.body)
      }
    }
  }

  def updateIncomeSourceTaxYearSpecific(nino: String, incomeSourceId: String, taxYearSpecific: List[TaxYearSpecific])(implicit headerCarrier: HeaderCarrier): Future[UpdateIncomeSourceResponse] = {
    val body = UpdateIncomeSourceRequestModel(nino = nino, incomeSourceId = incomeSourceId, taxYearSpecific = Some(taxYearSpecific))

    http.PUT[UpdateIncomeSourceRequestModel, HttpResponse](
      getUpdateIncomeSourceUrl,
      body, Seq[(String, String)]()).map { response =>
      response.status match {
        case OK => response.json.validate[UpdateIncomeSourceResponseModel].fold(
          invalid => {
            Logger("application").error(s"[IncomeTaxViewChangeConnector][updateIncomeSourceTaxYearSpecific] - Json validation error parsing repayment response, error $invalid")
            UpdateIncomeSourceResponseError(INTERNAL_SERVER_ERROR, "Json validation error parsing response")
          },
          valid => valid
        )
        case status =>
          if (status >= INTERNAL_SERVER_ERROR) {
            Logger("application").error(s"[IncomeTaxViewChangeConnector][updateIncomeSourceTaxYearSpecific] - Response status: ${response.status}, body: ${response.body}")
          } else {
            Logger("application").warn(s"[IncomeTaxViewChangeConnector][updateIncomeSourceTaxYearSpecific] - Response status: ${response.status}, body: ${response.body}")
          }
          UpdateIncomeSourceResponseError(response.status, response.body)
      }
    }
  }
  def getITSAStatusDetail(nino: String, taxYear: String, futureYears: Boolean, history: Boolean)
                         (implicit headerCarrier: HeaderCarrier): Future[Either[ITSAStatusResponse,List[ITSAStatusResponseModel]]] = {
    val url = getITSAStatusDetailUrl(nino,taxYear)
    val queryParams = Seq(("futureYears" -> futureYears.toString ) , ("history" -> history.toString))
    http.GET[HttpResponse](url = url,queryParams = queryParams)(
      httpReads,
      headerCarrier.withExtraHeaders("Accept" -> "application/vnd.hmrc.2.0+json"),
      ec
    ) map { response =>
      response.status match {
        case OK =>
          response.json.validate[List[ITSAStatusResponseModel]].fold(
            invalid => {
              Logger("application").error(s"[IncomeTaxViewChangeConnector][getITSAStatusDetail] - Json validation error parsing repayment response, error $invalid")
              Left(ITSAStatusResponseError(INTERNAL_SERVER_ERROR, "Json validation error parsing ITSA Status response"))
            },
            valid => Right(valid)
          )
        case status =>
          if (status >= INTERNAL_SERVER_ERROR) {
            Logger("application").error(s"[IncomeTaxViewChangeConnector][getITSAStatusDetail] - Response status: ${response.status}, body: ${response.body}")
          } else {
            Logger("application").warn(s"[IncomeTaxViewChangeConnector][getITSAStatusDetail] - Response status: ${response.status}, body: ${response.body}")
          }
          Left(ITSAStatusResponseError(response.status, response.body))
      }
    }
  }

}
