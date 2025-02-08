package io.github.edadma.pg

import scala.scalajs.js
import scala.scalajs.js.annotation._
import scala.concurrent.{Future, Promise}
import scala.scalajs.js.JSConverters._

@js.native
trait PgConfig extends js.Object {
  val user: String     = js.native
  val host: String     = js.native
  val database: String = js.native
  val password: String = js.native
  val port: Int        = js.native
}

object PgConfig {
  def apply(
      user: String,
      host: String,
      database: String,
      password: String,
      port: Int,
  ): PgConfig = {
    js.Dynamic.literal(
      user = user,
      host = host,
      database = database,
      password = password,
      port = port,
    ).asInstanceOf[PgConfig]
  }
}

@js.native
trait QueryConfig extends js.Object {
  val text: String                         = js.native
  val values: js.UndefOr[js.Array[js.Any]] = js.native
}

object QueryConfig {
  def apply(text: String, values: js.Array[js.Any] = js.Array()): QueryConfig = {
    js.Dynamic.literal(
      text = text,
      values = values,
    ).asInstanceOf[QueryConfig]
  }
}

@js.native
trait QueryResult extends js.Object {
  val rows: js.Array[js.Dynamic] = js.native
}

@js.native
@JSImport("pg", "Client")
class Client(config: PgConfig) extends js.Object {
  def connect(): js.Promise[Unit]                                            = js.native
  def end(): js.Promise[Unit]                                                = js.native
  def query(queryText: String): js.Promise[QueryResult]                      = js.native
  def query(config: QueryConfig): js.Promise[QueryResult]                    = js.native
  def query(text: String, values: js.Array[js.Any]): js.Promise[QueryResult] = js.native
}
