package io.github.edadma.pg

import scala.scalajs.js
import js.JSConverters._
import org.scalajs.macrotaskexecutor.MacrotaskExecutor.Implicits.global

// Case class using snake_case to match database columns
case class User(
    id: Int,
    name: String,                 // required
    email: String,                // required
    phone_number: Option[String], // optional
    age: Option[Int],             // optional
)

@main def run(): Unit =
  val config = PgConfig(
    user = "postgres",
    host = "localhost",
    database = "postgres",
    password = "postgres",
    port = 5432,
  )

  val client = new Client(config)

  val createTable = """
    DROP TABLE IF EXISTS users;
    CREATE TABLE users (
      id SERIAL PRIMARY KEY,
      name TEXT NOT NULL,
      email TEXT NOT NULL,
      phone_number TEXT,
      age INTEGER
    );

    INSERT INTO users (name, email, phone_number, age) VALUES
      ('Alice', 'alice@test.com', '+1234567890', 25),
      ('Bob', 'bob@test.com', NULL, NULL),
      ('Charlie', 'charlie@test.com', '+9876543210', NULL),
      ('David', 'david@test.com', NULL, 35);
  """

  val program = for {
    _      <- client.connect().toFuture
    _      <- client.query(createTable).toFuture
    result <- client.query("SELECT * FROM users ORDER BY name").toFuture
    _      <- client.end().toFuture
  } yield {
    val users = PgConverter.asList[User](result.rows)
    println("\nUsers with optional fields:")
    users.foreach { user =>
      println(s"""
                 |User: ${user.name}
                 |  Email: ${user.email}
                 |  Phone: ${user.phone_number.getOrElse("No phone number")}
                 |  Age: ${user.age.map(_.toString).getOrElse("Age not provided")}
                 |""".stripMargin)
    }
    users
  }

  program.recover { case error =>
    error.printStackTrace()
  }
