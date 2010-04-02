package org.purview.core.analysis

import org.purview.core.data.Matrix
import org.purview.core.data.MutableArrayMatrix
import org.purview.core.report.Information
import org.purview.core.report.Message
import org.purview.core.report.Point
import org.purview.core.report.Rectangle
import org.purview.core.report.ReportEntry
import org.specs.SpecificationWithJUnit
import scala.util.Random

class AnalyserSpec extends SpecificationWithJUnit {
  def randomNumbers(max: Int): Stream[Int] = Stream.cons(Random.nextInt(max), randomNumbers(max))
  
  "An analyser" should {
    "be able to generate reports" in {
      val a = new Analyser[Int] {
        val result = for(i <- input) yield Set[ReportEntry](new ReportEntry with Message {
            val level = Information
            val message = "Analysed " + i
          })
      }
      a.analyse(42) must not be empty
      a.analyse(2).partialMap {
        case x: Message =>
          x.message must_== "Analysed 2"
      }
    }
  }
/* //XXX: Doesn't work with Scala 2.8.0.Beta1
  "A heat map analyser" should {
    val matrix = new MutableArrayMatrix[Float](16, 16)

    "output a predefined message" in {
      val msg = "Hello, jakhlk"
      val analyser = new HeatMapAnalyser[Float, Matrix[Float]] {
        def heatmap = input
        override val message = msg
      }
      matrix(0, 0) = 500
      analyser.analyse(matrix).partialMap {
        case entry: Rectangle with Message =>
          entry.message must_== msg
      }
    }

    "find a rogue maximum in a matrix" in {
      val analyser = new HeatMapAnalyser[Float, Matrix[Float]] {
        def heatmap = input
        override def minRegionSize = 0
      }
      val posX = randomNumbers(16).head
      val posY = randomNumbers(16).head
      matrix(posX, posY) = 9000 //high number

      val report = analyser.analyse(matrix)

      report must have size 3 //there's only one rogue, right? + the raw images

      report partialMap {
        case p: Point with Rectangle =>
          p.x must_== posX
          p.y must_== posY
      }
    }

    "find maximi of the same magnitude" in {
      val randomPoints = (randomNumbers(16) zip randomNumbers(16)).take(16).force

      randomPoints.foreach(point => matrix(point._1, point._2) = 9000)

      val analyser = new HeatMapAnalyser[Float, Matrix[Float]] {
        def heatmap = input
        override def accumulate = false
        override def minRegionSize = 0
      }

      val report = analyser.analyse(matrix)

      report must have size 18 //points + raw images

      report partialMap {
        case p: Point with Rectangle =>
          randomPoints must contain (p.x, p.y)
      }
    }
  }
  */
}