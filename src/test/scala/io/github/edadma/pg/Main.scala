package io.github.edadma.pg

import scala.scalajs.js
import js.JSConverters._
import org.scalajs.macrotaskexecutor.MacrotaskExecutor.Implicits.global

// Case class using camelCase - it will automatically map to snake_case in DB
case class User(
    id: Int,
    firstName: String,           // will map to first_name
    lastName: String,            // will map to last_name
    emailAddress: String,        // will map to email_address
    phoneNumber: Option[String], // will map to phone_number
    dateOfBirth: Option[String], // will map to date_of_birth
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
      first_name TEXT NOT NULL,
      last_name TEXT NOT NULL,
      email_address TEXT NOT NULL,
      phone_number TEXT,
      date_of_birth TEXT
    );

    INSERT INTO users (first_name, last_name, email_address, phone_number, date_of_birth) VALUES
      ('Alice', 'Smith', 'alice.smith@test.com', '+1234567890', '1990-01-01'),
      ('Bob', 'Jones', 'bob.jones@test.com', NULL, NULL),
      ('Charlie', 'Brown', 'charlie.brown@test.com', '+9876543210', '1985-05-15'),
      ('David', 'Wilson', 'david.wilson@test.com', NULL, '1992-12-31');
  """

  val program = for {
    _      <- client.connect().toFuture
    _      <- client.query(createTable).toFuture
    result <- client.query("SELECT * FROM users ORDER BY first_name").toFuture
    _      <- client.end().toFuture
  } yield {
    val users = PgConverter.asList[User](result.rows)
    println("\nUsers with automatic camelCase to snake_case mapping:")
    users.foreach { user =>
      println(s"""
                 |User: ${user.firstName} ${user.lastName}
                 |  Email: ${user.emailAddress}
                 |  Phone: ${user.phoneNumber.getOrElse("No phone number")}
                 |  Birth Date: ${user.dateOfBirth.getOrElse("Not provided")}
                 |""".stripMargin)
    }
    users
  }

  program.recover { case error =>
    error.printStackTrace()
  }
