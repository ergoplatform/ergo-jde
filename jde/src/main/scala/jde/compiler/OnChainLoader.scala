package jde.compiler

import kiosk.explorer.Explorer
import jde.compiler.model.Protocol
import jde.reader.Reader

class OnChainLoader(explorer: Explorer)(implicit dictionary: Dictionary) {
  val reader = new Reader(explorer)
  def load(protocol: Protocol) = {
    optSeq(protocol.auxInputUuids).zipWithIndex.foreach { // fetch aux-input boxes from explorer and load into dictionary
      case ((auxInput, uuid), index) =>
        val boxes = reader.getBoxes(auxInput, dictionary.getAuxInputBoxIds)
        val boxesToAdd = if (auxInput.multi) boxes else boxes.take(1)
        if (!auxInput.optional && boxesToAdd.isEmpty) throw new Exception(s"No box matched for aux-input at index $index")
        dictionary.addAuxInput(boxesToAdd, uuid)
    }
    optSeq(protocol.dataInputUuids).zipWithIndex.foreach { // fetch data-input boxes from explorer and load into dictionary
      case ((dataInput, uuid), index) =>
        val boxes = reader.getBoxes(dataInput, dictionary.getDataInputBoxIds)
        val boxesToAdd = if (dataInput.multi) boxes else boxes.take(1)
        if (!dataInput.optional && boxesToAdd.isEmpty) throw new Exception(s"No box matched for data-input at index $index")
        dictionary.addDataInput(boxesToAdd, uuid)
    }
    optSeq(protocol.inputUuids).zipWithIndex.foreach { // fetch input boxes from explorer and load into dictionary
      case ((input, uuid), index) =>
        val boxes = reader.getBoxes(input, dictionary.getInputBoxIds)

        val boxesToAdd = if (input.multi) boxes else boxes.take(1)
        if (!input.optional && boxesToAdd.isEmpty) throw new Exception(s"No box matched for input at index $index")
        dictionary.addInput(boxesToAdd, uuid)
    }
  }
}
