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

package config

import play.api.Configuration
import uk.gov.hmrc.mongo.lock.{LockService, MongoLockRepository}
import uk.gov.hmrc.mongo.metrix.{MetricOrchestrator, MetricRepository, MetricSource}
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

import javax.inject.{Inject, Provider, Singleton}
import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

@Singleton
class MetricOrchestratorProvider @Inject() (
                                             lockRepository: MongoLockRepository,
                                             metricRepository: MetricRepository,
                                             metrics: Metrics,
                                             //submissionItemRepository: SubmissionItemRepository,
                                             configuration: Configuration
                                           ) extends Provider[MetricOrchestrator] {

  private val lockTtl: Duration = configuration.get[Duration]("workers.metric-orchestrator-worker.lock-ttl")
  private val lockService: LockService = LockService(lockRepository, lockId = "metrix-orchestrator", ttl = lockTtl)

  private val metricRegistry = metrics.defaultRegistry

  private val source = new MetricSource {
    override def metrics(implicit ec: ExecutionContext): Future[Map[String, Int]] =
      for {
        countOfSubmitted <- Future.successful( Random.nextInt(100) )
        countOfForwarded <- Future.successful( Random.nextInt(100) )
//        countOfFailed    <- submissionItemRepository.countByStatus(SubmissionItemStatus.Failed)
//        countOfCompleted <- submissionItemRepository.countByStatus(SubmissionItemStatus.Completed)
      } yield Map(
        "submission-item.submitted.count" -> countOfSubmitted,
        "submission-item.forwarded.count" -> countOfForwarded
//        "submission-item.failed.count"    -> countOfFailed.toInt,
//        "submission-item.completed.count" -> countOfCompleted.toInt,
      )
  }

  override def get(): MetricOrchestrator =  new MetricOrchestrator(
    metricSources    = List(source),
    lockService      = lockService,
    metricRepository = metricRepository,
    metricRegistry   = metricRegistry
  )
}