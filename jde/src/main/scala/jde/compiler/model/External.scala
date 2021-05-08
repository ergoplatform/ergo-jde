package jde.compiler.model

import jde.compiler._
import kiosk.ergo.{DataType, KioskBox, KioskCollByte, KioskErgoTree, KioskInt, KioskLong, KioskType}
import jde.compiler.model.FilterOp.Op
import jde.compiler.model.MatchingOptions.Options
import jde.compiler.model.RegNum.Num
import kiosk.ergo.DataType.Type

import java.util.UUID
import scala.util.Try

/*
  contains objects exposed externally (these form the primitives of the source code and the compiled results)
 */

case class Program(
    constants: Option[Seq[Constant]],
    auxInputs: Option[Seq[Input]],
    dataInputs: Option[Seq[Input]],
    inputs: Option[Seq[Input]],
    outputs: Option[Seq[Output]],
    fee: Option[scala.Long],
    binaryOps: Option[Seq[BinaryOp]],
    unaryOps: Option[Seq[UnaryOp]],
    branches: Option[Seq[Branch]],
    postConditions: Option[Seq[PostCondition]],
    returns: Option[Seq[String]]
) {
  private[compiler] def withUuid(input: Input): (Input, UUID) = input -> UUID.randomUUID
  private[compiler] lazy val auxInputUuids: Option[Seq[(Input, UUID)]] = auxInputs.map(_.map(withUuid))
  private[compiler] lazy val dataInputUuids: Option[Seq[(Input, UUID)]] = dataInputs.map(_.map(withUuid))
  private[compiler] lazy val inputUuids: Option[Seq[(Input, UUID)]] = inputs.map(_.map(withUuid))
}

case class Input(
    id: Option[Id],
    address: Option[Address],
    registers: Option[Seq[Register]],
    tokens: Option[Seq[Token]],
    nanoErgs: Option[Long],
    options: Option[Set[MatchingOptions.Options]]
) {
  atLeastOne(this)("id", "address")(id, address)
  private lazy val inputOptions: Set[Options] = options.getOrElse(Set.empty)
  lazy val strict: Boolean = inputOptions.contains(MatchingOptions.Strict) // applies to token matching only
  lazy val multi: Boolean = inputOptions.contains(MatchingOptions.Multi)
  lazy val optional: Boolean = inputOptions.contains(MatchingOptions.Optional)
}

case class Output(
    address: Address,
    registers: Option[Seq[Register]],
    tokens: Option[Seq[Token]],
    nanoErgs: Long,
    options: Option[Set[MatchingOptions.Options]]
) {
  optSeq(tokens).foreach(token => requireDefined(token.index -> "token index", token.id -> "token.id", token.amount -> "token amount"))
  optSeq(tokens).foreach(token =>
    for { id <- token.id; amount <- token.amount } requireEmpty(
      id.name -> "Output token.id.name",
      amount.name -> "Output token.amount.name",
      amount.filter -> "Output token.amount.filter"
    )
  )
  requireEmpty(optSeq(registers).map(_.name -> "Output register.name"): _*)
  requireEmpty(address.name -> "Output address.name", nanoErgs.name -> "Output nanoErgs.name", nanoErgs.filter -> "Output nanoErgs.filter")
  private lazy val outputOptions: Set[Options] = options.getOrElse(Set.empty)
  lazy val multi: Boolean = outputOptions.contains(MatchingOptions.Multi)
  lazy val optional: Boolean = outputOptions.contains(MatchingOptions.Optional)
  options.fold(())(optionSet => if (optionSet.contains(MatchingOptions.Strict)) throw new Exception(s"'Strict' option not allowed in output") else ())
}

case class Address(name: Option[String], value: Option[String]) extends Declaration {
  override lazy val maybeTargetId: Option[String] = name
  override lazy val pointerNames: Seq[String] = value.toSeq
  override var `type`: Type = DataType.Address
  override lazy val pointerTypes: Seq[Type] = pointerNames.map(_ => DataType.Address)
  override lazy val isLazy = false
  override lazy val canPointToOnChain: Boolean = true
  atLeastOne(this)("name", "value")(name, value)
  override def getValue(implicit dictionary: Dictionary): Multiple[KioskErgoTree] = to[KioskErgoTree](super.getValue)
  override def getTargets(implicit dictionary: Dictionary): Seq[KioskErgoTree] = super.getTargets.map(_.asInstanceOf[KioskErgoTree])
}

