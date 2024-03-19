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

import com.google.inject.AbstractModule

import java.io.{File, InputStream}
import javax.inject.{Inject, Provider}
import java.nio.file.{Files, Path}
import scala.concurrent.ExecutionContext
import controllers.{AssetEncoding, AssetsBuilder, AssetsConfiguration, AssetsMetadata, DefaultAssetsMetadata}
import play.api.Environment
import play.api.http.{DefaultFileMimeTypesProvider, FileMimeTypes, FileMimeTypesConfiguration, HttpErrorHandler}
import uk.gov.hmrc.hmrcfrontend.controllers.Assets

import javax.inject.Inject
import scala.collection.Seq

class Assets @Inject() (errorHandler: HttpErrorHandler, meta: AssetsMetadata, env: Environment) extends AssetsBuilder(errorHandler, meta, env)



val x: AssetsConfiguration = AssetsConfiguration(
                                         path = "/public",
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



