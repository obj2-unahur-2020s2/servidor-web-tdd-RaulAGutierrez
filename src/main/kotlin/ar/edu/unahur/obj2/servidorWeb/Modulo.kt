package ar.edu.unahur.obj2.servidorWeb

class Modulo(val extensionesSoportadas: List<String>, val body: String, val tiempoRespuesta: Int) {
  var consultasIpSospechosas: Int = 0
  fun puedeTrabajarCon(url: String) = extensionesSoportadas.any { ext -> url.endsWith(ext) }
  fun sumarUnaConsultaSospechosa() {
    consultasIpSospechosas += 1
  }
  fun cuantasVecesConsultaronLasIpSospechosas() = consultasIpSospechosas
}
