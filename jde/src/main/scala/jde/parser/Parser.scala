package jde.parser

import jde.compiler.model.{Long => JLong, Constant => JConstant, _}
import kiosk.ergo.{DataType, KioskBox, KioskType}
import play.api.libs.json._

object Parser {
  import scala.reflect.runtime.universe._

  def checkedReads[T](underlyingReads: Reads[T])(implicit typeTag: TypeTag[T]): Reads[T] =
    new Reads[T] {

      def classFields[B: TypeTag]: Set[String] =
        typeOf[B].members.collect {
          case m: MethodSymbol if m.isCaseAccessor => m.name.decodedName.toString
        }.toSet

      def reads(json: JsValue): JsResult[T] = {
        val caseClassFields = classFields[T]
        json match {
          case JsObject(fields) if (fields.keySet -- caseClassFields).nonEmpty =>
            JsError(s"Unexpected fields provided: ${(fields.keySet -- caseClassFields).mkString(", ")}")
          case _ => underlyingReads.reads(json)
        }
      }
    }

  private implicit val readsInputOptions = new Reads[MatchingOptions.Options] {
    override def reads(json: JsValue): JsResult[MatchingOptions.Options] = JsSuccess(MatchingOptions.fromString(json.as[String]))
  }
  private implicit val writesInputOptions = new Writes[MatchingOptions.Options] {
    override def writes(o: MatchingOptions.Options): JsValue = JsString(MatchingOptions.toString(o))
  }

  private implicit val readsUnaryOperator = new Reads[UnaryOperator.Operator] {
    override def reads(json: JsValue): JsResult[UnaryOperator.Operator] = JsSuccess(UnaryOperator.fromString(json.as[String]))
  }
  private implicit val writesUnaryOperator = new Writes[UnaryOperator.Operator] {
    override def writes(o: UnaryOperator.Operator): JsValue = JsString(UnaryOperator.toString(o))
  }

  private implicit val readsBinaryOperator = new Reads[BinaryOperator.Operator] {
    override def reads(json: JsValue): JsResult[BinaryOperator.Operator] = JsSuccess(BinaryOperator.fromString(json.as[String]))
  }
  private implicit val writesBinaryOperator = new Writes[BinaryOperator.Operator] {
    override def writes(o: BinaryOperator.Operator): JsValue = JsString(BinaryOperator.toString(o))
  }

  private implicit val readsRegId = new Reads[RegNum.Num] {
    override def reads(json: JsValue): JsResult[RegNum.Num] = JsSuccess(RegNum.fromString(json.as[String]))
  }
  private implicit val writesRegId = new Writes[RegNum.Num] {
    override def writes(o: RegNum.Num): JsValue = JsString(RegNum.toString(o))
  }

  private implicit val readsQuantifierOp = new Reads[FilterOp.Op] {
    override def reads(json: JsValue): JsResult[FilterOp.Op] = JsSuccess(FilterOp.fromString(json.as[String]))
  }
  private implicit val writesQuantifierOp = new Writes[FilterOp.Op] {
    override def writes(o: FilterOp.Op): JsValue = JsString(FilterOp.toString(o))
  }

  private implicit val readsDataType = new Reads[DataType.Type] {
    override def reads(json: JsValue): JsResult[DataType.Type] = JsSuccess(DataType.fromString(json.as[String]))
  }
  private implicit val writesDataType = new Writes[DataType.Type] {
    override def writes(o: DataType.Type): JsValue = JsString(DataType.toString(o))
  }

  private implicit val readsBinaryOp = checkedReads(Json.reads[BinaryOp])
  private implicit val writesBinaryOp = Json.writes[BinaryOp]
  private implicit val readsUnaryOp = checkedReads(Json.reads[UnaryOp])
  private implicit val writesUnaryOp = Json.writes[UnaryOp]
  private implicit val readsLong = checkedReads(Json.reads[JLong])
  private implicit val writesLong = Json.writes[JLong]
  private implicit val readsRegister = checkedReads(Json.reads[Register])
  private implicit val writesRegister = Json.writes[Register]
  private implicit val readsAddress = checkedReads(Json.reads[Address])
  private implicit val writesAddress = Json.writes[Address]
  private implicit val readsId = checkedReads(Json.reads[Id])
  private implicit val writesId = Json.writes[Id]
  private implicit val readsToken = checkedReads(Json.reads[Token])
  private implicit val writesToken = Json.writes[Token]
  private implicit val readsInput = checkedReads(Json.reads[Input])
  private implicit val writesInput = Json.writes[Input]
  private implicit val readsOutput = checkedReads(Json.reads[Output])
  private implicit val writesOutput = Json.writes[Output]
  private implicit val readsConstant = checkedReads(Json.reads[JConstant])
  private implicit val writesConstant = Json.writes[JConstant]
  private implicit val readsCondition = checkedReads(Json.reads[Condition])
  private implicit val writesCondition = Json.writes[Condition]
  private implicit val readsBranch = checkedReads(Json.reads[Branch])
  private implicit val writesBranch = Json.writes[Branch]
  private implicit val readsPostCondition = checkedReads(Json.reads[PostCondition])
  private implicit val writesPostCondition = Json.writes[PostCondition]

  private implicit val readsProgram = checkedReads(Json.reads[Program])
  private implicit val writesProgram = Json.writes[Program]

  private implicit val writeKioskType = new Writes[KioskType[_]] {
    override def writes(o: KioskType[_]): JsValue = JsString(o.hex)
  }
  private implicit val writesKioskBox = Json.writes[KioskBox]

  implicit val writesReturnedValue = new Writes[ReturnedValue] {
    override def writes(o: ReturnedValue): JsValue = {
      val (valueKey: String, valueVal: JsValue) =
        if (o.values.size == 1) ("value", JsString(o.values.head.toString)) else ("value", Json.arr(o.values.map(_.toString)))
      Json.obj("name" -> o.name, valueKey -> valueVal)
    }
  }
  implicit val writesCompileResult = Json.writes[CompileResult]
  def parse(string: String) = Json.parse(string).as[Program]
  def unparse(program: Program) = Json.toJson(program)
}