case class Register(name: Option[String], value: Option[String], num: Num, var `type`: Type) extends Declaration {
  override lazy val maybeTargetId: Option[String] = name
  override lazy val pointerNames: Seq[String] = value.toSeq
  override lazy val pointerTypes: Seq[Type] = pointerNames.map(_ => `type`)
  override lazy val isLazy = false
  override lazy val canPointToOnChain: Boolean = true
}

case class Id(name: Option[String], value: Option[String]) extends Declaration {
  override lazy val maybeTargetId: Option[String] = name
  override lazy val pointerNames: Seq[String] = value.toSeq
  override var `type`: Type = DataType.CollByte
  override lazy val pointerTypes: Seq[Type] = pointerNames.map(_ => DataType.CollByte)
  override lazy val isLazy = false
  override lazy val canPointToOnChain: Boolean = true
  override def getTargets(implicit dictionary: Dictionary): Seq[KioskCollByte] = to[KioskCollByte](super.getTargets)
  override def getValue(implicit dictionary: Dictionary): Multiple[KioskCollByte] = {
    val kioskCollBytes = to[KioskCollByte](super.getValue)
    kioskCollBytes.foreach(kioskCollByte => {
      if (kioskCollByte.arrayBytes.length != 32) {
        throw new Exception(s"Id $this (${kioskCollByte.stringValue}) size (${kioskCollByte.arrayBytes.length}) != 32")
      }
    })
    kioskCollBytes
  }
}

case class Long(name: Option[String], value: Option[String], filter: Option[FilterOp.Op]) extends Declaration {
  override lazy val maybeTargetId: Option[String] = name
  override lazy val pointerNames: Seq[String] = value.toSeq
  override var `type`: Type = DataType.Long
  override lazy val pointerTypes: Seq[Type] = pointerNames.map(_ => DataType.Long)
  override lazy val isLazy = false
  override lazy val canPointToOnChain: Boolean = true
  lazy val filterOp: Op = filter.getOrElse(FilterOp.Eq)
  override def getTargets(implicit dictionary: Dictionary): Seq[KioskLong] = to[KioskLong](super.getTargets)
  override def getValue(implicit dictionary: Dictionary): Multiple[KioskLong] = to[KioskLong](super.getValue)
  if (filter.nonEmpty && value.isEmpty) throw new Exception(s"Value cannot be empty if filter is defined")
  if (filter.contains(FilterOp.Eq)) throw new Exception(s"Filter cannot be Eq")
  atLeastOne(this)("name", "value")(name, value)
  for { _ <- name; _ <- value } require(filter.isDefined, s"Filter must be defined if both name and values are defined")
}

case class Token(index: Option[Int], id: Option[Id], amount: Option[Long]) {
  index.map(int => require(int >= 0, s"Token index must be >= 0. $this"))
  atLeastOne(this)("index", "id")(index, id)
  id.map(someId => atLeastOne(someId)("index", "id.value")(index, someId.value))
}

case class Constant(name: String, var `type`: DataType.Type, value: Option[String], values: Option[Seq[String]]) extends Declaration {
  override lazy val maybeTargetId: Option[String] = Some(name)
  override lazy val pointerNames: Seq[String] = Nil
  override lazy val pointerTypes: Seq[Type] = Nil
  override lazy val isLazy = true
  override def getValue(implicit dictionary: Dictionary): Multiple[KioskType[_]] =
    Multiple((value.toSeq ++ optSeq(values)).map(DataType.getValue(_, `type`)): _*)
  override lazy val canPointToOnChain: Boolean = false
  exactlyOne(this)("value", "values")(value, values)
  values.map(strings => require(strings.size > 1, s"At least two constants must be defined using 'values'. For single constant, use 'value'"))
  require(`type` != DataType.Unknown, "Data type cannot be unknown")
}

case class BinaryOp(name: String, first: String, op: BinaryOperator.Operator, second: String) extends Declaration {
  override lazy val maybeTargetId: Option[String] = Some(name)
  override lazy val pointerNames: Seq[String] = Seq(first, second)
  override var `type`: Type = DataType.Unknown
  override lazy val pointerTypes = Seq(DataType.Unknown, DataType.Unknown)
  override lazy val isLazy = true
  override lazy val canPointToOnChain: Boolean = false
  override def getValue(implicit dictionary: Dictionary): Multiple[KioskType[_]] = {
    Try(getMultiPairs(first, second))
      .fold(ex => throw new Exception(s"Error evaluating binary op $op ($name)").initCause(ex), pairs => pairs)
      .map(BinaryOperator.operate(op, _))
  }
}

