package controllers;

import play.mvc.*;
import views.html.*;

public class Application extends Controller {

    public Result index() {
        return ok(index.render());
    }

    public Result hello(String name) {
        return ok(hello.render(name));
    }

    public Result return400() {
        return badRequest(views.html.return400.render());
    }
}
