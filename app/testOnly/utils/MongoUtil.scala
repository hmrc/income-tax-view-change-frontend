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

package testOnly.utils

import com.google.inject.Singleton
import play.api.libs.json.Json
import testOnly.models.{UserModel, UserRecord}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.{ReplaceOptions, UpdateOptions}
import org.mongodb.scala.result.{DeleteResult, UpdateResult}
import uk.gov.hmrc.mongo.transaction.{TransactionConfiguration, Transactions}

@Singleton
class UserRepository @Inject()(val mongoComponent: MongoComponent, implicit val ec: ExecutionContext)
  extends PlayMongoRepository[UserRecord](
    collectionName = "user",
    mongoComponent = mongoComponent,
    domainFormat = UserRecord.formats,
    indexes = Seq()
  ) {

  def findAll(): Future[Seq[UserRecord]] =
    collection.find().toFuture()

  def findUser(nino: String): Future[UserRecord] =
    collection.find(equal("nino", nino)).toFuture()
      .map(records => records.head)

  def addUser(userRecord: UserRecord): Future[UpdateResult] = {
    collection.replaceOne(equal("nino", userRecord.nino), userRecord,
      options = ReplaceOptions().upsert(true)).toFuture()
  }

  def removeAll(): Future[DeleteResult] = {
    collection.deleteMany(empty()).toFuture()
  }


}
