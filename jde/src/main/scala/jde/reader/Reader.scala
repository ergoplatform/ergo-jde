package jde.reader

import kiosk.ergo._
import kiosk.explorer.Explorer
import jde.compiler.model._
import jde.compiler._

import scala.util.Try

class Reader(explorer: Explorer)(implicit dictionary: Dictionary) {

  def getBoxes(input: Input, alreadySelectedBoxIds: Seq[String]): Multiple[OnChainBox] = {
    val boxesById: Option[Seq[OnChainBox]] = for {
      id <- input.id
      _ <- id.value
    } yield Try(id.getValue.seq.map(boxId => OnChainBox.fromKioskBox(explorer.getBoxById(boxId.stringValue)))).getOrElse(Nil)

    // if box by id is defined then use those as the starting point to find boxes by address, otherwise use the set of all boxes
    def filterByAddress(address: String): Seq[OnChainBox] = {
      boxesById.map(_.filter(_.address == address)).getOrElse(explorer.getUnspentBoxes(address).map(OnChainBox.fromKioskBox))
    }

    val boxesByAddress: Option[Seq[OnChainBox]] = for {
      address <- input.address
      _ <- address.value
    } yield address.getTargets.map(tree2str).flatMap(filterByAddress)

    val matchedBoxes: Seq[OnChainBox] = boxesByAddress.getOrElse(boxesById.getOrElse(Nil))

    val filteredBySelectedBoxIds: Seq[OnChainBox] = matchedBoxes.filterNot(onChainBox => alreadySelectedBoxIds.contains(onChainBox.stringBoxId))

    val filteredByRegisters: Seq[OnChainBox] = filterByRegisters(filteredBySelectedBoxIds, optSeq(input.registers))

    val filteredByTokens: Seq[OnChainBox] = filterByTokens(filteredByRegisters, optSeq(input.tokens), input.strict)

    val filteredByNanoErgs: Seq[OnChainBox] = input.nanoErgs.fold(filteredByTokens) { nanoErgs =>
      val targets = nanoErgs.getTargets
      if (targets.isEmpty) filteredByTokens
      else
        filteredByTokens.filter(onChainBox => targets.exists(target => FilterOp.matches(onChainBox.nanoErgs.value, target.value, nanoErgs.filterOp)))
    }

    Multiple(filteredByNanoErgs: _*)
  }

  private def filterByRegisters(onChainBoxes: Seq[OnChainBox], registers: Seq[Register]): Seq[OnChainBox] =
    registers.foldLeft(onChainBoxes)((boxesBeforeFilter, register) => filterByRegister(boxesBeforeFilter, register))

  private def filterByRegister(onChainBoxes: Seq[OnChainBox], register: Register): Seq[OnChainBox] = {
    val index: Int = RegNum.getIndex(register.num)
    val filteredByType: Seq[OnChainBox] =
      onChainBoxes.filter(onChainBox => onChainBox.registers.size > index && DataType.isValid(onChainBox.registers(index), register.`type`))

    register.value.fold(filteredByType) { _ =>
      val expected: Multiple[KioskType[_]] = register.getValue
      filteredByType.filter { onChainBox =>
        val actual: KioskType[_] = onChainBox.registers(index)
        expected.exists(_.hex == actual.hex)
      }
    }
  }

  private case class TokenBox(onChainBox: OnChainBox, foundIds: Set[ID])

  private def filterByTokens(boxes: Seq[OnChainBox], tokens: Seq[model.Token], strict: Boolean): Seq[OnChainBox] = {
    tokens.foldLeft(boxes.map(box => TokenBox(box, Set.empty[String])))((before, token) => filterByToken(before, token)).collect {
      case TokenBox(box, ids) if !strict || box.stringTokenIds.toSet == ids => box
    }
  }

  private def matches(tokenAmount: KioskLong, long: model.Long): Boolean = {
    val targets: Seq[KioskLong] = long.getTargets
    targets.isEmpty || targets.exists(kioskLong => FilterOp.matches(tokenAmount.value, kioskLong.value, long.filterOp))
  }

  private def dummyId = Id(name = Some(randId), value = None)
  private def dummyLong = Long(name = Some(randId), value = None, filter = None)

  private def filterByToken(tokenBoxes: Seq[TokenBox], token: model.Token): Seq[TokenBox] = {
    val tokenId: Id = token.id.getOrElse(dummyId)
    val amount: Long = token.amount.getOrElse(dummyLong)
    (token.index, tokenId.value) match {
      case (Some(index), Some(_)) =>
        val expectedIds: Seq[String] = tokenId.getValue.map(_.stringValue).seq
        tokenBoxes
          .filter(tokenBox =>
            expectedIds.contains(tokenBox.onChainBox.stringTokenIds(index)) && matches(tokenBox.onChainBox.tokenAmounts(index), amount)
          )
          .map(box => box.copy(foundIds = box.foundIds ++ box.onChainBox.stringTokenIds.take(index + 1)))
      case (Some(index), None) =>
        tokenBoxes
          .filter(box => box.onChainBox.tokenIds.size > index && matches(box.onChainBox.tokenAmounts(index), amount))
          .map(box => box.copy(foundIds = box.foundIds ++ box.onChainBox.stringTokenIds.take(index + 1)))
      case (None, Some(_)) =>
        val expectedIds: Seq[String] = tokenId.getValue.map(_.stringValue).seq
        def findToken(tokenBox: TokenBox) = {
          tokenBox -> tokenBox.onChainBox.stringTokenIds.indices.find(index =>
            expectedIds.contains(tokenBox.onChainBox.stringTokenIds(index)) && matches(tokenBox.onChainBox.tokenAmounts(index), amount)
          )
        }

        tokenBoxes
          .map(findToken)
          .collect { case (tokenBox, Some(index)) => tokenBox -> index }
          .map { case (tokenBox, index) => tokenBox.copy(foundIds = tokenBox.foundIds ++ tokenBox.onChainBox.stringTokenIds.take(index + 1)) }
      case _ => throw new Exception(s"At least one of token.index or token.id.value must be defined in $token")
    }
  }
}
