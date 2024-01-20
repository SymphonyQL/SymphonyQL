package symphony
package parser
package adt

import Definition.ExecutableDefinition.*
import Definition.TypeSystemDefinition.*
import Definition.TypeSystemDefinition.TypeDefinition.*
import OperationType.*

final case class Document(definitions: List[Definition], sourceMapper: SourceMapper) {

  @transient lazy val directiveDefinitions: List[DirectiveDefinition] =
    definitions.collect { case dd: DirectiveDefinition => dd }

  @transient lazy val typeDefinitions: List[TypeDefinition] =
    definitions.collect { case td: TypeDefinition => td }

  @transient lazy val objectTypeDefinitions: List[ObjectTypeDefinition] =
    definitions.collect { case td: ObjectTypeDefinition => td }

  @transient lazy val inputObjectTypeDefinitions: List[InputObjectTypeDefinition] =
    definitions.collect { case td: InputObjectTypeDefinition => td }

  @transient lazy val interfaceTypeDefinitions: List[InterfaceTypeDefinition] =
    definitions.collect { case td: InterfaceTypeDefinition => td }

  @transient lazy val enumTypeDefinitions: List[EnumTypeDefinition] =
    definitions.collect { case td: EnumTypeDefinition => td }

  @transient lazy val scalarTypeDefinitions: List[ScalarTypeDefinition] =
    definitions.collect { case td: ScalarTypeDefinition => td }

  @transient lazy val unionTypeDefinitions: List[UnionTypeDefinition] =
    definitions.collect { case td: UnionTypeDefinition => td }

  @transient lazy val fragmentDefinitions: List[FragmentDefinition] =
    definitions.collect { case fd: FragmentDefinition => fd }

  @transient lazy val schemaDefinition: Option[SchemaDefinition] =
    definitions.collectFirst { case sd: SchemaDefinition => sd }

  @transient lazy val operationDefinitions: List[OperationDefinition] =
    definitions.collect { case od: OperationDefinition => od }

  @transient lazy val queryDefinitions: List[OperationDefinition] =
    definitions.collect { case od: OperationDefinition if od.operationType == Query => od }

  @transient lazy val mutationDefinitions: List[OperationDefinition] =
    definitions.collect { case od: OperationDefinition if od.operationType == Mutation => od }

  @transient lazy val subscriptionDefinitions: List[OperationDefinition] =
    definitions.collect { case od: OperationDefinition if od.operationType == Subscription => od }

  def objectTypeDefinition(name: String): Option[ObjectTypeDefinition] =
    objectTypeDefinitions.find(t => t.name == name)
}
