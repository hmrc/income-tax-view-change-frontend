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

package models.core

import exceptions.{MultipleIncomeSourcesFound, NoIncomeSourceFound}

import scala.util.Try

class IncomeSourceIdHash private(val hash: String) extends AnyVal {
  override def toString: String = s"IncomeSourceIdHash: $hash"

  def findIncomeSourceIdMatchingHash(incomeSourceIdHash: IncomeSourceIdHash, ids: List[IncomeSourceId]): Either[Throwable, IncomeSourceId] = {

    val matchingIncomeSourceIds = ids.filter(_.toHash.hash == incomeSourceIdHash.hash)

    val noIncomeSourceFound: Int = 0
    val success: Int = 1

    matchingIncomeSourceIds.length match {
      case matchedHash if matchedHash == noIncomeSourceFound => Left(NoIncomeSourceFound(hash = incomeSourceIdHash.hash))
      case matchedHash if matchedHash == success => Right(matchingIncomeSourceIds.head)
      case _ => Left(MultipleIncomeSourcesFound(hash = incomeSourceIdHash.hash, matchingIncomeSourceIds.map(_.value)))
    }
  }

}

case class Success(matches: Int)

object IncomeSourceIdHash {

  def apply(id: IncomeSourceId): IncomeSourceIdHash = {
    mkIncomeSourceIdHash(id)
  }
  def mkIncomeSourceIdHash(id: IncomeSourceId): IncomeSourceIdHash = {
    val hashA = id.value.hashCode().abs.toString
    val hashB = id.value.reverse.hashCode().abs.toString
    new IncomeSourceIdHash(s"${hashB}${hashA}")
  }

  def mkFromQueryString(hashCodeAsString: String): Either[Throwable, IncomeSourceIdHash] = Try {
    new IncomeSourceIdHash(hashCodeAsString)
  }.toEither

}
