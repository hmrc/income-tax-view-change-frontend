/*
 * Copyright 2021 HM Revenue & Customs
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

import connectors.CustomerFactsUpdateConnector
import play.api.Logger
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CustomerFactsUpdateService @Inject()(
                                            customerFactsUpdateConnector: CustomerFactsUpdateConnector
                                          ) {

  def updateCustomerFacts(mtdId: String, isAlreadyConfirmed: Boolean)
                                   (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = {

    if (isAlreadyConfirmed) {
      Logger("application").info(s"Customer facts already confirmed - skipping update for mtdId=$mtdId")
      Future.successful(())
    } else {
      customerFactsUpdateConnector.updateCustomerFacts(mtdId).map { response =>
        response.status match {
          case OK =>
            Logger("application").info(s"Customer facts update returned OK for mtdId=$mtdId")

          case status if status >= INTERNAL_SERVER_ERROR =>
            Logger("application").error(s"Customer facts update failed. status=$status for mtdId=$mtdId body=${response.body}")

          case status =>
            Logger("application").warn(s"Customer facts update returned  status=$status for mtdId=$mtdId body=${response.body}")

        }
      }.recover { case e: Exception =>
        Logger("application").error(s"Customer facts update failed due to exception for mtdId=$mtdId", e)
      }
    }
  }
}