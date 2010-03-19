package org.purview.core.analysis

import org.purview.core.analysis.settings.Setting
import org.purview.core.process.Computation
import org.purview.core.report.ReportEntry
import org.purview.core.session.AnalysisStats
import scala.util.DynamicVariable

private[core] object Analyser {
  val statsStore = new DynamicVariable[AnalysisStats](new AnalysisStats)

  /**
   * Runtime statistics for analysers to use
   * @returns The stacked thread-local statistics module
   */
  def statistics = statsStore.value
}

/**
 * An object that processes something and generates a report.
 */
abstract class Analyser[@specialized("Int,Float,Boolean") A] extends NotNull {

  /**
   * Runs this Analyser and generates a report
   * @param what That which should be analysed
   * @returns The report generated by this analyser
   */
  def analyse(what: A): Set[ReportEntry] = inputStore.withValue(Some(what)) {
    Computation.get(result())
  }

  /**
   * Runs this Analyser with the specified statistics module
   * @param what That which the analyser should analyser
   * @param s The statistics module to use
   * @returns The report generated by this analyser
   */
  def analyseWithStats(what: A)(implicit s: AnalysisStats) = Analyser.statsStore.withValue(s) {
    analyse(what)
  }

  /**
   * The result that this analyser should produce
   */
  protected def result(): Computation[Set[ReportEntry]]

  private object inputStore extends DynamicVariable[Option[A]](None)

  /**
   * The input that this analyser uses for computing the result
   */
  protected def input = Computation.unit(inputStore.value.get)

  /**
   * Reports this analysers current status
   * @param status The status to report
   */
  protected def status(status: String) = Analyser.statistics.reportStatus(status)
}

/**
 * Adds metadata information to an object, like for instance an Analyser.
 */
trait Metadata {
  /** The name of this object */
  def name: String

  /** The description of this object */
  def description: String

  /** This object's version */
  def version: String = ""

  /** The creator of this object */
  def author: String = ""
}

/**
 * Adds modifyable settings to an object, like for instance an analyser.
 */
trait Settings {
  /** A generalized settings object */
  type GenericSetting = Setting[A] forSome { type A }

  /** The sequence of settings that this object provides */
  val settings: Seq[GenericSetting]
}