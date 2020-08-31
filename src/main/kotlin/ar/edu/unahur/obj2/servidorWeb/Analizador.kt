package ar.edu.unahur.obj2.servidorWeb

abstract class Analizador {
    val respuestas = mutableListOf<Respuesta>()

    open fun registrar(respuesta: Respuesta) {
        respuestas.add(respuesta)
    }


    abstract fun hayDemora(): Boolean

    abstract fun cantidadDeDemoras(modulo: Modulo): Int

    open fun pedidosSospechosos(ipSospechosa: String): Int {return 0}
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
            respuestas.add(respuesta)
        }
    }

    override fun hayDemora(): Boolean { return false }

    override fun cantidadDeDemoras(modulo: Modulo): Int { return 0 }

    override fun pedidosSospechosos(ipSospechosa: String) = respuestas.sumBy { this.contarSiVieneDeUnaIPSospechosa(it,ipSospechosa) }

    fun contarSiVieneDeUnaIPSospechosa(respuesta: Respuesta,ipSospechosa:String) = if (respuesta.pedido?.ip == ipSospechosa) 1 else 0

    fun moduloMasConsultado(): Map<Modulo?, Int> {
        return respuestas.filter{ ipSopechosas.contains(it.pedido?.ip) }.groupingBy{ it.pedido?.modulo }.eachCount()
    }
}
