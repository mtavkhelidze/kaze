package query

val sumAgeQuery =
  """
    |{
    |    "type": "aggregate",
    |    "keys": [
    |        {
    |            "type": "col",
    |            "name": "team_name"
    |        }
    |    ],
    |    "aggs": [
    |        {
    |            "type": "sum",
    |            "col": "age"
    |        }
    |    ],
    |    "source": {
    |        "type": "scan",
    |        "table": "premier_league_squads"
    |    }
    |}
    |
    |""".stripMargin
