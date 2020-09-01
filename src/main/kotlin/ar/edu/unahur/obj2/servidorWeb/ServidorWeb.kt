package ar.edu.unahur.obj2.servidorWeb

import java.time.LocalDateTime

enum class CodigoHttp(val codigo: Int) {
  OK(200),
  NOT_IMPLEMENTED(501),
  NOT_FOUND(404),
}

class ServidorWeb {
  val modulos = mutableListOf<Modulo>()
  val analizadores = mutableListOf<Analizador>()

  fun realizarPedido(ip: String, url: String, fechaHora: LocalDateTime): Respuesta {
    if (!url.startsWith("http:")) {
      return Respuesta(codigo = CodigoHttp.NOT_IMPLEMENTED, body = "", tiempo = 10, pedido = null)
    }

    if (this.algunModuloSoporta(url)) {
      val moduloSeleccionado = this.modulos.find { it.puedeTrabajarCon(url) }!!
      val pedidoRespuesta = Pedido(ip,url,fechaHora,moduloSeleccionado)
      return Respuesta(CodigoHttp.OK, moduloSeleccionado.body, moduloSeleccionado.tiempoRespuesta,pedidoRespuesta)
    }

    return Respuesta(codigo = CodigoHttp.NOT_FOUND, body = "", tiempo = 10, pedido = null)
  }

  fun algunModuloSoporta(url: String) = this.modulos.any { it.puedeTrabajarCon(url) }

  fun agregarModulo(modulo: Modulo) {
    this.modulos.add(modulo)
  }

  fun agregarAnalizador(analizador: Analizador) {
    this.analizadores.add(analizador)
  }

  fun quitarAnalizador(analizador: Analizador) {
    this.analizadores.remove(analizador)
  }

  fun enviarALosAnalizadores(respuesta: Respuesta) {
    if (!this.analizadores.isEmpty()) {
      this.analizadores.forEach(){it.registrar(respuesta)}
    }
  }

  fun hayRespuestasDemoradas() = this.analizadores.any{ it.hayDemora() }

  fun cantidadDemorasEnElModulo(modulo: Modulo) = this.analizadores.sumBy{ it.cantidadDeDemoras(modulo) }

  fun cantidadPedidosSospechosos(ipSospechosa: String) = this.analizadores.sumBy{ it.pedidosSospechosos(ipSospechosa) }

  fun moduloMasConsultadoPorSospechos(): Modulo? {
    return analizadores.map(){ it.cuantasVecesConsultaronLasIpSospechosas(this) }.toSet().first()
  }

  fun conjuntoIpSospechosasEnLaRuta(url:String) = analizadores.map(){ it.ipSospechosasEnLaRuta(url).filter{it != ""} }.flatMap{it}.toSet()


}

class Respuesta(val codigo: CodigoHttp, val body: String, val tiempo: Int,val pedido: Pedido?)
class Pedido(val ip: String,val url: String,val fechaHora: LocalDateTime,val modulo: Modulo)