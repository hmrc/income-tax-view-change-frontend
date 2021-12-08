/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package helpers.agent

import akka.Done
import play.api.cache.AsyncCacheApi

import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.reflect.ClassTag

class DisabledAsyncCacheApi extends AsyncCacheApi {
  override def set(key: String, value: Any, expiration: Duration): Future[Done] = Future.successful(Done)

  override def remove(key: String): Future[Done] = Future.successful(Done)

  override def getOrElseUpdate[A](key: String, expiration: Duration)(orElse: => Future[A])(
    implicit evidence$1: ClassTag[A]): Future[A] = orElse

  override def get[T](key: String)(implicit evidence$2: ClassTag[T]): Future[Option[T]] = Future.successful(None)

  override def removeAll(): Future[Done] = Future.successful(Done)
}