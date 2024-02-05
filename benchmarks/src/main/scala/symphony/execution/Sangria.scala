package symphony.execution

import sangria.macros.derive._
import sangria.schema._
import sangria.execution._
import sangria.marshalling.circe._
import scala.concurrent.Future

object Sangria {

  sealed trait Origin

  object Origin {
    case object EARTH extends Origin

    case object MARS extends Origin

    case object BELT extends Origin
  }

  sealed trait Role

  object Role {
    case class Captain(shipName: String) extends Role

    case class Pilot(shipName: String) extends Role

    case class Engineer(shipName: String) extends Role

    case class Mechanic(shipName: String) extends Role
  }

  case class Character(name: String, nicknames: List[String], origin: Origin, role: Option[Role])

  val characters = List(
    Character("James Holden", List("Jim", "Hoss"), Origin.EARTH, Some(Role.Captain("Rocinante"))),
    Character("Naomi Nagata", Nil, Origin.BELT, Some(Role.Engineer("Rocinante"))),
    Character("Amos Burton", Nil, Origin.EARTH, Some(Role.Mechanic("Rocinante"))),
    Character("Alex Kamal", Nil, Origin.MARS, Some(Role.Pilot("Rocinante"))),
    Character("Chrisjen Avasarala", Nil, Origin.EARTH, None),
    Character("Josephus Miller", List("Joe"), Origin.BELT, None),
    Character("Roberta Draper", List("Bobbie", "Gunny"), Origin.MARS, None)
  )

  implicit val OriginEnum: EnumType[Origin]                  = deriveEnumType[Origin](IncludeValues("EARTH", "MARS", "BELT"))
  implicit val CaptainType: ObjectType[Unit, Role.Captain]   = deriveObjectType[Unit, Role.Captain]()
  implicit val PilotType: ObjectType[Unit, Role.Pilot]       = deriveObjectType[Unit, Role.Pilot]()
  implicit val EngineerType: ObjectType[Unit, Role.Engineer] = deriveObjectType[Unit, Role.Engineer]()
  implicit val MechanicType: ObjectType[Unit, Role.Mechanic] = deriveObjectType[Unit, Role.Mechanic]()
  implicit val RoleType: UnionType[Unit]                     = UnionType(
    "Role",
    types = List(PilotType, EngineerType, MechanicType, CaptainType)
  )
  implicit val CharacterType: ObjectType[Unit, Character]    = ObjectType(
    "Character",
    fields[Unit, Character](
      Field(
        "name",
        StringType,
        resolve = _.value.name
      ),
      Field(
        "nicknames",
        ListType(StringType),
        resolve = _.value.nicknames
      ),
      Field(
        "origin",
        OriginEnum,
        resolve = _.value.origin
      ),
      Field(
        "role",
        OptionType(RoleType),
        resolve = _.value.role
      )
    )
  )

  val OriginArg: Argument[Option[Origin]] = Argument("origin", OptionInputType(OriginEnum))
  val NameArg: Argument[String]           = Argument("name", StringType)

  val QueryType: ObjectType[Unit, Unit] = ObjectType(
    "Query",
    fields[Unit, Unit](
      Field(
        "characters",
        ListType(CharacterType),
        arguments = OriginArg :: Nil,
        resolve = args => Future.successful(characters.filter(c => (args arg OriginArg).forall(c.origin == _)))
      ),
      Field(
        "character",
        OptionType(CharacterType),
        arguments = NameArg :: Nil,
        resolve = args => Future.successful(characters.find(c => c.name == (args arg NameArg)))
      )
    )
  )

  val schema: Schema[Unit, Unit] = Schema(QueryType)

}
