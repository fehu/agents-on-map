package feh.tec.visual.render

import feh.tec.agent.{AgentExecutionLoop, AgentPerformanceMeasure, Environment, AbstractAction}
import feh.tec.visual.api._
import feh.tec.visual.NicolLike2DEasel
import feh.tec.agent.AgentDecision.CriteriaReasonedDecision
import feh.tec.visual.helpers.Font.Arial._
import java.awt.Color

object CriteriaReasonedDecisionRenderer{
  case class Scheme(criterionFont: String, criterionColor: Color,
                    beforeCriteriaFont: String, beforeCriteriaColor: Color,
                    afterCriteriaFont: String, afterCriteriaColor: Color,
                    frameColor: Option[Color], sepLineColor: Option[Color])
}

class CriteriaReasonedDecisionRenderer[Coord, State, Global, Action <: AbstractAction,
                                       Env <: Environment[Coord, State, Global, Action, Env],
                                       Exec <: AgentExecutionLoop[Coord, State, Global, Action, Env],
                                       M <: AgentPerformanceMeasure[Coord, State, Global, Action, Env, M]]
    (val scheme: CriteriaReasonedDecisionRenderer.Scheme)
  extends Renderer[CriteriaReasonedDecision[Coord, State, Global, Action, Env, Exec, M], NicolLike2DEasel]
{

  def criteriaValueOps = BasicStringDrawOps[NicolLike2DEasel](StringAlignment.Left, scheme.criterionColor, scheme.criterionFont, 12, 3)
  lazy val criteriaValueRenderer = new DecisionCriterionValueRenderer[Coord, State, Global, Action, Env, Exec, M](criteriaValueOps)

  lazy val beforeCriteriaOps = BasicStringDrawOps[NicolLike2DEasel](StringAlignment.Left, scheme.beforeCriteriaColor, scheme.beforeCriteriaFont, 12, 3)
  lazy val afterCriteriaOps = BasicStringDrawOps[NicolLike2DEasel](StringAlignment.Left, scheme.afterCriteriaColor, scheme.afterCriteriaFont, 12, 3)

  def render(t: CriteriaReasonedDecision[Coord, State, Global, Action, Env, Exec, M])(implicit easel: NicolLike2DEasel): Unit = {
    import easel.WithoutTextures

    val frameWidth = 450

    val vOffset = 30
    val beforeMsgOffset = t.message.beforeOpt.map{
      s =>

        easel.drawString(s, 10F -> vOffset, beforeCriteriaOps)
        val n = s.count('\n' ==)
        easel.Offset(0, 20 + vOffset + beforeCriteriaOps.size*(n+1) + beforeCriteriaOps.vSpacing*n)
    }.getOrElse(easel.Offset(0, vOffset))

    val intraCriteriaOffset = 30
    t.criteria.zipWithIndex.foreach{
      case (cval , i) =>
        def offset(i: Int) = beforeMsgOffset + easel.Offset(10, intraCriteriaOffset*i) // todo: multi-line criteria render value ?
        easel.withAffineTransform(offset(i)){
          criteriaValueRenderer.render(cval)
        }
    }
    val lineOffset = beforeMsgOffset + easel.Offset(0, t.criteria.size * intraCriteriaOffset + 10)
    scheme.sepLineColor.foreach(c => easel.withColor(c){
      easel.drawLine(lineOffset, lineOffset + (frameWidth, 0)).withoutTextures
    })
    easel.drawString("Criteria value: " + t.measure, lineOffset + (20, 10), afterCriteriaOps)
    t.message.afterOpt.foreach(easel.drawString(_, lineOffset + (10, 40), afterCriteriaOps))

    scheme.frameColor.foreach(c => easel.withColor(c){
      easel.drawRect((0, lineOffset.c._2 + 70), frameWidth.toFloat -> -1F).withoutTextures  // todo: size
    })
  }
}

class DecisionCriterionValueRenderer[Coord, State, Global, Action <: AbstractAction,
                                     Env <: Environment[Coord, State, Global, Action, Env],
                                     Exec <: AgentExecutionLoop[Coord, State, Global, Action, Env],
                                     M <: AgentPerformanceMeasure[Coord, State, Global, Action, Env, M]]
    (val how: NicolLike2DEasel#StrDrawOptions)
  extends CriterionValueRenderer[Coord, State, Global, Action, Env, M, NicolLike2DEasel]
{
  def render(t: M#CriterionValue)(implicit easel: NicolLike2DEasel): Unit = easel.drawString(s"${t.name}: ${t.value}", how)
}