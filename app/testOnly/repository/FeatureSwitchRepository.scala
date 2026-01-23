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

package testOnly.repository

import models.admin.{FeatureSwitch, FeatureSwitchName}
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model._
import play.api.Configuration
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}
import uk.gov.hmrc.mongo.transaction.Transactions
import org.mongodb.scala.ToSingleObservablePublisher
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FeatureSwitchRepository @Inject()(val mongoComponent: MongoComponent,
                                        val configuration: Configuration)
                                       (implicit ec: ExecutionContext) extends PlayMongoRepository[FeatureSwitch](
  collectionName = configuration.get[String]("mongodb.income-tax-view-change-frontend.feature-switches.name"),
  mongoComponent = mongoComponent,
  domainFormat = FeatureSwitch.format,
  indexes = Seq(
    IndexModel(
      keys = Indexes.ascending("name"),
      indexOptions = IndexOptions()
        .name("name")
        .unique(true)
    )
  ),
  extraCodecs = Codecs.playFormatSumCodecs(FeatureSwitchName.formats)
)
  with Transactions {


  def getFeatureSwitch(name: FeatureSwitchName): Future[Option[FeatureSwitch]] =
    collection
      .find(equal("name", name.name))
      .headOption()

  def getFeatureSwitches: Future[List[FeatureSwitch]] =
    collection
      .find()
      .toFuture()
      .map(_.toList)

  def setFeatureSwitch(name: FeatureSwitchName, enabled: Boolean): Future[Boolean] =
    collection
      .replaceOne(
        filter = equal("name", name),
        replacement = FeatureSwitch(
          name = name,
          isEnabled = enabled
        ),
        options = ReplaceOptions().upsert(true)
      )
      .map(_.wasAcknowledged())
      .toSingle()
      .toFuture()

  def setFeatureSwitches(featureSwitches: Map[FeatureSwitchName, Boolean]): Future[Unit] = {
    val switches: List[FeatureSwitch] = featureSwitches.map {
      case (flag, status) =>
        FeatureSwitch(
          name = flag,
          isEnabled = status
        )
    }.toList

    for {
      _ <- collection.deleteMany(in("name", featureSwitches.keys.toSeq: _*)).toFuture()
      _ <- collection.insertMany(switches).toFuture().map(_ => ())
    } yield ()
  }
}