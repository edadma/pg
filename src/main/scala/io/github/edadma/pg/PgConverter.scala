//package io.github.edadma.pg
//
//import scala.scalajs.js
//import scala.deriving.*
//import scala.compiletime.*
//
//trait ColumnReader[T]:
//  def read(value: js.Dynamic): T
//
//object ColumnReader:
//  given ColumnReader[Int] with
//    def read(value: js.Dynamic): Int = value.asInstanceOf[Int]
//
//  given ColumnReader[String] with
//    def read(value: js.Dynamic): String = value.asInstanceOf[String]
//
//  given ColumnReader[Boolean] with
//    def read(value: js.Dynamic): Boolean = value.asInstanceOf[Boolean]
//
//trait RowReader[T]:
//  def fromRow(row: js.Dynamic): T
//
//class DerivedRowReader[T](
//    readers: List[(String, ColumnReader[?])],
//    build: Product => T,
//) extends RowReader[T]:
//  def fromRow(row: js.Dynamic): T =
//    val values = readers.map { case (label, reader) =>
//      reader.asInstanceOf[ColumnReader[Any]].read(row.selectDynamic(label))
//    }
//    build(Tuple.fromArray(values.toArray))
//
//object RowReader:
//  inline def getLabels[T <: Tuple]: List[String] = inline erasedValue[T] match
//    case _: EmptyTuple => Nil
//    case _: (t *: ts)  => constValue[t].toString :: getLabels[ts]
//
//  inline given derived[T](using m: Mirror.Of[T]): RowReader[T] =
//    inline m match
//      case p: Mirror.ProductOf[T] =>
//        val labels = getLabels[p.MirroredElemLabels]
//        val readers = summonAll[Tuple.Map[p.MirroredElemTypes, ColumnReader]].toList
//          .asInstanceOf[List[ColumnReader[?]]]
//        val pairs = labels.zip(readers)
//        DerivedRowReader(pairs, p.fromProduct)
//
//object PgConverter:
//  def as[T](row: js.Dynamic)(using reader: RowReader[T]): T = reader.fromRow(row)
//  def asList[T](rows: js.Array[js.Dynamic])(using reader: RowReader[T]): List[T] =
//    rows.map(reader.fromRow).toList

package io.github.edadma.pg

import scala.scalajs.js
import scala.deriving.*
import scala.compiletime.*
import scala.annotation.StaticAnnotation

// Column name annotation
case class column(name: String) extends StaticAnnotation

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
  inline def getLabels[T <: Tuple]: List[String] = inline erasedValue[T] match
    case _: EmptyTuple => Nil
    case _: (t *: ts)  => constValue[t].toString :: getLabels[ts]

  // Note: For now, we'll use a simpler approach where field names exactly match column names
  // We'll need to evolve this to handle annotations properly
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
