package ar.edu.unahur.obj2.servidorWeb

import kotlin.reflect.jvm.internal.impl.renderer.KeywordStringsGenerated

abstract class Analizador {
    val respuestas = mutableListOf<Respuesta>()

    open fun registrar(respuesta: Respuesta) {
        respuestas.add(respuesta)
    }

    open fun hayDemora(): Boolean { return false }

    open fun cantidadDeDemoras(modulo: Modulo): Int { return 0 }

    open fun pedidosSospechosos(ipSospechosa: String): Int {return 0}

    open fun cuantasVecesConsultaronLasIpSospechosas(servidor: ServidorWeb): Modulo? { return null }

    open fun ipSospechosasEnLaRuta(url: String): Set<String?>  { return setOf("") }
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
        if (this.ipSopechosas.contains(respuesta.pedido?.ip)) {
            respuesta.pedido?.modulo?.sumarUnaConsultaSospechosa()
            respuestas.add(respuesta)
        }
    }

    override fun pedidosSospechosos(ipSospechosa: String) = respuestas.sumBy { this.contarSiVieneDeUnaIPSospechosa(it,ipSospechosa) }

    fun contarSiVieneDeUnaIPSospechosa(respuesta: Respuesta,ipSospechosa:String) = if (respuesta.pedido?.ip == ipSospechosa) 1 else 0

    override fun cuantasVecesConsultaronLasIpSospechosas(servidor: ServidorWeb): Modulo? {
        return servidor.modulos.maxBy{ it.cuantasVecesConsultaronLasIpSospechosas() }
    }

    override fun ipSospechosasEnLaRuta(url: String) =
            respuestas.map(){ this.ipSospechosaEnUrl(it,url) }.filter(){ it != "Nada" }.toSet()

    fun ipSospechosaEnUrl(respuesta:Respuesta,url: String) =
        if (respuesta.pedido?.url.toString().startsWith(url)) respuesta.pedido?.ip.toString() else "Nada"
}
