package io.github.edadma.pg

import scala.scalajs.js

trait ColumnWriter[T]:
  def write(value: T): js.Any

object ColumnWriter:
  given ColumnWriter[Int] with
    def write(value: Int): js.Any = value

  given ColumnWriter[String] with
    def write(value: String): js.Any = value

  given ColumnWriter[Boolean] with
    def write(value: Boolean): js.Any = value

  given [T](using writer: ColumnWriter[T]): ColumnWriter[Option[T]] with
    def write(value: Option[T]): js.Any =
      value.map(writer.write).getOrElse(null)
