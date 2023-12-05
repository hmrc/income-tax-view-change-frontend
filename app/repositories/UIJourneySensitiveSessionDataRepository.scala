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

package repositories

import config.FrontendAppConfig
import enums.JourneyType.Operation
import models.incomeSourceDetails.{SensitiveAddIncomeSourceData, SensitiveUIJourneySessionData, UIJourneySessionData}
import org.mongodb.scala.bson.collection.mutable.Document
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model._
import org.mongodb.scala.result.UpdateResult
import play.api.libs.json.Format
import services.EncryptionService
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}

import java.time.{Clock, Instant}
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UIJourneySensitiveSessionDataRepository @Inject()(
                                                mongoComponent: MongoComponent,
                                                appConfig: FrontendAppConfig,
                                                encryptionService: EncryptionService,
                                                clock: Clock
                                              )(implicit ec: ExecutionContext)
  extends PlayMongoRepository[SensitiveUIJourneySessionData](
    collectionName = "ui-journey-sensitive-session-data",
    mongoComponent = mongoComponent,
    domainFormat = SensitiveUIJourneySessionData.format(encryptionService.crypto),
    indexes = Seq(
      IndexModel(
        Indexes.ascending("lastUpdated"),
        IndexOptions()
          .name("lastUpdatedIdx")
          .expireAfter(appConfig.cacheTtl, TimeUnit.SECONDS)
      )
    ),
    replaceIndexes = true,
    extraCodecs      = Seq(
      Codecs.playFormatCodec(SensitiveAddIncomeSourceData.sensitiveStringFormat(encryptionService.crypto))
    )
  ) {

  implicit val instantFormat: Format[Instant] = MongoJavatimeFormats.instantFormat

  private def dataFilter(data: SensitiveUIJourneySessionData): Bson = {
    import Filters._
    and(equal("sessionId", data.sessionId), equal("journeyType", data.journeyType))
  }

  def keepAlive(data: SensitiveUIJourneySessionData): Future[Boolean] =
    collection
      .updateOne(
        filter = dataFilter(data),
        update = Updates.set("lastUpdated", Instant.now(clock))
      )
      .toFuture()
      .map(_ => true)

  def get(sessionId: String, journeyType: String): Future[Option[SensitiveUIJourneySessionData]] = {
    val data = SensitiveUIJourneySessionData(sessionId, journeyType)
    keepAlive(data).flatMap {
      _ =>
        collection
          .find(dataFilter(data))
          .headOption()
    }
  }

  def set(data: SensitiveUIJourneySessionData): Future[Boolean] = {
    val updatedAnswers = data copy (lastUpdated = Instant.now(clock))
    collection
      .replaceOne(
        filter = dataFilter(data),
        replacement = updatedAnswers,
        options = ReplaceOptions().upsert(true)
      )
      .toFuture()
      .map(_ => true)
  }
}