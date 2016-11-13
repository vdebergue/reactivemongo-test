import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.{global => ec}

import play.api._
import play.api.mvc._
import play.api.ApplicationLoader.Context
import play.api.http.HttpErrorHandler
import router.Routes

import reactivemongo.api.{ DefaultDB, MongoConnection, MongoDriver }

class AppLoader extends ApplicationLoader {
  def load(context: Context) = {
    LoggerConfigurator(context.environment.classLoader).foreach(_.configure(context.environment))
    new MyComponents(context).application
  }
}

class MyComponents(context: Context)
  extends BuiltInComponentsFromContext(context){

  val driver = MongoDriver()
  val parsedUri = MongoConnection.parseURI(configuration.getString("mongodb.uri").get).get
  val connection = driver.connection(parsedUri)

  val db = Await.result(connection.database(parsedUri.db.get)(ec), 10.seconds)

  lazy val router = new Routes(
    httpErrorHandler,
    new controllers.AppController(db)
  )

}
