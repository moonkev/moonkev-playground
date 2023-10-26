//credit to https://gist.github.com/davideicardi/2a6e2a93507a731026160c2d383528ab

object ConditionalFlow {
  def apply[M](condition: M => Boolean,
               conditionalFlow: Flow[M, M, NotUsed]): Graph[FlowShape[M, M], NotUsed] = {
    Flow.fromGraph(GraphDSL.create() {
      implicit builder => {
        import GraphDSL.Implicits._

        val partition = builder.add(Partition[M](2, {
          case x if condition(x) => 0
          case _ => 1
        }))
        val merge = builder.add(Merge[M](2))

        partition ~> conditionalFlow ~> merge.in(0)
        partition ~> merge.in(1)

        FlowShape(partition.in, merge.out)
      }
    })
  }
}
