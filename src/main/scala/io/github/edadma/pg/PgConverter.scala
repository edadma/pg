package io.github.edadma.pg

import scala.scalajs.js
import scala.deriving.*
import scala.compiletime.*

trait ColumnReader[T]:
  def read(value: js.Dynamic): T

object ColumnReader:
  given ColumnReader[Int] with
    def read(value: js.Dynamic): Int = value.asInstanceOf[Int]

  given ColumnReader[String] with
    def read(value: js.Dynamic): String = value.asInstanceOf[String]

  given ColumnReader[Boolean] with
    def read(value: js.Dynamic): Boolean = value.asInstanceOf[Boolean]

  given [T](using reader: ColumnReader[T]): ColumnReader[Option[T]] with
    def read(value: js.Dynamic): Option[T] =
      if (js.isUndefined(value) || value == null) None
      else Some(reader.read(value))

trait RowReader[T]:
  def fromRow(row: js.Dynamic): T

class DerivedRowReader[T](
    readers: List[(String, ColumnReader[?])],
    build: Product => T,
) extends RowReader[T]:
  def fromRow(row: js.Dynamic): T =
    val values = readers.map { case (dbColumn, reader) =>
      reader.asInstanceOf[ColumnReader[Any]].read(row.selectDynamic(dbColumn))
    }
    build(Tuple.fromArray(values.toArray))

object RowReader:
  // Convert camelCase to snake_case at compile time
  inline def camelToSnake(name: String): String =
    val result = new StringBuilder
    var i      = 0
    while i < name.length do
      val c = name(i)
      if c.isUpper && i > 0 then result.append('_')
      result.append(c.toLower)
      i += 1
    result.toString

  inline def getLabels[T <: Tuple]: List[String] = inline erasedValue[T] match
    case _: EmptyTuple => Nil
    case _: (t *: ts) =>
      val fieldName = constValue[t].toString
      camelToSnake(fieldName) :: getLabels[ts]

  inline given derived[T](using m: Mirror.Of[T]): RowReader[T] =
    inline m match
      case p: Mirror.ProductOf[T] =>
        val labels = getLabels[p.MirroredElemLabels]
        val readers = summonAll[Tuple.Map[p.MirroredElemTypes, ColumnReader]].toList
          .asInstanceOf[List[ColumnReader[?]]]
        val pairs = labels.zip(readers)
        DerivedRowReader(pairs, p.fromProduct)

object PgConverter:
  def as[T](row: js.Dynamic)(using reader: RowReader[T]): T = reader.fromRow(row)
  def asList[T](rows: js.Array[js.Dynamic])(using reader: RowReader[T]): List[T] =
    rows.map(reader.fromRow).toList
