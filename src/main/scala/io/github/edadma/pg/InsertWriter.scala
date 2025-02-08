//package io.github.edadma.pg
//
//import scala.scalajs.js
//import scala.deriving.*
//import scala.compiletime.*
//
//trait ColumnWriter[T]:
//  def write(value: T): js.Any
//
//object ColumnWriter:
//  given ColumnWriter[Int] with
//    def write(value: Int): js.Any = value
//
//  given ColumnWriter[String] with
//    def write(value: String): js.Any = value
//
//  given ColumnWriter[Boolean] with
//    def write(value: Boolean): js.Any = value
//
//  given [T](using writer: ColumnWriter[T]): ColumnWriter[Option[T]] with
//    def write(value: Option[T]): js.Any =
//      value.map(writer.write).getOrElse(null)
//
//trait InsertWriter[T]:
//  def toInsertValues(value: T): List[(String, js.Any)]
//  def tableName: String
//
//trait TableName[T]:
//  def name: String
//
//class DerivedInsertWriter[T](
//    writers: List[(String, ColumnWriter[?])],
//    table: String,
//    get: T => Product,
//) extends InsertWriter[T]:
//  def toInsertValues(value: T): List[(String, js.Any)] =
//    val product = get(value)
//    // Skip the id field if it's the first field and its value is 0
//    writers.zip(product.productIterator.toList)
//      .filterNot { case ((name, _), value) =>
//        name == "id" && value.asInstanceOf[Int] == 0
//      }
//      .map { case ((dbColumn, writer), value) =>
//        dbColumn -> writer.asInstanceOf[ColumnWriter[Any]].write(value)
//      }
//
//  def tableName: String = table
//
//object InsertWriter:
//  inline given derived[T](using m: Mirror.ProductOf[T], table: TableName[T]): InsertWriter[T] =
//    val labels = RowReader.getLabels[m.MirroredElemLabels]
//    val writers = summonAll[Tuple.Map[m.MirroredElemTypes, ColumnWriter]].toList
//      .asInstanceOf[List[ColumnWriter[?]]]
//    val pairs = labels.zip(writers)
//    DerivedInsertWriter(pairs, table.name, (t: T) => t.asInstanceOf[Product])
//
//object Database:
//  class InsertBuilder[T: InsertWriter](using client: Client):
//    def values(items: T*): InsertOperation[T] =
//      InsertOperation(items.toList)
//
//  class InsertOperation[T](items: List[T])(using writer: InsertWriter[T], client: Client):
//    def execute(): js.Promise[QueryResult] =
//      if items.isEmpty then
//        js.Promise.reject(new Exception("No values to insert")).asInstanceOf[js.Promise[QueryResult]]
//      else
//        val columnValues = writer.toInsertValues(items.head)
//        val columns      = columnValues.map(_._1)
//
//        val placeholders = items.indices.map { i =>
//          val offset = i * columns.length + 1
//          s"(${columns.indices.map(j => s"$$${offset + j}").mkString(", ")})"
//        }.mkString(", ")
//
//        val values = items.flatMap(item => writer.toInsertValues(item).map(_._2))
//        val query = s"""
//          INSERT INTO ${writer.tableName}
//          (${columns.mkString(", ")})
//          VALUES $placeholders
//          RETURNING *
//        """
//
//        client.query(query, js.Array(values*))

package io.github.edadma.pg

import scala.scalajs.js
import scala.deriving.*
import scala.compiletime.*

trait ColumnWriter[T]:
  def write(value: T): js.Any
  def isDefaultPK(value: T): Boolean = false // By default, no special PK handling

object ColumnWriter:
  given ColumnWriter[Int] with
    def write(value: Int): js.Any                 = value
    override def isDefaultPK(value: Int): Boolean = value == 0

  given ColumnWriter[String] with
    def write(value: String): js.Any                 = value
    override def isDefaultPK(value: String): Boolean = value == ""

  given ColumnWriter[Boolean] with
    def write(value: Boolean): js.Any = value

  given [T](using writer: ColumnWriter[T]): ColumnWriter[Option[T]] with
    def write(value: Option[T]): js.Any =
      value.map(writer.write).getOrElse(null)
    override def isDefaultPK(value: Option[T]): Boolean =
      value.isEmpty || value.exists(writer.isDefaultPK)

trait InsertWriter[T]:
  def toInsertValues(value: T): List[(String, js.Any)]
  def tableName: String

trait TableName[T]:
  def name: String

class DerivedInsertWriter[T](
    writers: List[(String, ColumnWriter[?])],
    table: String,
    get: T => Product,
) extends InsertWriter[T]:
  def toInsertValues(value: T): List[(String, js.Any)] =
    val product = get(value)
    writers.zip(product.productIterator.toList)
      .filterNot { case ((name, writer), value) =>
        // Skip if it's a primary key field with default value
        name == "id" && writer.asInstanceOf[ColumnWriter[Any]].isDefaultPK(value)
      }
      .map { case ((dbColumn, writer), value) =>
        dbColumn -> writer.asInstanceOf[ColumnWriter[Any]].write(value)
      }

  def tableName: String = table

object InsertWriter:
  inline given derived[T](using m: Mirror.ProductOf[T], table: TableName[T]): InsertWriter[T] =
    val labels = RowReader.getLabels[m.MirroredElemLabels]
    val writers = summonAll[Tuple.Map[m.MirroredElemTypes, ColumnWriter]].toList
      .asInstanceOf[List[ColumnWriter[?]]]
    val pairs = labels.zip(writers)
    DerivedInsertWriter(pairs, table.name, (t: T) => t.asInstanceOf[Product])

object Database:
  class InsertBuilder[T: InsertWriter](using client: Client):
    def values(items: T*): InsertOperation[T] =
      InsertOperation(items.toList)

  class InsertOperation[T](items: List[T])(using writer: InsertWriter[T], client: Client):
    def execute(): js.Promise[QueryResult] =
      if items.isEmpty then
        js.Promise.reject(new Exception("No values to insert")).asInstanceOf[js.Promise[QueryResult]]
      else
        val columnValues = writer.toInsertValues(items.head)
        val columns      = columnValues.map(_._1)

        val placeholders = items.indices.map { i =>
          val offset = i * columns.length + 1
          s"(${columns.indices.map(j => s"$$${offset + j}").mkString(", ")})"
        }.mkString(", ")

        val values = items.flatMap(item => writer.toInsertValues(item).map(_._2))
        val query = s"""
          INSERT INTO ${writer.tableName}
          (${columns.mkString(", ")})
          VALUES $placeholders
          RETURNING *
        """

        client.query(query, js.Array(values*))