case class UnaryOp(name: String, from: String, op: UnaryOperator.Operator) extends Declaration {
  override lazy val maybeTargetId: Option[String] = Some(name)
  override lazy val pointerNames = Seq(from)
  lazy val types: FromTo = UnaryOperator.getFromTo(op)
  override var `type`: Type = types.to
  override lazy val pointerTypes = Seq(types.from)
  override lazy val isLazy = true
  override lazy val canPointToOnChain: Boolean = false
  override def getValue(implicit dictionary: Dictionary): Multiple[KioskType[_]] =
    UnaryOperator.operate(op, dictionary.getDeclaration(from).getValue, `type`)
}

case class Condition(first: String, second: String, op: FilterOp.Op) {
  lazy val pointerNames: Seq[String] = Seq(first, second)
  def evaluate(implicit dictionary: Dictionary): Boolean = evaluateWithResult._1
  def evaluateWithResult(implicit dictionary: Dictionary): (Boolean, Seq[KioskType[_]], Seq[KioskType[_]]) = {
    Try(getMultiPairs(first, second))
      .fold(ex => throw new Exception(s"Error evaluating condition $op").initCause(ex), pairs => pairs)
      .seq
      .foldLeft((true, Seq[KioskType[_]](), Seq[KioskType[_]]())) {
        case ((booleanBefore, firsts, seconds), (thisFirst, thisSecond)) =>
          val thisBoolean = (thisFirst, thisSecond) match {
            case (left: KioskLong, right: KioskLong)                                   => FilterOp.matches(left.value, right.value, op)
            case (left: KioskLong, right: KioskInt)                                    => FilterOp.matches(left.value, right.value, op)
            case (left: KioskInt, right: KioskLong)                                    => FilterOp.matches(left.value, right.value, op)
            case (left: KioskInt, right: KioskInt)                                     => FilterOp.matches(left.value, right.value, op)
            case (left, right) if left.typeName == right.typeName && op == FilterOp.Eq => left.hex == right.hex
            case (left, right) if left.typeName == right.typeName && op == FilterOp.Ne => left.hex != right.hex
            case (left, right)                                                         => throw new Exception(s"Invalid types for $op: ${left.typeName},${right.typeName}")
          }
          (booleanBefore && thisBoolean, firsts :+ thisFirst, seconds :+ thisSecond)
      }
  }
}

case class Branch(name: String, ifTrue: String, ifFalse: String, condition: Condition) extends Declaration {
  override protected lazy val maybeTargetId: Option[String] = Some(name)
  override protected lazy val pointerNames: Seq[String] = Seq(ifTrue, ifFalse) ++ condition.pointerNames
  override var `type`: Type = DataType.Unknown
  override protected lazy val pointerTypes: Seq[Type] = pointerNames.map(_ => DataType.Unknown)
  override lazy val isLazy: Boolean = true
  override lazy val canPointToOnChain: Boolean = false
  override def getValue(implicit dictionary: Dictionary): Multiple[KioskType[_]] =
    (if (condition.evaluate) dictionary.getDeclaration(ifTrue) else dictionary.getDeclaration(ifFalse)).getValue
}

case class PostCondition(first: String, second: String, op: FilterOp.Op) extends Declaration {
  override protected lazy val maybeTargetId: Option[String] = None
  private lazy val condition = Condition(first, second, op)
  override protected lazy val pointerNames: Seq[String] = condition.pointerNames
  override var `type`: Type = DataType.Unknown
  override protected lazy val pointerTypes: Seq[Type] = pointerNames.map(_ => DataType.Unknown)
  override lazy val isLazy: Boolean = true
  override lazy val canPointToOnChain: Boolean = false
  def validate(implicit dictionary: Dictionary): Unit = {
    val (result, firsts, seconds) = condition.evaluateWithResult
    if (!result) throw new Exception(s"Failed post-condition: $first: (${firsts.mkString(",")}) $op $second (${seconds.mkString(",")})")
  }
  override def getValue(implicit dictionary: Dictionary): Multiple[KioskType[_]] = throw new Exception("Cannot call getValue on post-condition")
}

case class CompileResult(
    dataInputBoxIds: Seq[String],
    inputBoxIds: Seq[String],
    inputNanoErgs: scala.Long,
    inputTokens: Seq[(String, scala.Long)],
    outputs: Seq[KioskBox],
    outputNanoErgs: scala.Long,
    outputTokens: Seq[(String, scala.Long)],
    fee: Option[scala.Long],
    returned: Seq[ReturnedValue]
)

case class ReturnedValue(name: String, `type`: DataType.Type, values: Seq[KioskType[_]])
