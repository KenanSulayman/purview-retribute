package org.purview.principalcomponentanalyser

import org.purview.core.report._
import org.purview.core.transforms._
import org.purview.core.data.ImmutableMatrix
import org.purview.core.data.Matrix
import java.awt.image.BufferedImage
import org.purview.core.analysis.Analyser
import org.purview.core.data.ImageMatrix
import org.purview.core.process.Computation
import org.purview.core.report.ReportEntry
import org.purview.core.report.ReportImage
import scala.math._

class AnalyserImplementation extends Analyser[ImageMatrix]{
  val name = "PCA"
  val description = "Performs principal component analysis on the image."

  val splitChannels: Computation[(Matrix[Float], Matrix[Float], Matrix[Float])] =
    for(in <- input) yield (in.map(_.r), in.map(_.g), in.map(_.b))

  def mean(matrix: Matrix[Float]): Float = matrix.sum / (matrix.width * matrix.height)

  val meanR = for(channels <- splitChannels) yield mean(channels._1)
  val meanG = for(channels <- splitChannels) yield mean(channels._2)
  val meanB = for(channels <- splitChannels) yield mean(channels._3)
  
  val zeroMeanR = for(channels <- splitChannels; mean <- meanR) yield channels._1 map (_ - mean)
  val zeroMeanG = for(channels <- splitChannels; mean <- meanG) yield channels._2 map (_ - mean)
  val zeroMeanB = for(channels <- splitChannels; mean <- meanB) yield channels._3 map (_ - mean)

  val covarianceMatrix = for(r <- zeroMeanR; g <- zeroMeanG; b <- zeroMeanB) yield {
    def covariance(a1: Matrix[Float], a2: Matrix[Float]): Float = {
      val result = a1.zip(a2).foldLeft(0f) ((acc, elem) => acc + elem._1 * elem._2)
      (result / (a1.width * a1.height - 1)).toFloat
    }
    val covRR = covariance(r, r)
    val covRG = covariance(r, g)
    val covRB = covariance(r, b)

    val covGG = covariance(g, g)
    val covGB = covariance(g, b)

    val covBB = covariance(b, b)

    new ImmutableMatrix(3, 3, Array(
        covRR, covRG, covRB,
        covRG, covGG, covGB,
        covRB, covGB, covBB
      ))
  }

  val mergedZeroMeans = for(r <- zeroMeanR; g <- zeroMeanG; b <- zeroMeanB) yield
    for {
      (x, y, value1) <- r.cells
      value2 = g(x, y)
      value3 = b(x, y)
    } yield (value1, value2, value3)

  val eigenVectors = for(matrix <- covarianceMatrix) yield
    new EigenvalueDecomposition(matrix).vectors

  @inline private def dotProduct(vector1: (Float, Float, Float), vector2: (Float, Float, Float)) =
    vector1._1 * vector2._1 + vector1._2 * vector2._2 + vector1._3 * vector2._3

  @inline private def principalComponent(vector: (Float, Float, Float), rgb: Matrix[(Float, Float, Float)]) =
    rgb map (x => dotProduct(vector, x))

  val principalComponent1 = for(vectors <- eigenVectors; rgb <- mergedZeroMeans) yield
    principalComponent((vectors(0, 0), vectors(0, 1), vectors(0, 2)), rgb)
  val principalComponent2 = for(vectors <- eigenVectors; rgb <- mergedZeroMeans) yield
    principalComponent((vectors(1, 0), vectors(1, 1), vectors(1, 2)), rgb)
  val principalComponent3 = for(vectors <- eigenVectors; rgb <- mergedZeroMeans) yield
    principalComponent((vectors(2, 0), vectors(2, 1), vectors(2, 2)), rgb)

  @inline private def image(component: Matrix[Float]) = {
    val result = new BufferedImage(component.width, component.height, BufferedImage.TYPE_INT_RGB)
    for((x, y, value) <- component.cells) {
      val c = abs(component(x, y) * 255).toInt
      result.setRGB(x, y, c | c << 8 | c << 16 | 0xff000000)
    }
    result
  }

  val image1 = for(component <- principalComponent1) yield image(component)
  val image2 = for(component <- principalComponent2) yield image(component)
  val image3 = for(component <- principalComponent3) yield image(component)

  val result: Computation[Set[ReportEntry]] =
    for(img1 <- image1; img2 <- image2; img3 <- image3) yield
      Set(new ReportImage(Information, "Principal component #1", 0, 0, img1),
          new ReportImage(Information, "Principal component #2", 0, 0, img2),
          new ReportImage(Information, "Principal component #3", 0, 0, img3))
}