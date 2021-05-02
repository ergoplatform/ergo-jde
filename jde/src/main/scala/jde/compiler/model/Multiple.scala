package jde.compiler.model

case class Multiple[+This](seq: This*) {
  def exists(f: This => Boolean): Boolean = seq.exists(f)

  def take(i: Int): Multiple[This] = seq.take(i)

  def isEmpty = seq.isEmpty

  def map[That](thisToThat: This => That): Multiple[That] = seq.map(thisToThat)

  def foreach(thisToUnit: This => Unit): Unit = seq.foreach(thisToUnit)

  def forall(thisToBoolean: This => Boolean): Boolean = seq.forall(thisToBoolean)

  def filter(thisToBoolean: This => Boolean): Multiple[This] = seq.filter(thisToBoolean)

  def length: Int = seq.length

  def head = seq.head

  def zip[That](those: Multiple[That]): Multiple[(This, That)] = {
    (this.seq.length, those.seq.length) match {
      case (1, _)                                         => those.seq.map(this.seq.head -> _)
      case (_, 1)                                         => this.seq.map(_ -> those.seq.head)
      case (firstLen, secondLen) if firstLen == secondLen => this.seq zip those.seq
      case (firstLen, secondLen)                          => throw new Exception(s"Wrong number of elements in multi-pairs: first has $firstLen and second has $secondLen")
    }
  }

}

object Multiple {
  private implicit def seq2Multiple[T](seq: Seq[T]): Multiple[T] = Multiple(seq: _*)

  def sequence[T](seq: Seq[Multiple[T]]): Multiple[Seq[T]] = {
    seq.foldLeft(Multiple[Seq[T]](Nil)) {
      case (left, right) =>
        (left zip right).map {
          case (left, right) => left :+ right
        }
    }
  }
}
