package symphony.schema

final class RootSchema private {}

object RootSchema {
  def builder() = new RootSchemaBuilder

  final class RootSchemaBuilder {
    def build() = new RootSchema
  }
}
