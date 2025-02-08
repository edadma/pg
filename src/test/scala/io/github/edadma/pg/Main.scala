package io.github.edadma.pg

import scala.scalajs.js
import js.JSConverters._

import org.scalajs.macrotaskexecutor.MacrotaskExecutor.Implicits.global

case class User(id: Int, name: String, email: String)

@main def run(): Unit =
  val config = PgConfig(
    user = "postgres",
    host = "localhost",
    database = "postgres",
    password = "postgres",
    port = 5432,
  )

  val client = new Client(config)

  // Convert js.Dynamic to User case class
  def rowToUser(row: js.Dynamic): User = {
    User(
      id = row.id.asInstanceOf[Int],
      name = row.name.asInstanceOf[String],
      email = row.email.asInstanceOf[String],
    )
  }

  val program = for {
    _      <- client.connect().toFuture
    result <- client.query("SELECT * FROM users").toFuture
    _      <- client.end().toFuture
  } yield {
    result.rows.map(rowToUser).toList
  }

  program
    .map { users =>
      println(s"Found users: $users")
    }
    .recover { case error =>
      println(s"Database error: ${error.getMessage}")
      error.printStackTrace()
    }
