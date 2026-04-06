package kaze

trait KisokuKaze {
  final val maxCellLength: Long = 64
  final val maxRowLength: Long = 32 * maxCellLength
  final val batchSize: Long = 1024
  final val bufferLength: Long = maxRowLength * batchSize
  final val libraryPath = "./cpp/build/libkaze.dylib"
}

object KisokuKaze {
  given config: KisokuKaze = new KisokuKaze {}
}
