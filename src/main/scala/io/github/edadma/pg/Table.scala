package io.github.edadma.pg

import scala.scalajs.js
import scala.concurrent.{Future, Promise}
import js.JSConverters._
import scala.concurrent.ExecutionContext
import scala.deriving.*
import scala.compiletime.{summonInline, summonAll}

trait InsertWriter[T]:
  def toInsertValues(value: T, tableName: String): List[(String, js.Any)]

class DerivedInsertWriter[T](
    writers: List[(String, ColumnWriter[?], Boolean)],
    get: T => Product,
) extends InsertWriter[T]:
  def toInsertValues(value: T, tableName: String): List[(String, js.Any)] =
    val product = get(value)
    writers.zip(product.productIterator.toList)
      .filterNot { case ((_, _, isPK), value) =>
        isPK && (value == null || value == "")
      }
      .map { case ((dbColumn, writer, _), value) =>
        dbColumn -> writer.asInstanceOf[ColumnWriter[Any]].write(value)
      }

object InsertWriter:
  inline given derived[T]: InsertWriter[T] =
    inline summonInline[Mirror.Of[T]] match
      case m: Mirror.ProductOf[T] =>
        val labels = RowReader.getLabels[m.MirroredElemLabels]
        val writers = summonAll[Tuple.Map[m.MirroredElemTypes, ColumnWriter]].toList
          .asInstanceOf[List[ColumnWriter[?]]]
        val isPKs = PKHelper.summonIsPKs[T]
        val pairs = labels.zip(writers).zip(isPKs).map { case ((l, w), pk) => (l, w, pk) }
        DerivedInsertWriter(pairs, (t: T) => t.asInstanceOf[Product])

abstract class Table[T](using val reader: RowReader[T], val writer: InsertWriter[T]):
  def name: String

  def insert(items: T*)(using client: Client, ec: ExecutionContext): Future[List[T]] =
    if items.isEmpty then
      Future.failed(new Exception("No values to insert"))
    else
      val columnValues = writer.toInsertValues(items.head, name)
      val columns      = columnValues.map(_._1)

      val placeholders = items.indices.map { i =>
        val offset = i * columns.length + 1
        s"(${columns.indices.map(j => s"$$${offset + j}").mkString(", ")})"
      }.mkString(", ")

      val values = items.flatMap(item => writer.toInsertValues(item, name).map(_._2))
      val query = s"""
        INSERT INTO $name
        (${columns.mkString(", ")})
        VALUES $placeholders
        RETURNING *
      """

      client.query(query, js.Array(values*))
        .map(result => PgConverter.asList[T](result.rows))

  def findAll()(using client: Client, ec: ExecutionContext): Future[List[T]] =
    client.query(s"SELECT * FROM $name")
      .map(result => PgConverter.asList[T](result.rows))

  def findById[K](id: K)(using client: Client, ec: ExecutionContext): Future[Option[T]] =
    client.query(s"SELECT * FROM $name WHERE id = $$1", js.Array(id.asInstanceOf[js.Any]))
      .map(result => PgConverter.asList[T](result.rows).headOption)

  def where(condition: String)(using client: Client, ec: ExecutionContext): Future[List[T]] =
    client.query(s"SELECT * FROM $name WHERE $condition")
      .map(result => PgConverter.asList[T](result.rows))

  def orderBy(field: String, ascending: Boolean = true)(using client: Client, ec: ExecutionContext): Future[List[T]] =
    val direction = if ascending then "ASC" else "DESC"
    client.query(s"SELECT * FROM $name ORDER BY $field $direction")
      .map(result => PgConverter.asList[T](result.rows))
