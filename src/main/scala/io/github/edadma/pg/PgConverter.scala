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
//object RowReader:
//  inline def elemLabels[T <: Tuple]: List[String] = inline erasedValue[T] match
//    case _: EmptyTuple => Nil
//    case _: (t *: ts)  => constValue[t].toString :: elemLabels[ts]
//
//  inline given derived[T](using m: Mirror.Of[T]): RowReader[T] = new RowReader[T]:
//    def fromRow(row: js.Dynamic): T =
//      inline m match
//        case p: Mirror.ProductOf[T] =>
//          val labels  = elemLabels[p.MirroredElemLabels]
//          val readers = summonAll[Tuple.Map[p.MirroredElemTypes, ColumnReader]].toList
//          val values = labels.zip(readers).map { case (label, reader) =>
//            reader.asInstanceOf[ColumnReader[Any]].read(row.selectDynamic(label))
//          }
//          p.fromProduct(Tuple.fromArray(values.toArray))
//
//object PgConverter:
//  def as[T](row: js.Dynamic)(using reader: RowReader[T]): T = reader.fromRow(row)
//  def asList[T](rows: js.Array[js.Dynamic])(using reader: RowReader[T]): List[T] =
//    rows.map(reader.fromRow).toList

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

trait RowReader[T]:
  def fromRow(row: js.Dynamic): T

class DerivedRowReader[T](
    readers: List[(String, ColumnReader[?])],
    build: Product => T,
) extends RowReader[T]:
  def fromRow(row: js.Dynamic): T =
    val values = readers.map { case (label, reader) =>
      reader.asInstanceOf[ColumnReader[Any]].read(row.selectDynamic(label))
    }
    build(Tuple.fromArray(values.toArray))

object RowReader:
  inline def getLabels[T <: Tuple]: List[String] = inline erasedValue[T] match
    case _: EmptyTuple => Nil
    case _: (t *: ts)  => constValue[t].toString :: getLabels[ts]

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
