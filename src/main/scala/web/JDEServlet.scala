package web

import jde.compiler.Compiler.compileToJson

import javax.servlet.http.{HttpServlet, HttpServletRequest => HReq, HttpServletResponse => HResp}

class JDEServlet extends HttpServlet {
  override def doGet(hReq: HReq, hResp: HResp) = doPost(hReq, hResp)
  override def doPost(hReq: HReq, hResp: HResp) = {
    try {
      val resp = compileToJson(scala.io.Source.fromInputStream(hReq.getInputStream))
      hResp.getWriter.print(resp)
    } catch {
      case t: Throwable => hResp.sendError(400, t.getMessage)
    }

  }
}
