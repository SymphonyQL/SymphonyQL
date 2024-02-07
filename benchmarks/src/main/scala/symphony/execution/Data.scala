package symphony.execution

object Data {
  enum Origin extends Enum[Origin] {
    case MARS, EARTH, BELT
  }

  enum Role {
    case Captain(shipName: String)  extends Role
    case Pilot(shipName: String)    extends Role
    case Engineer(shipName: String) extends Role
    case Mechanic(shipName: String) extends Role
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

}
