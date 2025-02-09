package io.github.edadma.pg

import scala.annotation.StaticAnnotation
import scala.quoted.*

case class PrimaryKey() extends StaticAnnotation

object PKHelper:
  inline def summonIsPKs[T]: List[Boolean] = ${ summonIsPKsImpl[T] }

  private def summonIsPKsImpl[T: Type](using Quotes): Expr[List[Boolean]] =
    import quotes.reflect.*
    val params = TypeRepr.of[T].typeSymbol.primaryConstructor.paramSymss.flatten
    val isPKs = params.map { param =>
      param.annotations.exists(_.tpe =:= TypeRepr.of[PrimaryKey])
    }
    Expr(isPKs)
