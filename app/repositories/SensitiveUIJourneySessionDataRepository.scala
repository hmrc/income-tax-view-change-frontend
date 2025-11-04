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
import enums.JourneyType.{JourneyType, Operation}
import models.{SensitiveUIJourneySessionData, UIJourneySessionData}
import org.mongodb.scala.bson.collection.mutable.Document
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model._
import org.mongodb.scala.result.UpdateResult
import play.api.Configuration
import play.api.libs.json.Format
import uk.gov.hmrc.crypto.SymmetricCryptoFactory
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.{Clock, Instant}
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SensitiveUIJourneySessionDataRepository @Inject()(
                                                mongoComponent: MongoComponent,
                                                appConfig: FrontendAppConfig,
                                                config: Configuration,
                                                clock: Clock
                                              )(implicit ec: ExecutionContext)
  extends PlayMongoRepository[SensitiveUIJourneySessionData](
    collectionName = "ui-journey-session-data",
    mongoComponent = mongoComponent,
    domainFormat = SensitiveUIJourneySessionData.format(
      SymmetricCryptoFactory.aesCryptoFromConfig("encryption", config.underlying)
    ),
    indexes = Seq(
      IndexModel(
        Indexes.ascending("sessionId"),
        IndexOptions()
          .name("sessionIdIdx")
      ),
      IndexModel(
        Indexes.ascending("journeyType"),
        IndexOptions()
          .name("journeyTypeIdx")
      ),
      IndexModel(
        Indexes.ascending("lastUpdated"),
        IndexOptions()
          .name("lastUpdatedIdx")
          .expireAfter(appConfig.cacheTtl, TimeUnit.SECONDS)
      )
    ),
    replaceIndexes = true
  ) {

  implicit val instantFormat: Format[Instant] = MongoJavatimeFormats.instantFormat

  private def dataFilter(data: SensitiveUIJourneySessionData): Bson = {
    import Filters._
    and(equal("sessionId", data.sessionId), equal("journeyType", data.journeyType))
  }

  private def sessionFilter(sessionId: String, operation: Operation): Bson = {
    import Filters._
    and(equal("sessionId", sessionId), regex("journeyType", operation.operationType))
  }

  def keepAlive(data: SensitiveUIJourneySessionData): Future[Boolean] =
    collection
      .updateOne(
        filter = dataFilter(data),
        update = Updates.set("lastUpdated", Instant.now(clock))
      )
      .toFuture()
      .map(_.wasAcknowledged())

  def get(sessionId: String, journeyType: JourneyType): Future[Option[UIJourneySessionData]] = {
    val data = SensitiveUIJourneySessionData(sessionId, journeyType.toString)
    keepAlive(data).flatMap {
      _ =>
        collection
          .find(dataFilter(data))
          .map(_.decrypted)
          .headOption()
    }
  }

  def set(data: UIJourneySessionData): Future[Boolean] = {

    val updatedAnswers = data copy (lastUpdated = Instant.now(clock))

    collection
      .replaceOne(
        filter = dataFilter(data.encrypted),
        replacement = updatedAnswers.encrypted,
        options = ReplaceOptions().upsert(true)
      )
      .toFuture()
      .map(_.wasAcknowledged())
  }

  def updateData(data: UIJourneySessionData, key: String, value: String): Future[UpdateResult] = {
    collection.updateOne(
      filter = dataFilter(data.encrypted),
      update = Document("$set" -> Document(key -> value))
    ).toFuture()
  }

  def deleteOne(data: UIJourneySessionData): Future[Boolean] =
    collection
      .deleteOne(dataFilter(data.encrypted))
      .toFuture()
      .map(_.wasAcknowledged())

  def deleteJourneySession(sessionId: String, operation: Operation): Future[Boolean] =
    collection
      .deleteOne(sessionFilter(sessionId, operation))
      .toFuture()
      .map(_.wasAcknowledged())
}
