package org.http4s
package rho

import org.http4s.rho.bits.{TypedQuery, TypedHeader, HeaderAppendable}
import bits.PathAST._
import org.http4s.rho.bits.RequestAST.{AndRule, RequestRule}

import shapeless.ops.hlist.Prepend
import shapeless.{HNil, ::, HList}

/** Typed builder of query rules
  *
  * The [[QueryBuilder]] represents a builder for routes that already have a defined
  * method and path. It can accumulate query rules and mount decoders.
  *
  * @param method Request method to match.
  * @param path Path rules to execute.
  * @param rules Accumulated [[RequestRule]]'s.
  * @tparam T The HList representation of the types the route expects to extract
  *           from a `Request`.
  */
case class QueryBuilder[T <: HList](method: Method,
                                      path: PathRule,
                                     rules: RequestRule)
  extends RouteExecutable[T]
  with HeaderAppendable[T]
  with UriConvertible
  with RoutePrependable[QueryBuilder[T]]
{
  /** Capture a query rule
    *
    * @param query Query capture rule.
    * @tparam T1 types of elements captured by query.
    * @return a [[QueryBuilder]] with which to continue building the route.
    */
  def &[T1 <: HList](query: TypedQuery[T1])(implicit prep: Prepend[T1, T]): QueryBuilder[prep.Out] =
    QueryBuilder(method, path, AndRule(rules, query.rule))

  override def /:(prefix: TypedPath[HNil]): QueryBuilder[T] =
    new QueryBuilder[T](method, PathAnd(prefix.rule, path), rules)
  
  override type HeaderAppendResult[T <: HList] = Router[T]

  override def makeRoute(action: Action[T]): RhoRoute[T] = RhoRoute(Router(method, path, rules), action)

  override def >>>[T1 <: HList](rule: TypedHeader[T1])(implicit prep1: Prepend[T1, T]): Router[prep1.Out] =
    Router(method, path, AndRule(rules, rule.rule))
}
