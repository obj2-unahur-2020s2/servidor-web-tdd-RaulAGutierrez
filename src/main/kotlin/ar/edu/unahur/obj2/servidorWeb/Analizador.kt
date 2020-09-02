package ar.edu.unahur.obj2.servidorWeb

import  java.time.LocalDateTime
import kotlin.reflect.jvm.internal.impl.renderer.KeywordStringsGenerated

abstract class Analizador {
    val respuestas = mutableListOf<Respuesta>()

    open fun registrar(respuesta: Respuesta) {
        if (respuesta.codigo == CodigoHttp.OK) {
            respuestas.add(respuesta)
        }
    }

    open fun hayDemora(): Boolean { return false }

    open fun cantidadDeDemoras(modulo: Modulo): Int { return 0 }

    open fun pedidosSospechosos(ipSospechosa: String): Int {return 0}

    open fun cuantasVecesConsultaronLasIpSospechosas(servidor: ServidorWeb): Modulo? { return null }

    open fun ipSospechosasEnLaRuta(url: String): Set<String?>  { return setOf("") }

    open fun tiemposDeRespuesta(servidor: ServidorWeb): Int { return 0 }

    open fun cantPedidosEntre(fechaInicio: LocalDateTime, fechaFin: LocalDateTime): Int { return 0 }

    open  fun cantRespuestasConElBody(body: String): Int { return 0 }

    fun cantPedidosExistosos() = respuestas.size // devuelve un Int

}

class AnalizadorDeDemora(val demoraMinima: Int) : Analizador() {

    override fun hayDemora() = this.respuestas.any{ it.tiempo > demoraMinima }

    override fun cantidadDeDemoras(modulo: Modulo) =
        this.respuestas.filter{ this.respuestaQUeCorresponden(it,modulo) }.sumBy{ this.unaRespuestaConDemora(it) }

    fun respuestaQUeCorresponden(respuesta: Respuesta,modulo: Modulo) = respuesta.body == modulo.body // devuelve un booelano

    fun unaRespuestaConDemora(respuesta: Respuesta) = if (respuesta.tiempo > demoraMinima) 1 else 0 // devuelve un Int

}

class AnalizadorDeIP() : Analizador() {
    val ipSopechosas = mutableListOf<String>()

    fun agregar(ip: String) {
        ipSopechosas.add(ip)
    }

    override fun registrar(respuesta: Respuesta) {
        if (this.ipSopechosas.contains(respuesta.pedido?.ip) && respuesta.codigo == CodigoHttp.OK)  {
            respuesta.pedido?.modulo?.sumarUnaConsultaSospechosa()
            respuestas.add(respuesta)
        }
    }

    override fun pedidosSospechosos(ipSospechosa: String) = respuestas.sumBy { this.contarSiVieneDeUnaIPSospechosa(it,ipSospechosa) }

    fun contarSiVieneDeUnaIPSospechosa(respuesta: Respuesta,ipSospechosa:String) = if (respuesta.pedido?.ip == ipSospechosa) 1 else 0

    override fun cuantasVecesConsultaronLasIpSospechosas(servidor: ServidorWeb): Modulo? {  // devuelve un tipo Modulo?
        return servidor.modulos.maxBy{ it.cuantasVecesConsultaronLasIpSospechosas() }
    }

    override fun ipSospechosasEnLaRuta(url: String) =
            respuestas.map(){ this.ipSospechosaEnUrl(it,url) }.filter(){ it != "Nada" }.toSet()  // devuelve una conjunto de tipo String

    fun ipSospechosaEnUrl(respuesta:Respuesta,url: String) =
        if (respuesta.pedido?.url.toString().startsWith(url)) respuesta.pedido?.ip.toString() else "Nada"

}

class AnalizadorEstadistica(): Analizador() {

    override fun tiemposDeRespuesta(servidor: ServidorWeb) =
            servidor.tiemposRespuestaDeModulos() / servidor.modulos.size // devuelve un Int

    override fun cantPedidosEntre(fechaInicio: LocalDateTime, fechaFin: LocalDateTime) =
        respuestas.filter(){ it.pedido?.fechaHora!!.isAfter(fechaInicio) && it.pedido?.fechaHora.isBefore(fechaFin) }.size // devuelve un Int

    override fun cantRespuestasConElBody(body: String) =
            respuestas.filter() { it.body.contains(body) }.size // devuelve un Int

}
