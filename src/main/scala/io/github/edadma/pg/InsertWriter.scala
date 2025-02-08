package io.github.edadma.pg

import scala.scalajs.js
import scala.deriving.*
import scala.compiletime.*

// Column writer type class for converting Scala types to DB values
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

// Handles converting a case class to insertable values
trait InsertWriter[T]:
  def toInsertValues(value: T): List[(String, js.Any)]
  def tableName: String

class DerivedInsertWriter[T](
    writers: List[(String, ColumnWriter[?])],
    table: String,
    get: T => Product,
) extends InsertWriter[T]:
  def toInsertValues(value: T): List[(String, js.Any)] =
    val product = get(value)
    writers.zip(product.productIterator.toList).map {
      case ((dbColumn, writer), value) =>
        dbColumn -> writer.asInstanceOf[ColumnWriter[Any]].write(value)
    }

  def tableName: String = table

object InsertWriter:
  inline given derived[T](using m: Mirror.ProductOf[T], table: TableName[T]): InsertWriter[T] =
    val labels = RowReader.getLabels[m.MirroredElemLabels] // Reuse the snake_case conversion
    val writers = summonAll[Tuple.Map[m.MirroredElemTypes, ColumnWriter]].toList
      .asInstanceOf[List[ColumnWriter[?]]]
    val pairs = labels.zip(writers)
    DerivedInsertWriter(pairs, table.name, (t: T) => t.asInstanceOf[Product])

// Helper trait for providing table names
trait TableName[T]:
  def name: String

// Builder interface for insert operations
class InsertBuilder[T: InsertWriter](using client: Client):
  def values(items: T*): InsertOperation[T] =
    InsertOperation(items.toList)

class InsertOperation[T](items: List[T])(using writer: InsertWriter[T], client: Client):
  def execute(): js.Promise[QueryResult] =
    if items.isEmpty then
      js.Promise.reject(new Exception("No values to insert")).asInstanceOf[js.Promise[QueryResult]]
    else
      val columns = writer.toInsertValues(items.head).map(_._1)
      val placeholders = items.indices.map { i =>
        val offset = i * columns.length + 1
        s"(${columns.indices.map(j => s"$$${offset + j}").mkString(", ")})"
      }.mkString(", ")

      val values = items.flatMap(item => writer.toInsertValues(item).map(_._2))
      val query  = s"INSERT INTO ${writer.tableName} (${columns.mkString(", ")}) VALUES $placeholders"

      // Modify Client.scala to add this overload for parameterized queries
      client.query(query, js.Array(values*)) // Use parameterized query with values
