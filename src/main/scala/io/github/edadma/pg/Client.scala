package io.github.edadma.pg

import scala.scalajs.js
import scala.scalajs.js.annotation._
import scala.concurrent.{Future, Promise, ExecutionContext}
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

// The underlying JS client
@js.native
@JSImport("pg", "Client")
class JSClient(config: PgConfig) extends js.Object {
  def connect(): js.Promise[Unit]                                            = js.native
  def end(): js.Promise[Unit]                                                = js.native
  def query(queryText: String): js.Promise[QueryResult]                      = js.native
  def query(config: QueryConfig): js.Promise[QueryResult]                    = js.native
  def query(text: String, values: js.Array[js.Any]): js.Promise[QueryResult] = js.native
}

// Our Scala wrapper
class Client(config: PgConfig)(using ExecutionContext) {
  private val jsClient = new JSClient(config)

  def connect: Future[Unit] = jsClient.connect().toFuture

  def end: Future[Unit] = jsClient.end().toFuture

  def query(queryText: String): Future[QueryResult] =
    jsClient.query(queryText).toFuture

  def query(config: QueryConfig): Future[QueryResult] =
    jsClient.query(config).toFuture

  def query(text: String, values: js.Array[js.Any]): Future[QueryResult] =
    jsClient.query(text, values).toFuture
}
