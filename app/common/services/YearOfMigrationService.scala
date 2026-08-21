/*
 * Copyright 2026 HM Revenue & Customs
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

package common.services

import common.connectors.YearOfMigrationConnector
import common.models.itsaStatus.ITSAStatusYearOfMigrationModel
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class YearOfMigrationService @Inject()(yearOfMigrationConnector: YearOfMigrationConnector) {

  def getYearOfMigration(nino: String)(implicit headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[ITSAStatusYearOfMigrationModel] = {
    yearOfMigrationConnector.getYearOfMigration(nino).flatMap {
      case Right(yearOfMigration) =>
        Future.successful(yearOfMigration)
      case Left(error) =>
        Logger("application").error(s"$error")
        Future.failed(new Exception("Error while trying to retrieve Year of Migration"))
    }
  }

  def orderedTaxYearsByYearOfMigration(nino: String)(implicit headerCarrier: HeaderCarrier, ec: ExecutionContext, dateService: DateServiceInterface): Future[List[Int]] = {
    getYearOfMigration(nino).map(_.yearOfMigrationEndYear.map(year => (year.toInt to dateService.getCurrentTaxYearEnd).toList).getOrElse(Nil))
  }
}
