/*
 * Copyright 2021 HM Revenue & Customs
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
import models.domain.VatReturn
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model._
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.time.{Clock, Instant}
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CachedVatReturnRepository @Inject()(
                                         mongoComponent: MongoComponent,
                                         appConfig: FrontendAppConfig,
                                         clock: Clock
                                         )(implicit ec: ExecutionContext)
  extends PlayMongoRepository[CachedVatReturnWrapper](
    collectionName = "cachedVatReturns",
    mongoComponent = mongoComponent,
    domainFormat = CachedVatReturnWrapper.format,
    indexes = Seq(
      IndexModel(
        Indexes.ascending("lastUpdated"),
        IndexOptions()
          .name("lastUpdatedIdx")
          .expireAfter(appConfig.cacheTtl, TimeUnit.SECONDS) //TODO Need to change to DAYS when VEOSS-692 merged
      )
    )
  ) {

  private def byId(id: String): Bson = Filters.equal("_id", id)

  def get(id: String): Future[Option[VatReturn]] =
    collection
      .find(byId(id))
      .headOption
      .map(_.map(_.vatReturn))

  def set(userId: String, vatReturn: VatReturn): Future[Boolean] = {

    val wrapper = CachedVatReturnWrapper(userId, vatReturn, Instant.now(clock))

    collection
      .replaceOne(
        filter      = byId(userId),
        replacement = wrapper,
        options     = ReplaceOptions().upsert(true)
      )
      .toFuture()
      .map(_ => true)
  }
}

