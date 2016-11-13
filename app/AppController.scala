package controllers

import scala.concurrent.ExecutionContext.Implicits.global

import akka.stream._
import play.api.mvc._
import play.api.http.{HttpEntity, Writeable}
import reactivemongo.api._
import reactivemongo.akkastream.cursorProducer
import reactivemongo.bson._

class AppController(db: DefaultDB)(implicit mat: Materializer) extends Controller {

  val coll = db.collection("capped")

  object IdReader extends BSONDocumentReader[Int] {
    def read(doc: BSONDocument): Int = doc.getAs[Int]("id").get
  }

  def createCapped = Action.async {
    for {
      _ <- coll.createCapped(size = (4096 * 100), maxDocuments = None)
      _ <- coll.insert(BSONDocument("id" -> 1))
      _ <- coll.insert(BSONDocument("id" -> 2))
      _ <- coll.insert(BSONDocument("id" -> 3))
    } yield Ok("created collection capped")
  }

  def cursor() = {
    implicit val reader = IdReader
    coll.find(BSONDocument())
      .options(QueryOpts().tailable)
      .cursor[Int]()
    }

  def tailBulk = Action {
    val source = cursor()
      .bulkSource()
      .map { ids =>
        println(s"Got ${ids.size} ids")
        ids.mkString("\n")
      }
    Ok.chunked(source)
  }

  def tailBulkStream = Action {
    implicit val reader = IdReader
    val source = cursor()
      .bulkSource()
      .map { ids =>
        println(s"Got ${ids.size} ids")
        ids.mkString("\n")
      }
    val wr = Writeable.wString
    Ok.sendEntity(HttpEntity.Streamed(source.map(wr.transform(_)), None, wr.contentType))
  }

  def tailDoc = Action {
    val source = cursor()
      .documentSource()
      .map { id =>
        println(s"Got 1 id")
        id.toString + "\n"
      }
    Ok.chunked(source)
  }

  def home = Action {
    Ok("ok")
  }
}
