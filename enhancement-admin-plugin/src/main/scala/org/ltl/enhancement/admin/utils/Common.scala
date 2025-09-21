package org.ltl.enhancement.admin.utils

object Common {
  def getLastIndex(fileName: String) = fileName.lastIndexOf('.')

  def replaceWithWebp(fileName: String): String =
    getLastIndex(fileName) match
      case -1 => s"$fileName.webp"
      case i  => fileName.substring(0, i) + ".webp"

  def replaceWithBmp(fileName: String): String = {
    getLastIndex(fileName) match {
      case -1 => s"$fileName.bmp"
      case i  => fileName.substring(0, i) + ".bmp"
    }
  }
}
