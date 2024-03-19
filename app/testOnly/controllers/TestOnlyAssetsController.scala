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

package testOnly.controllers

import controllers.{AssetEncoding, AssetsBuilder, DefaultAssetsMetadata}
import play.api.Environment
import play.api.http.HttpErrorHandler

import javax.inject.Inject

case class AssetsConfiguration(
                                path: String = "/testOnlyPublic",
                                urlPrefix: String = "/assets",
                                defaultCharSet: String = "utf-8",
                                enableCaching: Boolean = true,
                                enableCacheControl: Boolean = false,
                                configuredCacheControl: Map[String, Option[String]] = Map.empty,
                                defaultCacheControl: String = "public, max-age=3600",
                                aggressiveCacheControl: String = "public, max-age=31536000, immutable",
                                digestAlgorithm: String = "md5",
                                checkForMinified: Boolean = true,
                                textContentTypes: Set[String] = Set("application/json", "application/javascript"),
                                encodings: Seq[AssetEncoding] = Seq(
                                  AssetEncoding.Brotli,
                                  AssetEncoding.Gzip,
                                  AssetEncoding.Xz,
                                  AssetEncoding.Bzip2
                                )
                              )

val a = AssetsConfiguration
class TestOnlyAssetsController @Inject()(errorHandler: HttpErrorHandler, a: AssetsConfiguration, meta: DefaultAssetsMetadata, env: Environment) extends AssetsBuilder(errorHandler, meta, env)

